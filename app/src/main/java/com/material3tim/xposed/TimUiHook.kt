package com.material3tim.xposed

import android.app.Activity
import android.util.Log

/**
 * TIM 侧 hook：在主框架 Activity 的生命周期节点上挂载自写 Compose 界面。
 *
 * - `Activity.onResume`：首帧进入主界面时挂载（TIM 在 `onCreate` 里完成 setContentView，
 *   `onResume` 时 decorView 已就绪）
 * - `Activity.onWindowFocusChanged(true)`：窗口获得焦点时补挂一次，防止 TIM 之后
 *   重建/替换界面导致覆盖层丢失
 *
 * 只处理 `SplashActivity`（TIM 的主框架页）。其它 Activity（聊天详情等）暂时保留
 * TIM 原样，后续步骤再逐步接管。
 */
internal object TimUiHook {

    private const val TAG = "Material3Tim"

    /** TIM 4.1.0 的主框架 Activity（Manifest 里的 launcher activity）。 */
    const val MAIN_ACTIVITY = "com.tencent.mobileqq.activity.SplashActivity"

    fun install(module: MainModule, classLoader: ClassLoader) {
        hookActivityResume(module)
        hookWindowFocus(module)
        hookLifecycle(module, "onPause") { activity -> UiHost.onActivityPause(activity) }
        hookLifecycle(module, "onStop") { activity -> UiHost.onActivityStop(activity) }
        hookLifecycle(module, "onDestroy") { activity -> UiHost.onActivityDestroyed(activity) }
        hookNewIntent(module)
        hookBackPressed(module, classLoader)
        hookKeyDown(module, classLoader)
        hookActivityResult(module, classLoader)
        hookExternalJump(module, classLoader)
        hookAnnouncementResponse(module, classLoader)
        NativeSettingsEntry.install(module, classLoader)
        Log.i(TAG, "TIM lifecycle hooks installed")
    }

    /** 拦截 TIM 群公告 SSO 响应（0x8f9_14 与 0x852_48），解析全部公告。 */
    private fun hookAnnouncementResponse(module: MainModule, classLoader: ClassLoader) {
        val clazz = runCatching {
            Class.forName(
                "com.tencent.mobileqq.troop.announcement.api.impl.TroopAnnouncementHandler",
                false,
                classLoader,
            )
        }.getOrNull()
        if (clazz == null) {
            Log.w(TAG, "announcement handler class not found")
            return
        }
        val k1 = clazz.declaredMethods.firstOrNull { it.name == "K1" && it.parameterCount == 3 }
        val j1 = clazz.declaredMethods.firstOrNull { it.name == "J1" && it.parameterCount == 3 }
        Log.i(TAG, "announcement hooks: K1=${k1 != null} J1=${j1 != null}")
        if (k1 != null) {
            runCatching {
                module.hook(k1).intercept { chain ->
                    TimBridge.rememberAnnouncementTag(chain.args.getOrNull(0))
                    val result = chain.proceed()
                    Log.i(TAG, "K1 hit")
                    (chain.args.getOrNull(2) as? ByteArray)?.let { TimBridge.captureFeedsResponse(it) }
                    result
                }
            }.onFailure { Log.w(TAG, "hook K1 failed: ${it.message}") }
        }
        if (j1 != null) {
            runCatching {
                module.hook(j1).intercept { chain ->
                    TimBridge.rememberAnnouncementTag(chain.args.getOrNull(0))
                    Log.i(TAG, "J1 args=${chain.args.map { it?.javaClass?.simpleName }}")
                    val result = chain.proceed()
                    Log.i(TAG, "J1 hit")
                    (chain.args.getOrNull(2) as? ByteArray)?.let { TimBridge.capture48Response(it) }
                    result
                }
            }.onFailure { Log.w(TAG, "hook J1 failed: ${it.message}") }
        }
    }

    private fun hookLifecycle(module: MainModule, name: String, callback: (Activity) -> Unit) {
        val method = Activity::class.java.getDeclaredMethod(name)
        module.hook(method).intercept { chain ->
            val result = chain.proceed()
            (chain.thisObject as? Activity)?.let(callback)
            result
        }
    }

    /**
     * 主界面里按返回键时先交给 Compose 的返回处理（NavigationBackHandler），处理不了才走 TIM 原逻辑。
     *
     * TIM 的 `AppActivity.onBackPressed()` 重写后**不会调用 super**，所以 hook `Activity.onBackPressed`
     * 抓不到；必须直接 hook `mqq.app.AppActivity.onBackPressed`。
     */
    private fun hookBackPressed(module: MainModule, classLoader: ClassLoader) {
        val hookTarget = runCatching {
            classLoader.loadClass("mqq.app.AppActivity").getDeclaredMethod("onBackPressed")
        }.getOrNull()
        if (hookTarget == null) {
            Log.w(TAG, "AppActivity.onBackPressed not found; back key passthrough")
            return
        }
        module.hook(hookTarget).intercept { chain ->
            val activity = chain.thisObject as? Activity
            if (activity != null && activity.javaClass.name == MAIN_ACTIVITY && UiHost.consumeBack(activity)) {
                null
            } else {
                chain.proceed()
            }
        }
    }

    /**
     * 通知点击：TIM 主 Activity 是 singleTask，onNewIntent 才是热启动入口。
     * 取出通知带的会话 id 交给 [com.material3tim.data.source.NavigationRequests]。
     */
    private fun hookNewIntent(module: MainModule) {
        val method = runCatching {
            Activity::class.java.getDeclaredMethod("onNewIntent", android.content.Intent::class.java)
        }.getOrNull() ?: return
        module.hook(method).intercept { chain ->
            val result = chain.proceed()
            val activity = chain.thisObject as? Activity
            val intent = chain.args.firstOrNull() as? android.content.Intent
            if (activity?.javaClass?.name == MAIN_ACTIVITY) {
                runCatching {
                    com.material3tim.data.source.NavigationRequests.requestFromIntent(intent)
                }
            }
            result
        }
    }

    private fun hookActivityResume(module: MainModule) {
        val method = Activity::class.java.getDeclaredMethod("onResume")
        module.hook(method).intercept { chain ->
            val result = chain.proceed()
            (chain.thisObject as? Activity)?.let { activity -> onActivityEvent(module, activity) }
            result
        }
    }

    private fun hookWindowFocus(module: MainModule) {
        val method = Activity::class.java.getDeclaredMethod(
            "onWindowFocusChanged",
            Boolean::class.javaPrimitiveType,
        )
        module.hook(method).intercept { chain ->
            val result = chain.proceed()
            val hasFocus = chain.args.firstOrNull() as? Boolean == true
            if (hasFocus) {
                (chain.thisObject as? Activity)?.let { activity -> onActivityEvent(module, activity) }
            }
            result
        }
    }

    /**
     * TIM 的 QBaseActivity 把 startActivityForResult 交给
     * `JefsClass.checkAndDoSync(activity, intent, TeleScreenRunnable)` 做「离开 TIM」安全确认，
     * 会弹确认框且让结果拿不到。我们自己发起的照片/拍照/文件选择：
     * 直接把 TeleScreenRunnable 跑掉（它内部调用 startActivityForResultOriginal，
     * requestCode 保留），跳过确认框。
     */
    private fun hookExternalJump(module: MainModule, classLoader: ClassLoader) {
        val method = runCatching {
            Class.forName("com.tencent.mobileqq.haoliyou.JefsClass", false, classLoader)
                .methods
                .firstOrNull { it.name == "checkAndDoSync" && it.parameterCount == 3 }
        }.getOrNull()
        if (method == null) {
            Log.w(TAG, "JefsClass.checkAndDoSync not found, external jump confirm stays")
            return
        }
        runCatching {
            module.hook(method).intercept { chain ->
                if (UiHost.consumeExternalLaunchExpectation()) {
                    val todo = chain.args[2] as? Runnable
                    if (todo != null) {
                        runCatching { todo.run() }
                            .onFailure { Log.w(TAG, "direct launch failed: ${it.message}") }
                        return@intercept null
                    }
                }
                chain.proceed()
            }
        }.onFailure { Log.w(TAG, "hook JefsClass failed: ${it.message}") }
    }

    /**
     * 照片选择 / 拍照 / 文件选择需要 Activity 结果：TIM 的返回结果最终会走到
     * `mqq.app.AppActivity.doOnActivityResult`（SplashActivity 重写后也调 super），
     * 这里把结果转发给 [UiHost.dispatchActivityResult]。
     */
    private fun hookActivityResult(module: MainModule, classLoader: ClassLoader) {
        val intentClass = android.content.Intent::class.java
        val appActivityResult = runCatching {
            Class.forName("mqq.app.AppActivity", false, classLoader)
                .getDeclaredMethod(
                    "doOnActivityResult",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    intentClass,
                )
        }.getOrNull()
        appActivityResult?.let { method ->
            runCatching {
                module.hook(method).intercept { chain ->
                    val activity = chain.thisObject as? Activity
                    val args = chain.args
                    if (activity != null && args.size >= 3) {
                        UiHost.dispatchActivityResult(
                            activity,
                            (args[0] as? Number)?.toInt() ?: 0,
                            (args[1] as? Number)?.toInt() ?: 0,
                            args[2] as? android.content.Intent,
                        )
                    }
                    chain.proceed()
                }
            }
        }
        // 兜底：Activity.onActivityResult（部分路径可能不经过 doOnActivityResult）
        runCatching {
            val baseMethod = Activity::class.java.getDeclaredMethod(
                "onActivityResult",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                intentClass,
            )
            module.hook(baseMethod).intercept { chain ->
                val activity = chain.thisObject as? Activity
                val args = chain.args
                if (activity != null && args.size >= 3) {
                    UiHost.dispatchActivityResult(
                        activity,
                        (args[0] as? Number)?.toInt() ?: 0,
                        (args[1] as? Number)?.toInt() ?: 0,
                        args[2] as? android.content.Intent,
                    )
                }
                chain.proceed()
            }
        }
    }

    /**
     * TIM 的返回键实际走 `SplashActivity.doOnKeyDown(KEYCODE_BACK)` → `MainFragment.doOnKeyDown`，
     * 根本不会到 `onBackPressed`。这里在 SplashActivity 这一层截住，先交给我们的 Compose 返回处理。
     */
    private fun hookKeyDown(module: MainModule, classLoader: ClassLoader) {
        val method = runCatching {
            classLoader.loadClass(MAIN_ACTIVITY).getDeclaredMethod(
                "doOnKeyDown",
                Int::class.javaPrimitiveType,
                android.view.KeyEvent::class.java,
            )
        }.getOrNull()
        if (method == null) {
            Log.w(TAG, "SplashActivity.doOnKeyDown not found")
            return
        }
        module.hook(method).intercept { chain ->
            val keyCode = chain.getArg(0) as? Int ?: -1
            val activity = chain.thisObject as? Activity
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && activity != null &&
                UiHost.consumeBack(activity)
            ) {
                true
            } else {
                chain.proceed()
            }
        }
    }

    private fun onActivityEvent(module: MainModule, activity: Activity) {
        if (activity.javaClass.name != MAIN_ACTIVITY) return
        if (activity.isFinishing) return
        // 用户点了「回到原版 TIM」：不再挂 Compose 覆盖层，保留 TIM 原生界面。
        if (!UiHost.isMaterialUiEnabled(activity)) return
        // 冷启动：从通知点进来时 launcher intent 带会话 id（热启动走 hookNewIntent）。
        runCatching {
            com.material3tim.data.source.NavigationRequests.requestFromIntent(activity.intent)
        }
        UiHost.ensureHosted(module, activity)
    }
}
