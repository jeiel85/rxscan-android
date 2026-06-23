package io.github.jeiel85.rxscan.feature.drugdetail

import io.github.jeiel85.rxscan.core.model.Freshness
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailCopyTest {
    @Test
    fun currentFreshnessHasNoBanner() {
        assertNull(DetailCopy.freshnessBanner(Freshness.CURRENT, 3))
    }

    @Test
    fun staleBannerMentionsAge() {
        val banner = DetailCopy.freshnessBanner(Freshness.STALE, 45)
        assertTrue(banner!!.contains("45일"))
    }

    @Test
    fun revokedBannerIsPresent() {
        assertTrue(!DetailCopy.freshnessBanner(Freshness.REVOKED, 1).isNullOrBlank())
    }

    @Test
    fun missingEasyInfoCopyIsFixedAndNonGenerated() {
        assertTrue(DetailCopy.MISSING_EASY_INFO.contains("내용이 없습니다"))
    }
}
