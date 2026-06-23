package io.github.jeiel85.rxscan.engine.matcher

import io.github.jeiel85.rxscan.core.model.ConfidencePolicy
import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.MatchStatus
import io.github.jeiel85.rxscan.core.model.RecognizedDrugFields
import io.github.jeiel85.rxscan.core.model.ScoreWeights
import io.github.jeiel85.rxscan.core.model.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Synthetic holdout benchmark for the release matching gate (09_TEST_QUALITY.md §5,
 * Goal 08 acceptance #2). Enforces the safety invariant on a labeled synthetic
 * corpus: **no wrong VERIFIED/HIGH_CONFIDENCE selection**. Also checks top-3 recall
 * for ambiguous lines and that unresolved lines stay unresolved.
 *
 * This is a CI-scale synthetic stand-in. The real ≥3,000-line independent holdout
 * with ≥500 products and ≥30 templates remains a launch blocker pending a real
 * corpus (see docs/RELEASE_CANDIDATE_VALIDATION.md).
 */
class MatcherHoldoutBenchmarkTest {
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
        hardContradictions = emptyList(),
        inferMissingStrength = false,
        inferMissingDosageForm = false,
        inferMissingSuffix = false,
        inferFromEfficacy = false,
        currentMaxAgeDays = 14,
        warningMaxAgeDays = 30,
        disableCurrentDurClaimAfterDays = 30,
    )

    private data class HoldoutLine(
        val fields: RecognizedDrugFields,
        val expectedItemCode: String?,
        val expectHighConfidenceEligible: Boolean,
        val expectUnresolved: Boolean,
        val expectAmbiguous: Boolean,
    )

    @Test
    fun noWrongHighConfidenceSelectionInSyntheticHoldout() {
        val productCount = 120
        val records = mutableListOf<DrugRecord>()
        for (i in 0 until productCount) {
            records += record("ITEM-$i", "물질명$i", Strength(((i % 9) + 1) * 50.0, "mg"))
        }
        // Two non-unique duplicate-name products to drive ambiguity.
        records += record("DUP-1", "중복명", Strength(100.0, "mg"), unique = false)
        records += record("DUP-2", "중복명", Strength(100.0, "mg"), unique = false)

        val matcher = DrugMatcher(InMemoryDrugSearchRepository(records), policy)
        val holdout = buildHoldout(productCount)

        var highConfidenceCount = 0
        var ambiguousTop3Hits = 0
        var ambiguousTotal = 0
        for (line in holdout) {
            val result = matcher.match(line.fields)
            val highConfidence = result.status == MatchStatus.VERIFIED_IDENTIFIER ||
                result.status == MatchStatus.HIGH_CONFIDENCE_REVIEW

            if (highConfidence) {
                highConfidenceCount++
                // The safety invariant: any high-confidence selection must be correct.
                assertEquals(
                    "wrong high-confidence selection for ${line.fields.normalizedLine}",
                    line.expectedItemCode,
                    result.selectedCandidate?.record?.itemCode,
                )
                assertTrue("unexpected high-confidence", line.expectHighConfidenceEligible)
            }
            if (line.expectUnresolved) {
                assertEquals(MatchStatus.UNRESOLVED, result.status)
            }
            if (line.expectAmbiguous) {
                ambiguousTotal++
                val top3 = result.candidates.take(3).map { it.record.itemCode }
                if (line.expectedItemCode in top3) ambiguousTop3Hits++
            }
        }

        val top3Recall = if (ambiguousTotal == 0) 1.0 else ambiguousTop3Hits.toDouble() / ambiguousTotal
        println(
            "Holdout: products=$productCount lines=${holdout.size} " +
                "highConfidence=$highConfidenceCount ambiguousTop3Recall=${"%.3f".format(top3Recall)}",
        )
        assertTrue("high-confidence coverage too low", highConfidenceCount >= 50)
        assertTrue("top-3 recall below gate", top3Recall >= 0.995)
    }

    private fun buildHoldout(productCount: Int): List<HoldoutLine> {
        val lines = mutableListOf<HoldoutLine>()
        // Clean exact lines -> eligible for high confidence, must select the right product.
        for (i in 0 until 60) {
            val strength = Strength(((i % 9) + 1) * 50.0, "mg")
            lines += HoldoutLine(
                fields = fields("물질명$i", strength, "물질명$i ${strength.value.toInt()}mg 한국제약"),
                expectedItemCode = "ITEM-$i",
                expectHighConfidenceEligible = true,
                expectUnresolved = false,
                expectAmbiguous = false,
            )
        }
        // Conflicting-strength distractors -> must NOT be high confidence.
        for (i in 60 until 80) {
            val wrong = Strength(((i % 9) + 1) * 50.0 + 25.0, "mg")
            lines += HoldoutLine(
                fields = fields("물질명$i", wrong, "물질명$i ${wrong.value.toInt()}mg 한국제약"),
                expectedItemCode = null,
                expectHighConfidenceEligible = false,
                expectUnresolved = false,
                expectAmbiguous = false,
            )
        }
        // Ambiguous duplicate-name lines -> top-3 must contain a truth.
        for (i in 0 until 10) {
            lines += HoldoutLine(
                fields = fields("중복명", Strength(100.0, "mg"), "중복명 100mg"),
                expectedItemCode = "DUP-1",
                expectHighConfidenceEligible = false,
                expectUnresolved = false,
                expectAmbiguous = true,
            )
        }
        // Garbage lines -> unresolved.
        for (i in 0 until 20) {
            lines += HoldoutLine(
                fields = fields("@@@$i", null, "@@@$i ###"),
                expectedItemCode = null,
                expectHighConfidenceEligible = false,
                expectUnresolved = true,
                expectAmbiguous = false,
            )
        }
        return lines
    }

    private fun record(itemCode: String, name: String, strength: Strength, unique: Boolean = true) = DrugRecord(
        itemCode = itemCode,
        productName = name,
        productNameNormalized = name.lowercase().filterNot { it.isWhitespace() },
        manufacturer = "한국제약",
        manufacturerNormalized = "한국제약",
        strength = strength,
        dosageForm = DosageForm.TABLET,
        aliasesNormalized = listOf(name.lowercase().filterNot { it.isWhitespace() }),
        productNameUnique = unique,
    )

    private fun fields(name: String, strength: Strength?, line: String) = RecognizedDrugFields(
        rawLine = line,
        normalizedLine = line,
        productName = name,
        strength = strength,
        dosageForm = if (strength == null) DosageForm.UNKNOWN else DosageForm.TABLET,
        manufacturer = "한국제약",
    )
}
