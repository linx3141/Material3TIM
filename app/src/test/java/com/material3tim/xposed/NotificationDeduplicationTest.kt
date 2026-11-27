package com.material3tim.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeduplicationTest {
    @Test
    fun suppressesRepeatedCallbacksAndCorrelatesSequenceWithLateMessageId() {
        val seen = NotificationDeduplication()
        assertTrue(seen.accept("a", "chat", msgId = 0L, seq = 20L))
        assertFalse(seen.accept("a", "chat", msgId = 40L, seq = 20L))
        assertFalse(seen.accept("a", "chatAlias", msgId = 40L, seq = 0L))
        assertTrue(seen.accept("a", "chat", msgId = 41L, seq = 21L))
    }

    @Test
    fun independentConversationsCanReuseSequenceNumbersAndAccountsCanReuseIds() {
        val seen = NotificationDeduplication()
        assertTrue(seen.accept("a", "one", 11L, 1L))
        assertTrue(seen.accept("a", "two", 12L, 1L))
        assertTrue(seen.accept("b", "one", 11L, 1L))
        assertFalse(seen.accept("a", "one", 0L, 0L))
    }

    @Test
    fun evictsOnlyOldestEntryWhenAtCapacity() {
        val seen = NotificationDeduplication(capacity = 2)
        assertTrue(seen.accept("a", "chat", 1L, 0L))
        assertTrue(seen.accept("a", "chat", 2L, 0L))
        assertTrue(seen.accept("a", "chat", 3L, 0L))
        assertFalse(seen.accept("a", "chat", 2L, 0L))
        assertFalse(seen.accept("a", "chat", 3L, 0L))
        assertTrue(seen.accept("a", "chat", 1L, 0L))
    }
}
