package com.material3tim.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.material3tim.data.repository.SettingsRepository
import com.material3tim.data.repository.SettingsRepositoryImpl
import com.material3tim.data.repository.UI_SETTINGS_KEYS
import com.material3tim.appContext
import com.material3tim.ui.UiMode
import com.material3tim.ui.theme.ThemeController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivityViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val prefs = appContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val settingRepo: SettingsRepository = SettingsRepositoryImpl()
    private val mainPageState = MainPageState(savedStateHandle)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in UI_SETTINGS_KEYS) {
            _uiState.value = readUiState()
        }
    }

    private val _uiState = MutableStateFlow(readUiState())
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()
    val selectedMainPage: StateFlow<Int> = mainPageState.selectedPage

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun setSelectedMainPage(page: Int) {
        mainPageState.updateSelectedPage(page)
    }

    private fun readUiState(): MainActivityUiState {
        return MainActivityUiState(
            appSettings = ThemeController.getAppSettings(),
            pageScale = settingRepo.pageScale,
            enableBlur = settingRepo.enableBlur,
            enableFloatingBottomBar = settingRepo.enableFloatingBottomBar,
            enableFloatingBottomBarBlur = settingRepo.enableFloatingBottomBarBlur,
            enableLauncherFloatingToolbar = settingRepo.enableLauncherFloatingToolbar,
            enableNavigationBadge = settingRepo.enableNavigationBadge,
            showTopAppBar = settingRepo.showTopAppBar,
            topAppBarStyle = settingRepo.topAppBarStyle,
            showNavigationBar = settingRepo.showNavigationBar,
            navigationBarMode = settingRepo.navigationBarMode,
            floatingNavigationBarStyle = settingRepo.floatingNavigationBarStyle,
            floatingNavigationBarPosition = settingRepo.floatingNavigationBarPosition,
            enableSwipeDismiss = settingRepo.enableSwipeDismiss,
            pagerInterceptionMode = settingRepo.pagerInterceptionMode,
            uiMode = UiMode.fromValue(settingRepo.uiMode),
        )
    }
}

private const val SELECTED_MAIN_PAGE_KEY = "selected_main_page"

private class MainPageState(
    private val savedStateHandle: SavedStateHandle,
) {
    val selectedPage: StateFlow<Int> = savedStateHandle.getStateFlow(SELECTED_MAIN_PAGE_KEY, 0)

    fun updateSelectedPage(page: Int) {
        savedStateHandle[SELECTED_MAIN_PAGE_KEY] = MainPagerConfig.coercePage(page)
    }
}

object MainPagerConfig {
    const val PAGE_COUNT = 3
    const val LAST_PAGE_INDEX = PAGE_COUNT - 1

    fun coercePage(page: Int): Int = page.coerceIn(0, LAST_PAGE_INDEX)
}
