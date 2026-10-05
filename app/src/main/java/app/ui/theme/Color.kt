package app.metroclock.ui.theme

import androidx.compose.ui.graphics.Color

// Default Windows Phone Metro Colors
val MetroBlack = Color(0xFF000000)
val MetroWhite = Color(0xFFFFFFFF)
val MetroLightGray = Color(0xFFD0D0D0)
val MetroMidGray = Color(0xFF888888)
val MetroDarkGray = Color(0xFF262626)
val MetroTileBg = Color(0xFF1F1F1F)
val MetroDivider = Color(0xFF333333)

// Classic Windows Phone 8 Accent Palette
data class AccentColor(
  val id: String,
  val name: String,
  val color: Color
)

// Prominent requested swatch colors (Yellow, Orange, Blue, Green, Purple, Red, White) + extended Windows Phone accents
val MetroAccentsList = listOf(
  AccentColor("yellow", "yellow", Color(0xFFFFB900)), // Default Metro Yellow/Orange
  AccentColor("orange", "orange", Color(0xFFFA6800)),
  AccentColor("blue", "blue", Color(0xFF1BA1E2)),
  AccentColor("green", "green", Color(0xFF60A917)),
  AccentColor("purple", "purple", Color(0xFFAA00FF)),
  AccentColor("red", "red", Color(0xFFE51400)),
  AccentColor("white", "white", Color(0xFFFFFFFF)),

  AccentColor("lime", "lime", Color(0xFFA4C400)),
  AccentColor("emerald", "emerald", Color(0xFF008A00)),
  AccentColor("teal", "teal", Color(0xFF00ABA9)),
  AccentColor("cobalt", "cobalt", Color(0xFF0050EF)),
  AccentColor("indigo", "indigo", Color(0xFF6A00FF)),
  AccentColor("pink", "pink", Color(0xFFF472D0)),
  AccentColor("magenta", "magenta", Color(0xFFD80073)),
  AccentColor("crimson", "crimson", Color(0xFFA20025)),
  AccentColor("amber", "amber", Color(0xFFF0A30A)),
  AccentColor("brown", "brown", Color(0xFF825A2C)),
  AccentColor("olive", "olive", Color(0xFF6D8764)),
  AccentColor("steel", "steel", Color(0xFF647687)),
  AccentColor("mauve", "mauve", Color(0xFF76608A)),
  AccentColor("taupe", "taupe", Color(0xFF87794E))
)

val DefaultAccent = MetroAccentsList.first { it.id == "yellow" }
