package com.jacobrozell.dartbuddy.ui.activity

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.support.accessibility.activityFilterModeAccessibilityLabel
import com.jacobrozell.dartbuddy.support.accessibility.activityFilterPeriodAccessibilityLabel
import com.jacobrozell.dartbuddy.support.accessibility.activityFilterPlayerAccessibilityLabel
import com.jacobrozell.dartbuddy.ui.designsystem.BrandSegmented
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun ActivityFilterBar(
    filter: ActivityFilter,
    onFilterChange: (ActivityFilter) -> Unit,
    period: ActivityPeriod,
    onPeriodChange: (ActivityPeriod) -> Unit,
    playerFilterId: String?,
    onPlayerFilterChange: (String?) -> Unit,
    filterLabels: Map<ActivityFilter, String>,
    periodLabels: Map<ActivityPeriod, String>,
    playerOptions: List<Pair<String?, String>>,
    modifier: Modifier = Modifier,
) {
    val selectedPlayerLabel = playerOptions.firstOrNull { it.first == playerFilterId }?.second
        ?: stringResource(R.string.activity_filter_all_players)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        ActivityFilterMenuButton(
            label = filterLabels[filter].orEmpty(),
            icon = Icons.Default.SportsEsports,
            accessibilityLabel = activityFilterModeAccessibilityLabel(filterLabels[filter].orEmpty()),
            testTag = AccessibilityIds.ACTIVITY_MODE_FILTER_MENU,
            modifier = Modifier.fillMaxWidth(),
        ) { dismiss ->
            ActivityFilter.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(DS.s2), verticalAlignment = Alignment.CenterVertically) {
                            when (option) {
                                ActivityFilter.X01 -> GameModeBadge("X01")
                                ActivityFilter.Cricket -> GameModeBadge("Cricket")
                                ActivityFilter.All -> Unit
                            }
                            Text(filterLabels[option].orEmpty())
                        }
                    },
                    onClick = {
                        onFilterChange(option)
                        dismiss()
                    },
                    leadingIcon = if (filter == option) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }

        BrandSegmented(
            options = ActivityPeriod.entries.map { it to (periodLabels[it] ?: it.name) },
            selected = period,
            onSelect = onPeriodChange,
            segmentAccessibilityLabel = { _, label -> activityFilterPeriodAccessibilityLabel(label) },
        )

        ActivityFilterMenuButton(
            label = selectedPlayerLabel,
            icon = Icons.Default.Person,
            accessibilityLabel = activityFilterPlayerAccessibilityLabel(selectedPlayerLabel),
            testTag = AccessibilityIds.ACTIVITY_PLAYER_FILTER_MENU,
            modifier = Modifier.fillMaxWidth(),
        ) { dismiss ->
            playerOptions.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onPlayerFilterChange(id)
                        dismiss()
                    },
                    leadingIcon = if (playerFilterId == id) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun ActivityFilterMenuButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accessibilityLabel: String,
    testTag: String?,
    modifier: Modifier = Modifier,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight

    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(DS.radiusSm),
            color = cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.a11yId(testTag) else Modifier)
                .semantics { contentDescription = accessibilityLabel },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = DS.s3, vertical = DS.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DS.s2),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Icon(Icons.Default.ExpandMore, contentDescription = null)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            menuContent { expanded = false }
        }
    }
}
