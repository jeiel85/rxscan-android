package io.github.jeiel85.rxscan.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySettingsTest {
    @Test
    fun defaultsAreDataMinimizing() {
        val settings = PrivacySettings()
        assertFalse(settings.saveHistory)
        assertFalse(settings.saveOriginalImage)
        assertFalse(settings.appLockEnabled)
        assertTrue(settings.protectScreenshots)
        assertTrue(settings.hideContentInBackground)
    }

    @Test
    fun originalImageRetentionRequiresHistoryAndOptIn() {
        assertFalse(PrivacySettings(saveOriginalImage = true).originalImageRetentionAllowed)
        assertFalse(PrivacySettings(saveHistory = true).originalImageRetentionAllowed)
        assertTrue(PrivacySettings(saveHistory = true, saveOriginalImage = true).originalImageRetentionAllowed)
    }
}
