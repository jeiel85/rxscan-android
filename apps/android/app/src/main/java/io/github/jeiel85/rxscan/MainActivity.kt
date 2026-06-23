package io.github.jeiel85.rxscan

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.jeiel85.rxscan.core.model.Freshness
import io.github.jeiel85.rxscan.core.model.ReviewSession
import io.github.jeiel85.rxscan.core.ui.RxScanTheme
import io.github.jeiel85.rxscan.feature.home.HomeRoute
import io.github.jeiel85.rxscan.feature.review.ReviewScreen
import io.github.jeiel85.rxscan.feature.safety.SafetyScreen
import io.github.jeiel85.rxscan.feature.scan.ScanRoute

private enum class Screen { HOME, SCAN, REVIEW, SAFETY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Protect private screens from recents thumbnails and screenshots
        // (07_SECURITY_PRIVACY.md §3; PrivacySettings.protectScreenshots defaults on).
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            RxScanTheme {
                var screen by remember { mutableStateOf(Screen.HOME) }
                var review by remember { mutableStateOf<ReviewSession?>(null) }

                when (screen) {
                    Screen.HOME -> HomeRoute(
                        onStartScan = { screen = Screen.SCAN },
                        onPreviewReview = {
                            review = SampleReview.session()
                            screen = Screen.REVIEW
                        },
                    )
                    Screen.SCAN -> ScanRoute()
                    Screen.REVIEW -> {
                        val session = review
                        if (session == null) {
                            screen = Screen.HOME
                        } else {
                            ReviewScreen(
                                session = session,
                                freshness = Freshness.CURRENT,
                                sourceAgeDays = 1,
                                onConfirm = { lineId, itemCode ->
                                    review = session.updateLine(lineId) { it.confirm(itemCode) }
                                },
                                onReject = { lineId -> review = session.updateLine(lineId) { it.reject() } },
                                onUnresolved = { lineId -> review = session.updateLine(lineId) { it.markUnresolved() } },
                                onFinalize = {
                                    session.finalize()
                                    screen = Screen.SAFETY
                                },
                            )
                        }
                    }
                    Screen.SAFETY -> SafetyScreen(
                        evaluation = SampleReview.durEvaluation(),
                        onBack = {
                            review = null
                            screen = Screen.HOME
                        },
                    )
                }
            }
        }
    }
}
