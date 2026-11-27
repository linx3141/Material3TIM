package com.material3tim.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize

// 宿主在旋转转场中的窗口指标可能比实际覆盖层尺寸晚更新。
internal val LocalHostWindowSize = compositionLocalOf { IntSize.Zero }

@Composable
fun shouldShowSplitPane(): Boolean {
    val hostSize = LocalHostWindowSize.current
    val size = if (hostSize.width > 0 && hostSize.height > 0) {
        hostSize
    } else {
        LocalWindowInfo.current.containerSize
    }
    return shouldShowSplitPane(size.width, size.height, LocalResources.current.displayMetrics.density)
}

internal fun shouldShowSplitPane(widthPx: Int, heightPx: Int, density: Float): Boolean {
    val widthDp = widthPx / density
    return widthDp >= 840f || (widthDp >= 600f && heightPx.toFloat() / widthPx < 1.2f)
}
