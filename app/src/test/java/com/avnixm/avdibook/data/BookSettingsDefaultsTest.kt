package com.avnixm.avdibook.data

import com.avnixm.avdibook.data.model.BookDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BookSettingsDefaultsTest {
    @Test
    fun defaults_matchPhase2Profile() {
        assertEquals(1.0f, BookDefaults.SPEED)
        assertEquals(10, BookDefaults.SKIP_BACK_SEC)
        assertEquals(30, BookDefaults.SKIP_FORWARD_SEC)
        assertEquals(10, BookDefaults.AUTO_REWIND_SEC)
        assertEquals(180, BookDefaults.AUTO_REWIND_AFTER_PAUSE_SEC)
        assertFalse(BookDefaults.USE_LOUDNESS_BOOST)
    }
}
