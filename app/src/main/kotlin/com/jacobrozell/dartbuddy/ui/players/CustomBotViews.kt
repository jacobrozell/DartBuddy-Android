package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.bots.AimSkillFacet
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.bots.CricketSkillFacet
import com.jacobrozell.dartbuddy.domain.bots.CustomBotConfiguration
import com.jacobrozell.dartbuddy.domain.bots.CustomBotFacetOverrides
import com.jacobrozell.dartbuddy.domain.bots.CustomBotMetrics
import com.jacobrozell.dartbuddy.domain.bots.X01SkillFacet
import com.jacobrozell.dartbuddy.domain.bots.displayProfile
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.theme.Brand

private enum class CustomBotEditorMode {
    Simple,
    Advanced,
}

@Composable
fun BotDifficulty.displayNameComposable(): String = when (this) {
    BotDifficulty.VeryEasy -> stringResource(R.string.bot_difficulty_veryEasy)
    BotDifficulty.Easy -> stringResource(R.string.bot_difficulty_easy)
    BotDifficulty.Medium -> stringResource(R.string.bot_difficulty_medium)
    BotDifficulty.Hard -> stringResource(R.string.bot_difficulty_hard)
    BotDifficulty.Pro -> stringResource(R.string.bot_difficulty_pro)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBotCreationSheet(
    onDismiss: () -> Unit,
    onCreate: (String, CustomBotMetrics) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var x01Average by remember { mutableFloatStateOf(CustomBotMetrics.DEFAULT_X01_AVERAGE.toFloat()) }
    var cricketMpr by remember { mutableFloatStateOf(CustomBotMetrics.DEFAULT_CRICKET_MPR.toFloat()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = DS.s4).padding(bottom = DS.s6),
            verticalArrangement = Arrangement.spacedBy(DS.s4),
        ) {
            Text(stringResource(R.string.custom_bot_create_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.custom_bot_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.custom_bot_create_section_stats), fontWeight = FontWeight.SemiBold)
            CustomBotMetricSlider(
                label = stringResource(R.string.custom_bot_x01_average_label),
                hint = stringResource(R.string.custom_bot_x01_average_hint),
                value = x01Average,
                valueRange = CustomBotMetrics.X01_AVERAGE_RANGE.start.toFloat()..CustomBotMetrics.X01_AVERAGE_RANGE.endInclusive.toFloat(),
                onValueChange = { x01Average = it },
                display = { "%.0f".format(it) },
            )
            CustomBotMetricSlider(
                label = stringResource(R.string.custom_bot_cricket_mpr_label),
                hint = stringResource(R.string.custom_bot_cricket_mpr_hint),
                value = cricketMpr,
                valueRange = CustomBotMetrics.CRICKET_MPR_RANGE.start.toFloat()..CustomBotMetrics.CRICKET_MPR_RANGE.endInclusive.toFloat(),
                onValueChange = { cricketMpr = it },
                display = { "%.2f".format(it) },
            )
            Text(stringResource(R.string.custom_bot_create_footer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryActionButton(
                title = stringResource(R.string.custom_bot_create_action),
                onClick = {
                    onCreate(
                        name,
                        CustomBotMetrics(
                            x01Average = x01Average.toDouble(),
                            cricketMPR = cricketMpr.toDouble(),
                        ),
                    )
                    onDismiss()
                },
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    }
}

@Composable
fun CustomBotDetailEditor(
    configuration: CustomBotConfiguration,
    onConfigurationChange: (CustomBotConfiguration) -> Unit,
) {
    var editorMode by remember(configuration.isAdvanced) {
        mutableStateOf(if (configuration.isAdvanced) CustomBotEditorMode.Advanced else CustomBotEditorMode.Simple)
    }
    var localConfiguration by remember(configuration) { mutableStateOf(configuration) }

    LaunchedEffect(configuration) {
        localConfiguration = configuration
        if (configuration.isAdvanced) {
            editorMode = CustomBotEditorMode.Advanced
        }
    }

    fun publish(updated: CustomBotConfiguration) {
        localConfiguration = updated
        onConfigurationChange(updated)
    }

    Column(verticalArrangement = Arrangement.spacedBy(DS.s4)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CustomBotEditorMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = editorMode == mode,
                    onClick = { editorMode = mode },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = CustomBotEditorMode.entries.size),
                ) {
                    Text(
                        when (mode) {
                            CustomBotEditorMode.Simple -> stringResource(R.string.custom_bot_editor_simple)
                            CustomBotEditorMode.Advanced -> stringResource(R.string.custom_bot_editor_advanced)
                        },
                    )
                }
            }
        }

        when (editorMode) {
            CustomBotEditorMode.Simple -> {
                Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
                    Text(stringResource(R.string.custom_bot_stats_section), fontWeight = FontWeight.SemiBold)
                    CustomBotMetricsEditor(
                        x01Average = localConfiguration.x01Average,
                        cricketMPR = localConfiguration.cricketMPR,
                        onX01AverageChange = { value ->
                            publish(
                                localConfiguration
                                    .withMetrics(value, localConfiguration.cricketMPR)
                                    .resetToSimpleTargets(),
                            )
                        },
                        onCricketMPRChange = { value ->
                            publish(
                                localConfiguration
                                    .withMetrics(localConfiguration.x01Average, value)
                                    .resetToSimpleTargets(),
                            )
                        },
                    )
                    Text(
                        stringResource(R.string.custom_bot_stats_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CustomBotEditorMode.Advanced -> {
                CustomBotAdvancedEditor(
                    configuration = localConfiguration,
                    onConfigurationChange = { publish(it) },
                )
            }
        }

        BotDifficultyStatsSection(profile = localConfiguration.resolvedCanonicalProfile().displayProfile)
    }
}

@Composable
fun CustomBotAdvancedEditor(
    configuration: CustomBotConfiguration,
    onConfigurationChange: (CustomBotConfiguration) -> Unit,
) {
    val profile = remember(configuration) { configuration.resolvedCanonicalProfile() }
    val initialFacets = remember(configuration) {
        configuration.facetOverrides ?: CustomBotFacetOverrides.extract(profile)
    }

    var x01Facet by remember(configuration) { mutableStateOf(initialFacets.x01 ?: X01SkillFacet.extract(profile)) }
    var cricketFacet by remember(configuration) { mutableStateOf(initialFacets.cricket ?: CricketSkillFacet.extract(profile)) }
    var aimFacet by remember(configuration) { mutableStateOf(initialFacets.aim ?: AimSkillFacet.extract(profile)) }
    var presetAnchor by remember(configuration) {
        mutableStateOf(configuration.scoringBehaviorTier ?: profile.x01.scoringBehaviorTier)
    }
    var suppressPresetAnchorChange by remember { mutableStateOf(false) }

    fun applyFacetsToConfiguration(tier: BotDifficulty? = null) {
        val base = tier?.let { configuration.withScoringBehaviorTier(it) } ?: configuration
        val updated = base
            .copy(
                schemaVersion = CustomBotConfiguration.CURRENT_SCHEMA_VERSION,
                facetOverrides = CustomBotFacetOverrides(
                    x01 = x01Facet,
                    cricket = cricketFacet,
                    aim = aimFacet,
                ),
            )
            .syncExplicitProfileFromFacets()
        onConfigurationChange(updated)
    }

    fun reloadFacetsFromConfiguration(config: CustomBotConfiguration) {
        val resolved = config.resolvedCanonicalProfile()
        val facets = config.facetOverrides ?: CustomBotFacetOverrides.extract(resolved)
        x01Facet = facets.x01 ?: X01SkillFacet.extract(resolved)
        cricketFacet = facets.cricket ?: CricketSkillFacet.extract(resolved)
        aimFacet = facets.aim ?: AimSkillFacet.extract(resolved)
        suppressPresetAnchorChange = true
        presetAnchor = config.scoringBehaviorTier ?: resolved.x01.scoringBehaviorTier
        suppressPresetAnchorChange = false
    }

    LaunchedEffect(configuration) {
        reloadFacetsFromConfiguration(configuration)
    }

    Column(verticalArrangement = Arrangement.spacedBy(DS.s4)) {
        Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
            PresetAnchorChips(
                selected = presetAnchor,
                onSelected = { difficulty ->
                    if (suppressPresetAnchorChange) return@PresetAnchorChips
                    presetAnchor = difficulty
                    applyFacetsToConfiguration(tier = difficulty)
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(DS.s3)) {
                OutlinedButton(
                    onClick = {
                        val reset = configuration.resetToSimpleTargets()
                        onConfigurationChange(reset)
                        reloadFacetsFromConfiguration(reset)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.custom_bot_advanced_reset_simple))
                }
                OutlinedButton(
                    onClick = {
                        val reset = configuration.resetToPreset(presetAnchor)
                        onConfigurationChange(reset)
                        reloadFacetsFromConfiguration(reset)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.custom_bot_advanced_reset_preset))
                }
            }
        }

        FacetSection(title = stringResource(R.string.custom_bot_advanced_x01_section)) {
            VisitRangeEditors(
                min = (x01Facet.scoringVisitMin ?: 0).toFloat(),
                max = (x01Facet.scoringVisitMax ?: 0).toFloat(),
                onMinChange = {
                    x01Facet = x01Facet.copy(scoringVisitMin = it.toInt())
                    applyFacetsToConfiguration()
                },
                onMaxChange = {
                    x01Facet = x01Facet.copy(scoringVisitMax = it.toInt())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_hit_single),
                value = (x01Facet.singleHitChance ?: 0.0).toFloat(),
                onValueChange = {
                    x01Facet = x01Facet.copy(singleHitChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_hit_double),
                value = (x01Facet.doubleHitChance ?: 0.0).toFloat(),
                onValueChange = {
                    x01Facet = x01Facet.copy(doubleHitChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_hit_triple),
                value = (x01Facet.tripleHitChance ?: 0.0).toFloat(),
                onValueChange = {
                    x01Facet = x01Facet.copy(tripleHitChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_checkout_attempt_rate),
                value = (x01Facet.checkoutAttemptChance ?: 0.0).toFloat(),
                onValueChange = {
                    x01Facet = x01Facet.copy(checkoutAttemptChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_off_board_miss),
                value = (x01Facet.offBoardMissChance ?: 0.0).toFloat(),
                onValueChange = {
                    x01Facet = x01Facet.copy(offBoardMissChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_bust_risk),
                value = (x01Facet.riskyBustChance ?: 0.0).toFloat(),
                onValueChange = {
                    x01Facet = x01Facet.copy(riskyBustChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_triple_preference),
                value = (x01Facet.triplePreference ?: 0.0).toFloat(),
                onValueChange = {
                    x01Facet = x01Facet.copy(triplePreference = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
        }

        FacetSection(title = stringResource(R.string.custom_bot_advanced_cricket_section)) {
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_hit_single),
                value = (cricketFacet.singleHitChance ?: 0.0).toFloat(),
                onValueChange = {
                    cricketFacet = cricketFacet.copy(singleHitChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_hit_double),
                value = (cricketFacet.doubleHitChance ?: 0.0).toFloat(),
                onValueChange = {
                    cricketFacet = cricketFacet.copy(doubleHitChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_hit_triple),
                value = (cricketFacet.tripleHitChance ?: 0.0).toFloat(),
                onValueChange = {
                    cricketFacet = cricketFacet.copy(tripleHitChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_off_board_miss),
                value = (cricketFacet.offBoardMissChance ?: 0.0).toFloat(),
                onValueChange = {
                    cricketFacet = cricketFacet.copy(offBoardMissChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_wrong_bed),
                value = (cricketFacet.wrongBedChance ?: 0.0).toFloat(),
                onValueChange = {
                    cricketFacet = cricketFacet.copy(wrongBedChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
        }

        FacetSection(title = stringResource(R.string.custom_bot_advanced_aim_section)) {
            PresetAnchorChips(
                selected = aimFacet.scoringBehaviorTier ?: presetAnchor,
                onSelected = {
                    aimFacet = aimFacet.copy(scoringBehaviorTierRaw = it.rawValue)
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_triple_preference),
                value = (aimFacet.tripleOnOpenChance ?: 0.0).toFloat(),
                onValueChange = {
                    aimFacet = aimFacet.copy(tripleOnOpenChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
            BotStatSliderRow(
                title = stringResource(R.string.bot_stats_hit_double),
                value = (aimFacet.doubleOnOpenChance ?: 0.0).toFloat(),
                onValueChange = {
                    aimFacet = aimFacet.copy(doubleOnOpenChance = it.toDouble())
                    applyFacetsToConfiguration()
                },
            )
        }

        Text(
            stringResource(R.string.custom_bot_advanced_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetAnchorChips(
    selected: BotDifficulty,
    onSelected: (BotDifficulty) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        Text(
            stringResource(R.string.custom_bot_advanced_preset_anchor),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DS.s2),
            verticalArrangement = Arrangement.spacedBy(DS.s2),
        ) {
            BotDifficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = difficulty == selected,
                    onClick = { onSelected(difficulty) },
                    label = { Text(difficulty.displayNameComposable()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Brand.Green.copy(alpha = 0.25f),
                        selectedLabelColor = Brand.Green,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FacetSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(DS.s3), content = content)
    }
}

@Composable
private fun VisitRangeEditors(
    min: Float,
    max: Float,
    onMinChange: (Float) -> Unit,
    onMaxChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        Text(stringResource(R.string.custom_bot_advanced_visit_min_format, min), fontWeight = FontWeight.Medium)
        Slider(value = min, onValueChange = onMinChange, valueRange = 0f..180f, steps = 179)
        Text(stringResource(R.string.custom_bot_advanced_visit_max_format, max), fontWeight = FontWeight.Medium)
        Slider(value = max, onValueChange = onMaxChange, valueRange = 0f..180f, steps = 179)
    }
}

@Composable
fun CustomBotMetricsEditor(
    x01Average: Double,
    cricketMPR: Double,
    onX01AverageChange: (Double) -> Unit,
    onCricketMPRChange: (Double) -> Unit,
) {
    var x01 by remember(x01Average) { mutableFloatStateOf(x01Average.toFloat()) }
    var mpr by remember(cricketMPR) { mutableFloatStateOf(cricketMPR.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
        CustomBotMetricSlider(
            label = stringResource(R.string.custom_bot_x01_average_label),
            hint = stringResource(R.string.custom_bot_x01_average_hint),
            value = x01,
            valueRange = CustomBotMetrics.X01_AVERAGE_RANGE.start.toFloat()..CustomBotMetrics.X01_AVERAGE_RANGE.endInclusive.toFloat(),
            onValueChange = {
                x01 = it
                onX01AverageChange(it.toDouble())
            },
            display = { "%.0f".format(it) },
        )
        CustomBotMetricSlider(
            label = stringResource(R.string.custom_bot_cricket_mpr_label),
            hint = stringResource(R.string.custom_bot_cricket_mpr_hint),
            value = mpr,
            valueRange = CustomBotMetrics.CRICKET_MPR_RANGE.start.toFloat()..CustomBotMetrics.CRICKET_MPR_RANGE.endInclusive.toFloat(),
            onValueChange = {
                mpr = it
                onCricketMPRChange(it.toDouble())
            },
            display = { "%.2f".format(it) },
        )
    }
}

/** Simple metrics-only editor for legacy call sites. */
@Composable
fun CustomBotMetricsEditor(
    metrics: CustomBotMetrics,
    onChange: (CustomBotMetrics) -> Unit,
) {
    CustomBotMetricsEditor(
        x01Average = metrics.x01Average,
        cricketMPR = metrics.cricketMPR,
        onX01AverageChange = { x01 ->
            onChange(CustomBotMetrics(x01Average = x01, cricketMPR = metrics.cricketMPR))
        },
        onCricketMPRChange = { mpr ->
            onChange(CustomBotMetrics(x01Average = metrics.x01Average, cricketMPR = mpr))
        },
    )
}

@Composable
private fun CustomBotMetricSlider(
    label: String,
    hint: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    display: (Float) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s1)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(display(value), color = Brand.Green, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
