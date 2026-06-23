package io.github.jeiel85.rxscan.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidencePolicyTest {
    // Mirrors config/confidence_policy.yaml. Kept in sync as the matcher's policy source.
    private val yaml = """
        policy_version: "1.0.0"

        decision:
          mandatory_user_review: true
          identifier_verified_state:
            enabled: true
            require_no_hard_contradictions: true

          high_confidence_review:
            minimum_score: 0.92
            minimum_top_two_margin: 0.12
            require_fields:
              - product_name
              - strength
              - dosage_form
            require_no_hard_contradictions: true
            disallow_when_capture_overridden: true

          ambiguous:
            maximum_candidates_shown: 5

        weights:
          product_name: 0.50
          strength: 0.20
          dosage_form: 0.12
          manufacturer: 0.08
          ingredient_context: 0.05
          ocr_layout_quality: 0.05

        hard_contradictions:
          - identifier_points_to_other_product
          - strength_conflict
          - dosage_form_conflict
          - release_form_conflict
          - route_conflict
          - non_unique_product_manufacturer_conflict
          - inactive_or_quarantined_record

        missing_field_behavior:
          infer_missing_strength: false
          infer_missing_dosage_form: false
          infer_missing_suffix: false
          infer_from_efficacy: false

        freshness:
          current_max_age_days: 14
          warning_max_age_days: 30
          disable_current_dur_claim_after_days: 30
    """.trimIndent()

    private val policy = ConfidencePolicy.parse(yaml)

    @Test
    fun parsesScalarsAndVersion() {
        assertEquals("1.0.0", policy.policyVersion)
        assertTrue(policy.mandatoryUserReview)
        assertEquals(0.92, policy.highConfidenceMinScore, 1e-9)
        assertEquals(0.12, policy.highConfidenceMinTopTwoMargin, 1e-9)
        assertEquals(5, policy.ambiguousMaxCandidatesShown)
    }

    @Test
    fun parsesWeights() {
        assertEquals(0.50, policy.weights.productName, 1e-9)
        assertEquals(0.20, policy.weights.strength, 1e-9)
        assertEquals(0.12, policy.weights.dosageForm, 1e-9)
        assertEquals(0.08, policy.weights.manufacturer, 1e-9)
        assertEquals(0.05, policy.weights.ingredientContext, 1e-9)
        assertEquals(0.05, policy.weights.ocrLayoutQuality, 1e-9)
    }

    @Test
    fun parsesLists() {
        assertEquals(listOf("product_name", "strength", "dosage_form"), policy.highConfidenceRequireFields)
        assertTrue(policy.hardContradictions.contains("strength_conflict"))
        assertEquals(7, policy.hardContradictions.size)
    }

    @Test
    fun missingFieldInferenceIsAlwaysOff() {
        assertFalse(policy.inferMissingStrength)
        assertFalse(policy.inferMissingDosageForm)
        assertFalse(policy.inferMissingSuffix)
        assertFalse(policy.inferFromEfficacy)
    }

    @Test
    fun parsesFreshnessThresholds() {
        assertEquals(14, policy.currentMaxAgeDays)
        assertEquals(30, policy.warningMaxAgeDays)
        assertEquals(30, policy.disableCurrentDurClaimAfterDays)
    }
}
