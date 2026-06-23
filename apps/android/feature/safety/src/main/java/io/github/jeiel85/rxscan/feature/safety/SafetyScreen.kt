package io.github.jeiel85.rxscan.feature.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.jeiel85.rxscan.core.model.DurEvaluation
import io.github.jeiel85.rxscan.core.model.DurStatus
import io.github.jeiel85.rxscan.core.model.DurWording

/**
 * Official DUR safety information (08_UX_SPEC.md §5). Shows fixed wording with
 * provenance; never an unconditional stop/change/double instruction. Stale data
 * disables the current-safety claim, and insufficient data is stated as such —
 * never as "no interaction".
 */
@Composable
fun SafetyScreen(
    evaluation: DurEvaluation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(DurWording.TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "데이터 버전: ${evaluation.publicDbVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            when (evaluation.status) {
                DurStatus.DISABLED_STALE -> StatusText(DurWording.DISABLED_STALE)
                DurStatus.INSUFFICIENT_DATA -> {
                    StatusText(DurWording.INSUFFICIENT)
                    if (evaluation.findings.isEmpty()) Unit else FindingsList(evaluation)
                }
                DurStatus.EVALUATED -> {
                    if (evaluation.findings.isEmpty()) StatusText(DurWording.NO_FINDINGS) else FindingsList(evaluation)
                }
            }

            if (evaluation.notEvaluatedTypes.isNotEmpty()) {
                Text(
                    "평가하지 않은 항목(환자 정보 필요): ${evaluation.notEvaluatedTypes.joinToString { it.koreanLabel }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            HorizontalDivider()
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("확인") }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { contentDescription = "안전사용 정보: $text" },
    )
}

@Composable
private fun FindingsList(evaluation: DurEvaluation) {
    for (finding in evaluation.findings) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(finding.type.koreanLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(DurWording.body(finding.type), style = MaterialTheme.typography.bodyMedium)
                Text("대상: ${finding.involvedProductNames.joinToString(" + ")}", style = MaterialTheme.typography.bodySmall)
                Text(DurWording.sourceFooter(finding), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
