package com.material3tim.ui.screen.chat

import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.model.ChatPart
import com.material3tim.data.model.ChatPartKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatImageGalleryTest {
    @Test
    fun preservesMessageAndPartOrderAndOpensExactPartEvenWhenPathsMatch() {
        val first = image("first", "same.jpg")
        val mixed = image("mixed", "unused.jpg").copy(
            msgId = 10,
            parts = listOf(
                ChatPart(ChatPartKind.TEXT, text = "caption"),
                ChatPart(ChatPartKind.IMAGE, imagePath = "same.jpg", elementId = 1),
                ChatPart(ChatPartKind.IMAGE, imagePath = "sticker.webp", emojiImage = true),
                ChatPart(ChatPartKind.IMAGE, imagePath = "same.jpg", elementId = 2),
            ),
        )
        val missing = image("missing", null)
        val last = image("last", "last.jpg")

        val gallery = requireNotNull(buildChatImageGallery(listOf(first, mixed, missing, last), mixed, 3))

        assertEquals(listOf("same.jpg", "same.jpg", "same.jpg", "last.jpg"), gallery.pages.map { it.imagePath })
        assertEquals(2, gallery.initialPage)
        assertEquals(2L, gallery.pages[gallery.initialPage].elementId)
        assertEquals(10L, gallery.pages[gallery.initialPage].msgId)
    }

    @Test
    fun keepsStickersAndImagesOutsideTheLoadedListAsSinglePreviews() {
        val photo = image("photo", "photo.jpg")
        val sticker = image("sticker", "sticker.webp").copy(isEmojiImage = true)
        assertEquals(listOf("sticker.webp"), buildChatImageGallery(listOf(photo, sticker), sticker)?.pages?.map { it.imagePath })
        assertEquals(listOf("photo.jpg"), buildChatImageGallery(emptyList(), photo)?.pages?.map { it.imagePath })
        assertNull(buildChatImageGallery(emptyList(), image("missing", null)))
    }

    private fun image(id: String, path: String?) = ChatMessage(
        id = id,
        chatId = "chat",
        senderName = "sender",
        senderSeed = 0,
        type = ChatMessageType.IMAGE,
        imagePath = path,
    )
}
