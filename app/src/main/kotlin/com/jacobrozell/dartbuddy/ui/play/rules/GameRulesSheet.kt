package com.jacobrozell.dartbuddy.ui.play.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.rules.GameRulesCatalog
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRulesSheet(
    initialMode: MatchType,
    showsModePicker: Boolean = false,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DS.s4)
                .padding(bottom = DS.s6),
            verticalArrangement = Arrangement.spacedBy(DS.s4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.play_rules_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.play_rules_sheet_done), color = Brand.Green, fontWeight = FontWeight.SemiBold)
                }
            }

            GameRulesGuideContent(mode = initialMode, showsModePicker = showsModePicker)
        }
    }
}

@Composable
fun GameRulesGuideContent(
    mode: MatchType,
    modifier: Modifier = Modifier,
    showsModePicker: Boolean = false,
) {
    var selectedMode by remember(mode) { mutableStateOf(mode) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DS.s3)) {
        if (showsModePicker) {
            RulesModePicker(selectedMode) { selectedMode = it }
        }
        val activeMode = if (showsModePicker) selectedMode else mode
        val guide = GameRulesCatalog.guide(activeMode)
        guide.sections.forEach { section ->
            SetupSectionCard {
                Text(stringResource(section.titleRes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(section.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RulesModePicker(selected: MatchType, onSelect: (MatchType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(DS.s2)) {
        GameRulesCatalog.supportedMatchTypes.forEach { mode ->
            val isSelected = mode == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(mode) },
                label = {
                    Text(
                        if (mode == MatchType.X01) {
                            stringResource(R.string.play_x01_title)
                        } else {
                            stringResource(R.string.play_cricket_title)
                        },
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Brand.Green.copy(alpha = 0.25f),
                    selectedLabelColor = Brand.Green,
                ),
            )
        }
    }
}

@Composable
fun LearnToPlayButton(onClick: () -> Unit, compact: Boolean = false) {
    TextButton(onClick = onClick, modifier = if (compact) Modifier else Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.play_rules_learnButton),
            color = Brand.Green,
            fontWeight = FontWeight.SemiBold,
            modifier = if (compact) Modifier else Modifier.fillMaxWidth(),
        )
    }
}
