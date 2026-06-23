package io.github.jeiel85.rxscan

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.jeiel85.rxscan.core.model.Freshness
import io.github.jeiel85.rxscan.core.model.ReviewSession
import io.github.jeiel85.rxscan.core.ui.RxScanTheme
import io.github.jeiel85.rxscan.feature.home.HomeRoute
import io.github.jeiel85.rxscan.feature.review.ReviewScreen
import io.github.jeiel85.rxscan.feature.scan.ScanRoute

private enum class Screen { HOME, SCAN, REVIEW }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RxScanTheme {
                val context = LocalContext.current
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
                                    Toast.makeText(context, "검토가 완료되었습니다", Toast.LENGTH_SHORT).show()
                                    review = null
                                    screen = Screen.HOME
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
