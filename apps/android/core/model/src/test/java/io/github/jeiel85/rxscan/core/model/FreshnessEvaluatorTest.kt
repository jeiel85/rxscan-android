package io.github.jeiel85.rxscan.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshnessEvaluatorTest {
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

    @Test
    fun classifiesByAge() {
        assertEquals(Freshness.CURRENT, FreshnessEvaluator.evaluate(5, false, policy))
        assertEquals(Freshness.CURRENT, FreshnessEvaluator.evaluate(14, false, policy))
        assertEquals(Freshness.WARNING, FreshnessEvaluator.evaluate(20, false, policy))
        assertEquals(Freshness.STALE, FreshnessEvaluator.evaluate(40, false, policy))
    }

    @Test
    fun revokedAlwaysWins() {
        assertEquals(Freshness.REVOKED, FreshnessEvaluator.evaluate(1, true, policy))
    }

    @Test
    fun durCurrentClaimDisabledWhenStaleOrRevoked() {
        assertTrue(FreshnessEvaluator.durCurrentClaimAllowed(10, false, policy))
        assertFalse(FreshnessEvaluator.durCurrentClaimAllowed(40, false, policy))
        assertFalse(FreshnessEvaluator.durCurrentClaimAllowed(10, true, policy))
    }
}
