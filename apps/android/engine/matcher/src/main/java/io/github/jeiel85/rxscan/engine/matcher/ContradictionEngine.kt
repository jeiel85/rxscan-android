package io.github.jeiel85.rxscan.engine.matcher

import io.github.jeiel85.rxscan.core.model.ContradictionType
import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.RecognizedDrugFields
import io.github.jeiel85.rxscan.core.model.RecordStatus
import io.github.jeiel85.rxscan.core.model.RejectionReason
import io.github.jeiel85.rxscan.core.model.ReleaseForm
import io.github.jeiel85.rxscan.core.model.Route

/**
 * Hard contradiction engine (04_DRUG_MATCHING_ENGINE.md §4). A candidate hit by
 * any rule is rejected regardless of fuzzy score. A *missing* field is never a
 * contradiction — only conflicting present evidence is.
 */
object ContradictionEngine {
    fun evaluate(fields: RecognizedDrugFields, record: DrugRecord): List<RejectionReason> {
        val reasons = mutableListOf<RejectionReason>()

        if (record.status != RecordStatus.ACTIVE_OR_UNKNOWN) {
            reasons += reason(record, ContradictionType.INACTIVE_OR_QUARANTINED_RECORD, "record status=${record.status}")
        }
        if (fields.itemCode != null && fields.itemCode != record.itemCode) {
            reasons += reason(
                record,
                ContradictionType.IDENTIFIER_POINTS_TO_OTHER_PRODUCT,
                "recognized ${fields.itemCode} != ${record.itemCode}",
            )
        }
        if (fields.strength != null && record.strength != null &&
            StrengthComparison.conflicts(fields.strength!!, record.strength!!)
        ) {
            reasons += reason(record, ContradictionType.STRENGTH_CONFLICT, "${fields.strength} vs ${record.strength}")
        }
        if (fields.dosageForm != DosageForm.UNKNOWN && record.dosageForm != DosageForm.UNKNOWN &&
            fields.dosageForm != record.dosageForm
        ) {
            reasons += reason(record, ContradictionType.DOSAGE_FORM_CONFLICT, "${fields.dosageForm} vs ${record.dosageForm}")
        }
        if (fields.releaseForm != ReleaseForm.UNKNOWN && record.releaseForm != ReleaseForm.UNKNOWN &&
            fields.releaseForm != record.releaseForm
        ) {
            reasons += reason(record, ContradictionType.RELEASE_FORM_CONFLICT, "${fields.releaseForm} vs ${record.releaseForm}")
        }
        if (fields.route != Route.UNKNOWN && record.route != Route.UNKNOWN && fields.route != record.route) {
            reasons += reason(record, ContradictionType.ROUTE_CONFLICT, "${fields.route} vs ${record.route}")
        }
        if (!record.productNameUnique && fields.manufacturer != null && record.manufacturerNormalized != null &&
            normalizeSearch(fields.manufacturer!!) != record.manufacturerNormalized
        ) {
            reasons += reason(
                record,
                ContradictionType.NON_UNIQUE_PRODUCT_MANUFACTURER_CONFLICT,
                "manufacturer mismatch on non-unique name",
            )
        }
        return reasons
    }

    private fun reason(record: DrugRecord, type: ContradictionType, detail: String) =
        RejectionReason(itemCode = record.itemCode, type = type, detail = detail)
}
