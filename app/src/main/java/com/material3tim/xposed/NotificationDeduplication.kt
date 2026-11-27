package com.material3tim.xposed

/** TIM 同一消息可能由多个回调上报，较早的记录还可能没有内核 msgId。 */
internal class NotificationDeduplication(private val capacity: Int = 4096) {
    private sealed interface Key {
        data class MessageId(val account: String, val id: Long) : Key
        data class Sequence(val account: String, val conversation: String, val seq: Long) : Key
    }

    private val recent = LinkedHashMap<Key, Unit>(32, 0.75f, true)

    @Synchronized
    fun accept(account: String, conversation: String, msgId: Long, seq: Long): Boolean {
        val idKey = msgId.takeIf { it != 0L }?.let { Key.MessageId(account, it) }
        val seqKey = seq.takeIf { it > 0L }?.let { Key.Sequence(account, conversation, it) }
        if (idKey == null && seqKey == null) return false
        // 即使已见过其中一个标识，也记录本次补齐的另一个，便于下一种回调命中。
        val seenId = idKey != null && recent.put(idKey, Unit) != null
        val seenSeq = seqKey != null && recent.put(seqKey, Unit) != null
        while (recent.size > capacity) {
            val oldest = recent.keys.iterator()
            oldest.next()
            oldest.remove()
        }
        return !seenId && !seenSeq
    }
}
