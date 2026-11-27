package com.material3tim.data.model

/** 合并转发的发言人是内层消息的作者，不是外层会话的联系人。 */
internal fun forwardedSenderName(
    nickname: String?,
    memberName: String?,
    remark: String?,
    uin: Long,
    unknown: String,
): String = sequenceOf(nickname, memberName, remark)
    .filterNotNull()
    .map(String::trim)
    .firstOrNull(String::isNotEmpty)
    ?: uin.takeIf { it > 0L }?.toString()
    ?: unknown

/** 只能用共同存在的稳定标识确认同一个人；转发记录不按同名猜测。 */
internal fun sameKnownSender(first: ChatMessage, second: ChatMessage): Boolean? = when {
    first.senderUid.isNotBlank() && second.senderUid.isNotBlank() -> first.senderUid == second.senderUid
    first.senderUin.isNotBlank() && second.senderUin.isNotBlank() -> first.senderUin == second.senderUin
    else -> null
}
