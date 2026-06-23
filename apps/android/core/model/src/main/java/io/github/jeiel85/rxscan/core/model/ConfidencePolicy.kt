package io.github.jeiel85.rxscan.core.model

/** Evidence-scoring weights (04_DRUG_MATCHING_ENGINE.md §5). */
data class ScoreWeights(
    val productName: Double,
    val strength: Double,
    val dosageForm: Double,
    val manufacturer: Double,
    val ingredientContext: Double,
    val ocrLayoutQuality: Double,
)

/**
 * Confidence policy loaded from `config/confidence_policy.yaml`. Scores and gates
 * are policy, not model output (04_DRUG_MATCHING_ENGINE.md §5). The policy version
 * is carried into every match result for reproducibility.
 */
data class ConfidencePolicy(
    val policyVersion: String,
    val mandatoryUserReview: Boolean,
    val identifierVerifiedEnabled: Boolean,
    val identifierRequireNoHardContradictions: Boolean,
    val highConfidenceMinScore: Double,
    val highConfidenceMinTopTwoMargin: Double,
    val highConfidenceRequireFields: List<String>,
    val highConfidenceRequireNoHardContradictions: Boolean,
    val highConfidenceDisallowWhenOverridden: Boolean,
    val ambiguousMaxCandidatesShown: Int,
    val weights: ScoreWeights,
    val hardContradictions: List<String>,
    val inferMissingStrength: Boolean,
    val inferMissingDosageForm: Boolean,
    val inferMissingSuffix: Boolean,
    val inferFromEfficacy: Boolean,
    val currentMaxAgeDays: Int,
    val warningMaxAgeDays: Int,
    val disableCurrentDurClaimAfterDays: Int,
) {
    companion object {
        fun parse(yaml: String): ConfidencePolicy {
            val root = YamlLite.parse(yaml)
            val decision = root.child("decision")
            val identifier = decision.child("identifier_verified_state")
            val highConfidence = decision.child("high_confidence_review")
            val ambiguous = decision.child("ambiguous")
            val weights = root.child("weights")
            val missing = root.child("missing_field_behavior")
            val freshness = root.child("freshness")
            return ConfidencePolicy(
                policyVersion = root.str("policy_version"),
                mandatoryUserReview = decision.bool("mandatory_user_review"),
                identifierVerifiedEnabled = identifier.bool("enabled"),
                identifierRequireNoHardContradictions = identifier.bool("require_no_hard_contradictions"),
                highConfidenceMinScore = highConfidence.dbl("minimum_score"),
                highConfidenceMinTopTwoMargin = highConfidence.dbl("minimum_top_two_margin"),
                highConfidenceRequireFields = highConfidence.strList("require_fields"),
                highConfidenceRequireNoHardContradictions = highConfidence.bool("require_no_hard_contradictions"),
                highConfidenceDisallowWhenOverridden = highConfidence.bool("disallow_when_capture_overridden"),
                ambiguousMaxCandidatesShown = ambiguous.int("maximum_candidates_shown"),
                weights = ScoreWeights(
                    productName = weights.dbl("product_name"),
                    strength = weights.dbl("strength"),
                    dosageForm = weights.dbl("dosage_form"),
                    manufacturer = weights.dbl("manufacturer"),
                    ingredientContext = weights.dbl("ingredient_context"),
                    ocrLayoutQuality = weights.dbl("ocr_layout_quality"),
                ),
                hardContradictions = root.strList("hard_contradictions"),
                inferMissingStrength = missing.bool("infer_missing_strength"),
                inferMissingDosageForm = missing.bool("infer_missing_dosage_form"),
                inferMissingSuffix = missing.bool("infer_missing_suffix"),
                inferFromEfficacy = missing.bool("infer_from_efficacy"),
                currentMaxAgeDays = freshness.int("current_max_age_days"),
                warningMaxAgeDays = freshness.int("warning_max_age_days"),
                disableCurrentDurClaimAfterDays = freshness.int("disable_current_dur_claim_after_days"),
            )
        }

        private fun Map<String, Any>.child(key: String): Map<String, Any> {
            @Suppress("UNCHECKED_CAST")
            return (this[key] as? Map<String, Any>) ?: error("missing map: $key")
        }

        private fun Map<String, Any>.str(key: String): String = (this[key] ?: error("missing: $key")).toString()
        private fun Map<String, Any>.bool(key: String): Boolean = this[key] as? Boolean ?: error("missing bool: $key")
        private fun Map<String, Any>.int(key: String): Int = (this[key] as? Number ?: error("missing int: $key")).toInt()
        private fun Map<String, Any>.dbl(key: String): Double = (this[key] as? Number ?: error("missing num: $key")).toDouble()

        private fun Map<String, Any>.strList(key: String): List<String> {
            val raw = this[key] as? List<*> ?: error("missing list: $key")
            return raw.map { it.toString() }
        }
    }
}

/**
 * Minimal indentation-based YAML reader for the small, fixed-shape policy file.
 * Supports nested maps, block scalar values, and `- item` lists. Avoids adding a
 * YAML dependency to the Android app (keeps the dependency surface minimal).
 */
internal object YamlLite {
    private data class Line(val indent: Int, val text: String)

    fun parse(text: String): Map<String, Any> {
        val lines = text.lines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .map { Line(it.takeWhile { ch -> ch == ' ' }.length, it.trim()) }
        if (lines.isEmpty()) return emptyMap()
        @Suppress("UNCHECKED_CAST")
        return parseBlock(lines, 0, lines[0].indent).first as Map<String, Any>
    }

    private fun parseBlock(lines: List<Line>, start: Int, indent: Int): Pair<Any, Int> {
        if (lines[start].text.startsWith("- ")) {
            val list = mutableListOf<Any>()
            var i = start
            while (i < lines.size && lines[i].indent == indent && lines[i].text.startsWith("- ")) {
                list.add(scalar(lines[i].text.removePrefix("- ").trim()))
                i++
            }
            return list to i
        }
        val map = LinkedHashMap<String, Any>()
        var i = start
        while (i < lines.size && lines[i].indent == indent) {
            val line = lines[i].text
            val colon = line.indexOf(':')
            val key = line.substring(0, colon).trim()
            val rest = line.substring(colon + 1).trim()
            if (rest.isNotEmpty()) {
                map[key] = scalar(rest)
                i++
            } else if (i + 1 < lines.size && lines[i + 1].indent > indent) {
                val (child, next) = parseBlock(lines, i + 1, lines[i + 1].indent)
                map[key] = child
                i = next
            } else {
                map[key] = emptyMap<String, Any>()
                i++
            }
        }
        return map to i
    }

    private fun scalar(raw: String): Any {
        if (raw.length >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) return raw.substring(1, raw.length - 1)
        return when (raw) {
            "true" -> true
            "false" -> false
            else -> raw.toIntOrNull() ?: raw.toDoubleOrNull() ?: raw
        }
    }
}
