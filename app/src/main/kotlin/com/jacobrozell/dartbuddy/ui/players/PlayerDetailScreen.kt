package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.bots.BotKind
import com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator
import com.jacobrozell.dartbuddy.domain.bots.displayProfile
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.designsystem.TabRootScaffold
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun PlayerDetailScreen(
    playerId: String,
    onBack: () -> Unit,
    onOpenMatch: (String) -> Unit = {},
    onPractice: (String, String, MatchType) -> Unit = { _, _, _ -> },
    viewModel: PlayerDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by rememberSaveable(playerId) { mutableStateOf("") }
    var notes by rememberSaveable(playerId) { mutableStateOf("") }
    var isPrimary by rememberSaveable(playerId) { mutableStateOf(false) }
    var colorToken by rememberSaveable(playerId) { mutableStateOf(PlayerColorToken.Green.raw) }
    var avatarStyle by rememberSaveable(playerId) { mutableStateOf(PlayerAvatarStyle.Dart.raw) }
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(playerId) { viewModel.load(playerId) }
    LaunchedEffect(uiState.player) {
        uiState.player?.let { player ->
            name = player.name
            notes = player.notes.orEmpty()
            isPrimary = player.isPrimary
            colorToken = player.colorToken ?: PlayerColorToken.defaultForPlayer(player.id).raw
            avatarStyle = player.avatarStyleRaw ?: PlayerAvatarStyle.defaultForPlayer(player.id, player.isBot).raw
        }
    }

    val player = uiState.player
    val title = when {
        player == null -> stringResource(R.string.players_detail_title)
        player.isBot -> stringResource(R.string.players_bot_detail_title)
        else -> stringResource(R.string.players_detail_title)
    }

    TabRootScaffold {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (uiState.isExporting) {
            CircularProgressIndicator()
        }
        uiState.exportErrorRes?.let {
            Text(stringResource(it), color = Brand.Red)
        }

        player?.let { detail ->
            PlayerIdentityCard(player = detail, colorToken = colorToken)

            if (detail.isArchived) {
                Text(
                    stringResource(R.string.players_archived),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!detail.isBot) {
                TrainingPartnerSection(
                    trainingBot = uiState.trainingBot,
                    x01Eligibility = uiState.x01Eligibility,
                    cricketEligibility = uiState.cricketEligibility,
                    isCreating = uiState.isCreatingTrainingBot,
                    errorRes = uiState.trainingBotErrorRes,
                    calibratedX01 = viewModel.calibratedSummary(MatchType.X01)?.toDoubleOrNull()?.let {
                        stringResource(R.string.training_bot_calibrated_x01_format, it)
                    },
                    calibratedCricket = viewModel.calibratedSummary(MatchType.Cricket)?.toDoubleOrNull()?.let {
                        stringResource(R.string.training_bot_calibrated_cricket_format, it)
                    },
                    onCreate = { viewModel.createTrainingBot(detail.id) },
                    onPractice = { mode ->
                        uiState.trainingBot?.let { bot -> onPractice(detail.id, bot.id, mode) }
                    },
                )
            }

            if (detail.isBot) {
                BotDetailExtras(detail, viewModel)
            }

            PlayerDetailStatsContent(
                isLoading = uiState.isLoadingStats,
                x01 = uiState.x01,
                cricket = uiState.cricket,
                x01TrendPoints = uiState.x01TrendPoints,
                recentMatches = uiState.recentMatches,
                lastPlayedLabel = uiState.lastPlayedLabel,
                onSelectMatch = onOpenMatch,
            )

            if (showEdit) {
                PlayerEditSheet(
                    name = name,
                    notes = notes,
                    colorToken = colorToken,
                    avatarStyle = PlayerAvatarStyle.resolve(avatarStyle),
                    isPrimary = isPrimary,
                    nameEnabled = !detail.isBot || detail.botKindRaw == BotKind.CUSTOM,
                    showAvatarPicker = !detail.isBot || detail.botKindRaw == BotKind.CUSTOM,
                    onNameChange = { name = it },
                    onNotesChange = { notes = it },
                    onColorChange = { colorToken = it },
                    onAvatarStyleChange = { avatarStyle = it.raw },
                    onPrimaryChange = { isPrimary = it },
                    onSave = {
                        viewModel.save(detail.id, name, colorToken, avatarStyle, notes, isPrimary)
                        showEdit = false
                    },
                    onDismiss = { showEdit = false },
                    onDelete = {
                        showEdit = false
                        showDeleteConfirm = true
                    },
                )
            } else {
                PlayerDetailActionRow(
                    isArchived = detail.isArchived,
                    showExport = !detail.isBot,
                    isExporting = uiState.isExporting,
                    onEdit = { showEdit = true },
                    onArchiveToggle = {
                        if (detail.isArchived) {
                            viewModel.unarchive(detail.id)
                        } else {
                            viewModel.archive(detail.id, onBack)
                        }
                    },
                    onExport = { viewModel.exportPlayer(context, detail.id) },
                )
            }
        }
    }

    if (showDeleteConfirm && player != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.players_delete)) },
            text = { Text(stringResource(R.string.players_delete_confirm, name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(playerId, onBack)
                    showDeleteConfirm = false
                }) { Text(stringResource(R.string.common_confirm), color = Brand.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun BotDetailExtras(
    detail: PlayerSummary,
    viewModel: PlayerDetailViewModel,
) {
    when (detail.botKindRaw) {
        BotKind.CUSTOM -> {
            BotTurnCoordinator.customConfigurationFor(detail)?.let { initialConfig ->
                var botConfiguration by androidx.compose.runtime.remember(detail.id) { mutableStateOf(initialConfig) }
                LaunchedEffect(detail.botDifficultyRaw) {
                    BotTurnCoordinator.customConfigurationFor(detail)?.let { botConfiguration = it }
                }
                SetupSectionCard {
                    CustomBotDetailEditor(
                        configuration = botConfiguration,
                        onConfigurationChange = {
                            botConfiguration = it
                            viewModel.updateCustomBotConfiguration(detail.id, it)
                        },
                    )
                }
            }
        }
        else -> {
            val difficulty = BotTurnCoordinator.difficultyFor(detail) ?: BotDifficulty.Medium
            SetupSectionCard {
                BotDifficultyStatsSection(profile = difficulty.skillProfile.displayProfile)
            }
        }
    }
}
