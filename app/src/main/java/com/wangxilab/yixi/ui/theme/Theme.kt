package com.wangxilab.yixi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YixiColors = lightColorScheme(
    primary = Color(0xFF315B4B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8DC),
    onPrimaryContainer = Color(0xFF17392D),
    background = Color(0xFFF6F3EC),
    onBackground = Color(0xFF252622),
    surface = Color(0xFFFFFCF5),
    onSurface = Color(0xFF252622),
    surfaceVariant = Color(0xFFE7E3DA),
    onSurfaceVariant = Color(0xFF60615B),
)

@Composable
fun YixiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YixiColors,
        content = content,
    )
}
