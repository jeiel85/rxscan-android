package io.github.jeiel85.rxscan.core.model

/** Dosing timings (03_OCR_PIPELINE.md §8). */
enum class Timing { MORNING, LUNCH, EVENING, BEDTIME, AS_NEEDED }

/** Relation of dosing to meals. */
enum class MealRelation { BEFORE, WITH, AFTER }

/** Outcome of parsing one photographed direction line. */
enum class DirectionStatus {
    PARSED,
    /** Contradictory evidence in the same row — requires user correction (no guess). */
    CONFLICT,
    /** Nothing structured could be extracted. */
    EMPTY,
}

/**
 * Structured directions parsed from the photographed prescription line
 * (03_OCR_PIPELINE.md §8). This is the *photographed* instruction, kept strictly
 * separate from official approved use (08_UX_SPEC.md §3, Screen E). Missing fields
 * stay null; nothing is inferred from a drug database.
 *
 * [rawText] is always preserved so the UI can show exactly what was on the bag.
 */
data class DirectionParse(
    val rawText: String,
    val status: DirectionStatus,
    val doseAmount: Double? = null,
    val doseUnit: String? = null,
    val frequencyPerDay: Int? = null,
    val timings: Set<Timing> = emptySet(),
    val mealRelation: MealRelation? = null,
    val mealOffsetMinutes: Int? = null,
    val durationDays: Int? = null,
    val route: Route = Route.UNKNOWN,
    val specialInstructionRaw: String? = null,
    val conflicts: List<String> = emptyList(),
)
