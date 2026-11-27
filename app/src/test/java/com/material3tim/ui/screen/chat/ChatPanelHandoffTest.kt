package com.material3tim.ui.screen.chat

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPanelHandoffTest {
    private val composer = ChatComposerState()
    private val keyboard = mutableStateOf(304.dp)
    private val nav = mutableStateOf(24.dp)
    private val handoff = ChatPanelHandoff(composer, keyboard, nav)

    // Mirror the screen's bottom padding + panel measurement + IME placement offset.
    private fun inputBottom() = handoff.panelSpace + maxOf(nav.value, handoff.remainingImeInset.value)

    @Test fun keyboardToEitherPanelKeepsInputBottomEvenAsInsetsChange() {
        for (target in listOf(ChatPanel.Emoji, ChatPanel.Attach)) {
            keyboard.value = 304.dp
            val before = inputBottom()
            handoff.captureKeyboard()
            composer.togglePanel(target)
            for (height in listOf(304, 248, 100, 24, 0)) {
                keyboard.value = height.dp
                assertEquals(before, inputBottom())
            }
            nav.value = 16.dp
            composer.togglePanel(if (target == ChatPanel.Emoji) ChatPanel.Attach else ChatPanel.Emoji)
            assertEquals(before, inputBottom())
            composer.closePanels()
            assertEquals(nav.value, inputBottom())
        }
    }

    @Test fun restoringKeyboardDoesNotCollapseOrDoubleCountTheSlot() {
        handoff.captureKeyboard()
        composer.togglePanel(ChatPanel.Attach)
        keyboard.value = 0.dp
        handoff.returnToKeyboard()
        composer.closePanels()
        for (height in listOf(0, 120, 240, 304)) {
            keyboard.value = height.dp
            assertEquals(304.dp, inputBottom())
        }
        assertTrue(handoff.keyboardTookOver)
        handoff.finishReturn()
        assertEquals(304.dp, inputBottom())
        keyboard.value = 0.dp
        assertEquals(nav.value, inputBottom())
    }

    @Test fun coldOpenDoesNotReuseHeightFromAnEarlierKeyboard() {
        handoff.captureKeyboard()
        composer.togglePanel(ChatPanel.Emoji)
        keyboard.value = 0.dp
        composer.closePanels()
        handoff.captureKeyboard()
        composer.togglePanel(ChatPanel.Attach)
        assertEquals(0.dp, handoff.panelSpace)
        handoff.returnToKeyboard()
        assertFalse(handoff.returningToIme)
    }
}
