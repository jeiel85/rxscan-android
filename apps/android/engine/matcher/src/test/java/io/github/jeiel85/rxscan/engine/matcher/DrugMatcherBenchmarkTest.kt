package io.github.jeiel85.rxscan.engine.matcher

import io.github.jeiel85.rxscan.core.model.ConfidencePolicy
import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.RecognizedDrugFields
import io.github.jeiel85.rxscan.core.model.ScoreWeights
import io.github.jeiel85.rxscan.core.model.Strength
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Candidate-lookup performance benchmark (04_DRUG_MATCHING_ENGINE.md acceptance:
 * "performance benchmark reports p95 candidate lookup against representative
 * synthetic DB"). Reports p95 over an indexed synthetic DB; the bound is generous
 * so it documents performance without becoming a flaky CI gate.
 */
class DrugMatcherBenchmarkTest {
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

    @Test
    fun reportsP95CandidateLookup() {
        val dbSize = 5000
        val records = (0 until dbSize).map { i ->
            val name = "신약$i"
            DrugRecord(
                itemCode = "ITEM-$i",
                productName = name,
                productNameNormalized = name,
                manufacturer = "제약사${i % 50}",
                manufacturerNormalized = "제약사${i % 50}",
                strength = Strength((i % 10 + 1) * 10.0, "mg"),
                dosageForm = DosageForm.TABLET,
                aliasesNormalized = listOf(name),
            )
        }
        val matcher = DrugMatcher(InMemoryDrugSearchRepository(records), policy)

        val queries = 400
        val timingsMs = DoubleArray(queries)
        // Warm up JIT so the reported figures reflect steady state.
        repeat(50) { matcher.match(queryFor(it % dbSize)) }
        for (q in 0 until queries) {
            val target = (q * 7) % dbSize
            val start = System.nanoTime()
            matcher.match(queryFor(target))
            timingsMs[q] = (System.nanoTime() - start) / 1_000_000.0
        }

        timingsMs.sort()
        val p50 = timingsMs[(queries * 0.50).toInt()]
        val p95 = timingsMs[(queries * 0.95).toInt()]
        println("DrugMatcher candidate lookup over $dbSize records: p50=${"%.3f".format(p50)}ms p95=${"%.3f".format(p95)}ms")

        assertTrue("p95 lookup unexpectedly slow: ${p95}ms", p95 < 250.0)
    }

    private fun queryFor(i: Int): RecognizedDrugFields {
        val name = "신약$i"
        return RecognizedDrugFields(
            rawLine = "$name 50mg",
            normalizedLine = "$name 50mg",
            productName = name,
            strength = Strength(((i % 10) + 1) * 10.0, "mg"),
            dosageForm = DosageForm.TABLET,
            manufacturer = "제약사${i % 50}",
        )
    }
}
