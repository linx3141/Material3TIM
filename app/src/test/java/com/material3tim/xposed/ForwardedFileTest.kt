package com.material3tim.xposed

import com.material3tim.data.model.ForwardedFile
import com.material3tim.data.model.ForwardedFilePeer
import com.material3tim.data.model.ForwardedFileTarget
import com.material3tim.data.model.forwardedFileTargets
import com.material3tim.data.model.forwardedMessageRowId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForwardedFileTest {
    private val outer = ForwardedFilePeer("outer", 2)
    private val original = ForwardedFilePeer("original", 1)
    private val file = ForwardedFile("chat", 200L, 21L, 150L, 100L, 400L, 41L, forwardedMessageRowId(3, 400L))

    @Test
    fun nestedAttachmentUsesOutermostRecordAndItsConversationFirst() {
        assertEquals(
            listOf(
                ForwardedFileTarget(100L, outer),
                ForwardedFileTarget(200L, outer),
                ForwardedFileTarget(150L, outer),
                ForwardedFileTarget(400L, original),
            ),
            forwardedFileTargets(file, original, outer),
        )
    }

    @Test
    fun attachmentWithoutInnerMessageIdStillUsesOuterRecordWithoutDuplicates() {
        assertEquals(
            listOf(ForwardedFileTarget(200L, outer)),
            forwardedFileTargets(file.copy(messageId = 0L, rootMsgId = 200L, parentMsgId = 0L), original, outer),
        )
    }

    @Test
    fun matchesTheRecordRowEvenWhenMultipleRowsShareAMessageId() {
        assertEquals(3, file.rowIndex)
        assertEquals(4, file.copy(rowId = forwardedMessageRowId(4, 400L)).rowIndex)
        assertNull(file.copy(messageId = 401L).rowIndex)
        assertNull(file.copy(rowId = "tim_400").rowIndex)
    }
}
