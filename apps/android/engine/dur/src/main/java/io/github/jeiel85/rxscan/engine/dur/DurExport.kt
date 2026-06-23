package io.github.jeiel85.rxscan.engine.dur

import io.github.jeiel85.rxscan.core.model.ConfirmedMedicine
import io.github.jeiel85.rxscan.core.model.DurEvaluation
import io.github.jeiel85.rxscan.core.model.DurStatus
import io.github.jeiel85.rxscan.core.model.DurWording

/**
 * Pharmacist-review export (Goal 06 deliverable). Plain text containing the
 * confirmed medicines and every finding with its fixed wording and provenance.
 * No patient identity is included; the export is not a prescription replacement.
 */
object DurExport {
    fun toReviewText(evaluation: DurEvaluation, confirmedMedicines: List<ConfirmedMedicine>): String =
        buildString {
            appendLine(DurWording.TITLE)
            appendLine("데이터 버전: ${evaluation.publicDbVersion} · 정책 버전: ${evaluation.policyVersion}")
            appendLine()
            appendLine("확인한 약:")
            for (med in confirmedMedicines) {
                appendLine("- ${med.productName} (${med.itemCode})")
            }
            appendLine()
            when (evaluation.status) {
                DurStatus.DISABLED_STALE -> appendLine(DurWording.DISABLED_STALE)
                DurStatus.INSUFFICIENT_DATA -> {
                    appendLine(DurWording.INSUFFICIENT)
                    if (evaluation.unresolvedItemCodes.isNotEmpty()) {
                        appendLine("성분 미확인: ${evaluation.unresolvedItemCodes.joinToString()}")
                    }
                }
                DurStatus.EVALUATED -> {
                    if (evaluation.findings.isEmpty()) appendLine(DurWording.NO_FINDINGS)
                }
            }
            for (finding in evaluation.findings) {
                appendLine()
                appendLine(DurWording.body(finding.type))
                appendLine("대상: ${finding.involvedProductNames.joinToString(" + ")}")
                appendLine("출처: ${DurWording.sourceFooter(finding)}")
            }
            if (evaluation.notEvaluatedTypes.isNotEmpty()) {
                appendLine()
                appendLine("평가하지 않은 항목(환자 정보 필요): ${evaluation.notEvaluatedTypes.joinToString { it.koreanLabel }}")
            }
            appendLine()
            appendLine("이 내용은 처방을 대체하지 않습니다.")
        }
}
