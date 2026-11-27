package com.material3tim.data.repository

import android.content.Context
import androidx.core.content.edit
import com.material3tim.appContext
import com.material3tim.ui.UiMode
import com.material3tim.ui.theme.PaletteStyle
import com.material3tim.ui.theme.ColorSpecVersion

private const val SETTINGS_PREFS = "settings"

class SettingsRepositoryImpl : SettingsRepository {

    private val prefs by lazy {
        appContext().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString("ui_mode", value) }

    override var themeMode: Int
        get() = prefs.getInt("color_mode", 0)
        set(value) = prefs.edit { putInt("color_mode", value) }

    override var miuixMonet: Boolean
        // 默认开启：Miuix 界面默认使用系统莫奈取色，跟随系统壁纸/主题。
        get() = prefs.getBoolean("miuix_monet", true)
        set(value) = prefs.edit { putBoolean("miuix_monet", value) }

    override var keyColor: Int
        get() = prefs.getInt("key_color", 0)
        set(value) = prefs.edit { putInt("key_color", value) }

    override var followSystem: Boolean
        // 默认跟随系统；用户选自定义颜色或关闭开关后走 keyColor/style 生成。
        get() = prefs.getBoolean("follow_system", true)
        set(value) = prefs.edit { putBoolean("follow_system", value) }

    override var colorStyle: String
        get() = prefs.getString("color_style", PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name
        set(value) = prefs.edit { putString("color_style", value) }

    override var colorSpec: String
        get() = prefs.getString("color_spec", ColorSpecVersion.SPEC_2025.name)
            ?: ColorSpecVersion.SPEC_2025.name
        set(value) = prefs.edit { putString("color_spec", value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", false)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var enableSwipeDismiss: Boolean
        get() = prefs.getBoolean("enable_swipe_dismiss", true)
        set(value) = prefs.edit { putBoolean("enable_swipe_dismiss", value) }

    override var pagerInterceptionMode: Int
        get() = prefs.getInt("pager_interception_mode", 1)
        set(value) = prefs.edit { putInt("pager_interception_mode", value.coerceIn(0, 2)) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean("enable_blur", false)
        set(value) = prefs.edit { putBoolean("enable_blur", value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar", value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar_blur", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar_blur", value) }

    override var enableNavigationBadge: Boolean
        get() = prefs.getBoolean("enable_navigation_badge", true)
        set(value) = prefs.edit { putBoolean("enable_navigation_badge", value) }

    override var showTopAppBar: Boolean
        get() = prefs.getBoolean("show_top_app_bar", true)
        set(value) = prefs.edit { putBoolean("show_top_app_bar", value) }

    override var topAppBarStyle: Int
        get() = prefs.getInt("top_app_bar_style", 1)
        set(value) = prefs.edit { putInt("top_app_bar_style", value.coerceIn(0, 1)) }

    override var showNavigationBar: Boolean
        get() = prefs.getBoolean("show_navigation_bar", true)
        set(value) = prefs.edit { putBoolean("show_navigation_bar", value) }

    override var navigationBarMode: Int
        get() = prefs.getInt("navigation_bar_mode", 0)
        set(value) = prefs.edit { putInt("navigation_bar_mode", value.coerceIn(0, 2)) }

    override var floatingNavigationBarStyle: Int
        get() = prefs.getInt("floating_navigation_bar_style", 1)
        set(value) = prefs.edit { putInt("floating_navigation_bar_style", value.coerceIn(0, 2)) }

    override var floatingNavigationBarPosition: Int
        get() = prefs.getInt("floating_navigation_bar_position", 0)
        set(value) = prefs.edit { putInt("floating_navigation_bar_position", value.coerceIn(0, 2)) }

    override var navigationRailExpanded: Boolean
        get() = prefs.getBoolean("nav_rail_expanded", false)
        set(value) = prefs.edit { putBoolean("nav_rail_expanded", value) }

    override var pageScale: Float
        get() = prefs.getFloat("page_scale", 1.0f)
        set(value) = prefs.edit { putFloat("page_scale", value) }
}

/** 监听 UI 相关设置项的变化。 */
val UI_SETTINGS_KEYS = setOf(
    "color_mode",
    "key_color",
    "follow_system",
    "color_style",
    "color_spec",
    "page_scale",
    "enable_blur",
    "enable_floating_bottom_bar",
    "enable_floating_bottom_bar_blur",
    "enable_navigation_badge",
    "show_top_app_bar",
    "top_app_bar_style",
    "show_navigation_bar",
    "navigation_bar_mode",
    "floating_navigation_bar_style",
    "floating_navigation_bar_position",
    "enable_swipe_dismiss",
    "pager_interception_mode",
    "ui_mode",
)
