package com.material3tim.telegram

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StickerLibraryTest {
    @get:Rule val temp = TemporaryFolder()
    private val sticker = TelegramSticker("7", "🙂", "abc.webp")
    private val pack = TelegramPack(12, "Animals", listOf(sticker))

    private fun archive(packs: List<TelegramPack>, vararg files: String): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for (name in files) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("packs.json"))
            zip.write(Json.encodeToString(packs).toByteArray())
            zip.closeEntry()
        }
        return bytes.toByteArray().inputStream()
    }

    @Test fun refreshFollowsServerOrderAndRemovesUninstalledPacks() = runBlocking {
        val root = temp.newFolder()
        assertNull(TelegramLibraries.load(root))
        val second = TelegramPack(19, "Faces", listOf(sticker))
        assertEquals(2, TelegramLibraries.install(archive(listOf(second, pack), sticker.file), root))
        val library = TelegramLibraries.load(root)!!
        assertEquals(listOf("tg:19", "tg:12"), library.covers().map { it.id })
        assertTrue(File(library.cells(pack).single().imagePath!!).isFile)
        assertEquals(1, TelegramLibraries.install(archive(listOf(pack), sticker.file), root))
        assertEquals(listOf(pack), TelegramLibraries.load(root)!!.packs)
        assertEquals(0, TelegramLibraries.install(archive(emptyList()), root))
        assertTrue(TelegramLibraries.load(root)!!.packs.isEmpty())
    }

    @Test fun incompleteOrUnsafeArchivePreservesPreviousGeneration() = runBlocking {
        val root = temp.newFolder()
        TelegramLibraries.install(archive(listOf(pack), sticker.file), root)
        val previous = TelegramLibraries.load(root)
        for (bad in listOf(
            archive(listOf(pack)),
            archive(listOf(pack, pack), sticker.file),
            archive(listOf(pack.copy(stickers = listOf(sticker.copy(preview = "no.jpg")))), sticker.file),
            archive(emptyList(), "../escaped.webp"),
        )) {
            assertTrue(runCatching { TelegramLibraries.install(bad, root) }.isFailure)
            assertEquals(previous, TelegramLibraries.load(root))
        }
        assertFalse(File(root, "escaped.webp").exists())
        assertEquals(1, root.listFiles()!!.count { it.isDirectory })
    }
}
