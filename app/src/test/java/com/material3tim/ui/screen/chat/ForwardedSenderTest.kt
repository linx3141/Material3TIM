package com.material3tim.ui.screen.chat

import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.forwardedSenderName
import org.junit.Assert.assertEquals
import org.junit.Test

class ForwardedSenderTest {
    @Test
    fun prefersOriginalNicknameAndNeverFallsBackToOuterConversationName() {
        assertEquals("作者", forwardedSenderName(" 作者 ", "群名片", "备注", 123L, "未知"))
        assertEquals("群名片", forwardedSenderName(" ", "群名片", null, 123L, "未知"))
        assertEquals("123", forwardedSenderName(null, null, null, 123L, "未知"))
        assertEquals("未知", forwardedSenderName(null, null, null, 0L, "未知"))
    }

    @Test
    fun distinctOrUnknownSendersWithSameNameNeverShareAnAvatarInForwardedRecords() {
        val first = message("one", uid = "u_one")
        val second = message("two", uid = "u_two")
        val third = message("three")
        val fourth = message("four")
        assertEquals(
            List(4) { ChatBubblePosition.Single },
            groupChatMessages(listOf(first, second, third, fourth), requireSenderId = true).map { it.group.position },
        )
    }

    @Test
    fun sameIdStillGroupsWhenDisplayNameChanges() {
        val first = message("one", uin = "123")
        val sameSender = message("two", uid = "u_one", uin = "123").copy(senderName = "新昵称")
        assertEquals(
            listOf(ChatBubblePosition.First, ChatBubblePosition.Last),
            groupChatMessages(listOf(first, sameSender), requireSenderId = true).map { it.group.position },
        )
    }

    private fun message(id: String, uid: String = "", uin: String = "") = ChatMessage(
        id = id,
        chatId = "forwarded",
        senderName = "同名",
        senderSeed = 1,
        timeLabel = "10:00",
        senderUid = uid,
        senderUin = uin,
    )
}
