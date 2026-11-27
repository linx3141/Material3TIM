package com.material3tim.telegram

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.os.Build
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

private val conversions = Mutex()

/** TIM 支持 GIF，首次发送时才转码，避免同步大表情库时为所有动画消耗 CPU。 */
internal suspend fun prepareStickerForTim(folder: File, sticker: TelegramSticker): String = conversions.withLock {
    val input = File(folder, sticker.file)
    check(input.isFile)
    if (input.extension == "webp") return@withLock input.path
    val result = File(folder, "${input.name}.gif")
    if (result.isFile) return@withLock result.path
    val temp = File.createTempFile("render-", ".tmp", folder)
    try {
        when (input.extension) {
            "tgs" -> {
                val composition = GZIPInputStream(input.inputStream()).use {
                    LottieCompositionFactory.fromJsonInputStreamSync(it, null).value
                } ?: error("Invalid TGS")
                val drawable = LottieDrawable().apply { setComposition(composition) }
                encodeGif(temp, composition.bounds.width(), composition.bounds.height(),
                    composition.duration.toLong()) { canvas, width, height, fraction ->
                    drawable.setBounds(0, 0, width, height)
                    drawable.progress = fraction
                    drawable.draw(canvas)
                }
            }
            "webm" -> {
                val video = MediaMetadataRetriever()
                try {
                    video.setDataSource(input.path)
                    fun metadata(key: Int) = checkNotNull(video.extractMetadata(key)).toLong()
                    val duration = metadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                    encodeGif(temp, metadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt(),
                        metadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt(), duration) { canvas, w, h, t ->
                        val timeUs = (duration * 1000 * t).toLong()
                        val frame = checkNotNull(if (Build.VERSION.SDK_INT >= 27) {
                            video.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, w, h)
                        } else {
                            video.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        })
                        try { canvas.drawBitmap(frame, null, Rect(0, 0, w, h), paint) } finally { frame.recycle() }
                    }
                } finally { video.release() }
            }
            else -> error("Unsupported sticker format")
        }
        check(temp.renameTo(result))
        result.path
    } finally {
        temp.delete()
    }
}

private suspend fun encodeGif(out: File, sourceWidth: Int, sourceHeight: Int, duration: Long,
    draw: (Canvas, Int, Int, Float) -> Unit) {
    require(sourceWidth > 0 && sourceHeight > 0 && duration in 1..60_000)
    val scale = minOf(1f, 256f / maxOf(sourceWidth, sourceHeight))
    val w = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
    val h = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
    val count = (duration / 80.0).roundToInt().coerceIn(1, 75)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        val pixels = IntArray(w * h)
        out.outputStream().buffered().use { stream ->
            val encoder = GifEncoder(stream, w, h, 0)
            val options = ImageOptions().setDelay((duration / count).coerceAtLeast(10), TimeUnit.MILLISECONDS)
            repeat(count) { idx ->
                currentCoroutineContext().ensureActive()
                bitmap.eraseColor(Color.WHITE)
                draw(canvas, w, h, idx.toFloat() / count)
                bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
                encoder.addImage(pixels, w, options)
            }
            encoder.finishEncoding()
        }
    } finally { bitmap.recycle() }
}
