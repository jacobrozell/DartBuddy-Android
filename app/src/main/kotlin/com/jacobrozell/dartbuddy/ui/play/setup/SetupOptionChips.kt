package com.jacobrozell.dartbuddy.ui.play.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.support.accessibility.setupChipAccessibilityLabel
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.layout.rememberLayoutContext
import com.jacobrozell.dartbuddy.ui.layout.usesAccessibilitySetupHomeLayout
import com.jacobrozell.dartbuddy.ui.layout.usesWideSetupHomeLayout
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun SetupOptionChipGrid(
    cells: List<@Composable () -> Unit>,
) {
    val layout = rememberLayoutContext()
    when {
        usesAccessibilitySetupHomeLayout(layout) -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DS.s3),
                verticalArrangement = Arrangement.spacedBy(DS.s3),
            ) {
                items(cells.size) { index -> cells[index]() }
            }
        }
        usesWideSetupHomeLayout(layout) -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DS.s3),
                verticalArrangement = Arrangement.spacedBy(DS.s3),
            ) {
                items(cells.size) { index -> cells[index]() }
            }
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
                cells.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DS.s3),
                    ) {
                        row.forEach { cell ->
                            Box(Modifier.weight(1f)) { cell() }
                        }
                        repeat(3 - row.size) {
                            Box(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SetupOptionChip(
    title: String,
    value: String,
    backgroundColor: Color,
    options: List<T>,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val minHeight = if (rememberLayoutContext().isAccessibilityFont) 56.dp else 52.dp
    val boxColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.35f)
    val chipA11y = setupChipAccessibilityLabel(title, value)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.a11yId(testTag) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(RoundedCornerShape(DS.radiusSm))
                    .background(boxColor)
                    .clickable(enabled = enabled) { expanded = true }
                    .semantics {
                        contentDescription = chipA11y
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (enabled) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labelFor(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun setupChipBackground(accent: Color = Brand.Green): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) Brand.CardElevatedDark else accent.copy(alpha = 0.22f)
}

@Composable
fun setupCricketModeBackground(): Color = Brand.RedAccent.copy(alpha = if (isSystemInDarkTheme()) 0.35f else 0.22f)
