package com.material3tim.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.material3tim.ui.theme.PaletteStyle
import com.material3tim.ui.theme.ColorSpecVersion
import com.material3tim.ui.UiMode

@Immutable
data class SettingsUiState(
    val uiMode: String = UiMode.DEFAULT_VALUE,
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = PaletteStyle.TonalSpot.name,
    val colorSpec: String = ColorSpecVersion.SPEC_2025.name,
    val enablePredictiveBack: Boolean = false,
    val enableSwipeDismiss: Boolean = true,
    val pagerInterceptionMode: Int = 1,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    /** Launcher3 微件页同款悬浮底栏。 */
    val enableLauncherFloatingToolbar: Boolean = false,
    val enableNavigationBadge: Boolean = true,
    /** 双击加一：气泡右侧 +1 / 双击复读。 */
    val enableDoubleTapPlusOne: Boolean = false,
    /** 聊天页顶栏名称左侧显示头像。 */
    val showChatTopBarAvatar: Boolean = true,
    /** 是否显示顶栏（Miuix 页面通用）。 */
    val showTopAppBar: Boolean = true,
    /** 顶栏风格：0=Original（普通 TopAppBar），1=Glass（GlassTopAppBar）。 */
    val topAppBarStyle: Int = 1,
    /** 是否显示底栏/侧栏（Miuix 主导航）。 */
    val showNavigationBar: Boolean = true,
    /** 底部导航栏显示模式：0=图标+文本，1=仅图标，2=选中项显示文本。 */
    val navigationBarMode: Int = 0,
    /** 悬浮底栏风格：0=Original，1=Glass，2=iOS-like。 */
    val floatingNavigationBarStyle: Int = 1,
    /** 悬浮底栏位置（仅 Original 风格）：0=居中，1=靠左，2=靠右。 */
    val floatingNavigationBarPosition: Int = 0,
    val pageScale: Float = 1.0f,
)

@Immutable
data class SettingsScreenActions(
    val onOpenTheme: () -> Unit,
    val onSetUiModeIndex: (Int) -> Unit,
    val onOpenAbout: () -> Unit,
    val onSetDoubleTapPlusOne: (Boolean) -> Unit,
    val onSetShowChatTopBarAvatar: (Boolean) -> Unit,
)
