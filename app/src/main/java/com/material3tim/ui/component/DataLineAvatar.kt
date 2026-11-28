package com.material3tim.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「我的手机 / 我的电脑 / 我的平板」DataLine 设备会话头像。
 * 使用固定设备图标 + 主题色底，避免显示成名字首字渐变/无意义头像。
 *
 * @param kind 1=手机，2=电脑，3=平板
 */
@Composable
fun DataLineAvatar(
    kind: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val uiMode = LocalUiMode.current
    val container = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.primaryContainer
        UiMode.Miuix -> MiuixTheme.colorScheme.primaryContainer
    }
    val content = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.onPrimaryContainer
        UiMode.Miuix -> MiuixTheme.colorScheme.onPrimaryContainer
    }
    val icon = when (kind) {
        2 -> Icons.Filled.Computer
        3 -> Icons.Filled.Tablet
        else -> Icons.Filled.PhoneAndroid
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size * 0.54f),
        )
    }
}
