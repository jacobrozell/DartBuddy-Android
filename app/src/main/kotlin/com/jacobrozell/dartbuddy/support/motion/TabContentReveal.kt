package com.jacobrozell.dartbuddy.support.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext

@Composable
fun TabContentReveal(
    isRevealed: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = !MotionPolicy.shouldAnimate(context)
    var visible by remember { mutableStateOf(reduceMotion) }

    LaunchedEffect(isRevealed, reduceMotion) {
        if (!isRevealed) {
            visible = false
            return@LaunchedEffect
        }
        if (reduceMotion) {
            visible = true
        } else if (!visible) {
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0f,
        animationSpec = MotionPolicy.fastAnimationSpec(reduceMotion),
        label = "tab_reveal_alpha",
    )

    Box(modifier.alpha(alpha)) {
        content()
    }
}
