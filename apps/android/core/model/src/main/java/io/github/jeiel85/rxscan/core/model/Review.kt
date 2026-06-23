package io.github.jeiel85.rxscan.core.model

/** Per-line review decision. Every line starts [UNREVIEWED] regardless of match status. */
enum class LineDecision { UNREVIEWED, CONFIRMED, REJECTED, UNRESOLVED }

/**
 * One medication line in the mandatory review (08_UX_SPEC.md §3 Screen D,
 * 04_DRUG_MATCHING_ENGINE.md §6). The matcher's selected candidate is only a
 * *suggestion*; the decision stays [LineDecision.UNREVIEWED] until the user acts,
 * so an ambiguous candidate is never pre-labeled as confirmed and no line is
 * auto-finalized.
 *
 * The photographed direction is kept separate from official information and is
 * never replaced by approved general use.
 */
data class MedicationLineReview(
    val lineId: String,
    val match: DrugMatchResult,
    val photographedDirection: DirectionParse,
    val decision: LineDecision = LineDecision.UNREVIEWED,
    val confirmedItemCode: String? = null,
    val userEditedFields: RecognizedDrugFields? = null,
) {
    val candidateItemCodes: List<String> get() = match.candidates.map { it.record.itemCode }

    /**
     * Confirm an explicit official candidate. Throws when [itemCode] is not one of
     * the retrieved candidates — an ambiguous or unresolved line cannot be silently
     * confirmed.
     */
    fun confirm(itemCode: String): MedicationLineReview {
        require(itemCode in candidateItemCodes) { "cannot confirm a non-candidate item: $itemCode" }
        return copy(decision = LineDecision.CONFIRMED, confirmedItemCode = itemCode)
    }

    fun reject(): MedicationLineReview = copy(decision = LineDecision.REJECTED, confirmedItemCode = null)

    fun markUnresolved(): MedicationLineReview =
        copy(decision = LineDecision.UNRESOLVED, confirmedItemCode = null)

    /** Editing recognized fields re-opens the line for review. */
    fun edit(fields: RecognizedDrugFields): MedicationLineReview =
        copy(userEditedFields = fields, decision = LineDecision.UNREVIEWED, confirmedItemCode = null)
}

/** A finalized review: only reachable when every line has been reviewed. */
data class FinalizedReview(val lines: List<MedicationLineReview>)

/**
 * The mandatory review session. Finalization is blocked until every line has an
 * explicit decision (08_UX_SPEC.md acceptance: "no path bypasses review before
 * finalization"). Unresolved lines remain visibly unresolved.
 */
data class ReviewSession(val lines: List<MedicationLineReview>) {
    val confirmedLines: List<MedicationLineReview> get() = lines.filter { it.decision == LineDecision.CONFIRMED }
    val unresolvedLines: List<MedicationLineReview> get() = lines.filter { it.decision == LineDecision.UNRESOLVED }
    val pendingLines: List<MedicationLineReview> get() = lines.filter { it.decision == LineDecision.UNREVIEWED }

    fun canFinalize(): Boolean = lines.isNotEmpty() && pendingLines.isEmpty()

    fun updateLine(lineId: String, transform: (MedicationLineReview) -> MedicationLineReview): ReviewSession =
        copy(lines = lines.map { if (it.lineId == lineId) transform(it) else it })

    /** @throws IllegalStateException if any line is still unreviewed. */
    fun finalize(): FinalizedReview {
        check(canFinalize()) { "review incomplete: ${pendingLines.size} line(s) not reviewed" }
        return FinalizedReview(lines)
    }
}
