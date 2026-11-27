package com.material3tim.ui.screen.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material3tim.R
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
    val context = LocalContext.current

    val actions = SettingsScreenActions(
        onSyncTelegram = rememberTelegramSyncAction(),
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onSetUiModeIndex = { index -> viewModel.setUiMode(uiModeFromIndex(index)) },
        onOpenAbout = { navigator.push(Route.About) },
        onReturnToOriginalTim = {
            val host = context.packageName == "com.tencent.tim"
            if (!host) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_return_original_tim_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                com.material3tim.xposed.UiHost.setMaterialUiEnabled(context, false)
                com.material3tim.xposed.UiHost.restartTim(context)
            }
        },
        onOpenTimSettings = {
            // Material3TIM 只覆盖 SplashActivity；TIM 原生设置是独立 Activity，
            // 打开后不会被 Compose 覆盖层挡住，因此可以正常看到 QAuxv 的入口。
            val opened = runCatching {
                context.startActivity(
                    Intent().apply {
                        setClassName(
                            "com.tencent.tim",
                            "com.tencent.mobileqq.activity.QPublicFragmentActivity",
                        )
                        putExtra(
                            "public_fragment_class",
                            "com.tencent.mobileqq.setting.main.MainSettingFragment",
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }.isSuccess
            if (!opened) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_open_tim_settings_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(uiState, actions)
        UiMode.Material -> SettingPagerMaterial(uiState, actions)
    }
}
