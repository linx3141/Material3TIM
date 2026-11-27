package com.material3tim.ui.screen.profile

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.material3tim.R

@Composable
internal fun OpenNativeLibraryPage(onOpen: () -> Boolean, onReturn: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (!onOpen()) {
            Toast.makeText(context, context.getString(R.string.profile_native_failed), Toast.LENGTH_LONG).show()
        }
        // 先移除占位路由，从 TIM 原生页面返回时直接回到「我的」。
        onReturn()
    }
}
