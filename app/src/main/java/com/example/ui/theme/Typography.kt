package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Strict Windows Phone Metro Typography Scale
// Consistent across Alarms, Timer, Stopwatch, World Clock, and Settings
object MetroTextStyle {
  val Category = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    letterSpacing = 1.5.sp
  )

  val PageTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    letterSpacing = (-0.5).sp
  )

  val LargeTime = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 88.sp,
    lineHeight = 92.sp,
    letterSpacing = (-2).sp
  )

  val StopwatchTime = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 78.sp,
    lineHeight = 82.sp,
    letterSpacing = (-1.5).sp
  )

  val Subtitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 18.sp,
    lineHeight = 24.sp
  )

  val SectionHeader = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 24.sp
  )

  val AlarmTime = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 46.sp,
    lineHeight = 50.sp,
    letterSpacing = (-0.5).sp
  )

  val AlarmAmPm = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 20.sp,
    lineHeight = 24.sp
  )

  val Body = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp
  )

  val Footnote = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 13.sp,
    lineHeight = 18.sp
  )
}

val MetroTypography = Typography(
  displayLarge = MetroTextStyle.LargeTime,
  displayMedium = MetroTextStyle.StopwatchTime,
  displaySmall = MetroTextStyle.PageTitle,
  headlineLarge = MetroTextStyle.PageTitle,
  headlineMedium = MetroTextStyle.AlarmTime,
  headlineSmall = MetroTextStyle.SectionHeader,
  titleLarge = MetroTextStyle.SectionHeader,
  titleMedium = MetroTextStyle.Subtitle,
  titleSmall = MetroTextStyle.Category,
  bodyLarge = MetroTextStyle.Body,
  bodyMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  bodySmall = MetroTextStyle.Footnote,
  labelLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  labelMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
  ),
  labelSmall = MetroTextStyle.Category
)
