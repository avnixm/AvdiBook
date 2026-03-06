package com.avnixm.avdibook.ui.design

import org.junit.Assert.assertEquals
import org.junit.Test

class AppWindowSizeTest {
    @Test
    fun appWindowSizeFromWidth_mapsBreakpoints() {
        assertEquals(AppWindowSize.COMPACT, appWindowSizeFromWidth(599))
        assertEquals(AppWindowSize.MEDIUM, appWindowSizeFromWidth(600))
        assertEquals(AppWindowSize.MEDIUM, appWindowSizeFromWidth(839))
        assertEquals(AppWindowSize.EXPANDED, appWindowSizeFromWidth(840))
    }
}
