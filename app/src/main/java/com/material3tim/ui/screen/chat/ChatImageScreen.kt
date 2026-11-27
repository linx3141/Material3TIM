package com.material3tim.ui.screen.chat

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.AnimatedOrStaticImage
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 查看器解码像素预算：优先保留原始像素密度，仅超大图按 2 的幂降采样避免 OOM。 */
private const val ViewerMaxDecodePixels = 12_000_000L
private const val ViewerHardMaxDecodePixels = 20_000_000L

/**
 * 图片详情页（M3E 悬浮顶栏）：
 * - 先显示气泡里的缩略图（立即有画面），宿主且有 id 时后台加载原图，返回后切换（小 loading、不闪黑）；
 * - 长图按「不低于屏幕像素密度 + 像素预算」解码，Fit 完整显示，可双指放大看细节；
 * - 双指缩放 / 拖动、双击放大（再次双击还原）、单击显隐顶栏；缩放/双击不会误触顶栏显隐。
 */
@Composable
fun ChatImageScreen(route: Route.ChatImage) {
    val navigator = LocalNavigator.current
    val gallery = remember(navigator, route) { navigator.chatImageGallery(route) }
    if (gallery.pages.size == 1) {
        ChatImagePage(gallery.pages[0], active = true)
        return
    }
    val pagerState = rememberPagerState(initialPage = gallery.initialPage) { gallery.pages.size }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) { page ->
        ChatImagePage(
            image = gallery.pages[page],
            active = pagerState.settledPage == page,
        )
    }
}

@Composable
private fun ChatImagePage(image: Route.ChatImage, active: Boolean) {
    val (imagePath, chatId, msgId, elementId) = image
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var saving by remember { mutableStateOf(false) }

    // 原图加载状态：originalPath 供「保存」使用；静态图用解码后的 bitmap 显示，动图交回 AnimatedOrStaticImage。
    var originalPath by remember(imagePath, chatId, msgId, elementId) { mutableStateOf<String?>(null) }
    var originalBitmap by remember(imagePath, chatId, msgId, elementId) {
        mutableStateOf<ImageBitmap?>(null)
    }
    var originalAnimatedPath by remember(imagePath, chatId, msgId, elementId) {
        mutableStateOf<String?>(null)
    }
    var originalLoading by remember(imagePath, chatId, msgId, elementId) { mutableStateOf(false) }

    val density = LocalDensity.current
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.roundToPx()
    }

    LaunchedEffect(chromeVisible, active) {
        if (chromeVisible && active) {
            delay(3000)
            chromeVisible = false
        }
    }

    // 仅停在该页后加载原图，避免快速翻页触发相邻图片的下载和大图解码。
    LaunchedEffect(image, active) {
        if (!active) {
            scale = 1f
            offset = Offset.Zero
            originalLoading = false
            return@LaunchedEffect
        }
        if (originalPath != null || AppDataSources.messageSource == null) return@LaunchedEffect
        if (chatId.isBlank() || msgId == 0L || elementId == 0L) return@LaunchedEffect
        originalLoading = true
        val loadedPath = withContext(Dispatchers.IO) {
            runCatching {
                AppDataSources.messageSource?.loadChatImageOriginal(chatId, msgId, elementId)
            }.getOrNull()
        }
        if (!loadedPath.isNullOrBlank() && File(loadedPath).isFile) {
            if (withContext(Dispatchers.IO) { isAnimatedImageFile(loadedPath) }) {
                // 动图：继续用 AnimatedOrStaticImage（ImageDecoder 播放），不降级成静态帧。
                originalAnimatedPath = loadedPath
                originalPath = loadedPath
            } else {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeViewerBitmap(loadedPath, screenWidthPx)
                }
                if (bitmap != null) {
                    originalBitmap = bitmap
                    originalPath = loadedPath
                }
            }
        }
        originalLoading = false
    }

    val savePath = originalPath ?: imagePath
    val imageGestures = Modifier.pointerInput(image) {
        var lastTapUptime = 0L
        var lastTapPos = Offset.Zero
        var pendingSingleTap: Job? = null
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var moved = false
            var transforming = scale > 1f
            var totalPan = Offset.Zero
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.count { it.pressed }
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                transforming = transforming || pressed > 1
                totalPan += pan
                if (pressed > 1 || totalPan.getDistance() > viewConfiguration.touchSlop) moved = true
                // 未放大时不要消费单指横划，交给 Pager；父层已开始滚动时不要判成点击。
                if (!transforming) {
                    if (awaitPointerEvent(PointerEventPass.Final).changes.any { it.isConsumed }) {
                        return@awaitEachGesture
                    }
                    if (pressed == 0) break
                    continue
                }
                // 每一帧都应用位移/缩放：保证放大后拖动严格 1:1 跟随手指。
                val newScale = (scale * zoom).coerceIn(1f, 6f)
                if (newScale == 1f) {
                    scale = 1f
                    offset = Offset.Zero
                } else {
                    scale = newScale
                    val maxX = size.width * (scale - 1f) / 2f
                    val maxY = size.height * (scale - 1f) / 2f
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                        y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                    )
                }
                if (moved) {
                    event.changes.forEach {
                        if (it.positionChanged()) it.consume()
                    }
                }
                if (pressed == 0) break
            }
            if (!moved) {
                val now = System.currentTimeMillis()
                val tapPos = down.position
                val isDoubleTap = now - lastTapUptime < 320L &&
                    (tapPos - lastTapPos).getDistance() < 100f
                if (isDoubleTap) {
                    pendingSingleTap?.cancel()
                    pendingSingleTap = null
                    lastTapUptime = 0L
                    if (scale > 1.2f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.6f
                        offset = Offset(
                            x = (scale - 1f) * (size.width / 2f - tapPos.x),
                            y = (scale - 1f) * (size.height / 2f - tapPos.y),
                        )
                    }
                    chromeVisible = true
                } else {
                    lastTapUptime = now
                    lastTapPos = tapPos
                    val expected = now
                    pendingSingleTap?.cancel()
                    pendingSingleTap = scope.launch {
                        delay(320L)
                        if (lastTapUptime == expected) {
                            chromeVisible = !chromeVisible
                        }
                    }
                }
            }
        }
    }
    val imageModifier = Modifier.fillMaxSize().graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = offset.x
        translationY = offset.y
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
            .then(imageGestures),
    ) {
        val original = originalBitmap
        val animatedOriginal = originalAnimatedPath
        if (original != null && animatedOriginal == null) {
            Image(
                bitmap = original,
                contentDescription = stringResource(R.string.chat_image_message),
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
            )
        } else {
            AnimatedOrStaticImage(
                path = animatedOriginal ?: imagePath,
                contentDescription = stringResource(R.string.chat_image_message),
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
                // 查看器需要清晰原图，单独放宽解码上限。
                maxDimension = 2048,
            )
        }

        // 原图加载中的小指示：底部右侧，不遮挡整图。
        if (originalLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 28.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // 悬浮顶栏：不参与布局，显隐时图片不移动
        AnimatedVisibility(
            visible = active && chromeVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent),
                        ),
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { navigator.pop() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        if (!saving && File(savePath).isFile) {
                            saving = true
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    saveImageToGallery(
                                        context = context,
                                        sourceFile = File(savePath),
                                        bitmap = null,
                                    )
                                }
                                saving = false
                                Toast.makeText(
                                    context,
                                    if (ok) R.string.chat_image_saved else R.string.chat_image_save_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = stringResource(R.string.chat_image_save),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 查看器原图解码：优先保留原始像素密度（宽/高不因长图被过度降采样），
 * 只对超大图按 2 的幂 inSampleSize 降采样到像素预算内，避免 OOM；
 * 长截图（如 1080×15000）会保持原图像素，放大查看细节不糊。
 */
private fun decodeViewerBitmap(path: String, screenWidthPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (sample < 8) {
        val sampledWidth = bounds.outWidth / sample
        val sampledHeight = bounds.outHeight / sample
        val pixels = sampledWidth.toLong() * sampledHeight.toLong()
        if (pixels <= ViewerMaxDecodePixels) break
        // 宽度已达到屏幕像素密度：再降采样会肉眼看糊，只在内存硬上限内继续保留原像素。
        if (sampledWidth <= screenWidthPx && pixels <= ViewerHardMaxDecodePixels) break
        sample *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}.getOrNull()

/** 嗅探动图（GIF / 动态 WebP / APNG）：动图仍交给 AnimatedOrStaticImage 播放，避免只显示首帧。 */
private fun isAnimatedImageFile(path: String): Boolean = runCatching {
    val bytes = ByteArray(4096)
    val read = File(path).inputStream().use { it.read(bytes) }
    if (read < 12) return false
    fun matches(offset: Int, text: String): Boolean {
        val token = text.toByteArray(Charsets.US_ASCII)
        if (offset + token.size > read) return false
        for (i in token.indices) {
            if (bytes[offset + i] != token[i]) return false
        }
        return true
    }
    fun contains(text: String): Boolean {
        val token = text.toByteArray(Charsets.US_ASCII)
        if (token.isEmpty() || read < token.size) return false
        for (start in 0..(read - token.size)) {
            var matched = true
            for (i in token.indices) {
                if (bytes[start + i] != token[i]) {
                    matched = false
                    break
                }
            }
            if (matched) return true
        }
        return false
    }
    when {
        matches(0, "GIF8") -> true
        matches(0, "RIFF") && matches(8, "WEBP") && contains("ANIM") -> true
        bytes[0] == 0x89.toByte() && contains("acTL") -> true
        else -> false
    }
}.getOrDefault(false)

private fun saveImageToGallery(
    context: android.content.Context,
    sourceFile: File,
    bitmap: Bitmap?,
): Boolean {
    return runCatching {
        val extension = sourceFile.extension.ifBlank { "png" }
        val name = "M3Tim_${System.currentTimeMillis()}.$extension"
        val writeTo = { out: java.io.OutputStream ->
            if (sourceFile.isFile) {
                sourceFile.inputStream().use { it.copyTo(out) }
            } else if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                error("no source image")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Material3Tim")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                writeTo(out)
            } ?: return false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Material3Tim",
            ).apply { mkdirs() }
            val file = File(dir, name)
            file.outputStream().use { out ->
                writeTo(out)
            }
            true
        }
    }.getOrDefault(false)
}
