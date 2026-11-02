// 잉크색 배경 + 라임 강조색의 앱 테마
package com.downloadanything.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF0E1116)
private val InkRaised = Color(0xFF1A1F27)
private val Lime = Color(0xFFC6FF3D)
private val LimeDeep = Color(0xFF4A6B00)
private val Paper = Color(0xFFF4F3EE)
private val Danger = Color(0xFFFF5C5C)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = InkRaised,
    onSurface = Paper,
    error = Danger,
)

private val LightColors = lightColorScheme(
    primary = LimeDeep,
    onPrimary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    error = Color(0xFFC62828),
)

@Composable
fun DownloadAnythingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
