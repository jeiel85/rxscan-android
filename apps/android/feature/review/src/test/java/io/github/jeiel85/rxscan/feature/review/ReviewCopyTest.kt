package io.github.jeiel85.rxscan.feature.review

import io.github.jeiel85.rxscan.core.model.DirectionParse
import io.github.jeiel85.rxscan.core.model.DirectionStatus
import io.github.jeiel85.rxscan.core.model.MatchStatus
import io.github.jeiel85.rxscan.core.model.MealRelation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewCopyTest {
    @Test
    fun confidenceLabelsMatchSpecAndAvoidBannedWording() {
        assertEquals("코드가 일치했습니다", ReviewCopy.confidenceLabel(MatchStatus.VERIFIED_IDENTIFIER))
        assertEquals("비슷한 약이 여러 개입니다", ReviewCopy.confidenceLabel(MatchStatus.AMBIGUOUS))
        assertEquals("정확한 약을 확인하지 못했습니다", ReviewCopy.confidenceLabel(MatchStatus.UNRESOLVED))
        for (status in MatchStatus.entries) {
            val label = ReviewCopy.confidenceLabel(status)
            assertFalse(label.contains("AI"))
            assertFalse(label.contains("100%"))
            assertFalse(label.contains("안전"))
        }
    }

    @Test
    fun directionSummaryFormatsParsedFields() {
        val direction = DirectionParse(
            rawText = "1회 1정, 1일 3회, 식후 30분, 5일분",
            status = DirectionStatus.PARSED,
            doseAmount = 1.0,
            doseUnit = "정",
            frequencyPerDay = 3,
            mealRelation = MealRelation.AFTER,
            mealOffsetMinutes = 30,
            durationDays = 5,
        )
        val summary = ReviewCopy.directionSummary(direction)
        assertTrue(summary.contains("1일 3회"))
        assertTrue(summary.contains("식후"))
        assertTrue(summary.contains("5일분"))
    }

    @Test
    fun conflictDirectionIsSurfacedNotHidden() {
        val direction = DirectionParse(rawText = "1일 3회 1일 2회", status = DirectionStatus.CONFLICT)
        assertTrue(ReviewCopy.directionSummary(direction).contains("맞지 않습니다"))
    }
}
