package com.material3tim.xposed

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
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
import com.material3tim.ui.viewmodel.MainActivityViewModel
import java.util.Collections
import java.util.WeakHashMap

/**
 * 把自写的 Compose UI 挂到 TIM 的 Activity 上（第二步：接管整个界面）。
 *
 * 实现方式：在 `decorView` 最上层放一个全屏 [ComposeView]（compose 接管视觉与交互），
 * 而不是把 TIM 的 content 清空 —— TIM 的主框架里大量 `findViewById(...).addView(...)`
 * 之类的强假设，清空 content 很容易让宿主 NPE 崩溃；覆盖层方案对宿主零侵入。
 * TIM 自己的 view 树留在下面继续运转（登录态、消息服务都正常）。
 *
 * 幂等：同一个 Activity 只挂一次；`onWindowFocusChanged` 时会检查挂载是否丢失并补挂。
 */
internal object UiHost {

    private const val TAG = "Material3Tim"
    private const val HOST_PREFS = "material3tim_host"

    private val hosted: MutableMap<Activity, ComposeView> =
        Collections.synchronizedMap(WeakHashMap<Activity, ComposeView>())

    /** 已注册的预测性返回回调（每 Activity 一个）。 */
    private val backCallbacks: MutableMap<Activity, Any> =
        Collections.synchronizedMap(WeakHashMap<Activity, Any>())

    /** 是否已给该 Activity 的 decor 挂过「重新应用沉浸」监听。 */
    private val immersiveHooked: MutableMap<Activity, Boolean> =
        Collections.synchronizedMap(WeakHashMap<Activity, Boolean>())

    /** 上次重新应用窗口标志的时间（防止 layout/insets 回调里频繁重设）。 */
    private val immersiveReapplyAt: MutableMap<Activity, Long> =
        Collections.synchronizedMap(WeakHashMap<Activity, Long>())

    /** 最近一次挂载的 Activity / 模块 / 模块 Resources（原生界面切换要用）。 */
    private var lastActivityRef: java.lang.ref.WeakReference<Activity>? = null
    private var lastModule: MainModule? = null
    private var lastModuleResources: android.content.res.Resources? = null

    /**
     * 是否处于「TIM 原生界面」模式：true 时我们的 Compose 覆盖层隐藏，
     * 不再自动补挂；decorView 上放一个返回按钮供用户切回。
     */
    @Volatile
    private var nativeUiMode = false

    /** TIM 原生界面模式下的返回按钮（挂在 TIM decorView 最上层）。 */
    private var nativeReturnButton: View? = null

    fun isNativeUiMode(): Boolean = nativeUiMode

    /** 切换到 TIM 原生界面：隐藏 Compose 覆盖层，保留 TIM 自己的界面，并加返回按钮。 */
    fun switchToNativeUi(): Boolean {
        val activity = lastActivityRef?.get() ?: hosted.keys.firstOrNull { !it.isFinishing }
            ?: return false
        nativeUiMode = true
        // 原生 TIM 界面按它自己的窗口配置布局：恢复 decorFitsSystemWindows，恢复系统栏颜色。
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            @Suppress("DEPRECATION")
            activity.window?.statusBarColor = Color.TRANSPARENT
        }
        hosted[activity]?.visibility = View.GONE
        showNativeReturnButton(activity)
        Log.i(TAG, "switched to native TIM ui on ${activity.javaClass.simpleName}")
        return true
    }

    /** 从 TIM 原生界面切回我们的 Compose 界面。 */
    fun switchToOurUi(): Boolean {
        val activity = lastActivityRef?.get() ?: return false
        val module = lastModule ?: return false
        nativeUiMode = false
        hideNativeReturnButton()
        ensureHosted(module, activity)
        hosted[activity]?.visibility = View.VISIBLE
        configureWindow(activity)
        HostOwners.get(activity)?.onResume()
        Log.i(TAG, "switched back to compose ui on ${activity.javaClass.simpleName}")
        return true
    }

    /** 原生界面模式的返回按钮：原生 TIM 页面没有我们的入口，需要一个常驻按钮兜底。 */
    private fun showNativeReturnButton(activity: Activity) {
        if (nativeReturnButton?.parent != null) return
        val decor = activity.window?.decorView as? ViewGroup ?: return
        val res = lastModuleResources
        val label = runCatching {
            res?.getString(com.material3tim.R.string.native_ui_return_button)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "返回 Material3Tim"
        val density = activity.resources.displayMetrics.density
        val button = TextView(activity).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            val padH = (14 * density).toInt()
            val padV = (8 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                cornerRadius = 22 * density
                setColor(Color.parseColor("#E61C1B1F"))
                setStroke((1 * density).toInt(), Color.parseColor("#66FFFFFF"))
            }
            elevation = 12 * density
            setOnClickListener { switchToOurUi() }
        }
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END,
        ).apply {
            setMargins(0, 0, (20 * density).toInt(), (96 * density).toInt())
        }
        decor.addView(button, lp)
        nativeReturnButton = button
    }

    private fun hideNativeReturnButton() {
        val button = nativeReturnButton ?: return
        (button.parent as? ViewGroup)?.removeView(button)
        nativeReturnButton = null
    }

    /** 供 hook 调用：保证目标 Activity 上已经挂上我们的 UI。 */
    fun ensureHosted(module: MainModule, activity: Activity) {
        if (nativeUiMode) return
        if (activity.isFinishing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) return
        try {
            val existing = hosted[activity]
            if (existing != null) {
                // 关键：从后台回到前台时把 Compose 的 lifecycle 恢复成 RESUMED。
                // 否则 Compose 的 WindowRecomposer 帧时钟一直暂停：点击能被收到但界面
                // 不重组、列表 fling 没有惯性、预测性返回动画卡在半路。
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
        lastActivityRef = java.lang.ref.WeakReference(activity)
        lastModule = module
        val moduleResources = ModuleResources.createModuleResources(module, activity)
        lastModuleResources = moduleResources
        if (moduleResources == null) {
            // 兜底：把模块资源注入宿主 Activity 的 Resources。
            val injected = ModuleResources.injectIntoHostResources(module, activity, activity.resources)
            if (!injected) {
                // 覆盖安装后当前进程注入的是已删除的旧 APK：自动重启 TIM 一次；
                // 若短时间内已重启过（避免循环），就不要用宿主资源表挂载 UI（否则文案全变 false），
                // 保留 TIM 原生界面，等用户下次冷启动即可恢复。
                if (ModuleResources.tryRecoverByRestart(activity)) return
                Log.e(TAG, "module resources unavailable, skip hosting ui to avoid wrong strings")
                return
            }
        }
        val resources = moduleResources ?: activity.resources
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
        com.material3tim.data.source.AppDataSources.nativeUiSource = TimNativeUiSource
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

        val composeView = ComposeView(moduleContext).apply {
            setContent {
                HostRoot(activity)
            }
        }
        attachToDecor(activity, composeView)
        hosted[activity] = composeView
        Log.i(TAG, "hosted compose ui on ${activity.javaClass.name}, moduleRes=${moduleResources != null}")
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
    fun consumeBack(activity: Activity): Boolean =
        if (nativeUiMode) false else HostOwners.get(activity)?.dispatchBack() == true

    /** 生命周期事件（由 hook 调用）。 */
    fun onActivityPause(activity: Activity) {
        HostOwners.get(activity)?.onPause()
    }

    fun onActivityStop(activity: Activity) {
        HostOwners.get(activity)?.onStop()
    }

    fun onActivityResumed(activity: Activity) {
        if (nativeUiMode) {
            lastActivityRef = java.lang.ref.WeakReference(activity)
            showNativeReturnButton(activity)
            return
        }
        val view = hosted[activity] ?: return
        configureWindow(activity)
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
        if (lastActivityRef?.get() === activity) {
            nativeReturnButton = null
            lastActivityRef = null
        }
    }

    private fun attachToDecor(activity: Activity, view: ComposeView) {
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
        if (nativeUiMode) return
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

/** [com.material3tim.data.source.NativeUiSource] 的宿主实现：直接驱动 [UiHost]。 */
internal object TimNativeUiSource : com.material3tim.data.source.NativeUiSource {
    override fun switchToNativeUi(): Boolean = UiHost.switchToNativeUi()
    override fun isNativeUiMode(): Boolean = UiHost.isNativeUiMode()
}
