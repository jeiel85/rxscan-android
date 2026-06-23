package io.github.jeiel85.rxscan.engine.parser

import io.github.jeiel85.rxscan.core.model.DirectionStatus
import io.github.jeiel85.rxscan.core.model.MealRelation
import io.github.jeiel85.rxscan.core.model.Timing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionParserTest {
    @Test
    fun parsesCanonicalDirectionExample() {
        val result = DirectionParser.parse("1회 1정, 1일 3회, 식후 30분, 5일분")
        assertEquals(DirectionStatus.PARSED, result.status)
        assertEquals(1.0, result.doseAmount!!, 1e-9)
        assertEquals("정", result.doseUnit)
        assertEquals(3, result.frequencyPerDay)
        assertEquals(MealRelation.AFTER, result.mealRelation)
        assertEquals(30, result.mealOffsetMinutes)
        assertEquals(5, result.durationDays)
    }

    @Test
    fun parsesTimings() {
        val result = DirectionParser.parse("아침 저녁 식후, 자기전 1정")
        assertTrue(result.timings.contains(Timing.MORNING))
        assertTrue(result.timings.contains(Timing.EVENING))
        assertTrue(result.timings.contains(Timing.BEDTIME))
    }

    @Test
    fun conflictingFrequenciesReturnConflict() {
        val result = DirectionParser.parse("1일 3회 그리고 1일 2회")
        assertEquals(DirectionStatus.CONFLICT, result.status)
        assertTrue(result.conflicts.isNotEmpty())
    }

    @Test
    fun conflictingMealRelationReturnsConflict() {
        val result = DirectionParser.parse("식전 식후 복용")
        assertEquals(DirectionStatus.CONFLICT, result.status)
    }

    @Test
    fun emptyTextIsEmptyStatus() {
        val result = DirectionParser.parse("   ")
        assertEquals(DirectionStatus.EMPTY, result.status)
        assertNull(result.frequencyPerDay)
    }

    @Test
    fun missingFieldsStayNull() {
        val result = DirectionParser.parse("1일 2회")
        assertEquals(2, result.frequencyPerDay)
        assertNull(result.durationDays)
        assertNull(result.mealRelation)
        assertNull(result.doseAmount)
    }
}
