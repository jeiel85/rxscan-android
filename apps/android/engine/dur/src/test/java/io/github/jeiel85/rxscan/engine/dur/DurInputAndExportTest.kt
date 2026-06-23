package io.github.jeiel85.rxscan.engine.dur

import io.github.jeiel85.rxscan.core.model.ConfirmedMedicine
import io.github.jeiel85.rxscan.core.model.DirectionParse
import io.github.jeiel85.rxscan.core.model.DirectionStatus
import io.github.jeiel85.rxscan.core.model.DrugCandidateScore
import io.github.jeiel85.rxscan.core.model.DrugMatchResult
import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.DurEvaluation
import io.github.jeiel85.rxscan.core.model.DurStatus
import io.github.jeiel85.rxscan.core.model.MatchStatus
import io.github.jeiel85.rxscan.core.model.MedicationLineReview
import io.github.jeiel85.rxscan.core.model.RecognizedDrugFields
import io.github.jeiel85.rxscan.core.model.RetrievalMethod
import io.github.jeiel85.rxscan.core.model.ReviewSession
import io.github.jeiel85.rxscan.core.model.ScoreComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DurInputAndExportTest {
    private fun line(id: String, candidateCodes: List<String>, decisionCode: String?) = MedicationLineReview(
        lineId = id,
        match = DrugMatchResult(
            status = MatchStatus.AMBIGUOUS,
            selectedCandidate = null,
            candidates = candidateCodes.map {
                DrugCandidateScore(
                    record = DrugRecord(itemCode = it, productName = "약-$it", productNameNormalized = it),
                    retrieval = RetrievalMethod.FTS_NAME,
                    score = 0.5,
                    components = ScoreComponents(),
                )
            },
            recognizedFields = RecognizedDrugFields("line", "line"),
            hardRejections = emptyList(),
            policyVersion = "1.0.0",
            publicDbVersion = "20260101-1",
        ),
        photographedDirection = DirectionParse("1일 3회", DirectionStatus.PARSED, frequencyPerDay = 3),
    ).let { base -> if (decisionCode != null) base.confirm(decisionCode) else base }

    @Test
    fun onlyConfirmedLinesEnterDurInput() {
        val session = ReviewSession(
            listOf(
                line("1", listOf("ITEM-1"), decisionCode = "ITEM-1"), // confirmed
                line("2", listOf("ITEM-2"), decisionCode = null).reject(), // rejected
                line("3", listOf("ITEM-3"), decisionCode = null), // unreviewed
            ),
        )
        val meds = DurInput.fromReview(session) { code -> listOf("INGR-${code.takeLast(1)}") }
        assertEquals(listOf("ITEM-1"), meds.map { it.itemCode })
    }

    @Test
    fun unresolvedIngredientsLeaveMedicineUnresolved() {
        val session = ReviewSession(listOf(line("1", listOf("ITEM-1"), decisionCode = "ITEM-1")))
        val meds = DurInput.fromReview(session) { emptyList() }
        assertEquals(1, meds.size)
        assertFalse(meds.single().isResolved)
    }

    @Test
    fun exportContainsFixedWordingAndProvenance() {
        val evaluation = DurEvaluation(
            status = DurStatus.INSUFFICIENT_DATA,
            findings = emptyList(),
            evaluatedItemCodes = listOf("ITEM-1"),
            unresolvedItemCodes = listOf("ITEM-2"),
            currentClaimAllowed = true,
            publicDbVersion = "20260101-1",
            policyVersion = "1.0.0",
            notEvaluatedTypes = emptyList(),
        )
        val text = DurExport.toReviewText(
            evaluation,
            listOf(ConfirmedMedicine("ITEM-1", "약1", listOf("INGR-A"))),
        )
        assertTrue(text.contains("공식 안전사용 정보가 있습니다"))
        assertTrue(text.contains("20260101-1"))
        assertTrue(text.contains("처방을 대체하지 않습니다"))
        assertTrue(text.contains("상호작용이 없다는 의미가 아닙니다"))
    }
}
