package com.chris.studyspot

import com.chris.studyspot.data.model.BookingRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingRulesTest {
    @Test fun overlappingIntervalsConflict() {
        assertTrue(BookingRules.overlaps(10, 30, 20, 40))
        assertTrue(BookingRules.overlaps(10, 30, 5, 15))
    }

    @Test fun adjacentIntervalsDoNotConflict() {
        assertFalse(BookingRules.overlaps(10, 30, 30, 40))
        assertFalse(BookingRules.overlaps(10, 30, 0, 10))
    }

    @Test fun onlyWholeHoursFromOneThroughThreeAreValid() {
        assertTrue(BookingRules.validDuration(1_000, 3_601_000))
        assertTrue(BookingRules.validDuration(1_000, 7_201_000))
        assertTrue(BookingRules.validDuration(1_000, 10_801_000))
        assertFalse(BookingRules.validDuration(1_000, 1_000 + 1_800_000))
        assertFalse(BookingRules.validDuration(1_000, 1_000 + 14_400_000))
    }
}
