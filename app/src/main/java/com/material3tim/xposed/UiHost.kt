package com.material3tim.xposed

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.material3tim.AppEnvironment
import com.material3tim.ui.M3TimRoot
import com.material3tim.ui.util.LocalHostWindowSize
import com.material3tim.ui.viewmodel.MainActivityViewModel
import java.util.Collections
import java.util.WeakHashMap

/**
 * 把自写的 Compose UI 挂到 TIM 的 Activity 上（第二步：接管整个界面）。
 *
 * 实现方式：在 `decorView` 最上层放一个全屏 Compose 覆盖层（compose 接管视觉与交互），
 * 而不是把 TIM 的 content 清空 —— TIM 的主框架里大量 `findViewById(...).addView(...)`
 * 之类的强假设，清空 content 很容易让宿主 NPE 崩溃；覆盖层方案对宿主零侵入。
 * TIM 自己的 view 树留在下面继续运转（登录态、消息服务都正常）。
 *
 * 幂等：同一个 Activity 只挂一次；`onWindowFocusChanged` 时会检查挂载是否丢失并补挂。
 */
internal object UiHost {

    private const val TAG = "Material3Tim"
    private const val HOST_PREFS = "material3tim_host"
    private const val KEY_MATERIAL_UI_ENABLED = "material3tim_enabled"

    private val hosted: MutableMap<Activity, HostComposeOverlay> =
        Collections.synchronizedMap(WeakHashMap<Activity, HostComposeOverlay>())

    private class HostComposeOverlay(
        private val activity: Activity,
        moduleContext: Context,
    ) : FrameLayout(moduleContext) {
        val windowSize = mutableStateOf(IntSize.Zero)

        fun refreshConfiguration() {
            val host = activity.resources
            if (resources.configuration != host.configuration ||
                resources.displayMetrics.densityDpi != host.displayMetrics.densityDpi
            ) {
                dispatchConfigurationChanged(host.configuration)
            }
        }

        override fun dispatchConfigurationChanged(newConfig: Configuration) {
            // 独立 AssetManager 的 Resources 不随 Activity 更新。先同步，再让 Compose 子 View
            // 读取新配置；保留模块资源隔离，不重建 Composition / 返回栈。
            @Suppress("DEPRECATION")
            resources.updateConfiguration(newConfig, activity.resources.displayMetrics)
            super.dispatchConfigurationChanged(newConfig)
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            windowSize.value = IntSize(w, h)
        }
    }

    /** 已注册的预测性返回回调（每 Activity 一个）。 */
    private val backCallbacks: MutableMap<Activity, Any> =
        Collections.synchronizedMap(WeakHashMap<Activity, Any>())

    /** 是否已给该 Activity 的 decor 挂过「重新应用沉浸」监听。 */
    private val immersiveHooked: MutableMap<Activity, Boolean> =
        Collections.synchronizedMap(WeakHashMap<Activity, Boolean>())

    /** 上次重新应用窗口标志的时间（防止 layout/insets 回调里频繁重设）。 */
    private val immersiveReapplyAt: MutableMap<Activity, Long> =
        Collections.synchronizedMap(WeakHashMap<Activity, Long>())

    /** 是否启用 Material3TIM 界面（关闭后显示 TIM 原生界面）。 */
    fun isMaterialUiEnabled(context: Context): Boolean =
        context.getSharedPreferences(HOST_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_MATERIAL_UI_ENABLED, true)

    /** 设置是否启用 Material3TIM 界面；新值在 TIM 进程重启后生效。 */
    fun setMaterialUiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(HOST_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MATERIAL_UI_ENABLED, enabled)
            .apply()
        Log.i(TAG, "material3tim_enabled=$enabled")
    }

    /**
     * 重启 TIM 进程，让“启用/停用 Material3TIM 界面”生效。
     * 仅在宿主 TIM 进程里执行；独立 App 只提示，不自杀。
     */
    fun restartTim(context: Context) {
        if (context.packageName != "com.tencent.tim") return
        val activity = findActivity(context)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching { activity?.finishAffinity() }
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 350L)
    }

    private fun findActivity(context: Context?): Activity? {
        var current = context
        var guard = 0
        while (current != null && guard++ < 8) {
            if (current is Activity) return current
            current = (current as? android.content.ContextWrapper)?.baseContext
        }
        return null
    }

    /** 供 hook 调用：保证目标 Activity 上已经挂上我们的 UI。 */
    fun ensureHosted(module: MainModule, activity: Activity) {
        if (activity.isFinishing) return
        if (!isMaterialUiEnabled(activity)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) return
        try {
            val existing = hosted[activity]
            if (existing != null) {
                // 关键：从后台回到前台时把 Compose 的 lifecycle 恢复成 RESUMED。
                // 否则 Compose 的 WindowRecomposer 帧时钟一直暂停：点击能被收到但界面
                // 不重组、列表 fling 没有惯性、预测性返回动画卡在半路。
                existing.refreshConfiguration()
                HostOwners.get(activity)?.onResume()
                if (existing.parent == null) attachToDecor(activity, existing)
                return
            }
            install(module, activity)
        } catch (t: Throwable) {
            Log.e(TAG, "host compose ui failed on ${activity.javaClass.name}", t)
        }
    }

    private fun install(module: MainModule, activity: Activity) {
        val moduleResources = ModuleResources.createModuleResources(module, activity)
        if (moduleResources == null) {
            // 注意：**不要**退化成把模块 APK 注入宿主 Activity 的 Resources。
            // QAuxiliary 也用 `Resources.addLoaders` 注入自己的 APK，两个 0x7f
            // 资源表挂在同一个 Resources 上会互相遮蔽（QAuxv 文案串号/自检失败）。
            // 覆盖安装后当前进程注入的是已删除的旧 APK 时，重启 TIM 一次即可
            // 拿到新路径；仍然失败就保留 TIM 原生界面，等下次冷启动恢复。
            if (ModuleResources.tryRecoverByRestart(activity)) return
            Log.e(
                TAG,
                "module resources unavailable, skip hosting ui (no host resource injection)",
            )
            return
        }
        val resources = moduleResources
        val remotePrefs = runCatching { module.getRemotePreferences("settings") }.getOrNull()
        val settingsPrefs = HostSettingsPrefs(
            remote = remotePrefs,
            local = activity.getSharedPreferences(HOST_PREFS, Context.MODE_PRIVATE),
        )
        val moduleContext: Context = ModuleContext(activity, resources, settingsPrefs)

        // 模块 UI 里所有 ThemeController / SettingsRepository 都从这里取 Context。
        AppEnvironment.init(moduleContext)
        // 第 3 步：把真实 TIM 数据接入 UI（会话列表 → 之后的消息/联系人/发送）。
        TimBridge.install()
        com.material3tim.data.source.AppDataSources.personalLibrary = TimPersonalLibrary(activity)
        com.material3tim.data.source.AppDataSources.chatSource = TimChatSource()
        com.material3tim.data.source.AppDataSources.messageSource = TimMessageSource()
        com.material3tim.data.source.AppDataSources.contactSource = TimContactSource()
        com.material3tim.data.source.AppDataSources.emojiSource = TimEmojiSource()
        com.material3tim.data.source.AppDataSources.chatSettingSource = TimChatSettingSource()
        com.material3tim.data.source.AppDataSources.addFriendSource = TimAddFriendSource()
        com.material3tim.data.source.AppDataSources.qrActionSource = TimQrActionSource()
        com.material3tim.data.source.AppDataSources.groupCreateSource = TimGroupCreateSource()
        com.material3tim.data.source.AppDataSources.voicePlaybackSource = TimVoicePlaybackSource()
        com.material3tim.data.source.AppDataSources.groupFileSource = TimGroupFileSource()
        com.material3tim.data.source.AppDataSources.groupManageSource = TimGroupManageSource()
        com.material3tim.data.source.AppDataSources.searchSource = TimSearchSource()
        com.material3tim.data.source.AppDataSources.callSource = TimCallSource()
        com.material3tim.data.source.AppDataSources.friendManageSource = TimFriendManageSource()
        com.material3tim.data.repository.LiveChats.update(null)
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            runCatching {
                TimBridge.loadSelfProfile()?.let { profile ->
                    com.material3tim.data.repository.LiveProfile.update(profile)
                    Log.i(TAG, "self profile loaded: ${profile.name} / ${profile.timId}")
                }
            }
        }
        configureWindow(activity)
        // TIM 的 Activity 不是 LifecycleOwner / ViewModelStoreOwner，Compose 需要它们：
        // 造一套 owner 挂到 decorView 的 ViewTree 上。
        val owners = HostOwners.obtain(activity)
        owners.init()
        ensureViewTreeOwners(activity, owners)
        registerPredictiveBack(activity, owners)

        val overlay = HostComposeOverlay(activity, moduleContext)
        val composeView = ComposeView(moduleContext).apply {
            setContent {
                CompositionLocalProvider(LocalHostWindowSize provides overlay.windowSize.value) {
                    HostRoot(activity)
                }
            }
        }
        val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
        overlay.addView(composeView, FrameLayout.LayoutParams(matchParent, matchParent))
        attachToDecor(activity, overlay)
        hosted[activity] = overlay
        Log.i(TAG, "hosted compose ui on ${activity.javaClass.name}")
    }

    @Composable
    private fun HostRoot(activity: Activity) {
        // 宿主的 ViewModelStore 属于 TIM，用独立实例避免和 TIM 的 ViewModel 混淆。
        val viewModel = remember { MainActivityViewModel(SavedStateHandle()) }
        M3TimRoot(
            viewModel = viewModel,
            onDarkModeChanged = { dark -> applySystemBarAppearance(activity, dark) },
            // 真实莫奈取色：用宿主 Activity 的 Resources（含系统强调色），
            // 而不是模块 Context（只有模块 APK 的资源）。
            monetContext = activity,
            // 宿主 Activity 提供 ActivityResultRegistry，照片/拍照/文件选择器的结果才能回来。
            activityResultRegistryOwner = HostOwners.get(activity),
        )
    }

    /**
     * 注册系统预测性返回回调（Android 13+）：把系统手势的 started/progressed/invoked
     * 进度转给我们的 NavigationEventDispatcher，让 Compose 的返回处理（以及依赖
     * transitionState 的动画）能跟着手势走。
     *
     * 需要 TIM 的 ApplicationInfo 打开 `enableOnBackInvokedCallback`（[MainModule] 里已设置）。
     */
    private fun registerPredictiveBack(activity: Activity, owners: HostOwners) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (backCallbacks.containsKey(activity)) return
        try {
            val callback = object : android.window.OnBackAnimationCallback {
                override fun onBackStarted(backEvent: android.window.BackEvent) {
                    owners.platformBackStarted(
                        backEvent.progress,
                        backEvent.swipeEdge,
                        backEvent.touchX,
                        backEvent.touchY,
                    )
                }

                override fun onBackProgressed(backEvent: android.window.BackEvent) {
                    owners.platformBackProgressed(
                        backEvent.progress,
                        backEvent.touchX,
                        backEvent.touchY,
                    )
                }

                override fun onBackInvoked() {
                    owners.platformBackInvoked()
                }

                override fun onBackCancelled() {
                    owners.platformBackCancelled()
                }
            }
            activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback,
            )
            backCallbacks[activity] = callback
            Log.i(TAG, "predictive back registered on ${activity.javaClass.simpleName}")
        } catch (t: Throwable) {
            Log.w(TAG, "register predictive back failed: ${t.javaClass.simpleName} ${t.message}")
        }
    }

    /** 给 decorView 挂上我们造的 ViewTree owner（Compose / 返回键 / 生命周期）。 */
    private fun ensureViewTreeOwners(activity: Activity, owners: HostOwners) {
        val decor = activity.window?.decorView ?: return
        try {
            decor.setViewTreeLifecycleOwner(owners)
            decor.setViewTreeViewModelStoreOwner(owners)
            decor.setViewTreeSavedStateRegistryOwner(owners)
            decor.setViewTreeOnBackPressedDispatcherOwner(owners)
            decor.setViewTreeNavigationEventDispatcherOwner(owners)
            Log.i(TAG, "view tree owners attached to decor=$decor")
        } catch (t: Throwable) {
            Log.w(TAG, "set view tree owners failed: ${t.javaClass.simpleName} ${t.message}")
        }
    }

    /** 标记「接下来几秒内 TIM 的外跳安全确认要直接放行」（照片/拍照/文件选择器用）。 */
    @Volatile
    private var externalLaunchExpectedUntil = 0L

    fun expectExternalLaunch() {
        externalLaunchExpectedUntil = android.os.SystemClock.uptimeMillis() + 8_000L
    }

    fun consumeExternalLaunchExpectation(): Boolean {
        val until = externalLaunchExpectedUntil
        if (until <= 0L || android.os.SystemClock.uptimeMillis() > until) return false
        externalLaunchExpectedUntil = 0L
        return true
    }

    /** 转发宿主 Activity 的 onActivityResult（由 hook 调用），用于照片/拍照/文件选择器。 */
    fun dispatchActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?,
    ) {
        // 系统录音机等桥接自有 requestCode 优先消费，避免继续派发给 Compose ActivityResultRegistry。
        if (TimBridge.handleVoiceRecordActivityResult(requestCode, resultCode, data)) return
        HostOwners.get(activity)?.dispatchActivityResult(requestCode, resultCode, data)
    }

    /** UI 消费返回键（由 Activity.onBackPressed hook 调用）。 */
    fun consumeBack(activity: Activity): Boolean = HostOwners.get(activity)?.dispatchBack() == true

    /** 生命周期事件（由 hook 调用）。 */
    fun onActivityPause(activity: Activity) {
        HostOwners.get(activity)?.onPause()
    }

    fun onActivityStop(activity: Activity) {
        HostOwners.get(activity)?.onStop()
    }

    fun onActivityResumed(activity: Activity) {
        val view = hosted[activity] ?: return
        configureWindow(activity)
        view.refreshConfiguration()
        HostOwners.get(activity)?.onResume()
        if (view.parent == null) attachToDecor(activity, view)
    }

    /** 回前台时驱动一次 lifecycle（onResume/onWindowFocusChanged 都会调）。 */
    fun resumeLifecycle(activity: Activity) {
        configureWindow(activity)
        HostOwners.get(activity)?.onResume()
    }

    fun onActivityDestroyed(activity: Activity) {
        hosted.remove(activity)
        HostOwners.remove(activity)
    }

    private fun attachToDecor(activity: Activity, view: HostComposeOverlay) {
        val decor = activity.window?.decorView as? ViewGroup ?: return
        if (view.parent === decor) return
        if (view.parent is ViewGroup) (view.parent as ViewGroup).removeView(view)
        decor.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        Log.i(
            TAG,
            "compose overlay attached to decorView (${activity.javaClass.simpleName})",
        )
    }

    /**
     * 让我们的覆盖层铺满整屏（含状态栏/导航栏区域），由 Compose 自己处理 insets。
     *
     * 注意：TIM 在大屏/平板模式下会自己把 decorFitsSystemWindows 改回 true（或在窗口尺寸变化时
     * 重新套用自己的窗口配置），导致我们的 Compose 覆盖层被底部导航栏 inset 掉、导航栏区域露出
     * TIM 窗口背景（灰色）。这里除了设置一次窗口标志，还给 decor 挂上 layout / insets 监听，
     * 在 TIM 改动之后重新应用，保证平板横竖屏都始终沉浸。
     */
    private fun configureWindow(activity: Activity) {
        applyImmersiveWindowFlags(activity)
        synchronized(immersiveHooked) {
            if (immersiveHooked[activity] == true) return
            immersiveHooked[activity] = true
        }
        val decor = activity.window?.decorView ?: return
        try {
            decor.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                reapplyImmersiveWindowFlags(activity)
            }
            decor.setOnApplyWindowInsetsListener { view, insets ->
                reapplyImmersiveWindowFlags(activity)
                view.onApplyWindowInsets(insets)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "attach immersive listeners failed: ${t.javaClass.simpleName} ${t.message}")
        }
    }

    /** 节流后重新应用沉浸窗口标志（TIM 可能在其后重置窗口配置）。 */
    private fun reapplyImmersiveWindowFlags(activity: Activity) {
        val now = android.os.SystemClock.uptimeMillis()
        synchronized(immersiveReapplyAt) {
            val last = immersiveReapplyAt[activity] ?: 0L
            if (now - last < 250L) return
            immersiveReapplyAt[activity] = now
        }
        applyImmersiveWindowFlags(activity)
    }

    private fun applyImmersiveWindowFlags(activity: Activity) {
        val window = activity.window ?: return
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            // TIM 默认随输入法 adjustResize 缩小窗口；我们的全屏 Compose 覆盖层同时用
            // WindowInsets.ime 抬升输入框，两者叠加会让输入框先被顶高一个键盘高度再回落
            // （点击输入框瞬间出现与键盘等高的空白区域）。改为 ADJUST_NOTHING，只由
            // Compose 的 ime insets 做动画，输入框就能与输入法同步升起。
            run {
                val attrs = window.attributes
                if (attrs.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST !=
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                ) {
                    attrs.softInputMode =
                        (attrs.softInputMode and
                            WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()) or
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                    window.attributes = attrs
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "configure window failed: ${t.javaClass.simpleName} ${t.message}")
        }
    }

    /** 状态栏 / 导航栏图标明暗跟随 Compose 主题。 */
    fun applySystemBarAppearance(activity: Activity, dark: Boolean) {
        val window = activity.window ?: return
        try {
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        } catch (_: Throwable) {
        }
    }
}
