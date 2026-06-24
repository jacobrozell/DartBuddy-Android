package com.jacobrozell.dartbuddy.ui.launch

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.MotionPreferences
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.ui.theme.Brand
import kotlinx.coroutines.delay

@Composable
fun LaunchSplashScreen(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduceMotion = MotionPreferences.prefersReducedMotion(context)
    var footerAppeared by remember { mutableStateOf(reduceMotion) }

    LaunchedEffect(visible, reduceMotion) {
        if (!visible) return@LaunchedEffect
        footerAppeared = reduceMotion
        if (!reduceMotion) {
            delay(150)
            footerAppeared = true
        }
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(300, easing = FastOutSlowInEasing),
        label = "splash_fade",
    )

    if (overlayAlpha <= 0f && !visible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(overlayAlpha)
            .semantics {
                contentDescription = context.getString(R.string.common_loading)
            }
            .a11yId(AccessibilityIds.LAUNCH_SPLASH),
    ) {
        Image(
            painter = painterResource(R.drawable.splash_backdrop),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            LaunchSplashWordmark(
                modifier = Modifier.alpha(if (footerAppeared) 1f else 0f),
            )
            LaunchDotsIndicator(
                modifier = Modifier
                    .alpha(if (footerAppeared) 1f else 0f)
                    .padding(top = 8.dp, bottom = 24.dp),
                reduceMotion = reduceMotion,
            )
        }
    }
}

@Composable
private fun LaunchSplashWordmark(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.brand_title),
        modifier = modifier,
        fontSize = 34.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onBackground,
        letterSpacing = 0.6.sp,
    )
}

@Composable
private fun LaunchDotsIndicator(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val activeIndex = if (reduceMotion) {
        0
    } else {
        val transition = rememberInfiniteTransition(label = "splash_dots")
        val step by transition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1260, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "splash_dot_step",
        )
        step.toInt() % 3
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) { index ->
            val active = index == activeIndex
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(if (active) 1.15f else 1f)
                    .clip(CircleShape)
                    .background(Brand.Green.copy(alpha = if (active) 1f else 0.35f)),
            )
        }
    }
}
