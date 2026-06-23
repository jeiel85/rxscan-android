package io.github.jeiel85.rxscan.core.model

/**
 * Fixed DUR safety wording (08_UX_SPEC.md §5). The app never produces an
 * unconditional "do not take these together / stop / change / double" instruction
 * (AGENTS.md, Goal 06 acceptance #2). Copy is fixed and pharmacist-reviewable, not
 * generated.
 */
object DurWording {
    const val TITLE: String = "공식 안전사용 정보가 있습니다"

    fun body(type: DurRuleType): String =
        "확인한 약들 사이에서 ${type.koreanLabel} 정보가 조회되었습니다. " +
            "처방에는 의료진의 판단에 따른 사유가 있을 수 있으므로 약을 임의로 중단하거나 변경하지 말고, " +
            "처방한 의료기관 또는 조제약국에 확인하세요."

    const val NO_FINDINGS: String =
        "확인한 약들에서 조회된 공식 병용·중복 안전사용 정보가 없습니다. " +
            "이는 안전함을 보장하지 않으며, 자세한 내용은 약사 또는 의료진에게 확인하세요."

    const val INSUFFICIENT: String =
        "성분 정보를 확인할 수 없는 약이 있어 안전사용 정보를 완전히 평가하지 못했습니다. " +
            "이는 상호작용이 없다는 의미가 아닙니다. 약사 또는 의료진에게 확인하세요."

    const val DISABLED_STALE: String = "현재 데이터로는 최신 DUR 확인을 제공하지 않습니다."

    /** Footer line for a finding: agency · type · rule id · date · DB version. */
    fun sourceFooter(finding: DurFinding): String = buildString {
        append(finding.agency)
        append(" · ${finding.type.koreanLabel}")
        append(" · 규칙 ${finding.ruleId}")
        append(" · 기준일 ${finding.noticeDate ?: "미상"}")
        append(" · 데이터 ${finding.publicDbVersion}")
    }
}
