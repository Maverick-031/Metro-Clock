package app.metroclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.metroclock.ui.theme.MetroBlack
import app.metroclock.ui.theme.MetroDarkGray
import app.metroclock.ui.theme.MetroWhite

data class MetroAppBarAction(
  val icon: ImageVector,
  val label: String,
  val testTag: String,
  val enabled: Boolean = true,
  val onClick: () -> Unit
)

data class MetroMenuItem(
  val label: String,
  val testTag: String,
  val onClick: () -> Unit
)

@Composable
fun MetroCircleIconButton(
  icon: ImageVector,
  contentDescription: String,
  testTag: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  borderColor: Color = MetroWhite,
  iconTint: Color = MetroWhite,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .size(46.dp)
      .testTag(testTag)
      .clip(CircleShape)
      .border(
        width = 1.5.dp,
        color = if (enabled) borderColor else borderColor.copy(alpha = 0.4f),
        shape = CircleShape
      )
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = MetroWhite),
        enabled = enabled,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f),
      modifier = Modifier.size(24.dp)
    )
  }
}

@Composable
fun MetroAppBar(
  actions: List<MetroAppBarAction>,
  modifier: Modifier = Modifier,
  menuItems: List<MetroMenuItem> = emptyList(),
  onSettingsClick: (() -> Unit)? = null,
  onAboutClick: (() -> Unit)? = null
) {
  var showMenu by remember { mutableStateOf(false) }
  val hasMenu = menuItems.isNotEmpty() || onSettingsClick != null || onAboutClick != null

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(MetroBlack)
      .padding(vertical = 12.dp, horizontal = 16.dp)
      .navigationBarsPadding()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        actions.forEach { action ->
          MetroCircleIconButton(
            icon = action.icon,
            contentDescription = action.label,
            testTag = action.testTag,
            enabled = action.enabled,
            onClick = action.onClick
          )
        }
      }
    }

    if (hasMenu) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .testTag("metro_app_bar_more")
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true, color = MetroWhite)
            ) {
              showMenu = true
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "More",
            tint = MetroWhite,
            modifier = Modifier.size(28.dp)
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier
            .background(MetroDarkGray)
            .border(1.dp, MetroWhite.copy(alpha = 0.2f))
        ) {
          menuItems.forEach { item ->
            DropdownMenuItem(
              text = {
                Text(
                  item.label,
                  color = MetroWhite,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Light
                )
              },
              onClick = {
                showMenu = false
                item.onClick()
              },
              modifier = Modifier.testTag(item.testTag)
            )
          }

          onSettingsClick?.let {
            DropdownMenuItem(
              text = {
                Text(
                  "settings",
                  color = MetroWhite,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Light
                )
              },
              onClick = {
                showMenu = false
                it()
              },
              modifier = Modifier.testTag("menu_settings")
            )
          }

          onAboutClick?.let {
            DropdownMenuItem(
              text = {
                Text(
                  "about",
                  color = MetroWhite,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Light
                )
              },
              onClick = {
                showMenu = false
                it()
              },
              modifier = Modifier.testTag("menu_about")
            )
          }
        }
      }
    }
  }
}
