package io.github.jeiel85.rxscan.feature.drugdetail

import io.github.jeiel85.rxscan.core.model.Freshness

/**
 * Fixed user-facing copy for the official-information screen (08_UX_SPEC.md §6/§7).
 * Pure functions so the exact wording is unit-tested; no medical text is improvised.
 */
object DetailCopy {
    const val MISSING_EASY_INFO: String =
        "이 제품은 현재 앱이 사용하는 소비자용 공식 설명 데이터에 내용이 없습니다. " +
            "아래에는 확인 가능한 허가정보 원문만 표시합니다."

    const val DUR_CURRENT_DISABLED: String = "현재 데이터로는 최신 DUR 확인을 제공하지 않습니다."

    fun freshnessBanner(freshness: Freshness, ageDays: Int): String? = when (freshness) {
        Freshness.CURRENT -> null
        Freshness.WARNING, Freshness.STALE ->
            "공식 정보 데이터가 ${ageDays}일 동안 업데이트되지 않았습니다. " +
                "표시된 기준일을 확인하고 최신 정보는 약사 또는 공식 원문에서 확인하세요."
        Freshness.REVOKED ->
            "이 데이터 버전은 더 이상 사용할 수 없습니다. 앱의 공식 데이터를 업데이트해 주세요."
    }
}
