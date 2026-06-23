package io.github.jeiel85.rxscan.feature.scan

import io.github.jeiel85.rxscan.core.model.ScanError

/** Immutable UI state for the scan screen (one-way data flow, 12_PROJECT_STRUCTURE.md §2). */
data class ScanUiState(
    val cameraPermissionGranted: Boolean = false,
    val permissionPermanentlyDenied: Boolean = false,
    val liveGuidanceKo: List<String> = emptyList(),
    val captureRecommended: Boolean = false,
    val capturedSessionId: String? = null,
    val lastError: ScanError? = null,
    val captureOverridden: Boolean = false,
) {
    /** Live message shown under the viewfinder; never a fake progress percentage. */
    val primaryGuidanceKo: String
        get() = liveGuidanceKo.firstOrNull()
            ?: "약 이름과 복용법이 모두 보이게 해 주세요."
}
