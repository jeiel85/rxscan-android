package io.github.jeiel85.rxscan.feature.drugdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jeiel85.rxscan.core.model.Freshness
import io.github.jeiel85.rxscan.core.model.OfficialDrugInfo

/**
 * Official medicine information (08_UX_SPEC.md §3 Screen E "공식 의약품 정보"). Shown in
 * its own section, never merged with the photographed direction. Missing
 * consumer-friendly content shows a fixed notice instead of a generated summary,
 * and source metadata + data version are always visible.
 */
@Composable
fun DrugDetailScreen(
    info: OfficialDrugInfo,
    freshness: Freshness,
    sourceAgeDays: Int,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailCopy.freshnessBanner(freshness, sourceAgeDays)?.let { banner ->
                Text(
                    text = banner,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { contentDescription = "데이터 신선도 경고: $banner" },
                )
                HorizontalDivider()
            }

            Text("공식 의약품 정보", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (info.hasEasyInfo) {
                Section("효능·효과", info.efficacyText)
                Section("복용 방법", info.useMethodText)
                Section("경고", info.warningText)
                Section("주의사항", info.cautionText)
                Section("상호작용", info.interactionText)
                Section("이상반응", info.adverseEffectText)
                Section("보관 방법", info.storageText)
            } else {
                Text(DetailCopy.MISSING_EASY_INFO, style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()
            SourceFooter(info)
        }
    }
}

@Composable
private fun Section(title: String, body: String?) {
    if (body.isNullOrBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SourceFooter(info: OfficialDrugInfo) {
    val source = info.source
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("출처", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text("제공기관: ${source.agency}", style = MaterialTheme.typography.bodySmall)
        Text("데이터셋: ${source.dataset}", style = MaterialTheme.typography.bodySmall)
        Text("품목코드: ${source.itemCode}", style = MaterialTheme.typography.bodySmall)
        Text("기준일: ${source.sourceUpdatedDate ?: "미상"}", style = MaterialTheme.typography.bodySmall)
        Text("데이터 버전: ${source.publicDbVersion}", style = MaterialTheme.typography.bodySmall)
        source.sourceUrl?.let { Text("공식 출처 보기: $it", style = MaterialTheme.typography.bodySmall) }
    }
}
