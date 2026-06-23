package io.github.jeiel85.rxscan.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DurWordingTest {
    // Unconditional app-generated instructions that must never appear (08_UX_SPEC §5, AGENTS.md).
    private val banned = listOf(
        "복용을 중단하세요",
        "복용을 멈추",
        "중단하세요",
        "끊으세요",
        "함께 복용하지 마",
        "같이 복용하지 마",
        "같이 먹지",
        "두 배",
        "안전합니다",
    )

    private fun assertNoBannedPhrases(text: String) {
        for (phrase in banned) {
            assertFalse("must not contain '$phrase': $text", text.contains(phrase))
        }
    }

    @Test
    fun bodyDefersToProfessionalWithoutStopOrChangeInstruction() {
        for (type in DurRuleType.entries) {
            val body = DurWording.body(type)
            assertTrue(body.contains(type.koreanLabel))
            assertTrue(body.contains("확인하세요"))
            assertNoBannedPhrases(body)
        }
    }

    @Test
    fun fixedStateCopyHasNoBannedPhrases() {
        assertNoBannedPhrases(DurWording.NO_FINDINGS)
        assertNoBannedPhrases(DurWording.INSUFFICIENT)
        assertNoBannedPhrases(DurWording.DISABLED_STALE)
        assertNoBannedPhrases(DurWording.TITLE)
    }

    @Test
    fun insufficientCopyDoesNotClaimNoInteraction() {
        assertTrue(DurWording.INSUFFICIENT.contains("없다는 의미가 아닙니다"))
    }
}
