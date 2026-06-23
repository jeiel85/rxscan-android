package io.github.jeiel85.rxscan.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {
    private fun candidate(itemCode: String) = DrugCandidateScore(
        record = DrugRecord(itemCode = itemCode, productName = itemCode, productNameNormalized = itemCode),
        retrieval = RetrievalMethod.FTS_NAME,
        score = 0.5,
        components = ScoreComponents(),
    )

    private fun match(status: MatchStatus, candidates: List<String>) = DrugMatchResult(
        status = status,
        selectedCandidate = null,
        candidates = candidates.map { candidate(it) },
        recognizedFields = RecognizedDrugFields(rawLine = "line", normalizedLine = "line"),
        hardRejections = emptyList(),
        policyVersion = "1.0.0",
        publicDbVersion = "20260101-1",
    )

    private fun line(id: String, status: MatchStatus, candidates: List<String>) = MedicationLineReview(
        lineId = id,
        match = match(status, candidates),
        photographedDirection = DirectionParse(rawText = "1일 3회", status = DirectionStatus.PARSED, frequencyPerDay = 3),
    )

    @Test
    fun everyLineStartsUnreviewedEvenHighConfidence() {
        val l = line("1", MatchStatus.HIGH_CONFIDENCE_REVIEW, listOf("ITEM-1"))
        assertEquals(LineDecision.UNREVIEWED, l.decision)
    }

    @Test
    fun cannotFinalizeWhileAnyLineUnreviewed() {
        val session = ReviewSession(listOf(line("1", MatchStatus.HIGH_CONFIDENCE_REVIEW, listOf("ITEM-1"))))
        assertFalse(session.canFinalize())
        assertThrows(IllegalStateException::class.java) { session.finalize() }
    }

    @Test
    fun confirmingARealCandidateAllowsFinalize() {
        val session = ReviewSession(listOf(line("1", MatchStatus.HIGH_CONFIDENCE_REVIEW, listOf("ITEM-1"))))
            .updateLine("1") { it.confirm("ITEM-1") }
        assertTrue(session.canFinalize())
        assertEquals("ITEM-1", session.confirmedLines.single().confirmedItemCode)
        assertEquals(1, session.finalize().lines.size)
    }

    @Test
    fun ambiguousLineIsNotPreConfirmedAndNeedsExplicitChoice() {
        val l = line("1", MatchStatus.AMBIGUOUS, listOf("ITEM-1", "ITEM-2"))
        assertEquals(LineDecision.UNREVIEWED, l.decision)
        // Confirming requires picking a real candidate explicitly.
        assertEquals(LineDecision.CONFIRMED, l.confirm("ITEM-2").decision)
    }

    @Test
    fun confirmingANonCandidateThrows() {
        val l = line("1", MatchStatus.AMBIGUOUS, listOf("ITEM-1"))
        assertThrows(IllegalArgumentException::class.java) { l.confirm("ITEM-999") }
    }

    @Test
    fun unresolvedLineCannotBeConfirmedButCanBeMarkedUnresolved() {
        val l = line("1", MatchStatus.UNRESOLVED, emptyList())
        assertThrows(IllegalArgumentException::class.java) { l.confirm("anything") }
        val resolved = l.markUnresolved()
        assertEquals(LineDecision.UNRESOLVED, resolved.decision)

        val session = ReviewSession(listOf(resolved))
        assertTrue(session.canFinalize())
        assertEquals(1, session.unresolvedLines.size)
    }

    @Test
    fun editingReopensReview() {
        val confirmed = line("1", MatchStatus.HIGH_CONFIDENCE_REVIEW, listOf("ITEM-1")).confirm("ITEM-1")
        val edited = confirmed.edit(confirmed.match.recognizedFields.copy(productName = "수정"))
        assertEquals(LineDecision.UNREVIEWED, edited.decision)
    }

    @Test
    fun mixedSessionRequiresAllLinesReviewed() {
        var session = ReviewSession(
            listOf(
                line("1", MatchStatus.HIGH_CONFIDENCE_REVIEW, listOf("ITEM-1")),
                line("2", MatchStatus.UNRESOLVED, emptyList()),
            ),
        )
        session = session.updateLine("1") { it.confirm("ITEM-1") }
        assertFalse(session.canFinalize())
        session = session.updateLine("2") { it.markUnresolved() }
        assertTrue(session.canFinalize())
    }
}
