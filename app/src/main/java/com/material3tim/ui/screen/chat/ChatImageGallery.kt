package com.material3tim.ui.screen.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.model.ChatPart
import com.material3tim.data.model.ChatPartKind
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route

internal val LocalChatImageMessages = compositionLocalOf { emptyList<ChatMessage>() }

internal data class ChatImageGallery(val pages: List<Route.ChatImage>, val initialPage: Int)

private fun ChatMessage.imageRoute(part: ChatPart?) = Route.ChatImage(
    imagePath = (if (part == null) imagePath else part.imagePath).orEmpty(),
    chatId = chatId,
    msgId = msgId,
    elementId = part?.elementId ?: elementId,
)

internal fun buildChatImageGallery(
    messages: List<ChatMessage>,
    clickedMessage: ChatMessage,
    clickedPartIndex: Int? = null,
): ChatImageGallery? {
    val clickedPart = clickedPartIndex?.let { clickedMessage.parts[it] }
    val clicked = clickedMessage.imageRoute(clickedPart)
    if (clicked.imagePath.isBlank()) return null
    val single = ChatImageGallery(listOf(clicked), 0)
    // 表情维持原来的单图预览，不混入会话照片。
    if (clickedPart?.emojiImage ?: clickedMessage.isEmojiImage) return single

    val pages = mutableListOf<Route.ChatImage>()
    var initialPage = -1
    for (message in messages) {
        fun addImage(partIndex: Int?, part: ChatPart?) {
            val selected = message.id == clickedMessage.id && partIndex == clickedPartIndex
            val image = if (selected) clicked else message.imageRoute(part)
            if (image.imagePath.isBlank()) return
            if (selected) initialPage = pages.size
            pages.add(image)
        }
        if (message.parts.isNotEmpty()) {
            message.parts.forEachIndexed { index, part ->
                if (part.kind == ChatPartKind.IMAGE && !part.emojiImage) addImage(index, part)
            }
        } else if (message.type == ChatMessageType.IMAGE && !message.isEmojiImage) {
            addImage(null, null)
        }
    }
    return if (initialPage >= 0) ChatImageGallery(pages, initialPage) else single
}

@Composable
internal fun openChatImage(message: ChatMessage): (partIndex: Int?) -> Unit {
    val navigator = LocalNavigator.current
    val messages = LocalChatImageMessages.current
    return { index ->
        buildChatImageGallery(messages, message, index)?.let { gallery ->
            navigator.pushChatImage(gallery)
        }
    }
}
