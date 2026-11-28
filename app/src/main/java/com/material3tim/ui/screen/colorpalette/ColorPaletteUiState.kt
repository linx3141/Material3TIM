package com.material3tim.ui.screen.colorpalette

import androidx.compose.runtime.Immutable
import com.material3tim.ui.theme.PaletteStyle
import com.material3tim.ui.theme.ColorSpecVersion
import com.material3tim.ui.screen.settings.SettingsUiState
import com.material3tim.ui.theme.ColorMode

@Immutable
data class ColorPaletteUiState(
    val uiState: SettingsUiState,
    val currentColorMode: ColorMode,
    val currentPaletteStyle: PaletteStyle,
    val currentColorSpec: ColorSpecVersion,
)

@Immutable
data class ColorPaletteScreenActions(
    val onBack: () -> Unit,
    val onSetThemeMode: (Int) -> Unit,
    val onSetMiuixMonet: (Boolean) -> Unit,
    val onSetKeyColor: (Int) -> Unit,
    val onSetColorMode: (ColorMode) -> Unit,
    val onSetColorStyle: (String) -> Unit,
    val onSetColorSpec: (String) -> Unit,
    val onSetEnableBlur: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBar: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBarBlur: (Boolean) -> Unit,
    val onSetEnableLauncherFloatingToolbar: (Boolean) -> Unit,
    val onSetEnableNavigationBadge: (Boolean) -> Unit,
    val onSetShowTopAppBar: (Boolean) -> Unit,
    val onSetTopAppBarStyle: (Int) -> Unit,
    val onSetShowNavigationBar: (Boolean) -> Unit,
    val onSetNavigationBarMode: (Int) -> Unit,
    val onSetFloatingNavigationBarStyle: (Int) -> Unit,
    val onSetFloatingNavigationBarPosition: (Int) -> Unit,
    val onSetEnablePredictiveBack: (Boolean) -> Unit,
    val onSetEnableSwipeDismiss: (Boolean) -> Unit,
    val onSetPagerInterceptionMode: (Int) -> Unit,
    val onSetPageScale: (Float) -> Unit,
)
