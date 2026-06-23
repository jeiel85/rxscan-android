package io.github.jeiel85.rxscan.feature.review

import io.github.jeiel85.rxscan.core.model.DirectionParse
import io.github.jeiel85.rxscan.core.model.DirectionStatus
import io.github.jeiel85.rxscan.core.model.LineDecision
import io.github.jeiel85.rxscan.core.model.MatchStatus
import io.github.jeiel85.rxscan.core.model.MealRelation

/**
 * Fixed review-screen copy (08_UX_SPEC.md §3/§4). Confidence is stated in words
 * (not just color), and the photographed direction is summarized verbatim-derived
 * — never replaced with official approved use. Pure functions so wording is tested.
 */
object ReviewCopy {
    const val FINALIZE: String = "확인한 약으로 계속"
    const val DIRECTION_SECTION: String = "약봉지에 적힌 복용법"
    const val OFFICIAL_SECTION: String = "공식 의약품 정보"
    const val EDIT_HINT: String = "흐리거나 잘린 글자는 직접 확인해 주세요. 앱은 보이지 않는 내용을 추측하지 않습니다."

    /** Confidence wording from 08_UX_SPEC.md §4 — avoids "AI가 확정함", percentages, etc. */
    fun confidenceLabel(status: MatchStatus): String = when (status) {
        MatchStatus.VERIFIED_IDENTIFIER -> "코드가 일치했습니다"
        MatchStatus.HIGH_CONFIDENCE_REVIEW -> "이름·함량·제형이 일치합니다. 직접 확인해 주세요"
        MatchStatus.AMBIGUOUS -> "비슷한 약이 여러 개입니다"
        MatchStatus.UNRESOLVED -> "정확한 약을 확인하지 못했습니다"
    }

    fun decisionLabel(decision: LineDecision): String = when (decision) {
        LineDecision.UNREVIEWED -> "검토 전"
        LineDecision.CONFIRMED -> "확인함"
        LineDecision.REJECTED -> "아님"
        LineDecision.UNRESOLVED -> "확인 못함"
    }

    /** Human summary of the photographed direction; CONFLICT is surfaced, never hidden. */
    fun directionSummary(direction: DirectionParse): String {
        if (direction.status == DirectionStatus.CONFLICT) {
            return "복용법이 서로 맞지 않습니다. 직접 확인해 주세요."
        }
        if (direction.status == DirectionStatus.EMPTY) return "복용법을 읽지 못했습니다."
        val parts = buildList {
            direction.frequencyPerDay?.let { add("1일 ${it}회") }
            direction.doseAmount?.let { amount ->
                val unit = direction.doseUnit ?: ""
                add("1회 ${formatAmount(amount)}$unit")
            }
            when (direction.mealRelation) {
                MealRelation.AFTER -> add("식후")
                MealRelation.BEFORE -> add("식전")
                MealRelation.WITH -> add("식사와 함께")
                null -> {}
            }
            direction.mealOffsetMinutes?.let { add("${it}분") }
            direction.durationDays?.let { add("${it}일분") }
        }
        return if (parts.isEmpty()) direction.rawText else parts.joinToString(", ")
    }

    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
