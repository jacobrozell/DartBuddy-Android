package com.jacobrozell.dartbuddy.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.jacobrozell.dartbuddy.ui.components.ResumeBanner
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import com.jacobrozell.dartbuddy.domain.model.X01StartScores
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.designsystem.BrandAppTitle
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.ui.designsystem.HorizontalOptionChips
import com.jacobrozell.dartbuddy.ui.designsystem.ErrorBanner
import com.jacobrozell.dartbuddy.ui.designsystem.SetupEmptyHint
import com.jacobrozell.dartbuddy.ui.designsystem.SetupHomeChrome
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.designsystem.WideOrCompactSetup
import com.jacobrozell.dartbuddy.ui.players.CustomBotCreationSheet
import com.jacobrozell.dartbuddy.ui.players.displayNameComposable
import com.jacobrozell.dartbuddy.ui.play.rules.LearnToPlayButton
import com.jacobrozell.dartbuddy.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    onStartMatch: (MatchType, String) -> Unit,
    onResumeMatch: (String, String) -> Unit,
    viewModel: PlayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showsEditOptions by remember { mutableStateOf(false) }
    var showModePicker by remember { mutableStateOf(false) }
    var showAddPlayer by remember { mutableStateOf(false) }
    var showAddBot by remember { mutableStateOf(false) }
    var showCustomBot by remember { mutableStateOf(false) }
    var showRulesSheet by remember { mutableStateOf(false) }
    var newPlayerName by remember { mutableStateOf("") }

    val validationMessages = state.validationErrors.mapNotNull { key ->
        when (key) {
            // Shown inline in RosterSection — iOS filters minimum-players from footer when roster is empty.
            "empty_roster" -> null
            "cricket_min" -> stringResource(R.string.play_setup_cricketMinPlayers)
            else -> null
        }
    }
    val configSummary = viewModel.setupConfigSummary()

    SetupHomeChrome(
        onStart = { viewModel.startMatch(onStartMatch) },
        startTitle = if (state.isStarting) {
            stringResource(R.string.play_setup_startingButton)
        } else {
            stringResource(R.string.play_setup_startButton)
        },
        startEnabled = state.canStart,
        startLoading = state.isStarting,
        validationErrors = validationMessages,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DS.s4)) {
            BrandAppTitle(title = stringResource(R.string.brand_title))
            state.errorMessage?.let { ErrorBanner(it) }
            state.activeMatch?.let { match ->
                ResumeBanner(
                    matchType = match.matchType,
                    onClick = { onResumeMatch(match.matchType, match.id) },
                )
            }

            WideOrCompactSetup(
                wideContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(DS.s4), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DS.s4)) {
                            ModeSection(
                                state,
                                showsEditOptions,
                                onToggleOptions = { showsEditOptions = !showsEditOptions },
                                onChangeMode = { showModePicker = true },
                                onLearnToPlay = { showRulesSheet = true },
                                configSummary = configSummary,
                            )
                            if (showsEditOptions) ModeOptionsSection(state, viewModel)
                        }
                        Column(Modifier.weight(1f)) {
                            RosterSection(state, viewModel, onAddPlayer = { showAddPlayer = true }, onAddBot = { showAddBot = true })
                        }
                    }
                },
                compactContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(DS.s4)) {
                        ModeSection(
                            state,
                            showsEditOptions,
                            onToggleOptions = { showsEditOptions = !showsEditOptions },
                            onChangeMode = { showModePicker = true },
                            onLearnToPlay = { showRulesSheet = true },
                            configSummary = configSummary,
                        )
                        if (showsEditOptions) ModeOptionsSection(state, viewModel)
                        RosterSection(state, viewModel, onAddPlayer = { showAddPlayer = true }, onAddBot = { showAddBot = true })
                    }
                },
            )
        }
    }

    if (showModePicker) {
        ModalBottomSheet(onDismissRequest = { showModePicker = false }) {
            Column(Modifier.padding(DS.s4), verticalArrangement = Arrangement.spacedBy(DS.s2)) {
                Text(stringResource(R.string.play_setup_changeMode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                ModePickerRow(MatchType.X01, state.setupMode == MatchType.X01) {
                    viewModel.setSetupMode(MatchType.X01)
                    showModePicker = false
                }
                ModePickerRow(MatchType.Cricket, state.setupMode == MatchType.Cricket) {
                    viewModel.setSetupMode(MatchType.Cricket)
                    showModePicker = false
                }
                Spacer(Modifier.height(DS.s4))
            }
        }
    }

    if (showAddPlayer) {
        AlertDialog(
            onDismissRequest = { showAddPlayer = false },
            title = { Text(stringResource(R.string.players_add)) },
            text = {
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it },
                    label = { Text(stringResource(R.string.players_new_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlayerName.isNotBlank()) {
                        viewModel.addPlayer(newPlayerName.trim())
                        newPlayerName = ""
                        showAddPlayer = false
                    }
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlayer = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showAddBot) {
        ModalBottomSheet(onDismissRequest = { showAddBot = false }) {
            Column(Modifier.padding(DS.s4), verticalArrangement = Arrangement.spacedBy(DS.s2)) {
                Text(stringResource(R.string.play_setup_addBot_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                BotDifficulty.entries.forEach { difficulty ->
                    TextButton(
                        onClick = {
                            viewModel.addPresetBot(difficulty)
                            showAddBot = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(difficulty.displayNameComposable(), color = Brand.Green, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = { showAddBot = false; showCustomBot = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.custom_bot_add_menu), color = Brand.ProBot, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(DS.s4))
            }
        }
    }

    if (showCustomBot) {
        CustomBotCreationSheet(
            onDismiss = { showCustomBot = false },
            onCreate = { name, metrics -> viewModel.addCustomBot(name, metrics) },
        )
    }

    if (showRulesSheet) {
        com.jacobrozell.dartbuddy.ui.play.rules.GameRulesSheet(
            initialMode = state.setupMode,
            onDismiss = { showRulesSheet = false },
        )
    }
}

@Composable
private fun ModeSection(
    state: PlayUiState,
    showsEditOptions: Boolean,
    onToggleOptions: () -> Unit,
    onChangeMode: () -> Unit,
    onLearnToPlay: () -> Unit,
    configSummary: String,
) {
    val modeName = if (state.setupMode == MatchType.X01) stringResource(R.string.play_x01_title) else stringResource(R.string.play_cricket_title)

    Column(verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.play_setup_selectedMode),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            LearnToPlayButton(onClick = onLearnToPlay, compact = true)
        }
        SetupSectionCard {
            Row(verticalAlignment = Alignment.Top) {
                GameModeBadge(state.setupMode.name)
                Column(Modifier.weight(1f).padding(start = DS.s3)) {
                    Text(modeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(configSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onChangeMode) {
                    Text(stringResource(R.string.play_setup_changeMode), color = Brand.Green, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onToggleOptions) {
                    Icon(
                        if (showsEditOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Brand.Green,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (showsEditOptions) stringResource(R.string.play_setup_hideOptions) else stringResource(R.string.play_setup_editOptions),
                        color = Brand.Green,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOptionsSection(state: PlayUiState, viewModel: PlayViewModel) {
    SetupSectionCard {
        if (state.setupMode == MatchType.X01) {
            X01SetupOptions(state, viewModel)
        } else {
            CricketSetupOptions(state, viewModel)
        }
    }
}

@Composable
private fun RosterSection(state: PlayUiState, viewModel: PlayViewModel, onAddPlayer: () -> Unit, onAddBot: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s3)) {
        Text(stringResource(R.string.add_to_match_section), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        if (state.selectedPlayers.isEmpty()) {
            SetupEmptyHint(message = stringResource(R.string.play_setup_playersEmptyHint))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.play_setup_turnOrder),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RandomOrderToggle(state.randomOrder, viewModel::toggleRandomOrder)
            }
            if (state.randomOrder) {
                Text(
                    stringResource(R.string.play_setup_turnOrderRandomHint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.selectedPlayers.forEachIndexed { index, player ->
                TurnOrderRow(
                    player = player,
                    position = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.selectedPlayers.lastIndex,
                    onMoveUp = { viewModel.moveSelectedPlayer(index, index - 1) },
                    onMoveDown = { viewModel.moveSelectedPlayer(index, index + 1) },
                    onRemove = { viewModel.removeFromSelection(player.id) },
                )
            }
        }

        RosterActionButtons(onAddPlayer = onAddPlayer, onAddBot = onAddBot)

        if (state.availablePlayers.isNotEmpty()) {
            state.availablePlayers.forEach { player ->
                AvailablePlayerRow(player) { viewModel.togglePlayer(player.id) }
            }
        }
    }
}

@Composable
private fun RosterActionButtons(onAddPlayer: () -> Unit, onAddBot: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        TextButton(onClick = onAddPlayer) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Brand.Green)
            Text(stringResource(R.string.play_setup_addPlayers), color = Brand.Green, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onAddBot) {
            Text(stringResource(R.string.play_setup_addBot), color = Brand.Green, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RandomOrderToggle(checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        Icon(
            if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (checked) Brand.Green else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.play_setup_randomOrder))
    }
}

@Composable
private fun TurnOrderRow(
    player: PlayerSummary,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardElevatedDark else Brand.CardElevatedLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DS.s1)
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(horizontal = DS.s3, vertical = DS.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.s3),
    ) {
        Text(stringResource(R.string.common_playerOrdinal, position), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PlayerAvatar(name = player.name, colorToken = player.colorToken, playerId = player.id, size = 28.dp)
        Text(player.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (player.isBot) {
            Text(
                if (player.botKindRaw == "custom") {
                    com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator.customMetricsFor(player)?.let {
                        stringResource(R.string.custom_bot_badge_format, it.x01Average, it.cricketMPR)
                    } ?: stringResource(R.string.play_setup_botBadge)
                } else {
                    stringResource(R.string.play_setup_botBadge)
                },
                style = MaterialTheme.typography.labelSmall,
                color = Brand.ProBot,
            )
        }
        if (canMoveUp) IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up") }
        if (canMoveDown) IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down") }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.play_setup_removeFromMatch))
        }
    }
}

@Composable
private fun AvailablePlayerRow(player: PlayerSummary, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
            .padding(vertical = DS.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.s3),
    ) {
        PlayerAvatar(name = player.name, colorToken = player.colorToken, playerId = player.id, size = 28.dp)
        Text(player.name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Add, contentDescription = null, tint = Brand.Green)
    }
}

@Composable
private fun ModePickerRow(type: MatchType, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = DS.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.s3),
    ) {
        GameModeBadge(type.name)
        Text(if (type == MatchType.X01) stringResource(R.string.play_x01_title) else stringResource(R.string.play_cricket_title), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun X01SetupOptions(state: PlayUiState, viewModel: PlayViewModel) {
    val checkoutLabels = mapOf(
        X01CheckoutMode.SingleOut to stringResource(R.string.x01_checkout_straightOut),
        X01CheckoutMode.DoubleOut to stringResource(R.string.x01_checkout_doubleOut),
        X01CheckoutMode.MasterOut to stringResource(R.string.x01_checkout_masterOut),
    )
    val checkInLabels = mapOf(
        X01CheckInMode.StraightIn to stringResource(R.string.x01_checkin_straightIn),
        X01CheckInMode.DoubleIn to stringResource(R.string.x01_checkin_doubleIn),
        X01CheckInMode.MasterIn to stringResource(R.string.x01_checkin_masterIn),
    )
    val legFormatLabels = mapOf(
        X01LegFormat.FirstTo to stringResource(R.string.x01_legFormat_firstTo),
        X01LegFormat.BestOf to stringResource(R.string.x01_legFormat_bestOf),
    )
    HorizontalOptionChips(stringResource(R.string.settings_label_start_score), X01StartScores.all, state.x01StartScore, viewModel::updateX01StartScore)
    HorizontalOptionChips(stringResource(R.string.settings_label_checkout), X01CheckoutMode.entries, state.x01CheckoutMode, viewModel::updateX01CheckoutMode) { checkoutLabels[it] ?: it.name }
    HorizontalOptionChips(stringResource(R.string.settings_label_checkin), X01CheckInMode.entries, state.x01CheckInMode, viewModel::updateX01CheckInMode) { checkInLabels[it] ?: it.name }
    HorizontalOptionChips(stringResource(R.string.settings_label_leg_format), X01LegFormat.entries, state.x01LegFormat, viewModel::updateX01LegFormat) { legFormatLabels[it] ?: it.name }
    HorizontalOptionChips(stringResource(R.string.settings_label_legs_to_win), listOf(1, 2, 3, 5), state.x01LegsToWin, viewModel::updateX01LegsToWin)
    ToggleRow(stringResource(R.string.settings_sets_enabled), state.x01SetsEnabled, viewModel::updateX01SetsEnabled)
    if (state.x01SetsEnabled) {
        HorizontalOptionChips(stringResource(R.string.settings_sets_enabled), listOf(1, 2, 3), state.x01SetsToWin, viewModel::updateX01SetsToWin)
    }
}

@Composable
private fun CricketSetupOptions(state: PlayUiState, viewModel: PlayViewModel) {
    val scoringLabels = mapOf(
        CricketScoringMode.Standard to stringResource(R.string.play_cricket_mode_normal),
        CricketScoringMode.CutThroat to stringResource(R.string.play_cricket_mode_cutThroat),
    )
    val legFormatLabels = mapOf(
        X01LegFormat.FirstTo to stringResource(R.string.x01_legFormat_firstTo),
        X01LegFormat.BestOf to stringResource(R.string.x01_legFormat_bestOf),
    )
    ToggleRow(stringResource(R.string.settings_points_scoring), state.cricketPointsEnabled, viewModel::updateCricketPointsEnabled)
    if (state.cricketPointsEnabled) {
        HorizontalOptionChips(stringResource(R.string.settings_label_scoring_mode), CricketScoringMode.entries, state.cricketScoringMode, viewModel::updateCricketScoringMode) { scoringLabels[it] ?: it.name }
    }
    HorizontalOptionChips(stringResource(R.string.settings_label_leg_format), X01LegFormat.entries, state.cricketLegFormat, viewModel::updateCricketLegFormat) { legFormatLabels[it] ?: it.name }
    HorizontalOptionChips(stringResource(R.string.settings_label_legs_to_win), listOf(1, 2, 3, 5), state.cricketLegsToWin, viewModel::updateCricketLegsToWin)
    ToggleRow(stringResource(R.string.settings_sets_enabled), state.cricketSetsEnabled, viewModel::updateCricketSetsEnabled)
    if (state.cricketSetsEnabled) {
        HorizontalOptionChips(stringResource(R.string.settings_sets_enabled), listOf(1, 2, 3), state.cricketSetsToWin, viewModel::updateCricketSetsToWin)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
