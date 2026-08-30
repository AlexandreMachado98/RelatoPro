package com.relatopro.app.ui.components.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Standard Durations and Easings across Relato Pro
 */
object MotionTokens {
    const val DURATION_FAST = 150
    const val DURATION_MEDIUM = 240
    const val DURATION_SLOW = 320

    val EaseOut = FastOutSlowInEasing
    val EaseInOut = LinearOutSlowInEasing
}

/**
 * Scale on Click Modifier for tactile button micro-interaction
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BounceAnimation"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Animated Container for List Items (Fade + Subtle Slide)
 */
@Composable
fun AnimatedListItem(
    index: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(MotionTokens.DURATION_MEDIUM, delayMillis = minOf(index * 30, 200))) +
                slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(MotionTokens.DURATION_MEDIUM, delayMillis = minOf(index * 30, 200), easing = MotionTokens.EaseOut)
                ),
        exit = fadeOut(animationSpec = tween(MotionTokens.DURATION_FAST))
    ) {
        content()
    }
}
