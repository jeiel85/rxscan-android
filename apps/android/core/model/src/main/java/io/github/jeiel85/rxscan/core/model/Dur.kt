package io.github.jeiel85.rxscan.core.model

/**
 * DUR rule types (14_SOURCE_REGISTRY.md). [pairwise] types relate two medicines
 * by ingredient and can be evaluated from confirmed medicines alone. The others
 * depend on patient context (age, pregnancy, dose, duration) that this app does
 * not collect, so they are reported as not-evaluated rather than guessed.
 */
enum class DurRuleType(val koreanLabel: String, val pairwise: Boolean) {
    CO_ADMINISTRATION_CONTRAINDICATION("병용금기", true),
    THERAPEUTIC_DUPLICATION("효능군중복", true),
    ELDERLY_CAUTION("노인주의", false),
    PREGNANCY_CONTRAINDICATION("임부금기", false),
    DOSE_CAUTION("용량주의", false),
    DURATION_CAUTION("투여기간주의", false),
    AGE_CONTRAINDICATION("특정연령대금기", false),
    ER_SPLIT_CAUTION("서방정분할주의", false),
}

/** One official DUR rule (mirrors the public DB `dur_rule` row + provenance). */
data class DurRule(
    val ruleId: String,
    val type: DurRuleType,
    val subjectIngredientCode: String?,
    val relatedIngredientCode: String?,
    val itemCode: String?,
    val noticeDate: String?,
    val contentText: String,
    val sourceId: String,
    val agency: String,
)

/**
 * A medicine the user explicitly confirmed in review. Only confirmed medicines
 * enter DUR evaluation (Goal 06 acceptance #1). A medicine with no resolved
 * ingredient codes is unresolved — it cannot be declared interaction-free.
 */
data class ConfirmedMedicine(
    val itemCode: String,
    val productName: String,
    val ingredientCodes: List<String>,
) {
    val isResolved: Boolean get() = ingredientCodes.isNotEmpty()
}

/** A retrieved DUR finding. Always carries source type, date, and DB version. */
data class DurFinding(
    val type: DurRuleType,
    val ruleId: String,
    val noticeDate: String?,
    val sourceId: String,
    val agency: String,
    val involvedItemCodes: List<String>,
    val involvedProductNames: List<String>,
    val publicDbVersion: String,
)

enum class DurStatus {
    /** All confirmed medicines had resolved ingredients; supported rules evaluated. */
    EVALUATED,
    /** At least one confirmed medicine could not be resolved — evaluation is incomplete. */
    INSUFFICIENT_DATA,
    /** Data too stale/revoked to claim a current safety check. */
    DISABLED_STALE,
}

/**
 * Deterministic DUR result (Goal 06). Never instructs the user to stop, change, or
 * double a medication; presents official findings with provenance and defers to a
 * professional. [notEvaluatedTypes] lists context-dependent types the app cannot
 * assess without patient data.
 */
data class DurEvaluation(
    val status: DurStatus,
    val findings: List<DurFinding>,
    val evaluatedItemCodes: List<String>,
    val unresolvedItemCodes: List<String>,
    val currentClaimAllowed: Boolean,
    val publicDbVersion: String,
    val policyVersion: String,
    val notEvaluatedTypes: List<DurRuleType>,
)

/**
 * Read-only DUR rule retrieval over the signed public DB. Only pairwise
 * (ingredient-to-ingredient) rules are queried, matching what can be evaluated
 * from confirmed medicines without patient context.
 */
interface DurRuleRepository {
    fun databaseVersion(): String
    fun findPairwiseRules(ingredientA: String, ingredientB: String): List<DurRule>
}
