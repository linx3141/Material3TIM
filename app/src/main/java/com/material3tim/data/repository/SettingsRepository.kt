package com.material3tim.data.repository

/**
 * 仅保留 UI 层需要的设置项。与 KernelSU Manager 的 SettingsRepository 保持相同的键名与默认值，
 * 方便后续继续沿用其实现。
 */
interface SettingsRepository {
    var uiMode: String
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String

    var enablePredictiveBack: Boolean
    var enableSwipeDismiss: Boolean
    var pagerInterceptionMode: Int

    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    /** Launcher3 微件页同款可滚动悬浮底栏（Material3 FloatingToolbar 风格）。 */
    var enableLauncherFloatingToolbar: Boolean
    var enableNavigationBadge: Boolean
    /** 双击加一：聊天里每条气泡右侧显示 +1，双击气泡复读（与长按菜单 +1 相同）。 */
    /** 聊天页顶栏在聊天名称左侧显示圆形头像。 */
    var showChatTopBarAvatar: Boolean
    var enableDoubleTapPlusOne: Boolean
    /** 是否显示顶部栏（Miuix 页面）。 */
    var showTopAppBar: Boolean
    /** 顶部栏风格：0=Original（普通 TopAppBar），1=Glass（GlassTopAppBar）。 */
    var topAppBarStyle: Int
    /** 是否显示底部导航栏（Miuix）。 */
    var showNavigationBar: Boolean
    /** 底部导航栏显示模式：0=图标+文本，1=仅图标，2=选中项显示文本。 */
    var navigationBarMode: Int
    /** 悬浮底栏风格：0=Original（miuix FloatingNavigationBar），1=Glass（GlassNavigationBar），2=iOS-like。 */
    var floatingNavigationBarStyle: Int
    /** 悬浮底栏位置（仅 Original 风格）：0=居中，1=靠左，2=靠右。 */
    var floatingNavigationBarPosition: Int
    var navigationRailExpanded: Boolean
    var pageScale: Float
}
