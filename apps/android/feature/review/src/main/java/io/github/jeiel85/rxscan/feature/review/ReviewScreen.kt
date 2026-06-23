package io.github.jeiel85.rxscan.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jeiel85.rxscan.core.model.Freshness
import io.github.jeiel85.rxscan.core.model.LineDecision
import io.github.jeiel85.rxscan.core.model.MedicationLineReview
import io.github.jeiel85.rxscan.core.model.ReviewSession
import io.github.jeiel85.rxscan.feature.drugdetail.DetailCopy

/**
 * Mandatory review screen (08_UX_SPEC.md §3 Screen D). Every line must be reviewed
 * before "확인한 약으로 계속" is enabled; nothing is preselected. The photographed
 * direction is shown in its own section, separate from official information.
 */
@Composable
fun ReviewScreen(
    session: ReviewSession,
    freshness: Freshness,
    sourceAgeDays: Int,
    onConfirm: (lineId: String, itemCode: String) -> Unit,
    onReject: (lineId: String) -> Unit,
    onUnresolved: (lineId: String) -> Unit,
    onFinalize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("약 확인", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(ReviewCopy.EDIT_HINT, style = MaterialTheme.typography.bodyMedium)

            DetailCopy.freshnessBanner(freshness, sourceAgeDays)?.let { banner ->
                Text(
                    text = banner,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { contentDescription = "데이터 신선도 경고: $banner" },
                )
            }

            for (line in session.lines) {
                MedicationReviewCard(line, onConfirm, onReject, onUnresolved)
            }

            HorizontalDivider()
            Button(
                onClick = onFinalize,
                enabled = session.canFinalize(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(ReviewCopy.FINALIZE)
            }
            if (!session.canFinalize()) {
                Text(
                    "모든 약을 확인해야 계속할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun MedicationReviewCard(
    line: MedicationLineReview,
    onConfirm: (lineId: String, itemCode: String) -> Unit,
    onReject: (lineId: String) -> Unit,
    onUnresolved: (lineId: String) -> Unit,
) {
    val fields = line.match.recognizedFields
    val statusLabel = ReviewCopy.confidenceLabel(line.match.status)
    val decisionLabel = ReviewCopy.decisionLabel(line.decision)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .semantics { contentDescription = "약 항목. 상태: $statusLabel. 검토: $decisionLabel" },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("약봉지에 적힌 글자", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(fields.rawLine, style = MaterialTheme.typography.bodyMedium)

            Text("인식 결과", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                buildString {
                    append("이름: ${fields.productName ?: "미상"}")
                    append(" · 함량: ${fields.strength?.let { "${it.value}${it.unit}" } ?: "미상"}")
                    append(" · 제형: ${fields.dosageForm}")
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            // Confidence stated in words (non-color semantics) plus current decision.
            Text("상태: $statusLabel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("검토: $decisionLabel", style = MaterialTheme.typography.bodySmall)

            HorizontalDivider()
            Text(ReviewCopy.DIRECTION_SECTION, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(ReviewCopy.directionSummary(line.photographedDirection), style = MaterialTheme.typography.bodyMedium)

            if (line.match.candidates.isNotEmpty()) {
                HorizontalDivider()
                Text("공식 후보", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                for (candidate in line.match.candidates) {
                    val record = candidate.record
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "${record.productName} (${record.itemCode})",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                        OutlinedButton(onClick = { onConfirm(line.lineId, record.itemCode) }) {
                            Text("이 약으로 확인")
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onReject(line.lineId) }) { Text("아님") }
                OutlinedButton(onClick = { onUnresolved(line.lineId) }) { Text("확인 못함") }
            }

            if (line.decision == LineDecision.CONFIRMED && line.confirmedItemCode != null) {
                Text("확인한 약: ${line.confirmedItemCode}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
