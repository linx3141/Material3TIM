package com.material3tim.ui.screen.colorpalette

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material3tim.ui.theme.PaletteStyle
import com.material3tim.ui.theme.ColorSpecVersion
import com.material3tim.M3TimApplication
import com.material3tim.R
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.theme.ColorMode
import com.material3tim.ui.viewmodel.SettingsViewModel

/** 当前是否跑在宿主（TIM）进程里：宿主的包名不是我们自己的包名。 */
private fun isHostProcess(context: android.content.Context): Boolean =
    context.packageName != "io.github.linx3141.material3tim"

@Composable
fun ColorPaletteScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPaletteStyle = try {
        PaletteStyle.valueOf(uiState.colorStyle)
    } catch (_: Exception) {
        PaletteStyle.TonalSpot
    }
    val currentColorSpec = try {
        ColorSpecVersion.valueOf(uiState.colorSpec)
    } catch (_: Exception) {
        ColorSpecVersion.SPEC_2025
    }
    val state = ColorPaletteUiState(
        uiState = uiState,
        currentColorMode = ColorMode.fromValue(uiState.themeMode),
        currentPaletteStyle = currentPaletteStyle,
        currentColorSpec = currentColorSpec,
    )
    val actions = ColorPaletteScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSetThemeMode = viewModel::setThemeMode,
        onSetMiuixMonet = viewModel::setMiuixMonet,
        onSetKeyColor = viewModel::setKeyColor,
        onSetColorMode = viewModel::setColorMode,
        onSetColorStyle = viewModel::setColorStyle,
        onSetColorSpec = viewModel::setColorSpec,
        onSetEnableBlur = viewModel::setEnableBlur,
        onSetEnableFloatingBottomBar = viewModel::setEnableFloatingBottomBar,
        onSetEnableFloatingBottomBarBlur = viewModel::setEnableFloatingBottomBarBlur,
        onSetEnableLauncherFloatingToolbar = viewModel::setEnableLauncherFloatingToolbar,
        onSetEnableNavigationBadge = viewModel::setEnableNavigationBadge,
        onSetShowTopAppBar = viewModel::setShowTopAppBar,
        onSetTopAppBarStyle = viewModel::setTopAppBarStyle,
        onSetShowNavigationBar = viewModel::setShowNavigationBar,
        onSetNavigationBarMode = viewModel::setNavigationBarMode,
        onSetFloatingNavigationBarStyle = viewModel::setFloatingNavigationBarStyle,
        onSetFloatingNavigationBarPosition = viewModel::setFloatingNavigationBarPosition,
        onSetEnablePredictiveBack = { enabled ->
            viewModel.setEnablePredictiveBack(enabled)
            M3TimApplication.setEnableOnBackInvokedCallback(context.applicationInfo, enabled)
            if (isHostProcess(context)) {
                // 宿主(TIM)里 recreate 会把 Activity 生命周期交给 TIM 自己管理，实测黑屏；
                // 预测性返回标志在窗口创建时生效，提示用户重启 TIM 即可。
                Toast.makeText(
                    context,
                    R.string.settings_predictive_back_restart_hint,
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                activity?.recreate()
            }
        },
        onSetEnableSwipeDismiss = viewModel::setEnableSwipeDismiss,
        onSetPagerInterceptionMode = viewModel::setPagerInterceptionMode,
        onSetPageScale = viewModel::setPageScale,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> ColorPaletteScreenMiuix(state, actions)
        UiMode.Material -> ColorPaletteScreenMaterial(state, actions)
    }
}
