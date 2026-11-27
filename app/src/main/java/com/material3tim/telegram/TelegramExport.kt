package com.material3tim.telegram

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal suspend fun exportInstalledStickers(folder: File, progress: (Int, Int, String) -> Unit): File {
    folder.mkdirs()
    val expiry = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
    folder.listFiles()?.filter { it.lastModified() < expiry }?.forEach { it.delete() }
    val output = File.createTempFile("packs-", ".zip", folder)
    try {
        val sets = listOf(TdApi.StickerTypeRegular(), TdApi.StickerTypeMask(), TdApi.StickerTypeCustomEmoji())
            .flatMap { TelegramClient.request(TdApi.GetInstalledStickerSets(it)).sets.toList() }
            .distinctBy { it.id }
        val packs = mutableListOf<TelegramPack>()
        val included = mutableSetOf<String>()
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            // WEBP、WebM 和 TGS 本身均已压缩。
            zip.setLevel(0)
            suspend fun include(file: TdApi.File, ext: String): String {
                val id = file.remote.uniqueId
                require(id.matches(Regex("[A-Za-z0-9_-]{1,128}")))
                val name = "$id.$ext"
                if (included.add(name)) {
                    val local = TelegramClient.request(TdApi.DownloadFile(file.id, 16, 0, 0, true)).local
                    check(local.isDownloadingCompleted)
                    zip.putNextEntry(ZipEntry(name))
                    File(local.path).inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                return name
            }
            sets.forEachIndexed { index, info ->
                progress(index + 1, sets.size, info.title)
                val set = TelegramClient.request(TdApi.GetStickerSet(info.id))
                val stickers = set.stickers.mapIndexed { position, sticker ->
                    currentCoroutineContext().ensureActive()
                    val ext = when (sticker.format) {
                        is TdApi.StickerFormatWebp -> "webp"
                        is TdApi.StickerFormatWebm -> "webm"
                        is TdApi.StickerFormatTgs -> "tgs"
                        else -> error("Unsupported sticker format")
                    }
                    val media = include(sticker.sticker, ext)
                    val thumb = sticker.thumbnail
                    val preview = if (ext == "webp" || thumb == null) null else when (thumb.format) {
                        is TdApi.ThumbnailFormatWebp -> include(thumb.file, "webp")
                        is TdApi.ThumbnailFormatJpeg -> include(thumb.file, "jpg")
                        is TdApi.ThumbnailFormatPng -> include(thumb.file, "png")
                        else -> null
                    }
                    val id = if (sticker.id != 0L) sticker.id.toString() else "file:$media:$position"
                    TelegramSticker(id, sticker.emoji, media, preview)
                }
                packs += TelegramPack(set.id, set.title, stickers)
            }
            zip.putNextEntry(ZipEntry("packs.json"))
            zip.write(Json.encodeToString(packs).toByteArray())
            zip.closeEntry()
        }
        return output
    } catch (e: Throwable) {
        output.delete()
        throw e
    }
}
