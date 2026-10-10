package com.audihelp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary         = Color(0xFF7ECFFF),
    onPrimary       = Color(0xFF003549),
    primaryContainer= Color(0xFF004D67),
    secondary       = Color(0xFFB3CBDA),
    background      = Color(0xFF0F1C22),
    surface         = Color(0xFF1A2B34),
)

private val LightColorScheme = lightColorScheme(
    primary         = Color(0xFF006688),
    onPrimary       = Color.White,
    primaryContainer= Color(0xFFBFE9FF),
    secondary       = Color(0xFF4E616D),
    background      = Color(0xFFF5FAFE),
    surface         = Color.White,
)

@Composable
fun AudihelpTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content,
    )
}
