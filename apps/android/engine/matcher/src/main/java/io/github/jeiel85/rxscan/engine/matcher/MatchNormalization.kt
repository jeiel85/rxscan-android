package io.github.jeiel85.rxscan.engine.matcher

import io.github.jeiel85.rxscan.core.model.Strength

/** Search normalization mirroring the public-DB builder (casefold + drop whitespace). */
internal fun normalizeSearch(value: String): String =
    value.lowercase().filterNot { it.isWhitespace() }

/** Deterministic normalized-name similarity in [0,1] (Levenshtein ratio). */
internal object DrugTextSimilarity {
    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val distance = levenshtein(a, b)
        return 1.0 - distance.toDouble() / maxOf(a.length, b.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[b.length]
    }
}

/** Strength comparison with unit conversion (04_DRUG_MATCHING_ENGINE.md §4). */
internal object StrengthComparison {
    private fun toBase(strength: Strength): Pair<Double, String>? = when (strength.unit.lowercase()) {
        "mg" -> strength.value to "mass"
        "g" -> strength.value * 1000.0 to "mass"
        "mcg", "µg", "ug" -> strength.value / 1000.0 to "mass"
        "ml" -> strength.value to "volume"
        "iu" -> strength.value to "iu"
        "%" -> strength.value to "percent"
        else -> null
    }

    /** True when two strengths conflict (different dimension, or different value after conversion). */
    fun conflicts(a: Strength, b: Strength): Boolean {
        val baseA = toBase(a)
        val baseB = toBase(b)
        if (baseA == null || baseB == null) {
            // Unknown unit: conflict unless both value and unit match exactly.
            return a.value != b.value || !a.unit.equals(b.unit, ignoreCase = true)
        }
        if (baseA.second != baseB.second) return true
        return kotlin.math.abs(baseA.first - baseB.first) > 1e-6
    }

    fun matches(a: Strength, b: Strength): Boolean = !conflicts(a, b)
}
