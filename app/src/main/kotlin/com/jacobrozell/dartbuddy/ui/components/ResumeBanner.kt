package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.MotionPreferences
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun ResumeBanner(
    matchType: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resumeLabel = stringResource(R.string.play_home_resumeButton)
    val context = LocalContext.current
    val reduceMotion = MotionPreferences.prefersReducedMotion(context)
    var visible by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) { if (!reduceMotion) visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0.94f,
        animationSpec = if (reduceMotion) spring(stiffness = Spring.StiffnessHigh) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "resumeBannerScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "resumeBannerAlpha",
    )
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(if (reduceMotion) 1f else scale)
            .alpha(if (reduceMotion) 1f else alpha)
            .clip(RoundedCornerShape(DS.radiusSm))
            .border(2.dp, Brand.Green, RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(DS.s4)
            .semantics { contentDescription = resumeLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.s3),
    ) {
        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Brand.Green, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.play_home_resumeButton), fontWeight = FontWeight.Bold)
            Text(matchType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
