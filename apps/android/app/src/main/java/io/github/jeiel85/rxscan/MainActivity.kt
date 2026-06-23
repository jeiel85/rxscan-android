package io.github.jeiel85.rxscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.jeiel85.rxscan.core.ui.RxScanTheme
import io.github.jeiel85.rxscan.feature.home.HomeRoute
import io.github.jeiel85.rxscan.feature.scan.ScanRoute

private enum class Screen { HOME, SCAN }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RxScanTheme {
                var screen by remember { mutableStateOf(Screen.HOME) }
                when (screen) {
                    Screen.HOME -> HomeRoute(onStartScan = { screen = Screen.SCAN })
                    Screen.SCAN -> ScanRoute()
                }
            }
        }
    }
}
