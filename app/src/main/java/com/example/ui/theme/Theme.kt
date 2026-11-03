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

private val DarkColorScheme = darkColorScheme(
  primary = CyanPrimaryDark,
  onPrimary = Color(0xFF00354E),
  primaryContainer = Color(0xFF004D6F),
  onPrimaryContainer = Color(0xFFC7E7FF),
  secondary = TealSecondaryDark,
  onSecondary = Color(0xFF003732),
  background = SlateBackgroundDark,
  onBackground = Color(0xFFF1F5F9),
  surface = SlateSurfaceDark,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = SlateSurfaceVariantDark,
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = SlateBorderDark
)

private val LightColorScheme = lightColorScheme(
  primary = CyanPrimaryLight,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFC7E7FF),
  onPrimaryContainer = Color(0xFF001E2E),
  secondary = TealSecondary,
  onSecondary = Color.White,
  background = SlateBackgroundLight,
  onBackground = Color(0xFF0F172A),
  surface = SlateSurfaceLight,
  onSurface = Color(0xFF0F172A),
  surfaceVariant = SlateSurfaceVariantLight,
  onSurfaceVariant = Color(0xFF475569),
  outline = SlateBorderLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our high-fidelity Docker palette by default for strong identity
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
