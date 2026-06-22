package io.github.jeiel85.rxscan.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RxScanLightScheme = lightColorScheme(
    primary = Color(0xFF0E6F63),
    onPrimary = Color.White,
    secondary = Color(0xFF52677A),
    onSecondary = Color.White,
    background = Color(0xFFF8FAF9),
    onBackground = Color(0xFF17211E),
    surface = Color.White,
    onSurface = Color(0xFF17211E),
)

@Composable
fun RxScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RxScanLightScheme,
        typography = Typography(),
        content = content,
    )
}

