package com.material3tim.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.material3tim.data.repository.SettingsRepository
import com.material3tim.data.repository.SettingsRepositoryImpl
import com.material3tim.ui.UiMode
import com.material3tim.ui.screen.settings.SettingsUiState
import com.material3tim.ui.theme.ColorMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI 层设置（外观 / 主题 / 导航栏）。与 KernelSU Manager 的 SettingsViewModel 保持同名方法，
 * 去掉所有与 root / 模块相关的部分。
 */
class SettingsViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    uiMode = repo.uiMode,
                    themeMode = repo.themeMode,
                    miuixMonet = repo.miuixMonet,
                    keyColor = repo.keyColor,
                    colorStyle = repo.colorStyle,
                    colorSpec = repo.colorSpec,
                    enablePredictiveBack = repo.enablePredictiveBack,
                    enableSwipeDismiss = repo.enableSwipeDismiss,
                    pagerInterceptionMode = repo.pagerInterceptionMode,
                    enableBlur = repo.enableBlur,
                    enableFloatingBottomBar = repo.enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = repo.enableFloatingBottomBarBlur,
                    enableNavigationBadge = repo.enableNavigationBadge,
                    showTopAppBar = repo.showTopAppBar,
                    enableDoubleTapPlusOne = repo.enableDoubleTapPlusOne,
                    showChatTopBarAvatar = repo.showChatTopBarAvatar,
                    topAppBarStyle = repo.topAppBarStyle,
                    showNavigationBar = repo.showNavigationBar,
                    navigationBarMode = repo.navigationBarMode,
                    floatingNavigationBarStyle = repo.floatingNavigationBarStyle,
                    floatingNavigationBarPosition = repo.floatingNavigationBarPosition,
                    pageScale = repo.pageScale,
                )
            }
        }
    }

    fun setUiMode(mode: String) {
    fun setShowChatTopBarAvatar(enabled: Boolean) {
        repo.showChatTopBarAvatar = enabled
        _uiState.update { it.copy(showChatTopBarAvatar = enabled) }
    }

    fun setDoubleTapPlusOne(enabled: Boolean) {
        repo.enableDoubleTapPlusOne = enabled
        _uiState.update { it.copy(enableDoubleTapPlusOne = enabled) }
    }

        val oldMode = repo.uiMode
        val currentThemeMode = repo.themeMode

        val newThemeMode = when (oldMode) {
            "material" if mode == "miuix" -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                val baseMode = if (colorMode == ColorMode.DARK_AMOLED) 2 else currentThemeMode
                if (repo.miuixMonet && !colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toMonetMode()
                } else if (!repo.miuixMonet && colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toNonMonetMode()
                } else baseMode
            }

            "miuix" if mode == "material" -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                if (colorMode.isMonet) colorMode.toNonMonetMode() else currentThemeMode
            }

            else -> currentThemeMode
        }

        repo.uiMode = mode
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(uiMode = mode, themeMode = newThemeMode) }
    }

    fun setThemeMode(mode: Int) {
        val currentUiMode = repo.uiMode
        val effectiveMode = if (currentUiMode == "miuix" && _uiState.value.miuixMonet) {
            mode + 3
        } else {
            mode
        }
        repo.themeMode = effectiveMode
        _uiState.update { it.copy(themeMode = effectiveMode) }
    }

    fun setColorMode(mode: ColorMode) {
        repo.themeMode = mode.value
        _uiState.update { it.copy(themeMode = mode.value) }
    }

    fun setMiuixMonet(enabled: Boolean) {
        val currentThemeMode = repo.themeMode
        val colorMode = ColorMode.fromValue(currentThemeMode)
        val newThemeMode = if (enabled) {
            if (!colorMode.isMonet) colorMode.toMonetMode() else currentThemeMode
        } else {
            if (colorMode.isMonet) colorMode.toNonMonetMode() else currentThemeMode
        }
        repo.miuixMonet = enabled
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(miuixMonet = enabled, themeMode = newThemeMode) }
    }

    fun setKeyColor(color: Int) {
        repo.keyColor = color
        _uiState.update { it.copy(keyColor = color) }
    }

    fun setColorStyle(style: String) {
        repo.colorStyle = style
        _uiState.update { it.copy(colorStyle = style) }
    }

    fun setColorSpec(spec: String) {
        repo.colorSpec = spec
        _uiState.update { it.copy(colorSpec = spec) }
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        repo.enablePredictiveBack = enabled
        _uiState.update { it.copy(enablePredictiveBack = enabled) }
    }

    fun setEnableSwipeDismiss(enabled: Boolean) {
        repo.enableSwipeDismiss = enabled
        _uiState.update { it.copy(enableSwipeDismiss = enabled) }
    }

    fun setPagerInterceptionMode(mode: Int) {
        repo.pagerInterceptionMode = mode
        _uiState.update { it.copy(pagerInterceptionMode = mode.coerceIn(0, 2)) }
    }

    fun setEnableBlur(enabled: Boolean) {
        repo.enableBlur = enabled
        _uiState.update { it.copy(enableBlur = enabled) }
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        repo.enableFloatingBottomBar = enabled
        _uiState.update { it.copy(enableFloatingBottomBar = enabled) }
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        repo.enableFloatingBottomBarBlur = enabled
        _uiState.update { it.copy(enableFloatingBottomBarBlur = enabled) }
    }

    fun setEnableNavigationBadge(enabled: Boolean) {
        repo.enableNavigationBadge = enabled
        _uiState.update { it.copy(enableNavigationBadge = enabled) }
    }

    fun setShowTopAppBar(show: Boolean) {
        repo.showTopAppBar = show
        _uiState.update { it.copy(showTopAppBar = show) }
    }

    fun setTopAppBarStyle(style: Int) {
        val value = style.coerceIn(0, 1)
        repo.topAppBarStyle = value
        _uiState.update { it.copy(topAppBarStyle = value) }
    }

    fun setShowNavigationBar(show: Boolean) {
        repo.showNavigationBar = show
        _uiState.update { it.copy(showNavigationBar = show) }
    }

    fun setNavigationBarMode(mode: Int) {
        val value = mode.coerceIn(0, 2)
        repo.navigationBarMode = value
        _uiState.update { it.copy(navigationBarMode = value) }
    }

    fun setFloatingNavigationBarStyle(style: Int) {
        val value = style.coerceIn(0, 2)
        repo.floatingNavigationBarStyle = value
        _uiState.update { it.copy(floatingNavigationBarStyle = value) }
    }

    fun setFloatingNavigationBarPosition(position: Int) {
        val value = position.coerceIn(0, 2)
        repo.floatingNavigationBarPosition = value
        _uiState.update { it.copy(floatingNavigationBarPosition = value) }
    }

    fun setPageScale(scale: Float) {
        repo.pageScale = scale
        _uiState.update { it.copy(pageScale = scale) }
    }
}

/** 供 SettingPager 使用：把下拉索引转换成 UiMode 字符串。 */
fun uiModeFromIndex(index: Int): String =
    if (index == 0) UiMode.Miuix.value else UiMode.Material.value
