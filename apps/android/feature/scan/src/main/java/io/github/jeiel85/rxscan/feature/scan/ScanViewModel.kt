package io.github.jeiel85.rxscan.feature.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.jeiel85.rxscan.engine.imagequality.QualityReport

/**
 * Holds scan UI state. Live quality is guidance only (03_OCR_PIPELINE.md §2):
 * it updates the on-screen hints and whether capture is recommended, but the
 * user may still override and capture (an overridden capture is never eligible
 * for high-confidence auto-resolution — enforced by the matcher in Goal 04).
 */
class ScanViewModel : ViewModel() {
    var uiState by mutableStateOf(ScanUiState())
        private set

    fun onCameraPermissionResult(granted: Boolean) {
        uiState = uiState.copy(
            cameraPermissionGranted = granted,
            permissionPermanentlyDenied = !granted && uiState.cameraPermissionGranted.not(),
        )
    }

    fun onLiveQuality(report: QualityReport) {
        uiState = uiState.copy(
            liveGuidanceKo = report.guidanceKo,
            captureRecommended = report.accepted,
        )
    }

    fun onCaptured(sessionId: String, overridden: Boolean) {
        uiState = uiState.copy(capturedSessionId = sessionId, captureOverridden = overridden)
    }

    fun reset() {
        uiState = uiState.copy(capturedSessionId = null, lastError = null, captureOverridden = false)
    }
}
