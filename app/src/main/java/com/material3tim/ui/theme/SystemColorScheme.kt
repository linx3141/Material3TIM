package com.material3tim.ui.theme

import android.content.res.Resources
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * “跟随系统”配色：直接读系统 Material You 的语义色（`android.R.color.system_*`），
 * 而不是“拿系统 seed 再生成一遍”。
 *
 * 为什么不用 Compose 的 `dynamicLightColorScheme(context)`：在 TIM 宿主里
 * `LocalContext` 是模块 Context，且实测该 API 返回全 0 色板；这里统一用
 * `Resources.getSystem()` 的 framework-res，宿主/独立 App 都一样。
 *
 * 缺失的角色（Android 12/13 没有容器色、OEM 没实现、RRO 没覆盖）回退到
 * [fallback]（MaterialKolor 按系统 seed 生成的结果），保证不会出现 0 色。
 */
@Composable
fun rememberSystemFollowColorScheme(
    isDark: Boolean,
    fallback: ColorScheme,
): ColorScheme = remember(isDark, fallback) {
    buildSystemFollowColorScheme(isDark, fallback)
}

private fun buildSystemFollowColorScheme(isDark: Boolean, fallback: ColorScheme): ColorScheme {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return fallback

    val res = Resources.getSystem()
    val suffix = if (isDark) "_dark" else "_light"

    fun raw(name: String): Int? {
        val id = res.getIdentifier(name, "color", "android")
        if (id == 0) return null
        return runCatching { res.getColor(id, null) }
            .getOrNull()
            ?.takeIf { (it ushr 24) != 0 }
    }

    /** 先试带 light/dark 后缀的语义色，再试无后缀的 fixed 角色。 */
    fun sys(name: String): Color? =
        (raw(name + suffix) ?: raw(name))?.let { Color(it) }

    return fallback.copy(
        primary = sys("system_primary") ?: fallback.primary,
        onPrimary = sys("system_on_primary") ?: fallback.onPrimary,
        primaryContainer = sys("system_primary_container") ?: fallback.primaryContainer,
        onPrimaryContainer = sys("system_on_primary_container") ?: fallback.onPrimaryContainer,
        inversePrimary = sys("system_inverse_primary") ?: fallback.inversePrimary,
        primaryFixed = sys("system_primary_fixed") ?: fallback.primaryFixed,
        primaryFixedDim = sys("system_primary_fixed_dim") ?: fallback.primaryFixedDim,
        onPrimaryFixed = sys("system_on_primary_fixed") ?: fallback.onPrimaryFixed,
        onPrimaryFixedVariant = sys("system_on_primary_fixed_variant")
            ?: fallback.onPrimaryFixedVariant,

        secondary = sys("system_secondary") ?: fallback.secondary,
        onSecondary = sys("system_on_secondary") ?: fallback.onSecondary,
        secondaryContainer = sys("system_secondary_container") ?: fallback.secondaryContainer,
        onSecondaryContainer = sys("system_on_secondary_container")
            ?: fallback.onSecondaryContainer,
        secondaryFixed = sys("system_secondary_fixed") ?: fallback.secondaryFixed,
        secondaryFixedDim = sys("system_secondary_fixed_dim") ?: fallback.secondaryFixedDim,
        onSecondaryFixed = sys("system_on_secondary_fixed") ?: fallback.onSecondaryFixed,
        onSecondaryFixedVariant = sys("system_on_secondary_fixed_variant")
            ?: fallback.onSecondaryFixedVariant,

        tertiary = sys("system_tertiary") ?: fallback.tertiary,
        onTertiary = sys("system_on_tertiary") ?: fallback.onTertiary,
        tertiaryContainer = sys("system_tertiary_container") ?: fallback.tertiaryContainer,
        onTertiaryContainer = sys("system_on_tertiary_container")
            ?: fallback.onTertiaryContainer,
        tertiaryFixed = sys("system_tertiary_fixed") ?: fallback.tertiaryFixed,
        tertiaryFixedDim = sys("system_tertiary_fixed_dim") ?: fallback.tertiaryFixedDim,
        onTertiaryFixed = sys("system_on_tertiary_fixed") ?: fallback.onTertiaryFixed,
        onTertiaryFixedVariant = sys("system_on_tertiary_fixed_variant")
            ?: fallback.onTertiaryFixedVariant,

        background = sys("system_background") ?: fallback.background,
        onBackground = sys("system_on_background") ?: fallback.onBackground,
        surface = sys("system_surface") ?: fallback.surface,
        onSurface = sys("system_on_surface") ?: fallback.onSurface,
        surfaceVariant = sys("system_surface_variant") ?: fallback.surfaceVariant,
        onSurfaceVariant = sys("system_on_surface_variant") ?: fallback.onSurfaceVariant,
        surfaceTint = sys("system_primary") ?: fallback.surfaceTint,
        inverseSurface = sys("system_inverse_surface") ?: fallback.inverseSurface,
        inverseOnSurface = sys("system_inverse_on_surface") ?: fallback.inverseOnSurface,

        surfaceBright = sys("system_surface_bright") ?: fallback.surfaceBright,
        surfaceDim = sys("system_surface_dim") ?: fallback.surfaceDim,
        surfaceContainer = sys("system_surface_container") ?: fallback.surfaceContainer,
        surfaceContainerLow = sys("system_surface_container_low")
            ?: fallback.surfaceContainerLow,
        surfaceContainerLowest = sys("system_surface_container_lowest")
            ?: fallback.surfaceContainerLowest,
        surfaceContainerHigh = sys("system_surface_container_high")
            ?: fallback.surfaceContainerHigh,
        surfaceContainerHighest = sys("system_surface_container_highest")
            ?: fallback.surfaceContainerHighest,

        error = sys("system_error") ?: fallback.error,
        onError = sys("system_on_error") ?: fallback.onError,
        errorContainer = sys("system_error_container") ?: fallback.errorContainer,
        onErrorContainer = sys("system_on_error_container") ?: fallback.onErrorContainer,

        outline = sys("system_outline") ?: fallback.outline,
        outlineVariant = sys("system_outline_variant") ?: fallback.outlineVariant,
        scrim = fallback.scrim,
    )
}
