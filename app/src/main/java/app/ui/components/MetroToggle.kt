package app.metroclock.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.metroclock.ui.theme.LocalAccentColor
import app.metroclock.ui.theme.MetroBlack
import app.metroclock.ui.theme.MetroMidGray

/**
 * Windows Phone Metro style rectangular toggle switch.
 * Completely sharp rectangular borders and thumb.
 */
@Composable
fun MetroToggle(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = "metro_toggle"
) {
  val accentColor = LocalAccentColor.current
  val borderColor = if (checked) accentColor else MetroMidGray
  val thumbColor = if (checked) accentColor else MetroMidGray

  // Switch size: 54dp wide x 26dp high.
  // Thumb size: 16dp wide x 20dp high.
  val thumbOffset by animateDpAsState(
    targetValue = if (checked) 32.dp else 4.dp,
    label = "MetroToggleThumbOffset"
  )

  Box(
    modifier = modifier
      .size(width = 54.dp, height = 26.dp)
      .testTag(testTag)
      .border(width = 2.dp, color = borderColor)
      .background(MetroBlack)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled
      ) {
        onCheckedChange(!checked)
      },
    contentAlignment = Alignment.CenterStart
  ) {
    Box(
      modifier = Modifier
        .offset(x = thumbOffset)
        .size(width = 14.dp, height = 18.dp)
        .background(thumbColor)
    )
  }
}
