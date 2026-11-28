package com.material3tim.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.material3tim.ui.UiMode
import com.material3tim.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val enableLauncherFloatingToolbar: Boolean,
    val enableNavigationBadge: Boolean,
    val showTopAppBar: Boolean,
    val topAppBarStyle: Int,
    val showNavigationBar: Boolean,
    val navigationBarMode: Int,
    val floatingNavigationBarStyle: Int,
    val floatingNavigationBarPosition: Int,
    val enableSwipeDismiss: Boolean,
    val pagerInterceptionMode: Int,
    val uiMode: UiMode,
)
