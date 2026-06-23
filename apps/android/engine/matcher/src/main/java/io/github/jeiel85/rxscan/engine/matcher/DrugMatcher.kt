package io.github.jeiel85.rxscan.engine.matcher

import io.github.jeiel85.rxscan.core.model.ConfidencePolicy
import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.DrugCandidateScore
import io.github.jeiel85.rxscan.core.model.DrugMatchResult
import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.DrugSearchRepository
import io.github.jeiel85.rxscan.core.model.MatchStatus
import io.github.jeiel85.rxscan.core.model.RecognizedDrugFields
import io.github.jeiel85.rxscan.core.model.RejectionReason
import io.github.jeiel85.rxscan.core.model.RetrievalMethod
import io.github.jeiel85.rxscan.core.model.ScoreComponents

/**
 * Fail-closed drug matcher (04_DRUG_MATCHING_ENGINE.md). Retrieval is permissive;
 * acceptance is conservative. Hard contradictions reject candidates before scoring,
 * so no fuzzy score can promote a contradicted candidate. Missing required fields
 * keep a result out of the high-confidence state but are never auto-filled. The
 * same inputs, DB, and policy always produce the same result.
 */
class DrugMatcher(
    private val repository: DrugSearchRepository,
    private val policy: ConfidencePolicy,
    private val ftsLimit: Int = 20,
) {
    fun match(fields: RecognizedDrugFields): DrugMatchResult {
        val dbVersion = repository.databaseVersion()
        val retrieved = retrieveCandidates(fields)

        val hardRejections = mutableListOf<RejectionReason>()
        val scored = mutableListOf<DrugCandidateScore>()
        for ((record, method) in retrieved) {
            val contradictions = ContradictionEngine.evaluate(fields, record)
            if (contradictions.isNotEmpty()) {
                hardRejections += contradictions
                continue
            }
            val components = scoreComponents(fields, record)
            scored += DrugCandidateScore(
                record = record,
                retrieval = method,
                score = components.total,
                components = components,
                identifierVerified = method == RetrievalMethod.IDENTIFIER && fields.itemCode == record.itemCode,
            )
        }
        scored.sortWith(compareByDescending<DrugCandidateScore> { it.score }.thenBy { it.record.itemCode })

        val (status, selected) = decide(fields, scored)
        val candidatesShown = when (status) {
            MatchStatus.VERIFIED_IDENTIFIER, MatchStatus.HIGH_CONFIDENCE_REVIEW -> scored
            else -> scored.take(policy.ambiguousMaxCandidatesShown)
        }
        return DrugMatchResult(
            status = status,
            selectedCandidate = selected,
            candidates = candidatesShown,
            recognizedFields = fields,
            hardRejections = hardRejections,
            policyVersion = policy.policyVersion,
            publicDbVersion = dbVersion,
        )
    }

    private fun retrieveCandidates(fields: RecognizedDrugFields): List<Pair<DrugRecord, RetrievalMethod>> {
        val ordered = LinkedHashMap<String, Pair<DrugRecord, RetrievalMethod>>()
        fun consider(record: DrugRecord, method: RetrievalMethod) {
            if (!ordered.containsKey(record.itemCode)) ordered[record.itemCode] = record to method
        }

        fields.itemCode?.let { code ->
            repository.findByItemCode(code)?.let { consider(it, RetrievalMethod.IDENTIFIER) }
        }
        val nameKey = fields.productName?.let { normalizeSearch(it) }.orEmpty()
        if (nameKey.isNotEmpty()) {
            repository.findByExactNormalizedName(nameKey).forEach { consider(it, RetrievalMethod.EXACT_NORMALIZED_NAME) }
            repository.findByNormalizedAlias(nameKey).forEach { consider(it, RetrievalMethod.ALIAS_WITH_STRENGTH_FORM) }
        }
        repository.searchByName(normalizeSearch(fields.normalizedLine), ftsLimit)
            .forEach { consider(it, RetrievalMethod.FTS_NAME) }
        return ordered.values.toList()
    }

    private fun scoreComponents(fields: RecognizedDrugFields, record: DrugRecord): ScoreComponents {
        val recognizedName = fields.productName?.let { normalizeSearch(it) } ?: normalizeSearch(fields.normalizedLine)
        val nameSimilarity = DrugTextSimilarity.similarity(recognizedName, record.productNameNormalized)

        val strengthScore = if (fields.strength != null && record.strength != null &&
            StrengthComparison.matches(fields.strength!!, record.strength!!)
        ) policy.weights.strength else 0.0

        val formScore = if (fields.dosageForm != DosageForm.UNKNOWN && record.dosageForm != DosageForm.UNKNOWN &&
            fields.dosageForm == record.dosageForm
        ) policy.weights.dosageForm else 0.0

        val manufacturerScore = if (fields.manufacturer != null && record.manufacturerNormalized != null &&
            normalizeSearch(fields.manufacturer!!) == record.manufacturerNormalized
        ) policy.weights.manufacturer else 0.0

        val ingredientScore = if (fields.ingredientText != null &&
            record.ingredientNames.any { fields.ingredientText!!.contains(it) }
        ) policy.weights.ingredientContext else 0.0

        val ocrScore = if (fields.ocrQualityFlags.isEmpty()) policy.weights.ocrLayoutQuality else 0.0

        return ScoreComponents(
            productName = nameSimilarity * policy.weights.productName,
            strength = strengthScore,
            dosageForm = formScore,
            manufacturer = manufacturerScore,
            ingredientContext = ingredientScore,
            ocrLayoutQuality = ocrScore,
        )
    }

    private fun decide(
        fields: RecognizedDrugFields,
        scored: List<DrugCandidateScore>,
    ): Pair<MatchStatus, DrugCandidateScore?> {
        val verified = scored.firstOrNull { it.identifierVerified }
        if (policy.identifierVerifiedEnabled && verified != null) {
            return MatchStatus.VERIFIED_IDENTIFIER to verified
        }
        if (scored.isEmpty()) return MatchStatus.UNRESOLVED to null

        val top = scored.first()
        val secondScore = scored.getOrNull(1)?.score ?: 0.0
        val margin = top.score - secondScore
        val highConfidence = top.score >= policy.highConfidenceMinScore &&
            margin >= policy.highConfidenceMinTopTwoMargin &&
            requiredFieldsPresent(fields) &&
            !(policy.highConfidenceDisallowWhenOverridden && fields.captureOverridden)
        if (highConfidence) return MatchStatus.HIGH_CONFIDENCE_REVIEW to top

        return MatchStatus.AMBIGUOUS to null
    }

    private fun requiredFieldsPresent(fields: RecognizedDrugFields): Boolean =
        policy.highConfidenceRequireFields.all { field ->
            when (field) {
                "product_name" -> fields.productName != null
                "strength" -> fields.strength != null
                "dosage_form" -> fields.dosageForm != DosageForm.UNKNOWN
                "manufacturer" -> fields.manufacturer != null
                else -> true
            }
        }
}
