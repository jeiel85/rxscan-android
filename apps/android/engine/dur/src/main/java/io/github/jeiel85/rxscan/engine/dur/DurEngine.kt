package io.github.jeiel85.rxscan.engine.dur

import io.github.jeiel85.rxscan.core.model.ConfidencePolicy
import io.github.jeiel85.rxscan.core.model.ConfirmedMedicine
import io.github.jeiel85.rxscan.core.model.DurEvaluation
import io.github.jeiel85.rxscan.core.model.DurFinding
import io.github.jeiel85.rxscan.core.model.DurRule
import io.github.jeiel85.rxscan.core.model.DurRuleRepository
import io.github.jeiel85.rxscan.core.model.DurRuleType
import io.github.jeiel85.rxscan.core.model.DurStatus
import io.github.jeiel85.rxscan.core.model.FreshnessEvaluator

/**
 * Local DUR engine (Goal 06). Evaluates only ingredient-to-ingredient (pairwise)
 * rules among **confirmed** medicines, because context-dependent rule types need
 * patient data the app does not collect. It never tells the user to stop, change,
 * or double a medication; it presents official findings with provenance.
 *
 * Fail-closed: an unresolved medicine makes the result INSUFFICIENT_DATA (never
 * "no interaction"), and stale/revoked data disables the current-safety claim.
 * Deterministic: same medicines, DB, and policy always produce the same result.
 */
class DurEngine(
    private val repository: DurRuleRepository,
    private val policy: ConfidencePolicy,
) {
    private val contextDependentTypes: List<DurRuleType> =
        DurRuleType.entries.filterNot { it.pairwise }

    fun evaluate(
        confirmedMedicines: List<ConfirmedMedicine>,
        sourceAgeDays: Int,
        revoked: Boolean,
    ): DurEvaluation {
        val dbVersion = repository.databaseVersion()

        if (!FreshnessEvaluator.durCurrentClaimAllowed(sourceAgeDays, revoked, policy)) {
            return DurEvaluation(
                status = DurStatus.DISABLED_STALE,
                findings = emptyList(),
                evaluatedItemCodes = emptyList(),
                unresolvedItemCodes = confirmedMedicines.map { it.itemCode },
                currentClaimAllowed = false,
                publicDbVersion = dbVersion,
                policyVersion = policy.policyVersion,
                notEvaluatedTypes = DurRuleType.entries.toList(),
            )
        }

        val resolved = confirmedMedicines.filter { it.isResolved }
        val unresolved = confirmedMedicines.filterNot { it.isResolved }.map { it.itemCode }

        val findings = mutableListOf<DurFinding>()
        for (i in resolved.indices) {
            for (j in i + 1 until resolved.size) {
                findings += findingsForPair(resolved[i], resolved[j], dbVersion)
            }
        }

        val status = if (unresolved.isNotEmpty()) DurStatus.INSUFFICIENT_DATA else DurStatus.EVALUATED
        return DurEvaluation(
            status = status,
            findings = findings.sortedWith(compareBy({ it.ruleId }, { it.involvedItemCodes.joinToString() })),
            evaluatedItemCodes = resolved.map { it.itemCode },
            unresolvedItemCodes = unresolved,
            currentClaimAllowed = true,
            publicDbVersion = dbVersion,
            policyVersion = policy.policyVersion,
            notEvaluatedTypes = contextDependentTypes,
        )
    }

    private fun findingsForPair(
        a: ConfirmedMedicine,
        b: ConfirmedMedicine,
        dbVersion: String,
    ): List<DurFinding> {
        val rules = LinkedHashMap<String, DurRule>()
        for (ingredientA in a.ingredientCodes) {
            for (ingredientB in b.ingredientCodes) {
                for (rule in repository.findPairwiseRules(ingredientA, ingredientB)) {
                    if (rule.type.pairwise) rules.putIfAbsent(rule.ruleId, rule)
                }
            }
        }
        return rules.values.map { rule ->
            DurFinding(
                type = rule.type,
                ruleId = rule.ruleId,
                noticeDate = rule.noticeDate,
                sourceId = rule.sourceId,
                agency = rule.agency,
                involvedItemCodes = listOf(a.itemCode, b.itemCode),
                involvedProductNames = listOf(a.productName, b.productName),
                publicDbVersion = dbVersion,
            )
        }
    }
}
