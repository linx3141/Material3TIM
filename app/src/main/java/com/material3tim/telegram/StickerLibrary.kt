package com.material3tim.telegram

import android.content.Context
import com.material3tim.data.model.CustomEmoji
import com.material3tim.data.model.StickerPack
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID
import java.util.zip.ZipInputStream

@Serializable
internal data class TelegramPack(val id: Long, val title: String, val stickers: List<TelegramSticker>)

@Serializable
internal data class TelegramSticker(val id: String, val emoji: String, val file: String, val preview: String? = null)

internal data class StickerLibrary(val folder: File, val packs: List<TelegramPack>) {
    fun covers() = packs.map { pack ->
        val first = pack.stickers.firstOrNull()
        StickerPack("tg:${pack.id}", pack.title, first?.emoji.orEmpty(), coverPath = first?.let(::imagePath))
    }

    fun cells(pack: TelegramPack) = pack.stickers.map {
        CustomEmoji(it.id, imagePath(it), it.emoji)
    }

    private fun imagePath(sticker: TelegramSticker): String? =
        (sticker.preview ?: sticker.file.takeIf { it.endsWith(".webp") })?.let { File(folder, it).path }
}

internal object TelegramLibraries {
    private val writes = Mutex()
    private val mediaName = Regex("[a-zA-Z0-9_-]{1,128}\\.(webp|webm|tgs|jpg|png)")
    private const val INDEX = "packs.json"

    fun root(context: Context, account: String) = File(context.noBackupFilesDir,
        "m3tim-stickers/" + UUID.nameUUIDFromBytes(account.toByteArray(Charsets.UTF_8)))

    private fun pointer(root: File) = File(root, "current")

    fun load(root: File): StickerLibrary? {
        val generation = try {
            pointer(root).readText()
        } catch (_: java.io.FileNotFoundException) { return null }
        val folder = File(root, UUID.fromString(generation).toString())
        return StickerLibrary(folder, Json.decodeFromString<List<TelegramPack>>(File(folder, INDEX).readText()))
    }

    // 只有全部文件与索引匹配时才替换当前版本，中断时仍可使用上次的表情包。
    suspend fun install(archive: InputStream, root: File): Int = writes.withLock {
        val previous = pointer(root).takeIf { it.isFile }?.readText()
        val generation = UUID.randomUUID().toString()
        val folder = File(root, generation)
        check(folder.mkdirs())
        var published = false
        try {
            val names = mutableSetOf<String>()
            ZipInputStream(archive.buffered()).use { zip ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val name = zip.nextEntry?.name ?: break
                    require((name == INDEX || mediaName.matches(name)) && names.add(name))
                    val limit = if (name == INDEX) 32L shl 20 else 16L shl 20
                    File(folder, name).outputStream().buffered().use { out ->
                        val buffer = ByteArray(16 * 1024)
                        var size = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = zip.read(buffer)
                            if (count < 0) break
                            size += count
                            require(size <= limit)
                            out.write(buffer, 0, count)
                        }
                        require(size > 0)
                    }
                }
            }
            val packs = Json.decodeFromString<List<TelegramPack>>(File(folder, INDEX).readText())
            require(packs.distinctBy { it.id }.size == packs.size)
            for (pack in packs) {
                require(pack.stickers.distinctBy { it.id }.size == pack.stickers.size)
                for (sticker in pack.stickers) {
                    require(mediaName.matches(sticker.file) && sticker.file in names)
                    sticker.preview?.let { require(mediaName.matches(it) && it in names) }
                }
            }
            currentCoroutineContext().ensureActive()
            val next = File(root, "current.tmp")
            next.writeText(generation)
            Files.move(next.toPath(), pointer(root).toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            published = true
            // 保存上一代供尚未退出的聊天面板使用，同时回收更旧和中断遗留的目录。
            root.listFiles()?.filter { it.isDirectory && it != folder && it.name != previous }
                ?.forEach { it.deleteRecursively() }
            packs.size
        } finally {
            if (!published) folder.deleteRecursively()
        }
    }
}
