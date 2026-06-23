package io.github.jeiel85.rxscan.core.security

/**
 * User privacy preferences (06_LOCAL_DATA_MODEL.md §3, 08_UX_SPEC.md §8). Defaults
 * are data-minimizing: history and original-image retention are OFF, and private
 * screens are protected. The app is fully usable with all of these off and never
 * requires an account or network.
 */
data class PrivacySettings(
    val saveHistory: Boolean = false,
    val saveOriginalImage: Boolean = false,
    val appLockEnabled: Boolean = false,
    val protectScreenshots: Boolean = true,
    val hideContentInBackground: Boolean = true,
) {
    /** Original images may only be retained when history is on and retention is opted in. */
    val originalImageRetentionAllowed: Boolean
        get() = saveHistory && saveOriginalImage
}
