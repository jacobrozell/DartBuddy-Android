package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.ui.theme.Brand

enum class ScoringPadKeyStyle {
    Number,
    Modifier,
    Action,
    Bull,
    Miss,
}

@Composable
fun ScoringPadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ScoringPadKeyStyle = ScoringPadKeyStyle.Number,
    enabled: Boolean = true,
    selected: Boolean = false,
    accessibilityLabel: String? = null,
    testTag: String? = null,
) {
    val dark = isSystemInDarkTheme()
    val keyBg = if (dark) Brand.CardElevatedDark else Brand.CardElevatedLight
    val modifierBg = Brand.Green.copy(alpha = if (selected) 0.35f else 0.15f)
    val actionBg = Brand.Orange.copy(alpha = 0.2f)
    val bullBg = Brand.Amber.copy(alpha = 0.25f)
    val missBg = Brand.Red.copy(alpha = 0.15f)

    val container = when (style) {
        ScoringPadKeyStyle.Number -> keyBg
        ScoringPadKeyStyle.Modifier -> modifierBg
        ScoringPadKeyStyle.Action -> actionBg
        ScoringPadKeyStyle.Bull -> bullBg
        ScoringPadKeyStyle.Miss -> missBg
    }

    val contentColor = when (style) {
        ScoringPadKeyStyle.Action -> Brand.Orange
        ScoringPadKeyStyle.Bull -> Brand.Amber
        ScoringPadKeyStyle.Miss -> Brand.RedAccent
        else -> MaterialTheme.colorScheme.onSurface
    }

    val shape = RoundedCornerShape(10.dp)
    val semanticsMod = Modifier.semantics {
        if (accessibilityLabel != null) contentDescription = accessibilityLabel
    }
    val tagMod = if (testTag != null) Modifier.a11yId(testTag) else Modifier

    if (style == ScoringPadKeyStyle.Action) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.then(tagMod).then(semanticsMod),
            shape = shape,
            contentPadding = PaddingValues(vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = contentColor,
                disabledContainerColor = container.copy(alpha = 0.4f),
            ),
        ) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.then(tagMod).then(semanticsMod),
            shape = shape,
            contentPadding = PaddingValues(vertical = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = container,
                contentColor = contentColor,
                disabledContainerColor = container.copy(alpha = 0.4f),
            ),
            border = null,
        ) {
            Text(
                text = label,
                fontWeight = if (style == ScoringPadKeyStyle.Modifier) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (label.length <= 2) 18.sp else 14.sp,
            )
        }
    }
}

@Composable
fun RowScope.ScoringPadKeyWeight(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    style: ScoringPadKeyStyle = ScoringPadKeyStyle.Number,
    enabled: Boolean = true,
    selected: Boolean = false,
    accessibilityLabel: String? = null,
    testTag: String? = null,
) {
    ScoringPadKey(
        label = label,
        onClick = onClick,
        modifier = modifier.weight(weight),
        style = style,
        enabled = enabled,
        selected = selected,
        accessibilityLabel = accessibilityLabel,
        testTag = testTag,
    )
}
