package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun CheckoutSuggestionBanner(
    routes: List<List<String>>,
    selectedIndex: Int,
    displayLabel: (String) -> String,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val route = routes.getOrNull(selectedIndex) ?: return
    val label = route.joinToString(" → ") { displayLabel(it) }
    val a11y = stringResource(R.string.play_x01_checkout_accessibilityFormat, label)
    val indexLabel = if (routes.size > 1) {
        stringResource(R.string.play_x01_checkout_optionIndexFormat, selectedIndex + 1, routes.size)
    } else {
        null
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCycle)
            .semantics { contentDescription = a11y },
        shape = RoundedCornerShape(10.dp),
        color = Brand.Amber.copy(alpha = 0.25f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColumnText(title = stringResource(R.string.play_x01_checkout_title), body = label)
            indexLabel?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ColumnText(title: String, body: String) {
    androidx.compose.foundation.layout.Column {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Text(body, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MatchFeedbackBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    MatchFeedbackBanner(message = message, style = MatchFeedbackBannerStyle.Error, modifier = modifier)
}

enum class MatchFeedbackBannerStyle {
    Error,
    LegWin,
    CheckIn,
}

@Composable
fun MatchFeedbackBanner(
    message: String,
    style: MatchFeedbackBannerStyle,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val background = when (style) {
        MatchFeedbackBannerStyle.Error -> Brand.Red.copy(alpha = 0.15f)
        MatchFeedbackBannerStyle.LegWin -> Brand.Green.copy(alpha = 0.18f)
        MatchFeedbackBannerStyle.CheckIn -> Brand.Amber.copy(alpha = 0.22f)
    }
    val foreground = when (style) {
        MatchFeedbackBannerStyle.Error -> Brand.RedAccent
        MatchFeedbackBannerStyle.LegWin -> Brand.Green
        MatchFeedbackBannerStyle.CheckIn -> Brand.Orange
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "feedbackBannerScale",
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(if (style == MatchFeedbackBannerStyle.LegWin) scale else 1f),
            shape = RoundedCornerShape(10.dp),
            color = background,
        ) {
            Text(
                message,
                modifier = Modifier.padding(12.dp),
                color = foreground,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
