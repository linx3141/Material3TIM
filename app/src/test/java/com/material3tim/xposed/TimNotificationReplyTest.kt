package com.material3tim.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimNotificationReplyTest {
    @Test
    fun trimsOutsideWhitespaceAndPreservesUnicodeAndLineBreaks() {
        assertEquals("收到 👍\n明天见", normalizedNotificationReply(StringBuilder("  收到 👍\n明天见  ")))
    }

    @Test
    fun rejectsEmptyAndOversizeInputWithoutTruncating() {
        assertNull(normalizedNotificationReply(null))
        assertNull(normalizedNotificationReply(" \n\t "))
        val atLimit = "字".repeat(4000)
        assertEquals(atLimit, normalizedNotificationReply(atLimit))
        assertNull(normalizedNotificationReply(atLimit + "字"))
    }
}
