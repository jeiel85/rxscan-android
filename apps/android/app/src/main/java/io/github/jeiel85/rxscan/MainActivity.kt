package io.github.jeiel85.rxscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.jeiel85.rxscan.core.ui.RxScanTheme
import io.github.jeiel85.rxscan.feature.home.HomeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RxScanTheme {
                HomeRoute()
            }
        }
    }
}

