package com.avnixm.avdibook.ui

import com.avnixm.avdibook.ui.common.TimeFormatters
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkFormattingTest {
    @Test
    fun formatClock_supportsHourAndMinuteFormats() {
        assertEquals("0:09", TimeFormatters.formatClock(9_000))
        assertEquals("1:02:03", TimeFormatters.formatClock(3_723_000))
    }
}
