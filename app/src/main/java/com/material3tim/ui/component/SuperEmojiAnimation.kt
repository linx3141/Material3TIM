package com.material3tim.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * QQ 超级表情播放组件。
 *
 * TIM 的超级表情资源是 Lottie JSON（`lottiDirPath/{sevrId}.json`），优先用 Lottie 播放动画；
 * 资源未下载（lottiePath 为空）时退回 TIM 的静态 APNG/PNG 兜底图；两者都没有则显示 [fallbackText]。
 *
 * 聊天气泡和大表情面板共用，尺寸由调用方给定。
 */
@Composable
fun SuperEmojiAnimation(
    lottiePath: String?,
    imagePath: String?,
    contentDescription: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
    /**
     * false：只显示首帧/静态图。
     * 默认 false：聊天里每个表情一个 Lottie、每帧独立动画，表情一多会非常卡；
     * 行内/小尺寸下动画也几乎看不清（与文字同高），静态首帧能大幅降低 CPU/GPU 占用。
     */
    animate: Boolean = false,
) {
    // 优先用 TIM 的静态 PNG 缩略图：解码一次、绘制成本极低；
    // Lottie 仅作为没有 PNG 时的兜底（避免每个表情都创建 Lottie composition）。
    if (!imagePath.isNullOrBlank()) {
        AnimatedOrStaticImage(
            path = imagePath,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier,
            animate = true,
        )
        return
    }
    if (!lottiePath.isNullOrBlank()) {
        val composition by rememberLottieComposition(LottieCompositionSpec.File(lottiePath))
        if (composition != null) {
            if (animate) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = modifier,
                    contentScale = ContentScale.Fit,
                )
            } else {
                LottieAnimation(
                    composition = composition,
                    progress = { 0f },
                    modifier = modifier,
                    contentScale = ContentScale.Fit,
                )
            }
            return
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = fallbackText)
    }
}
