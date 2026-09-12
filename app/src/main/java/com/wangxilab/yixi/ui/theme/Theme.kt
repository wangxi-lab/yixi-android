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
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF171918),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171918),
    surfaceVariant = Color(0xFFE9F3EE),
    onSurfaceVariant = Color(0xFF626865),
    outlineVariant = Color(0xFFE4E6E5),
)

@Composable
fun YixiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YixiColors,
        content = content,
    )
}
