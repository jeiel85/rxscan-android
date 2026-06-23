package io.github.jeiel85.rxscan.core.model

/** Numeric strength with its unit, e.g. 500 mg. Never inferred when missing. */
data class Strength(val value: Double, val unit: String) {
    fun normalizedUnit(): String = unit.lowercase()
}

/** Dosage form (04_DRUG_MATCHING_ENGINE.md). UNKNOWN is a first-class value, never guessed away. */
enum class DosageForm {
    TABLET,
    CAPSULE,
    GRANULE,
    POWDER,
    SYRUP,
    LIQUID,
    INJECTION,
    OINTMENT,
    PATCH,
    DROPS,
    OTHER,
    UNKNOWN,
}

/** Release formulation; a conflict here is a hard contradiction (서방/ER/CR vs immediate). */
enum class ReleaseForm {
    IMMEDIATE,
    EXTENDED,
    UNKNOWN,
}

/** Administration route; a conflict here is a hard contradiction. */
enum class Route {
    ORAL,
    TOPICAL,
    INJECTION,
    OPHTHALMIC,
    OTIC,
    NASAL,
    INHALATION,
    RECTAL,
    UNKNOWN,
}

/** Active/eligibility status of a public DB record. */
enum class RecordStatus {
    ACTIVE_OR_UNKNOWN,
    INACTIVE_OR_QUARANTINED,
    REVOKED,
}

/**
 * Fields recognized from one photographed medicine line. Missing values stay
 * null and are never inferred from the candidate database (AGENTS.md,
 * 04_DRUG_MATCHING_ENGINE.md §4).
 */
data class RecognizedDrugFields(
    val rawLine: String,
    val normalizedLine: String,
    val itemCode: String? = null,
    val productName: String? = null,
    val strength: Strength? = null,
    val dosageForm: DosageForm = DosageForm.UNKNOWN,
    val releaseForm: ReleaseForm = ReleaseForm.UNKNOWN,
    val route: Route = Route.UNKNOWN,
    val manufacturer: String? = null,
    val ingredientText: String? = null,
    val ocrQualityFlags: Set<String> = emptySet(),
    val captureOverridden: Boolean = false,
)

/** A normalized public-DB drug record (read-only view used by the matcher). */
data class DrugRecord(
    val itemCode: String,
    val productName: String,
    val productNameNormalized: String,
    val manufacturer: String? = null,
    val manufacturerNormalized: String? = null,
    val strength: Strength? = null,
    val dosageForm: DosageForm = DosageForm.UNKNOWN,
    val releaseForm: ReleaseForm = ReleaseForm.UNKNOWN,
    val route: Route = Route.UNKNOWN,
    val ingredientNames: List<String> = emptyList(),
    val aliasesNormalized: List<String> = emptyList(),
    val status: RecordStatus = RecordStatus.ACTIVE_OR_UNKNOWN,
    val productNameUnique: Boolean = true,
)

/** How a candidate was retrieved (04_DRUG_MATCHING_ENGINE.md §3). */
enum class RetrievalMethod {
    IDENTIFIER,
    EXACT_NORMALIZED_NAME,
    ALIAS_WITH_STRENGTH_FORM,
    FTS_NAME,
    TOKEN_LEVEL,
    MANUAL_SEARCH,
}

/** Hard contradiction categories (04_DRUG_MATCHING_ENGINE.md §4, confidence_policy.yaml). */
enum class ContradictionType {
    IDENTIFIER_POINTS_TO_OTHER_PRODUCT,
    STRENGTH_CONFLICT,
    DOSAGE_FORM_CONFLICT,
    RELEASE_FORM_CONFLICT,
    ROUTE_CONFLICT,
    NON_UNIQUE_PRODUCT_MANUFACTURER_CONFLICT,
    INACTIVE_OR_QUARANTINED_RECORD,
}

/** Why a candidate was hard-rejected, regardless of fuzzy score. */
data class RejectionReason(
    val itemCode: String,
    val type: ContradictionType,
    val detail: String,
)

/** Per-component score breakdown so the UI can show why a candidate was selected. */
data class ScoreComponents(
    val productName: Double = 0.0,
    val strength: Double = 0.0,
    val dosageForm: Double = 0.0,
    val manufacturer: Double = 0.0,
    val ingredientContext: Double = 0.0,
    val ocrLayoutQuality: Double = 0.0,
) {
    val total: Double
        get() = productName + strength + dosageForm + manufacturer + ingredientContext + ocrLayoutQuality
}

/** A scored, non-rejected candidate. */
data class DrugCandidateScore(
    val record: DrugRecord,
    val retrieval: RetrievalMethod,
    val score: Double,
    val components: ScoreComponents,
    val identifierVerified: Boolean = false,
)

/** Final match decision class (04_DRUG_MATCHING_ENGINE.md §6). */
enum class MatchStatus {
    VERIFIED_IDENTIFIER,
    HIGH_CONFIDENCE_REVIEW,
    AMBIGUOUS,
    UNRESOLVED,
}

/**
 * Evidence-rich match result (04_DRUG_MATCHING_ENGINE.md §7). The selected
 * candidate is null for AMBIGUOUS/UNRESOLVED — nothing is preselected. Policy and
 * DB versions are always carried for auditability and reproducibility.
 */
data class DrugMatchResult(
    val status: MatchStatus,
    val selectedCandidate: DrugCandidateScore?,
    val candidates: List<DrugCandidateScore>,
    val recognizedFields: RecognizedDrugFields,
    val hardRejections: List<RejectionReason>,
    val policyVersion: String,
    val publicDbVersion: String,
)
