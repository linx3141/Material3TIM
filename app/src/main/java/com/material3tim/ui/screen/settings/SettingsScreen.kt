package com.material3tim.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.Navigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.viewmodel.SettingsViewModel
import com.material3tim.ui.viewmodel.uiModeFromIndex

/**
 * 设置页入口（以独立路由打开，属于「我的」页的下级页面）。
 * 只保留 KernelSU Manager 中与 UI 有关的设置：界面风格、主题、关于。
 */
@Composable
fun SettingPager(navigator: Navigator) {
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val actions = SettingsScreenActions(
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onSetUiModeIndex = { index -> viewModel.setUiMode(uiModeFromIndex(index)) },
        onOpenAbout = { navigator.push(Route.About) },
        onSetDoubleTapPlusOne = { enabled -> viewModel.setDoubleTapPlusOne(enabled) },
        onSetShowChatTopBarAvatar = { enabled -> viewModel.setShowChatTopBarAvatar(enabled) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(uiState, actions)
        UiMode.Material -> SettingPagerMaterial(uiState, actions)
    }
}
