package com.material3tim.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
fun MiuixM3TimTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && systemDarkTheme)
    // SPEC 2026 只支持 CMF：实际生效风格归一成 CMF（miuix 的 ThemePaletteStyle 已包含 Cmf）。
    val colorSpec = appSettings.colorSpec
    val colorStyle = colorSpec.effectiveStyle(appSettings.paletteStyle)

    val miuixPaletteStyle = try {
        ThemePaletteStyle.valueOf(colorStyle.name)
    } catch (_: Exception) {
        ThemePaletteStyle.TonalSpot
    }

    // miuix 的 ThemeColorSpec 只有 2021/2025；CMF 风格在 ThemeController 内部会强制 SPEC_2026。
    val miuixColorSpec = if (colorSpec.effectiveFor(colorStyle) == ColorSpecVersion.SPEC_2025) {
        ThemeColorSpec.Spec2025
    } else {
        ThemeColorSpec.Spec2021
    }

    val monetSeed = rememberMonetSeedColor(darkTheme)
    val monetContext = LocalMonetContext.current ?: context
    val dynamicSeed = if (appSettings.keyColor == 0 && appSettings.colorMode.isMonet && monetSeed == null) {
        try {
            if (darkTheme) dynamicDarkColorScheme(monetContext).primary
            else dynamicLightColorScheme(monetContext).primary
        } catch (t: Throwable) {
            null
        }
    } else {
        null
    }
    // 默认“跟随系统”：不传 keyColor，让 Miuix 的 ThemeController 走
    // platformDynamicColors（系统语义色 / 系统 palette overlay）。
    // 用户自定义颜色或选了别的风格/规范时，仍以取到的系统色为种子生成。
    val followSystem = appSettings.followSystem &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val resolvedKeyColor: Color? = when {
        followSystem -> null
        appSettings.keyColor != 0 -> Color(appSettings.keyColor)
        appSettings.colorMode.isMonet -> monetSeed ?: dynamicSeed
        else -> null
    }

    val baseMode = when (appSettings.colorMode) {
        ColorMode.SYSTEM -> ColorSchemeMode.System
        ColorMode.LIGHT -> ColorSchemeMode.Light
        ColorMode.DARK -> ColorSchemeMode.Dark
        ColorMode.MONET_SYSTEM -> ColorSchemeMode.MonetSystem
        ColorMode.MONET_LIGHT -> ColorSchemeMode.MonetLight
        ColorMode.MONET_DARK, ColorMode.DARK_AMOLED -> ColorSchemeMode.MonetDark
    }
    // “跟随系统”开关开启时强制走 MonetSystem/Light/Dark，让 ThemeController
    // 调用 platformDynamicColors 直接采用系统语义色；关闭时保持原 colorMode
    // （用户关掉 miuixMonet 就是静态 Miuix 配色）。
    val colorSchemeMode = if (followSystem) {
        when (baseMode) {
            ColorSchemeMode.System -> ColorSchemeMode.MonetSystem
            ColorSchemeMode.Light -> ColorSchemeMode.MonetLight
            ColorSchemeMode.Dark -> ColorSchemeMode.MonetDark
            else -> baseMode
        }
    } else {
        baseMode
    }
    val controller = ThemeController(
        colorSchemeMode,
        keyColor = resolvedKeyColor,
        isDark = darkTheme,
        paletteStyle = miuixPaletteStyle,
        colorSpec = miuixColorSpec,
    )

    MiuixTheme(
        controller = controller,
        content = {
            LaunchedEffect(darkTheme) {
                val window = (context as? Activity)?.window ?: return@LaunchedEffect
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            CompositionLocalProvider(
                LocalContentColor provides MiuixTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    )
}
