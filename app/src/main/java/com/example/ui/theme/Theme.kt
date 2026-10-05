package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Composition Locals for Metro Theming
val LocalAccentColor = staticCompositionLocalOf { DefaultAccent.color }
val LocalIsLightTheme = staticCompositionLocalOf { false }
val LocalMetroBackground = staticCompositionLocalOf { MetroBlack }
val LocalMetroTextColor = staticCompositionLocalOf { MetroWhite }
val LocalMetroSubtextColor = staticCompositionLocalOf { MetroMidGray }
val LocalMetroTileBg = staticCompositionLocalOf { MetroTileBg }
val LocalMetroDivider = staticCompositionLocalOf { MetroDivider }

// Completely sharp rectangular shapes (0.dp)
val MetroShapes = Shapes(
  extraSmall = RoundedCornerShape(0.dp),
  small = RoundedCornerShape(0.dp),
  medium = RoundedCornerShape(0.dp),
  large = RoundedCornerShape(0.dp),
  extraLarge = RoundedCornerShape(0.dp)
)

fun createMetroColorScheme(accent: Color, isLight: Boolean): ColorScheme {
  val bg = if (isLight) Color(0xFFFFFFFF) else MetroBlack
  val onBg = if (isLight) Color(0xFF000000) else MetroWhite
  val tileBg = if (isLight) Color(0xFFF2F2F2) else MetroTileBg
  val divider = if (isLight) Color(0xFFE0E0E0) else MetroDivider

  return if (isLight) {
    lightColorScheme(
      primary = accent,
      onPrimary = Color.White,
      primaryContainer = accent.copy(alpha = 0.2f),
      onPrimaryContainer = Color.Black,
      secondary = accent,
      onSecondary = Color.White,
      secondaryContainer = tileBg,
      onSecondaryContainer = Color.Black,
      tertiary = accent,
      onTertiary = Color.White,
      background = bg,
      onBackground = onBg,
      surface = bg,
      onSurface = onBg,
      surfaceVariant = tileBg,
      onSurfaceVariant = Color(0xFF555555),
      outline = Color(0xFF999999),
      outlineVariant = divider
    )
  } else {
    darkColorScheme(
      primary = accent,
      onPrimary = Color.White,
      primaryContainer = accent.copy(alpha = 0.2f),
      onPrimaryContainer = Color.White,
      secondary = accent,
      onSecondary = Color.White,
      secondaryContainer = tileBg,
      onSecondaryContainer = Color.White,
      tertiary = accent,
      onTertiary = Color.White,
      background = bg,
      onBackground = onBg,
      surface = bg,
      onSurface = onBg,
      surfaceVariant = tileBg,
      onSurfaceVariant = MetroLightGray,
      outline = MetroMidGray,
      outlineVariant = divider
    )
  }
}

@Composable
fun MetroClockTheme(
  accentColor: Color = DefaultAccent.color,
  useDynamicColor: Boolean = false,
  isLightTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val (colorScheme, resolvedAccent) = when {
    useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val dynamic = if (isLightTheme) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
      val bg = if (isLightTheme) Color(0xFFFFFFFF) else MetroBlack
      Pair(dynamic.copy(background = bg, surface = bg), dynamic.primary)
    }
    else -> {
      Pair(createMetroColorScheme(accentColor, isLightTheme), accentColor)
    }
  }

  val bg = if (isLightTheme) Color(0xFFFFFFFF) else MetroBlack
  val text = if (isLightTheme) Color(0xFF000000) else MetroWhite
  val subtext = if (isLightTheme) Color(0xFF666666) else MetroMidGray
  val tileBg = if (isLightTheme) Color(0xFFEBEBEB) else MetroTileBg
  val divider = if (isLightTheme) Color(0xFFD6D6D6) else MetroDivider

  CompositionLocalProvider(
    LocalAccentColor provides resolvedAccent,
    LocalIsLightTheme provides isLightTheme,
    LocalMetroBackground provides bg,
    LocalMetroTextColor provides text,
    LocalMetroSubtextColor provides subtext,
    LocalMetroTileBg provides tileBg,
    LocalMetroDivider provides divider
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = MetroTypography,
      shapes = MetroShapes,
      content = content
    )
  }
}
