package io.github.jeiel85.rxscan.core.model

/** Provenance for an official record (08_UX_SPEC.md §3 Screen D/E, §5 footer). */
data class SourceMetadata(
    val agency: String,
    val dataset: String,
    val itemCode: String,
    val sourceUpdatedDate: String?,
    val publicDbVersion: String,
    val sourceUrl: String?,
)

/**
 * Official drug information shown in its own section, strictly separate from the
 * photographed prescription direction (08_UX_SPEC.md §3 Screen E: "Do not merge
 * approved general use with prescribed directions"). All consumer-friendly
 * (`e약은요`) fields are nullable; when absent they stay null and the UI shows the
 * missing-content state rather than generating a summary (08_UX_SPEC.md §6).
 */
data class OfficialDrugInfo(
    val source: SourceMetadata,
    val efficacyText: String? = null,
    val useMethodText: String? = null,
    val warningText: String? = null,
    val cautionText: String? = null,
    val interactionText: String? = null,
    val adverseEffectText: String? = null,
    val storageText: String? = null,
) {
    /** True when no consumer-friendly easy-information content is available. */
    val hasEasyInfo: Boolean
        get() = listOf(
            efficacyText, useMethodText, warningText, cautionText,
            interactionText, adverseEffectText, storageText,
        ).any { !it.isNullOrBlank() }
}
