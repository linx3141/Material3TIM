package com.material3tim.ui.screen.chat

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.model.sameKnownSender

/**
 * 连续消息分组的位置。圆角由 [chatBubbleShape] 按“对齐边小圆角、参差边大圆角”的规则计算：
 * 单条 → 四角 24dp 胶囊；组内相邻角只在头像/对齐边一侧收小为 6dp。
 */
internal enum class ChatBubblePosition {
    Single,
    First,
    Middle,
    Last,
}

/** 一条消息在分组中的展示信息。 */
@Immutable
internal data class ChatBubbleGroup(
    val position: ChatBubblePosition,
    /** 与视觉上一条消息之间的间距（分组内 2dp，分组之间 / 分隔条 / 系统消息 10dp）。 */
    val spacingBefore: Dp,
) {
    /** 是否为分组的首条（用于显示发送者昵称）。 */
    val isGroupStart: Boolean
        get() = position == ChatBubblePosition.First || position == ChatBubblePosition.Single

    /** 是否为分组的末条（用于显示头像、时间、已读状态）。 */
    val isGroupEnd: Boolean
        get() = position == ChatBubblePosition.Last || position == ChatBubblePosition.Single
}

/** 一条消息 + 它的分组信息。 */
@Immutable
internal data class GroupedChatMessage(
    val message: ChatMessage,
    val group: ChatBubbleGroup,
)

/** 同一发送者的连续消息只有时间间隔小于 5 分钟才保持在同一组。 */
internal const val ChatGroupGapMinutes = 5

private val GroupSpacing = 10.dp
private val InGroupSpacing = 2.dp

private val ChatTimeRegex = Regex("""^(\d{1,2}):(\d{2})$""")

/**
 * 把按时间正序排列的消息列表切分成视觉分组，并算好每条气泡的形状位置和间距。
 *
 * 分组规则（任一条件成立即开启新组）：
 * 1. 发送者变化（优先比较 UID/QQ 号；仅普通聊天允许无标识时按昵称回退）；
 * 2. 当前或上一条是 [ChatMessageType.SYSTEM]；
 * 3. 当前消息带时间分隔条（`timeSeparator != null`）；
 * 4. 两条消息的 "HH:mm" 时间差 >= 5 分钟（跨天 / 无法解析时按解析结果决定：
 *    无法解析时不在时间维度上拆组）。
 */
internal fun groupChatMessages(
    messages: List<ChatMessage>,
    requireSenderId: Boolean = false,
): List<GroupedChatMessage> {
    if (messages.isEmpty()) return emptyList()

    val startsNewGroup = BooleanArray(messages.size) { true }
    for (index in 1 until messages.size) {
        startsNewGroup[index] = isNewGroupBetween(messages[index - 1], messages[index], requireSenderId)
    }

    val result = ArrayList<GroupedChatMessage>(messages.size)
    var start = 0
    while (start < messages.size) {
        var end = start
        while (end + 1 < messages.size && !startsNewGroup[end + 1]) {
            end += 1
        }
        val size = end - start + 1
        for (index in start..end) {
            val position = when {
                size == 1 -> ChatBubblePosition.Single
                index == start -> ChatBubblePosition.First
                index == end -> ChatBubblePosition.Last
                else -> ChatBubblePosition.Middle
            }
            // 整个列表最上面的消息由 LazyColumn 的 contentPadding 提供间距。
            val spacingBefore = when {
                index == 0 -> 0.dp
                position == ChatBubblePosition.First || position == ChatBubblePosition.Single -> GroupSpacing
                else -> InGroupSpacing
            }
            result += GroupedChatMessage(
                message = messages[index],
                group = ChatBubbleGroup(position = position, spacingBefore = spacingBefore),
            )
        }
        start = end + 1
    }
    return result
}

/**
 * 分组感知的气泡圆角。
 *
 * 规则（按用户要求修正）：
 * - 单条消息：四角都是胶囊大圆角（24dp）；
 * - 同一发送者的连续消息：只在**贴近头像/屏幕对齐边的那一侧**、且**与相邻气泡相接的角**收成小圆角（6dp），
 *   另一侧（气泡长度参差的一侧）始终保持胶囊大圆角，这样不同长度的气泡也能对齐好看。
 *   - 自己发送（右对齐、头像在右）：topEnd / bottomEnd 视分组位置收小；
 *   - 对方发送（左对齐、头像在左）：topStart / bottomStart 视分组位置收小。
 */
internal fun chatBubbleShape(position: ChatBubblePosition, isMe: Boolean): RoundedCornerShape {
    if (position == ChatBubblePosition.Single) {
        return RoundedCornerShape(ChatBubbleCornerLarge)
    }
    val large = ChatBubbleCornerLarge
    val small = ChatBubbleCornerSmall
    val topJunction = position == ChatBubblePosition.Middle || position == ChatBubblePosition.Last
    val bottomJunction = position == ChatBubblePosition.First || position == ChatBubblePosition.Middle
    return if (isMe) {
        RoundedCornerShape(
            topStart = large,
            topEnd = if (topJunction) small else large,
            bottomEnd = if (bottomJunction) small else large,
            bottomStart = large,
        )
    } else {
        RoundedCornerShape(
            topStart = if (topJunction) small else large,
            topEnd = large,
            bottomEnd = large,
            bottomStart = if (bottomJunction) small else large,
        )
    }
}

internal val ChatBubbleCornerLarge = 24.dp

/**
 * Miuix 风格：气泡始终是胶囊形（四角 24dp），不做 M3 Expressive 的分段小圆角；
 * 消息分组仍然影响组内间距、头像/昵称/时间的显示位置。
 */
internal val ChatMiuixBubbleShape: RoundedCornerShape = RoundedCornerShape(ChatBubbleCornerLarge)
internal val ChatBubbleCornerSmall = 6.dp

/** 解析 "HH:mm" 为当天的分钟数；解析失败返回 null（此时不在时间维度上拆组）。 */
internal fun parseChatTimeMinutes(timeLabel: String): Int? {
    val match = ChatTimeRegex.matchEntire(timeLabel.trim()) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun isNewGroupBetween(previous: ChatMessage, current: ChatMessage, requireSenderId: Boolean): Boolean {
    if (previous.type == ChatMessageType.SYSTEM || current.type == ChatMessageType.SYSTEM) return true
    if (current.timeSeparator != null) return true
    if (previous.isMe != current.isMe) return true
    val sameSender = sameKnownSender(previous, current)
    if (sameSender == false || (requireSenderId && sameSender != true)) return true
    if (sameSender == null && !current.isMe && previous.senderName != current.senderName) return true

    val previousMinutes = parseChatTimeMinutes(previous.timeLabel) ?: return false
    val currentMinutes = parseChatTimeMinutes(current.timeLabel) ?: return false
    val gap = currentMinutes - previousMinutes
    // 负数说明跨天（只解析了时分），同样视为大于 5 分钟。
    return gap < 0 || gap >= ChatGroupGapMinutes
}
