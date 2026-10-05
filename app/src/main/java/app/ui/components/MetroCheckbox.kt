package app.metroclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.metroclock.ui.theme.LocalAccentColor
import app.metroclock.ui.theme.MetroBlack
import app.metroclock.ui.theme.MetroWhite

/**
 * Large, flat, sharp-edged rectangular Metro Checkbox as shown in Windows Phone.
 */
@Composable
fun MetroCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = "metro_checkbox"
) {
  val accentColor = LocalAccentColor.current

  Box(
    modifier = modifier
      .size(32.dp)
      .testTag(testTag)
      .border(
        width = 2.dp,
        color = if (checked) accentColor else MetroWhite
      )
      .background(if (checked) accentColor else MetroBlack)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled
      ) {
        onCheckedChange(!checked)
      },
    contentAlignment = Alignment.Center
  ) {
    if (checked) {
      Icon(
        imageVector = Icons.Default.Check,
        contentDescription = "Checked",
        tint = MetroWhite,
        modifier = Modifier.size(22.dp)
      )
    }
  }
}
