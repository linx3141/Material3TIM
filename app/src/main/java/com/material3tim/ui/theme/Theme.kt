package com.material3tim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.material3tim.data.repository.SettingsRepository
import com.material3tim.data.repository.SettingsRepositoryImpl
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode

enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    MONET_SYSTEM(3),
    MONET_LIGHT(4),
    MONET_DARK(5),
    DARK_AMOLED(6);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }

    val isSystem: Boolean get() = value == 0 || value == 3
    val isDark: Boolean get() = value == 2 || value == 5 || value == 6
    val isAmoled: Boolean get() = value == 6
    val isMonet: Boolean get() = value >= 3

    fun toNonMonetMode(): Int = when (this) {
        MONET_SYSTEM -> 0
        MONET_LIGHT -> 1
        MONET_DARK, DARK_AMOLED -> 2
        else -> value
    }

    fun toMonetMode(): Int = when (this) {
        SYSTEM -> 3
        LIGHT -> 4
        DARK -> 5
        else -> value
    }
}

data class AppSettings(
    val colorMode: ColorMode,
    val keyColor: Int,
    /** 主题直接跟随系统 Material You 调色板（默认开启）。 */
    val followSystem: Boolean = true,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpecVersion,
)

val PaletteStyle.supportsSpec2025: Boolean
    get() = this == PaletteStyle.TonalSpot ||
            this == PaletteStyle.Neutral ||
            this == PaletteStyle.Vibrant ||
            this == PaletteStyle.Expressive

/**
 * SPEC 2026 目前只支持 CMF 风格（MaterialKolor 6.0：CMF scheme 固定 SPEC_2026）。
 * 颜色标准为 SPEC_2026 时，风格强制锁定 CMF；其它 spec 下沿用用户选择的风格。
 */
fun ColorSpecVersion.effectiveStyle(style: PaletteStyle): PaletteStyle =
    if (this == ColorSpecVersion.SPEC_2026) PaletteStyle.Cmf else style

/** 把「用户选择的风格 + spec」归一成实际生效的 spec。CMF 风格固定 SPEC_2026。 */
fun ColorSpecVersion.effectiveFor(style: PaletteStyle): ColorSpecVersion = when {
    style == PaletteStyle.Cmf -> ColorSpecVersion.SPEC_2026
    this == ColorSpecVersion.SPEC_2025 && !style.supportsSpec2025 -> ColorSpecVersion.SPEC_2021
    else -> this
}

/** SPEC 2026 下风格是否只能为 CMF（主题页据此把风格下拉置灰锁定）。 */
val ColorSpecVersion.locksPaletteStyleToCmf: Boolean
    get() = this == ColorSpecVersion.SPEC_2026

object ThemeController {
    fun getAppSettings(repo: SettingsRepository = SettingsRepositoryImpl()): AppSettings {
        val uiMode = repo.uiMode
        var colorModeValue = repo.themeMode

        if (uiMode == "miuix") {
            val miuixMonet = repo.miuixMonet
            val colorMode = ColorMode.fromValue(colorModeValue)
            colorModeValue = if (!miuixMonet && colorMode.isMonet) {
                colorMode.toNonMonetMode()
            } else if (miuixMonet && !colorMode.isMonet) {
                colorMode.toMonetMode()
            } else {
                colorModeValue
            }
        }

        val colorMode = ColorMode.fromValue(colorModeValue)
        val keyColor = repo.keyColor
        val paletteStyleStr = repo.colorStyle
        val paletteStyle = try {
            PaletteStyle.valueOf(paletteStyleStr)
        } catch (_: Exception) {
            PaletteStyle.TonalSpot
        }
        val colorSpecStr = repo.colorSpec
        val colorSpec = try {
            ColorSpecVersion.valueOf(colorSpecStr)
        } catch (_: Exception) {
            ColorSpecVersion.SPEC_2025
        }

        return AppSettings(
            colorMode = colorMode,
            keyColor = keyColor,
            followSystem = repo.followSystem,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
        )
    }
}

@Composable
fun M3TimTheme(
    appSettings: AppSettings = ThemeController.getAppSettings(),
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit
) {

    when (uiMode) {
        UiMode.Miuix -> MiuixM3TimTheme(
            appSettings = appSettings,
            content = content
        )

        UiMode.Material -> MaterialM3TimTheme(
            appSettings = appSettings,
            content = content
        )
    }
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    return when (LocalColorMode.current) {
        1, 4 -> false  // Force light mode
        2, 5, 6 -> true   // Force dark mode
        else -> isSystemInDarkTheme()  // Follow system (0 or default)
    }
}

val LocalColorMode = staticCompositionLocalOf { 0 }

val LocalEnableBlur = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { false }

val LocalEnableNavigationBadge = staticCompositionLocalOf { true }

/** 是否显示顶栏（Miuix 页面通用；来自主题外观页「布局」分组）。 */
val LocalShowTopAppBar = staticCompositionLocalOf { true }

/** 顶栏风格：0=Original（普通 TopAppBar），1=Glass（GlassTopAppBar）。 */
val LocalTopAppBarStyle = staticCompositionLocalOf { 1 }

/** 是否显示底栏/侧栏（Miuix 主导航）。 */
val LocalShowNavigationBar = staticCompositionLocalOf { true }

/** 底部导航栏显示模式：0=图标+文本，1=仅图标，2=选中项显示文本。 */
val LocalNavigationBarMode = staticCompositionLocalOf { 0 }

/** 悬浮底栏风格：0=Original（miuix FloatingNavigationBar），1=Glass，2=iOS-like。 */
val LocalFloatingNavigationBarStyle = staticCompositionLocalOf { 1 }

/** 悬浮底栏位置（仅 Original 风格）：0=居中，1=靠左，2=靠右。 */
val LocalFloatingNavigationBarPosition = staticCompositionLocalOf { 0 }

val LocalModuleDescriptionMaxLines = staticCompositionLocalOf { 4 }
