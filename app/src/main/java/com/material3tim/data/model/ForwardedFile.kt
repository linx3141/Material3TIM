package com.material3tim.data.model

/** 用外层记录定位原始缓存，再用行位置和内层消息/元素 ID 精确定位附件。 */
data class ForwardedFile(
    val chatId: String,
    val recordMsgId: Long,
    val recordElementId: Long,
    val parentMsgId: Long,
    val rootMsgId: Long,
    val messageId: Long,
    val elementId: Long,
    val rowId: String,
) {
    internal val rowIndex: Int?
        get() = rowId.removePrefix("tim_record_").substringBefore('_').toIntOrNull()
            ?.takeIf { it >= 0 && rowId == forwardedMessageRowId(it, messageId) }
}

internal data class ForwardedFilePeer(val uid: String, val chatType: Int)
internal data class ForwardedFileTarget(val msgId: Long, val peer: ForwardedFilePeer)

internal fun forwardedMessageRowId(index: Int, messageId: Long): String = "tim_record_${index}_$messageId"

/** 原版 AIOMsgItem 优先 root MsgRecordParams。只有容器索引不可用才试内层原始消息。 */
internal fun forwardedFileTargets(
    file: ForwardedFile,
    original: ForwardedFilePeer?,
    outer: ForwardedFilePeer?,
): List<ForwardedFileTarget> {
    return buildList {
        if (outer != null && outer.uid.isNotBlank() && outer.chatType > 0) {
            listOf(file.rootMsgId, file.recordMsgId, file.parentMsgId).filter { it != 0L }.forEach {
                add(ForwardedFileTarget(it, outer))
            }
        }
        if (original != null && original.uid.isNotBlank() && original.chatType > 0 && file.messageId != 0L) {
            add(ForwardedFileTarget(file.messageId, original))
        }
    }.distinct()
}
