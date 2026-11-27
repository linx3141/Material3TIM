package com.material3tim.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 「真实莫奈取色」使用的 Context（需要能访问系统资源 `android.R.color.system_accent*`）。
 *
 * - 独立 App：不需要提供（为 null 时回退到 LocalContext）。
 * - Xposed 宿主(TIM)：`LocalContext` 是模块 Context，其 Resources 只包含模块 APK，
 *   读不到系统强调色 → 主题会退化成默认紫。宿主侧通过这个 local 传入宿主 Activity。
 */
val LocalMonetContext = staticCompositionLocalOf<Context?> { null }

/**
 * 从系统壁纸取色（Monet）得到的种子色。
 *
 * Android 12+ 的系统语义色 `system_primary_light/dark` 就是系统主题的 primary；
 * 旧实现固定读 `system_accent1_200`（深色档），浅色模式会拿到偏暗的种子。
 * 现在按 demand 读对应明暗的 primary，旧字段只作兜底。
 *
 * 统一用 `Resources.getSystem()`：宿主 `LocalContext` 是模块 Context，它的
 * Resources 不一定含 framework 资源；framework-res 里的系统色则始终可读。
 */
@Composable
fun rememberMonetSeedColor(isDark: Boolean): Color? {
    return remember(isDark) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return@remember null
        runCatching {
            val res = android.content.res.Resources.getSystem()
            fun color(name: String): Int? {
                val id = res.getIdentifier(name, "color", "android")
                if (id == 0) return null
                return res.getColor(id, null).takeIf { it != 0 }
            }
            when {
                isDark -> color("system_primary_dark") ?: color("system_accent1_200")
                else -> color("system_primary_light") ?: color("system_accent1_600")
            }
        }.getOrNull()?.let { Color(it) }
    }
}
