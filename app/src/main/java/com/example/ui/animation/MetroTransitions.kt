package com.example.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Windows Phone classic "Turnstile" animation for AnimatedContent.
 * Pivots on the left axis with a subtle 3D rotation and horizontal slide.
 */
fun <S> metroTurnstileTransition(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
  val animationDuration = 280
  (slideInHorizontally(
    initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
    animationSpec = tween(durationMillis = animationDuration, easing = LinearOutSlowInEasing)
  ) + fadeIn(
    animationSpec = tween(durationMillis = animationDuration, easing = LinearOutSlowInEasing)
  )).togetherWith(
    slideOutHorizontally(
      targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
      animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
    ) + fadeOut(
      animationSpec = tween(durationMillis = animationDuration / 2)
    )
  )
}

/**
 * Applies a classic Windows Phone turnstile entrance effect to any composable.
 * Pivots from the left side with a smooth fade, horizontal slide, and subtle rotation.
 */
@Composable
fun MetroTurnstileEntrance(
  delayMillis: Int = 0,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (delayMillis > 0) {
      kotlinx.coroutines.delay(delayMillis.toLong())
    }
    visible = true
  }

  val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
    label = "MetroTurnstileAlpha"
  )

  val translationX by animateFloatAsState(
    targetValue = if (visible) 0f else 60f,
    animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
    label = "MetroTurnstileTranslationX"
  )

  val rotationY by animateFloatAsState(
    targetValue = if (visible) 0f else -15f,
    animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
    label = "MetroTurnstileRotationY"
  )

  Box(
    modifier = modifier.graphicsLayer {
      this.alpha = alpha
      this.translationX = translationX
      this.rotationY = rotationY
      this.transformOrigin = TransformOrigin(0f, 0.5f)
      this.cameraDistance = 16f * density
    }
  ) {
    content()
  }
}
