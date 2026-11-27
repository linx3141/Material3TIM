package com.material3tim.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowSizeTest {
    @Test
    fun phoneReturnsToBottomNavigationWhenPortraitLayoutArrives() {
        assertTrue(shouldShowSplitPane(2340, 1080, density = 3f))
        assertFalse(shouldShowSplitPane(1080, 2340, density = 3f))
    }

    @Test
    fun orientationDoesNotDisableRailOnWideTablets() {
        assertTrue(shouldShowSplitPane(1800, 2400, density = 2f))
    }
}
