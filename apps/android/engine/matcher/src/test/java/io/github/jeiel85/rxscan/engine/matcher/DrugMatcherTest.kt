package io.github.jeiel85.rxscan.engine.matcher

import io.github.jeiel85.rxscan.core.model.ConfidencePolicy
import io.github.jeiel85.rxscan.core.model.ContradictionType
import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.MatchStatus
import io.github.jeiel85.rxscan.core.model.RecognizedDrugFields
import io.github.jeiel85.rxscan.core.model.ScoreWeights
import io.github.jeiel85.rxscan.core.model.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrugMatcherTest {
    private val policy = ConfidencePolicy(
        policyVersion = "1.0.0",
        mandatoryUserReview = true,
        identifierVerifiedEnabled = true,
        identifierRequireNoHardContradictions = true,
        highConfidenceMinScore = 0.92,
        highConfidenceMinTopTwoMargin = 0.12,
        highConfidenceRequireFields = listOf("product_name", "strength", "dosage_form"),
        highConfidenceRequireNoHardContradictions = true,
        highConfidenceDisallowWhenOverridden = true,
        ambiguousMaxCandidatesShown = 5,
        weights = ScoreWeights(0.50, 0.20, 0.12, 0.08, 0.05, 0.05),
        hardContradictions = listOf("strength_conflict"),
        inferMissingStrength = false,
        inferMissingDosageForm = false,
        inferMissingSuffix = false,
        inferFromEfficacy = false,
        currentMaxAgeDays = 14,
        warningMaxAgeDays = 30,
        disableCurrentDurClaimAfterDays = 30,
    )

    private fun record(
        itemCode: String,
        name: String,
        strength: Strength? = Strength(500.0, "mg"),
        form: DosageForm = DosageForm.TABLET,
        manufacturer: String? = "한국얀센",
        unique: Boolean = true,
    ) = DrugRecord(
        itemCode = itemCode,
        productName = name,
        productNameNormalized = name.lowercase().filterNot { it.isWhitespace() },
        manufacturer = manufacturer,
        manufacturerNormalized = manufacturer?.lowercase()?.filterNot { it.isWhitespace() },
        strength = strength,
        dosageForm = form,
        ingredientNames = listOf("아세트아미노펜"),
        aliasesNormalized = listOf(name.lowercase().filterNot { it.isWhitespace() }),
        productNameUnique = unique,
    )

    private fun fields(
        name: String? = "타이레놀정",
        strength: Strength? = Strength(500.0, "mg"),
        form: DosageForm = DosageForm.TABLET,
        manufacturer: String? = "한국얀센",
        itemCode: String? = null,
        overridden: Boolean = false,
        line: String = "타이레놀정 500mg 한국얀센",
    ) = RecognizedDrugFields(
        rawLine = line,
        normalizedLine = line,
        itemCode = itemCode,
        productName = name,
        strength = strength,
        dosageForm = form,
        manufacturer = manufacturer,
        captureOverridden = overridden,
    )

    private fun matcher(vararg records: DrugRecord) =
        DrugMatcher(InMemoryDrugSearchRepository(records.toList()), policy)

    @Test
    fun exactIdentifierProducesVerifiedState() {
        val result = matcher(record("ITEM-1", "타이레놀정")).match(fields(itemCode = "ITEM-1"))
        assertEquals(MatchStatus.VERIFIED_IDENTIFIER, result.status)
        assertEquals("ITEM-1", result.selectedCandidate!!.record.itemCode)
        assertEquals("20260101-1", result.publicDbVersion)
        assertEquals("1.0.0", result.policyVersion)
    }

    @Test
    fun exactNameStrengthFormProducesHighConfidence() {
        val result = matcher(record("ITEM-1", "타이레놀정")).match(fields())
        assertEquals(MatchStatus.HIGH_CONFIDENCE_REVIEW, result.status)
        assertEquals("ITEM-1", result.selectedCandidate!!.record.itemCode)
        assertTrue(result.selectedCandidate!!.score >= policy.highConfidenceMinScore)
    }

    @Test
    fun strengthConflictNeverReachesVerifiedOrHighConfidence() {
        val result = matcher(record("ITEM-1", "타이레놀정", strength = Strength(250.0, "mg")))
            .match(fields(itemCode = "ITEM-1", strength = Strength(500.0, "mg")))
        assertNotEquals(MatchStatus.VERIFIED_IDENTIFIER, result.status)
        assertNotEquals(MatchStatus.HIGH_CONFIDENCE_REVIEW, result.status)
        assertTrue(result.hardRejections.any { it.type == ContradictionType.STRENGTH_CONFLICT })
        assertNull(result.selectedCandidate)
    }

    @Test
    fun perfectNameSimilarityCannotOverrideStrengthContradiction() {
        // Identical name (similarity 1.0) but conflicting strength must stay rejected.
        val result = matcher(record("ITEM-1", "타이레놀정", strength = Strength(250.0, "mg")))
            .match(fields(name = "타이레놀정", strength = Strength(500.0, "mg")))
        assertNotEquals(MatchStatus.HIGH_CONFIDENCE_REVIEW, result.status)
        assertTrue(result.candidates.none { it.record.itemCode == "ITEM-1" })
    }

    @Test
    fun dosageFormConflictBlocksHighConfidence() {
        val result = matcher(record("ITEM-1", "타이레놀정", form = DosageForm.CAPSULE))
            .match(fields(form = DosageForm.TABLET))
        assertNotEquals(MatchStatus.HIGH_CONFIDENCE_REVIEW, result.status)
        assertTrue(result.hardRejections.any { it.type == ContradictionType.DOSAGE_FORM_CONFLICT })
    }

    @Test
    fun missingStrengthIsNeverAutoFilledIntoHighConfidence() {
        val result = matcher(record("ITEM-1", "타이레놀정")).match(fields(strength = null))
        assertEquals(MatchStatus.AMBIGUOUS, result.status)
        assertNull(result.selectedCandidate)
    }

    @Test
    fun multiplePlausibleCandidatesAreAmbiguous() {
        val result = matcher(
            record("ITEM-1", "비슷한약"),
            record("ITEM-2", "비슷한약"),
        ).match(fields(name = "비슷한약", line = "비슷한약 500mg"))
        assertEquals(MatchStatus.AMBIGUOUS, result.status)
        assertNull(result.selectedCandidate)
        assertTrue(result.candidates.size >= 2)
    }

    @Test
    fun noCandidateIsUnresolved() {
        val result = matcher(record("ITEM-1", "타이레놀정")).match(fields(name = "존재하지않는약", line = "존재하지않는약"))
        assertEquals(MatchStatus.UNRESOLVED, result.status)
        assertNull(result.selectedCandidate)
    }

    @Test
    fun overriddenCaptureCannotAutoResolveToHighConfidence() {
        val result = matcher(record("ITEM-1", "타이레놀정")).match(fields(overridden = true))
        assertNotEquals(MatchStatus.HIGH_CONFIDENCE_REVIEW, result.status)
    }

    @Test
    fun malformedInputDoesNotCrashOrProduceHighConfidence() {
        val result = matcher(record("ITEM-1", "타이레놀정"))
            .match(fields(name = "", strength = null, form = DosageForm.UNKNOWN, line = "@@@###"))
        assertTrue(result.status == MatchStatus.UNRESOLVED || result.status == MatchStatus.AMBIGUOUS)
        assertNull(result.selectedCandidate)
    }

    @Test
    fun sameInputsProduceIdenticalResult() {
        val m = matcher(record("ITEM-1", "타이레놀정"))
        assertEquals(m.match(fields()), m.match(fields()))
    }
}
