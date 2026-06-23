package io.github.jeiel85.rxscan.engine.parser

import io.github.jeiel85.rxscan.core.model.DirectionParse
import io.github.jeiel85.rxscan.core.model.DirectionStatus
import io.github.jeiel85.rxscan.core.model.MealRelation
import io.github.jeiel85.rxscan.core.model.Timing

/**
 * Deterministic direction grammar (03_OCR_PIPELINE.md §8). Extracts dose amount,
 * per-day frequency, timings, meal relation/offset, and duration from a
 * photographed direction line. Contradictory evidence in one row returns
 * [DirectionStatus.CONFLICT] — the app asks the user to correct it rather than
 * guessing (AGENTS.md). Raw text is always preserved.
 */
object DirectionParser {
    private val doseAmount = Regex("""(\d+(?:\.\d+)?)\s*(정|캡슐|포|알|방울|mL|ml|병|스푼)""")
    private val perDay = Regex("""(?:1일|하루)\s*(\d+)\s*(?:회|번)""")
    private val perDayAlt = Regex("""(\d+)\s*(?:회|번)\s*/\s*(?:일|day)""", RegexOption.IGNORE_CASE)
    private val durationDays = Regex("""(\d+)\s*일\s*(?:분|치)""")
    private val mealOffset = Regex("""식(?:후|전)\s*(\d+)\s*분""")

    fun parse(rawText: String): DirectionParse {
        val text = rawText.trim()
        if (text.isEmpty()) return DirectionParse(rawText = rawText, status = DirectionStatus.EMPTY)

        val conflicts = mutableListOf<String>()

        val frequencies = (perDay.findAll(text) + perDayAlt.findAll(text))
            .map { it.groupValues[1].toInt() }
            .toSet()
        if (frequencies.size > 1) {
            conflicts.add("frequencyPerDay 값이 충돌합니다: ${frequencies.sorted()}")
        }
        val frequencyPerDay = frequencies.singleOrNull() ?: frequencies.minOrNull()

        val before = text.contains("식전")
        val after = text.contains("식후")
        if (before && after) conflicts.add("식전/식후가 동시에 표기되었습니다")
        val mealRelation = when {
            after && !before -> MealRelation.AFTER
            before && !after -> MealRelation.BEFORE
            text.contains("식간") || text.contains("식사와 함께") -> MealRelation.WITH
            else -> null
        }

        val timings = buildSet {
            if (text.contains("아침")) add(Timing.MORNING)
            if (text.contains("점심")) add(Timing.LUNCH)
            if (text.contains("저녁")) add(Timing.EVENING)
            if (text.contains("자기전") || text.contains("취침전") || text.contains("취침 전")) add(Timing.BEDTIME)
            if (text.contains("필요시") || text.contains("필요할 때")) add(Timing.AS_NEEDED)
        }

        val doseMatch = doseAmount.find(text)
        val amount = doseMatch?.groupValues?.get(1)?.toDouble()
        val unit = doseMatch?.groupValues?.get(2)
        val offset = mealOffset.find(text)?.groupValues?.get(1)?.toInt()
        val duration = durationDays.find(text)?.groupValues?.get(1)?.toInt()

        val anyField = amount != null || frequencyPerDay != null || timings.isNotEmpty() ||
            mealRelation != null || offset != null || duration != null
        val status = when {
            conflicts.isNotEmpty() -> DirectionStatus.CONFLICT
            anyField -> DirectionStatus.PARSED
            else -> DirectionStatus.EMPTY
        }

        return DirectionParse(
            rawText = rawText,
            status = status,
            doseAmount = amount,
            doseUnit = unit,
            frequencyPerDay = frequencyPerDay,
            timings = timings,
            mealRelation = mealRelation,
            mealOffsetMinutes = offset,
            durationDays = duration,
            specialInstructionRaw = if (status == DirectionStatus.PARSED && !anyField) text else null,
            conflicts = conflicts,
        )
    }
}
