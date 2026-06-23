package io.github.jeiel85.rxscan

import io.github.jeiel85.rxscan.core.model.DirectionParse
import io.github.jeiel85.rxscan.core.model.DirectionStatus
import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.DrugCandidateScore
import io.github.jeiel85.rxscan.core.model.DrugMatchResult
import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.DurEvaluation
import io.github.jeiel85.rxscan.core.model.DurFinding
import io.github.jeiel85.rxscan.core.model.DurRuleType
import io.github.jeiel85.rxscan.core.model.DurStatus
import io.github.jeiel85.rxscan.core.model.MatchStatus
import io.github.jeiel85.rxscan.core.model.MealRelation
import io.github.jeiel85.rxscan.core.model.MedicationLineReview
import io.github.jeiel85.rxscan.core.model.RecognizedDrugFields
import io.github.jeiel85.rxscan.core.model.RetrievalMethod
import io.github.jeiel85.rxscan.core.model.ReviewSession
import io.github.jeiel85.rxscan.core.model.ScoreComponents
import io.github.jeiel85.rxscan.core.model.Strength

/**
 * Synthetic review session for the engineering preview only (AGENTS.md: synthetic
 * data for development). Not produced from a real scan; it exercises the review UI
 * so the mandatory-review and unresolved states are visible.
 */
internal object SampleReview {
    fun session(): ReviewSession = ReviewSession(
        listOf(
            MedicationLineReview(
                lineId = "line-1",
                match = DrugMatchResult(
                    status = MatchStatus.HIGH_CONFIDENCE_REVIEW,
                    selectedCandidate = null,
                    candidates = listOf(
                        candidate("SYNTH-0001", "합성테스트정", 0.95),
                        candidate("SYNTH-0002", "합성테스트정에스", 0.80),
                    ),
                    recognizedFields = RecognizedDrugFields(
                        rawLine = "합성테스트정 500mg",
                        normalizedLine = "합성테스트정 500mg",
                        productName = "합성테스트정",
                        strength = Strength(500.0, "mg"),
                        dosageForm = DosageForm.TABLET,
                    ),
                    hardRejections = emptyList(),
                    policyVersion = "1.0.0",
                    publicDbVersion = "20260623-1",
                ),
                photographedDirection = DirectionParse(
                    rawText = "1회 1정, 1일 3회, 식후 30분, 5일분",
                    status = DirectionStatus.PARSED,
                    doseAmount = 1.0,
                    doseUnit = "정",
                    frequencyPerDay = 3,
                    mealRelation = MealRelation.AFTER,
                    mealOffsetMinutes = 30,
                    durationDays = 5,
                ),
            ),
            MedicationLineReview(
                lineId = "line-2",
                match = DrugMatchResult(
                    status = MatchStatus.UNRESOLVED,
                    selectedCandidate = null,
                    candidates = emptyList(),
                    recognizedFields = RecognizedDrugFields(
                        rawLine = "흐릿한약 ??mg",
                        normalizedLine = "흐릿한약",
                        productName = null,
                    ),
                    hardRejections = emptyList(),
                    policyVersion = "1.0.0",
                    publicDbVersion = "20260623-1",
                ),
                photographedDirection = DirectionParse(rawText = "1일 2회", status = DirectionStatus.PARSED, frequencyPerDay = 2),
            ),
        ),
    )

    /** Synthetic DUR result for the preview, shown after the synthetic review finalizes. */
    fun durEvaluation(): DurEvaluation = DurEvaluation(
        status = DurStatus.INSUFFICIENT_DATA,
        findings = listOf(
            DurFinding(
                type = DurRuleType.CO_ADMINISTRATION_CONTRAINDICATION,
                ruleId = "SYNTH-RULE-1",
                noticeDate = "2026-01-01",
                sourceId = "mfds_dur_ingredient",
                agency = "식품의약품안전처",
                involvedItemCodes = listOf("SYNTH-0001", "SYNTH-0003"),
                involvedProductNames = listOf("합성테스트정", "합성병용약"),
                publicDbVersion = "20260623-1",
            ),
        ),
        evaluatedItemCodes = listOf("SYNTH-0001"),
        unresolvedItemCodes = listOf("line-2"),
        currentClaimAllowed = true,
        publicDbVersion = "20260623-1",
        policyVersion = "1.0.0",
        notEvaluatedTypes = DurRuleType.entries.filterNot { it.pairwise },
    )

    private fun candidate(itemCode: String, name: String, score: Double) = DrugCandidateScore(
        record = DrugRecord(
            itemCode = itemCode,
            productName = name,
            productNameNormalized = name,
            strength = Strength(500.0, "mg"),
            dosageForm = DosageForm.TABLET,
        ),
        retrieval = RetrievalMethod.EXACT_NORMALIZED_NAME,
        score = score,
        components = ScoreComponents(productName = 0.5, strength = 0.2, dosageForm = 0.12),
    )
}
