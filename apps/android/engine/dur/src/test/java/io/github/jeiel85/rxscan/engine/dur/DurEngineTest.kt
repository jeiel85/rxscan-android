package io.github.jeiel85.rxscan.engine.dur

import io.github.jeiel85.rxscan.core.model.ConfidencePolicy
import io.github.jeiel85.rxscan.core.model.ConfirmedMedicine
import io.github.jeiel85.rxscan.core.model.DurRule
import io.github.jeiel85.rxscan.core.model.DurRuleRepository
import io.github.jeiel85.rxscan.core.model.DurRuleType
import io.github.jeiel85.rxscan.core.model.DurStatus
import io.github.jeiel85.rxscan.core.model.ScoreWeights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DurEngineTest {
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

    /** In-memory DUR repository keyed by unordered ingredient pair. */
    private class FakeDurRepo(
        rules: List<DurRule>,
        private val version: String = "20260101-1",
    ) : DurRuleRepository {
        private val byPair = HashMap<Set<String>, MutableList<DurRule>>()

        init {
            for (rule in rules) {
                val a = rule.subjectIngredientCode
                val b = rule.relatedIngredientCode
                if (a != null && b != null) byPair.getOrPut(setOf(a, b)) { mutableListOf() }.add(rule)
            }
        }

        override fun databaseVersion(): String = version
        override fun findPairwiseRules(ingredientA: String, ingredientB: String): List<DurRule> =
            byPair[setOf(ingredientA, ingredientB)].orEmpty()
    }

    private fun coAdminRule(id: String, a: String, b: String) = DurRule(
        ruleId = id,
        type = DurRuleType.CO_ADMINISTRATION_CONTRAINDICATION,
        subjectIngredientCode = a,
        relatedIngredientCode = b,
        itemCode = null,
        noticeDate = "2026-01-01",
        contentText = "synthetic",
        sourceId = "mfds_dur_ingredient",
        agency = "식품의약품안전처",
    )

    private fun med(itemCode: String, vararg ingredients: String) =
        ConfirmedMedicine(itemCode, itemCode, ingredients.toList())

    @Test
    fun findsCoAdministrationBetweenConfirmedMedicines() {
        val engine = DurEngine(FakeDurRepo(listOf(coAdminRule("RULE-1", "INGR-A", "INGR-B"))), policy)
        val result = engine.evaluate(listOf(med("ITEM-1", "INGR-A"), med("ITEM-2", "INGR-B")), sourceAgeDays = 1, revoked = false)

        assertEquals(DurStatus.EVALUATED, result.status)
        assertEquals(1, result.findings.size)
        val finding = result.findings.single()
        assertEquals(DurRuleType.CO_ADMINISTRATION_CONTRAINDICATION, finding.type)
        // Every warning carries source type, date, and DB version.
        assertEquals("2026-01-01", finding.noticeDate)
        assertEquals("20260101-1", finding.publicDbVersion)
        assertTrue(finding.involvedItemCodes.containsAll(listOf("ITEM-1", "ITEM-2")))
    }

    @Test
    fun unresolvedMedicineYieldsInsufficientNotNoInteraction() {
        val engine = DurEngine(FakeDurRepo(emptyList()), policy)
        val result = engine.evaluate(listOf(med("ITEM-1", "INGR-A"), med("ITEM-2")), sourceAgeDays = 1, revoked = false)
        assertEquals(DurStatus.INSUFFICIENT_DATA, result.status)
        assertTrue(result.unresolvedItemCodes.contains("ITEM-2"))
    }

    @Test
    fun fullyResolvedWithNoRulesIsEvaluatedWithEmptyFindings() {
        val engine = DurEngine(FakeDurRepo(emptyList()), policy)
        val result = engine.evaluate(listOf(med("ITEM-1", "INGR-A"), med("ITEM-2", "INGR-B")), sourceAgeDays = 1, revoked = false)
        assertEquals(DurStatus.EVALUATED, result.status)
        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun staleDataDisablesCurrentClaim() {
        val engine = DurEngine(FakeDurRepo(listOf(coAdminRule("RULE-1", "INGR-A", "INGR-B"))), policy)
        val result = engine.evaluate(
            listOf(med("ITEM-1", "INGR-A"), med("ITEM-2", "INGR-B")),
            sourceAgeDays = 45,
            revoked = false,
        )
        assertEquals(DurStatus.DISABLED_STALE, result.status)
        assertFalse(result.currentClaimAllowed)
        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun revokedDataDisablesCurrentClaim() {
        val engine = DurEngine(FakeDurRepo(listOf(coAdminRule("RULE-1", "INGR-A", "INGR-B"))), policy)
        val result = engine.evaluate(
            listOf(med("ITEM-1", "INGR-A"), med("ITEM-2", "INGR-B")),
            sourceAgeDays = 1,
            revoked = true,
        )
        assertEquals(DurStatus.DISABLED_STALE, result.status)
    }

    @Test
    fun contextDependentTypesAreReportedNotEvaluated() {
        val engine = DurEngine(FakeDurRepo(emptyList()), policy)
        val result = engine.evaluate(listOf(med("ITEM-1", "INGR-A")), sourceAgeDays = 1, revoked = false)
        assertTrue(result.notEvaluatedTypes.contains(DurRuleType.PREGNANCY_CONTRAINDICATION))
        assertTrue(result.notEvaluatedTypes.contains(DurRuleType.ELDERLY_CAUTION))
        assertFalse(result.notEvaluatedTypes.contains(DurRuleType.CO_ADMINISTRATION_CONTRAINDICATION))
    }

    @Test
    fun singleConfirmedMedicineHasNoPairwiseFindings() {
        val engine = DurEngine(FakeDurRepo(listOf(coAdminRule("RULE-1", "INGR-A", "INGR-B"))), policy)
        val result = engine.evaluate(listOf(med("ITEM-1", "INGR-A")), sourceAgeDays = 1, revoked = false)
        assertEquals(DurStatus.EVALUATED, result.status)
        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun deterministicForSameInput() {
        val engine = DurEngine(FakeDurRepo(listOf(coAdminRule("RULE-1", "INGR-A", "INGR-B"))), policy)
        val meds = listOf(med("ITEM-1", "INGR-A"), med("ITEM-2", "INGR-B"))
        assertEquals(engine.evaluate(meds, 1, false), engine.evaluate(meds, 1, false))
    }
}
