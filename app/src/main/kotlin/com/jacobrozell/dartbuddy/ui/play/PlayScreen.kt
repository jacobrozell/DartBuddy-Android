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
import com.jacobrozell.dartbuddy.ui.components.AddPlayerSheet
import com.jacobrozell.dartbuddy.ui.components.TurnOrderDragHandle
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.surface.ProductSurface
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import com.jacobrozell.dartbuddy.domain.model.X01StartScores
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.designsystem.BrandAppTitle
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.GameModeBadge
import com.jacobrozell.dartbuddy.ui.play.setup.SetupOptionChip
import com.jacobrozell.dartbuddy.ui.play.setup.SetupOptionChipGrid
import com.jacobrozell.dartbuddy.ui.play.setup.setupChipBackground
import com.jacobrozell.dartbuddy.ui.play.setup.setupCricketModeBackground
import com.jacobrozell.dartbuddy.ui.designsystem.ErrorBanner
import com.jacobrozell.dartbuddy.ui.designsystem.HintBanner
import com.jacobrozell.dartbuddy.ui.designsystem.SetupEmptyHint
import com.jacobrozell.dartbuddy.ui.designsystem.SetupHomeChrome
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.components.ResumeBanner
import com.jacobrozell.dartbuddy.ui.designsystem.rememberOptionLabelMaps
import com.jacobrozell.dartbuddy.ui.designsystem.WideOrCompactSetup
import com.jacobrozell.dartbuddy.ui.players.CustomBotCreationSheet
import com.jacobrozell.dartbuddy.ui.players.displayNameComposable
import com.jacobrozell.dartbuddy.ui.play.rules.LearnToPlayButton
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.support.accessibility.selectedModeAccessibilityLabel
import com.jacobrozell.dartbuddy.ui.layout.rememberLayoutContext
import com.jacobrozell.dartbuddy.ui.layout.usesAccessibilitySetupHomeLayout
import com.jacobrozell.dartbuddy.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    onStartMatch: (MatchType, String) -> Unit,
    onResumeMatch: (String, String) -> Unit,
    viewModel: PlayViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.consumePendingNavigationSetup()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.consumePendingNavigationSetup()
    }

    DisposableEffect(state.activeMatch?.id) {
        viewModel.logPlayHomeReady(state.activeMatch != null)
        onDispose { }
    }

    var showsEditOptions by remember { mutableStateOf(false) }
    var showModePicker by remember { mutableStateOf(false) }
    var showAddPlayer by remember { mutableStateOf(false) }
    var showAddBot by remember { mutableStateOf(false) }
    var showCustomBot by remember { mutableStateOf(false) }
    var showRulesSheet by remember { mutableStateOf(false) }
    var newPlayerName by remember { mutableStateOf("") }
    var newPlayerAvatar by remember { mutableStateOf(PlayerAvatarStyle.Dart) }
    var newPlayerColor by remember { mutableStateOf(PlayerColorToken.Green) }

    val layout = rememberLayoutContext()
    val usesAxSetup = usesAccessibilitySetupHomeLayout(layout)
    val displayValidationErrors = state.validationErrors.filter { key ->
        !(key == "empty_roster" && state.selectedPlayers.isEmpty())
    }
    val validationMessages = if (usesAxSetup) {
        emptyList()
    } else {
        displayValidationErrors.mapNotNull { key ->
            when (key) {
                "empty_roster" -> null
                "cricket_min" -> stringResource(R.string.play_setup_cricketMinPlayers)
                else -> null
            }
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
            state.rematchErrorMessage?.let { message -> HintBanner(message) }
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
                            if (usesAxSetup) {
                                SetupValidationSection(displayValidationErrors)
                            }
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
                        if (usesAxSetup) {
                            SetupValidationSection(displayValidationErrors)
                        }
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
        AddPlayerSheet(
            name = newPlayerName,
            onNameChange = { newPlayerName = it },
            avatarStyle = newPlayerAvatar,
            onAvatarStyleChange = { newPlayerAvatar = it },
            colorToken = newPlayerColor,
            onColorChange = { newPlayerColor = it },
            onConfirm = {
                if (newPlayerName.isNotBlank()) {
                    viewModel.addPlayer(
                        newPlayerName.trim(),
                        newPlayerColor.raw,
                        newPlayerAvatar.raw,
                    )
                    newPlayerName = ""
                    newPlayerAvatar = PlayerAvatarStyle.Dart
                    newPlayerColor = PlayerColorToken.Green
                    showAddPlayer = false
                }
            },
            onDismiss = { showAddPlayer = false },
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
                if (ProductSurface.showsCustomBots) {
                    TextButton(onClick = { showAddBot = false; showCustomBot = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.custom_bot_add_menu), color = Brand.ProBot, fontWeight = FontWeight.SemiBold)
                    }
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
private fun SetupValidationSection(errors: List<String>) {
    if (errors.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        errors.forEach { key ->
            when (key) {
                "cricket_min" -> ErrorBanner(stringResource(R.string.play_setup_cricketMinPlayers))
            }
        }
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
    val modeA11y = selectedModeAccessibilityLabel(modeName, configSummary)

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
            LearnToPlayButton(onClick = onLearnToPlay, compact = true, testTag = AccessibilityIds.SETUP_LEARN_TO_PLAY)
        }
        SetupSectionCard(
            modifier = Modifier.semantics { contentDescription = modeA11y },
        ) {
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
                TextButton(onClick = onToggleOptions, modifier = Modifier.a11yId(AccessibilityIds.SETUP_EDIT_OPTIONS)) {
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
                    reorderEnabled = !state.randomOrder,
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
        TextButton(onClick = onAddPlayer, modifier = Modifier.a11yId(AccessibilityIds.SETUP_ADD_PLAYER)) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Brand.Green)
            Text(stringResource(R.string.play_setup_addPlayers), color = Brand.Green, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onAddBot, modifier = Modifier.a11yId(AccessibilityIds.SETUP_ADD_BOT)) {
            Text(stringResource(R.string.play_setup_addBot), color = Brand.Green, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RandomOrderToggle(checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onToggle).a11yId(AccessibilityIds.SETUP_RANDOM_ORDER),
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
    reorderEnabled: Boolean,
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
        PlayerAvatar(
            name = player.name,
            colorToken = player.colorToken,
            playerId = player.id,
            avatarStyleRaw = player.avatarStyleRaw,
            isBot = player.isBot,
            size = 28.dp,
        )
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
        if (canMoveUp) {
            IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.play_setup_move_up))
            }
        }
        if (canMoveDown) {
            IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.play_setup_move_down))
            }
        }
        TurnOrderDragHandle(
            enabled = reorderEnabled,
            onDragUp = onMoveUp,
            onDragDown = onMoveDown,
        )
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
        PlayerAvatar(
            name = player.name,
            colorToken = player.colorToken,
            playerId = player.id,
            avatarStyleRaw = player.avatarStyleRaw,
            isBot = player.isBot,
            size = 28.dp,
        )
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
    val labels = rememberOptionLabelMaps()
    val chipBg = setupChipBackground()
    SetupOptionChipGrid(
        cells = listOf<@Composable () -> Unit>(
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_label_start_score),
                    value = state.x01StartScore.toString(),
                    backgroundColor = chipBg,
                    options = com.jacobrozell.dartbuddy.domain.model.X01StartScores.all,
                    labelFor = { it.toString() },
                    onSelect = viewModel::updateX01StartScore,
                    testTag = AccessibilityIds.SETUP_START_SCORE_CHIP,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_label_checkout),
                    value = labels.checkout(state.x01CheckoutMode),
                    backgroundColor = chipBg,
                    options = com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode.entries,
                    labelFor = labels.checkout,
                    onSelect = viewModel::updateX01CheckoutMode,
                    testTag = AccessibilityIds.SETUP_CHECKOUT_CHIP,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_sets_enabled),
                    value = if (state.x01SetsEnabled) state.x01SetsToWin.toString() else stringResource(R.string.common_off),
                    backgroundColor = chipBg,
                    options = listOf(1, 2, 3),
                    labelFor = { it.toString() },
                    onSelect = {
                        viewModel.updateX01SetsEnabled(true)
                        viewModel.updateX01SetsToWin(it)
                    },
                    testTag = AccessibilityIds.SETUP_SETS_CHIP,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_label_leg_format),
                    value = labels.legFormat(state.x01LegFormat),
                    backgroundColor = chipBg,
                    options = com.jacobrozell.dartbuddy.domain.model.X01LegFormat.entries,
                    labelFor = labels.legFormat,
                    onSelect = viewModel::updateX01LegFormat,
                    testTag = AccessibilityIds.SETUP_LEG_FORMAT_CHIP,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_label_checkin),
                    value = labels.checkIn(state.x01CheckInMode),
                    backgroundColor = chipBg,
                    options = com.jacobrozell.dartbuddy.domain.model.X01CheckInMode.entries,
                    labelFor = labels.checkIn,
                    onSelect = viewModel::updateX01CheckInMode,
                    testTag = AccessibilityIds.SETUP_CHECKIN_CHIP,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_label_legs_to_win),
                    value = state.x01LegsToWin.toString(),
                    backgroundColor = chipBg,
                    options = listOf(1, 2, 3, 5),
                    labelFor = { it.toString() },
                    onSelect = viewModel::updateX01LegsToWin,
                    testTag = AccessibilityIds.SETUP_LEGS_CHIP,
                )
            },
        ),
    )
}

@Composable
private fun CricketSetupOptions(state: PlayUiState, viewModel: PlayViewModel) {
    val labels = rememberOptionLabelMaps()
    val chipBg = setupChipBackground()
    val modeBg = setupCricketModeBackground()
    val onLabel = stringResource(R.string.common_on)
    val offLabel = stringResource(R.string.common_off)
    SetupOptionChipGrid(
        cells = listOf<@Composable () -> Unit>(
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_points_scoring),
                    value = if (state.cricketPointsEnabled) onLabel else offLabel,
                    backgroundColor = chipBg,
                    options = listOf(true, false),
                    labelFor = { if (it) onLabel else offLabel },
                    onSelect = viewModel::updateCricketPointsEnabled,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_label_scoring_mode),
                    value = labels.cricketScoring(state.cricketScoringMode),
                    backgroundColor = modeBg,
                    options = com.jacobrozell.dartbuddy.domain.model.CricketScoringMode.entries,
                    labelFor = labels.cricketScoring,
                    onSelect = viewModel::updateCricketScoringMode,
                    enabled = state.cricketPointsEnabled,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_label_leg_format),
                    value = labels.legFormat(state.cricketLegFormat),
                    backgroundColor = chipBg,
                    options = com.jacobrozell.dartbuddy.domain.model.X01LegFormat.entries,
                    labelFor = labels.legFormat,
                    onSelect = viewModel::updateCricketLegFormat,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_label_legs_to_win),
                    value = state.cricketLegsToWin.toString(),
                    backgroundColor = chipBg,
                    options = listOf(1, 2, 3, 5),
                    labelFor = { it.toString() },
                    onSelect = viewModel::updateCricketLegsToWin,
                )
            },
            {
                SetupOptionChip(
                    title = stringResource(R.string.settings_sets_enabled),
                    value = if (state.cricketSetsEnabled) state.cricketSetsToWin.toString() else stringResource(R.string.common_off),
                    backgroundColor = chipBg,
                    options = listOf(1, 2, 3),
                    labelFor = { it.toString() },
                    onSelect = {
                        viewModel.updateCricketSetsEnabled(true)
                        viewModel.updateCricketSetsToWin(it)
                    },
                )
            },
        ),
    )
}
