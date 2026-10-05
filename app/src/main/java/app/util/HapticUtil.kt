package app.metroclock.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun rememberMetroHaptic(): () -> Unit {
  val haptic = LocalHapticFeedback.current
  return remember(haptic) {
    {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
  }
}

fun HapticFeedback.metroBuzz() {
  performHapticFeedback(HapticFeedbackType.TextHandleMove)
}
