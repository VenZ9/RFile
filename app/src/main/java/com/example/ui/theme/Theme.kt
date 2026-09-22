package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = CyanAccent,
    onSecondary = Color(0xFF003648),
    secondaryContainer = CyanAccentContainer,
    onSecondaryContainer = Color(0xFFBAE6FD),
    background = SlateBackground,
    onBackground = Color(0xFFECF2EF),
    surface = SlateSurface,
    onSurface = Color(0xFFECF2EF),
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = Color(0xFFC3D2CC),
    outline = SlateOutline,
    outlineVariant = SlateSurfaceHigh,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFA7F3D0),
    onPrimaryContainer = Color(0xFF003820),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    background = LightBackground,
    onBackground = Color(0xFF131C19),
    surface = LightSurface,
    onSurface = Color(0xFF131C19),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF3F4F48),
    outline = LightOutline,
    outlineVariant = Color(0xFFDDE4E0),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent brand colors by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> DarkColorScheme // Power users prefer the iconic ZArchiver dark palette
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

