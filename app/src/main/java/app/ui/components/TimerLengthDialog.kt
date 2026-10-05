package app.metroclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.metroclock.ui.theme.LocalAccentColor
import app.metroclock.ui.theme.MetroBlack
import app.metroclock.ui.theme.MetroDarkGray
import app.metroclock.ui.theme.MetroLightGray
import app.metroclock.ui.theme.MetroMidGray
import app.metroclock.ui.theme.MetroWhite

enum class TimeUnit { HOURS, MINUTES, SECONDS }

@Composable
fun TimerLengthDialog(
  initialTotalSeconds: Int,
  onConfirm: (totalSeconds: Int) -> Unit,
  onDismiss: () -> Unit
) {
  val accentColor = LocalAccentColor.current

  var hours by remember { mutableIntStateOf(initialTotalSeconds / 3600) }
  var minutes by remember { mutableIntStateOf((initialTotalSeconds % 3600) / 60) }
  var seconds by remember { mutableIntStateOf(initialTotalSeconds % 60) }

  var activeUnit by remember { mutableStateOf(TimeUnit.MINUTES) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MetroBlack.copy(alpha = 0.95f))
      .padding(horizontal = 24.dp, vertical = 32.dp)
      .testTag("timer_length_dialog")
  ) {
    Column(
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "timer length",
        fontSize = 36.sp,
        fontWeight = FontWeight.Light,
        color = MetroWhite,
        modifier = Modifier.padding(bottom = 20.dp)
      )

      // Three boxes for Hours, Min, Sec
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Hours
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.Start
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .background(if (activeUnit == TimeUnit.HOURS) MetroBlack else MetroDarkGray)
              .border(
                width = 2.dp,
                color = if (activeUnit == TimeUnit.HOURS) accentColor else MetroMidGray
              )
              .clickable { activeUnit = TimeUnit.HOURS }
              .testTag("timer_unit_hours"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = hours.toString(),
              fontSize = 32.sp,
              fontWeight = FontWeight.Normal,
              color = MetroWhite
            )
          }
          Text(
            text = "hours",
            fontSize = 13.sp,
            color = MetroMidGray,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        // Min
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.Start
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .background(if (activeUnit == TimeUnit.MINUTES) MetroBlack else MetroDarkGray)
              .border(
                width = 2.dp,
                color = if (activeUnit == TimeUnit.MINUTES) accentColor else MetroMidGray
              )
              .clickable { activeUnit = TimeUnit.MINUTES }
              .testTag("timer_unit_min"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = minutes.toString(),
              fontSize = 32.sp,
              fontWeight = FontWeight.Normal,
              color = MetroWhite
            )
          }
          Text(
            text = "min",
            fontSize = 13.sp,
            color = MetroMidGray,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        // Sec
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.Start
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .background(if (activeUnit == TimeUnit.SECONDS) MetroBlack else MetroDarkGray)
              .border(
                width = 2.dp,
                color = if (activeUnit == TimeUnit.SECONDS) accentColor else MetroMidGray
              )
              .clickable { activeUnit = TimeUnit.SECONDS }
              .testTag("timer_unit_sec"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = seconds.toString(),
              fontSize = 32.sp,
              fontWeight = FontWeight.Normal,
              color = MetroWhite
            )
          }
          Text(
            text = "sec",
            fontSize = 13.sp,
            color = MetroMidGray,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Flat Action Buttons: [ done ] and [ cancel ]
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .border(2.dp, MetroWhite)
            .clickable {
              val total = (hours * 3600) + (minutes * 60) + seconds
              onConfirm(if (total > 0) total else 60)
            }
            .testTag("timer_length_done"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "done",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = MetroWhite
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .border(2.dp, MetroWhite)
            .clickable { onDismiss() }
            .testTag("timer_length_cancel"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "cancel",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = MetroWhite
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Numeric keypad to easily type/change the active value
      Text(
        text = "Set value for ${activeUnit.name.lowercase()}:",
        fontSize = 14.sp,
        color = MetroMidGray,
        modifier = Modifier.padding(bottom = 12.dp)
      )

      val keypad = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("+5", "0", "CLEAR")
      )

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        keypad.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            row.forEach { key ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(52.dp)
                  .background(MetroDarkGray)
                  .clickable {
                    when (key) {
                      "CLEAR" -> {
                        when (activeUnit) {
                          TimeUnit.HOURS -> hours = 0
                          TimeUnit.MINUTES -> minutes = 0
                          TimeUnit.SECONDS -> seconds = 0
                        }
                      }
                      "+5" -> {
                        when (activeUnit) {
                          TimeUnit.HOURS -> hours = (hours + 1) % 24
                          TimeUnit.MINUTES -> minutes = (minutes + 5) % 60
                          TimeUnit.SECONDS -> seconds = (seconds + 5) % 60
                        }
                      }
                      else -> {
                        val digit = key.toIntOrNull() ?: 0
                        when (activeUnit) {
                          TimeUnit.HOURS -> hours = ((hours * 10) + digit).coerceIn(0, 23)
                          TimeUnit.MINUTES -> minutes = ((minutes * 10) + digit).coerceIn(0, 59)
                          TimeUnit.SECONDS -> seconds = ((seconds * 10) + digit).coerceIn(0, 59)
                        }
                      }
                    }
                  }
                  .testTag("timer_key_$key"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = key,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Normal,
                  color = MetroWhite
                )
              }
            }
          }
        }
      }
    }
  }
}
