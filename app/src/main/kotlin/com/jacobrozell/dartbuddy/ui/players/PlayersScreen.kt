package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator
import com.jacobrozell.dartbuddy.domain.bots.CustomBotConfiguration
import com.jacobrozell.dartbuddy.domain.bots.CustomBotMetrics
import com.jacobrozell.dartbuddy.ui.players.displayNameComposable
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.designsystem.BrandRootScreenTitle
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.HorizontalOptionChips
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.designsystem.SetupSectionCard
import com.jacobrozell.dartbuddy.ui.designsystem.TabRootScaffold
import com.jacobrozell.dartbuddy.ui.theme.Brand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayersViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
) : ViewModel() {
    private val query = kotlinx.coroutines.flow.MutableStateFlow("")

    val players = combine(
        playerRepository.observeAllPlayers(),
        query,
    ) { list, q ->
        val needle = q.trim().lowercase()
        if (needle.isBlank()) list else list.filter { it.name.lowercase().contains(needle) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun addPlayer(name: String) {
        viewModelScope.launch { playerRepository.create(name) }
    }

    fun addBot(difficulty: BotDifficulty) {
        viewModelScope.launch { playerRepository.createPresetBot(difficulty) }
    }

    fun addCustomBot(name: String, metrics: CustomBotMetrics) {
        viewModelScope.launch { playerRepository.createCustomBot(name, metrics) }
    }

    fun archivePlayer(id: String) {
        viewModelScope.launch { playerRepository.archive(id) }
    }

    fun unarchivePlayer(id: String) {
        viewModelScope.launch { playerRepository.unarchive(id) }
    }
}

@HiltViewModel
class PlayerDetailViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
) : ViewModel() {
    private val _player = kotlinx.coroutines.flow.MutableStateFlow<PlayerSummary?>(null)
    val player = _player.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun load(id: String) {
        viewModelScope.launch { _player.value = playerRepository.getById(id) }
    }

    fun save(id: String, name: String, colorToken: String?, notes: String?, isPrimary: Boolean) {
        viewModelScope.launch {
            playerRepository.update(id, name, colorToken, notes, isPrimary)
            _player.value = playerRepository.getById(id)
        }
    }

    fun updateCustomBotConfiguration(id: String, configuration: CustomBotConfiguration) {
        viewModelScope.launch {
            playerRepository.updateCustomBotConfiguration(id, configuration)
            _player.value = playerRepository.getById(id)
        }
    }

    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            playerRepository.delete(id)
            onDone()
        }
    }

    fun archive(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            playerRepository.archive(id)
            onDone()
        }
    }
}

@Composable
fun PlayersScreen(
    onOpenPlayer: (String) -> Unit,
    viewModel: PlayersViewModel = hiltViewModel(),
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    var newName by rememberSaveable { mutableStateOf("") }
    var showAddBot by rememberSaveable { mutableStateOf(false) }
    var showCustomBot by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    val active = players.filter { !it.isArchived }
    val archived = players.filter { it.isArchived }

    TabRootScaffold {
        BrandRootScreenTitle(title = stringResource(R.string.players_title))

        OutlinedTextField(
            value = search,
            onValueChange = {
                search = it
                viewModel.setQuery(it)
            },
            label = { Text(stringResource(R.string.players_search)) },
            modifier = Modifier.fillMaxWidth(),
        )

        SetupSectionCard {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.players_new_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryActionButton(
                title = stringResource(R.string.players_add),
                onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.addPlayer(newName.trim())
                        newName = ""
                    }
                },
                enabled = newName.isNotBlank(),
            )
            TextButton(onClick = { showAddBot = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_setup_addBot), color = Brand.Green)
            }
        }

        if (active.isNotEmpty()) {
            Text(stringResource(R.string.players_active_section), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            active.forEach { player ->
                PlayerRow(player = player, onOpen = { onOpenPlayer(player.id) }, onArchive = { viewModel.archivePlayer(player.id) })
            }
        }

        if (archived.isNotEmpty()) {
            Text(stringResource(R.string.players_archived_section), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            archived.forEach { player ->
                PlayerRow(player = player, archived = true, onOpen = { onOpenPlayer(player.id) }, onArchive = {}, onUnarchive = { viewModel.unarchivePlayer(player.id) })
            }
        }
    }

    if (showAddBot) {
        AlertDialog(
            onDismissRequest = { showAddBot = false },
            title = { Text(stringResource(R.string.play_setup_addBot_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DS.s1)) {
                    BotDifficulty.entries.forEach { difficulty ->
                        TextButton(onClick = {
                            viewModel.addBot(difficulty)
                            showAddBot = false
                        }) { Text(difficulty.displayNameComposable()) }
                    }
                    TextButton(onClick = { showAddBot = false; showCustomBot = true }) {
                        Text(stringResource(R.string.custom_bot_add_menu), color = Brand.ProBot)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddBot = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showCustomBot) {
        CustomBotCreationSheet(
            onDismiss = { showCustomBot = false },
            onCreate = { name, metrics -> viewModel.addCustomBot(name, metrics) },
        )
    }
}

@Composable
fun PlayerDetailScreen(
    playerId: String,
    onBack: () -> Unit,
    viewModel: PlayerDetailViewModel = hiltViewModel(),
) {
    val player by viewModel.player.collectAsStateWithLifecycle()
    var name by rememberSaveable(playerId) { mutableStateOf("") }
    var notes by rememberSaveable(playerId) { mutableStateOf("") }
    var isPrimary by rememberSaveable(playerId) { mutableStateOf(false) }
    var colorToken by rememberSaveable(playerId) { mutableStateOf(PlayerColorToken.Green.raw) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(playerId) { viewModel.load(playerId) }
    androidx.compose.runtime.LaunchedEffect(player) {
        player?.let {
            name = it.name
            notes = it.notes.orEmpty()
            isPrimary = it.isPrimary
            colorToken = it.colorToken ?: PlayerColorToken.defaultForPlayer(it.id).raw
        }
    }

    TabRootScaffold {
        TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
        Text(stringResource(R.string.players_detail_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        player?.let { detail ->
            SetupSectionCard {
                PlayerAvatar(name = detail.name, colorToken = colorToken, playerId = detail.id, size = 56.dp)
                if (detail.isBot) {
                    Text(
                        if (detail.botKindRaw == "custom") {
                            BotTurnCoordinator.customMetricsFor(detail)?.let { metrics ->
                                stringResource(R.string.custom_bot_badge_format, metrics.x01Average, metrics.cricketMPR)
                            } ?: stringResource(R.string.players_bot_label)
                        } else {
                            stringResource(R.string.players_bot_label)
                        },
                        color = Brand.ProBot,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (detail.botKindRaw == "custom") {
                    BotTurnCoordinator.customConfigurationFor(detail)?.let { initialConfig ->
                        var botConfiguration by remember(detail.id) { mutableStateOf(initialConfig) }
                        androidx.compose.runtime.LaunchedEffect(detail.botDifficultyRaw) {
                            BotTurnCoordinator.customConfigurationFor(detail)?.let { botConfiguration = it }
                        }
                        CustomBotDetailEditor(
                            configuration = botConfiguration,
                            onConfigurationChange = {
                                botConfiguration = it
                                viewModel.updateCustomBotConfiguration(detail.id, it)
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (!detail.isBot || detail.botKindRaw == "custom") name = it },
                    label = { Text(stringResource(R.string.players_new_name)) },
                    enabled = !detail.isBot || detail.botKindRaw == "custom",
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.players_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalOptionChips(
                    label = "Color",
                    options = PlayerColorToken.all.take(8),
                    selected = PlayerColorToken.resolve(colorToken),
                    onSelect = { colorToken = it.raw },
                    labelFor = { it.raw },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.players_primary))
                    androidx.compose.material3.Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
                }
                PrimaryActionButton(
                    title = stringResource(R.string.players_edit),
                    onClick = { viewModel.save(detail.id, name, colorToken, notes, isPrimary) },
                    enabled = name.isNotBlank(),
                )
            }

            if (!detail.isArchived) {
                TextButton(onClick = { viewModel.archive(detail.id, onBack) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.players_archive), color = Brand.Red)
                }
            }
            TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.players_delete), color = Brand.Red)
            }
        }
    }

    if (showDeleteConfirm) {
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
private fun PlayerRow(
    player: PlayerSummary,
    archived: Boolean = false,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: (() -> Unit)? = null,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .clickable(onClick = onOpen)
            .padding(DS.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.s3),
    ) {
        PlayerAvatar(name = player.name, colorToken = player.colorToken, playerId = player.id, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(player.name, fontWeight = FontWeight.SemiBold)
            if (player.isBot) {
                Text(
                    if (player.botKindRaw == "custom") {
                        BotTurnCoordinator.customMetricsFor(player)?.let {
                            stringResource(R.string.custom_bot_badge_format, it.x01Average, it.cricketMPR)
                        } ?: stringResource(R.string.players_bot_label)
                    } else {
                        stringResource(R.string.play_setup_botBadge)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.ProBot,
                )
            }
            if (archived) {
                Text(stringResource(R.string.players_archived), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (archived && onUnarchive != null) {
            TextButton(onClick = onUnarchive) { Text(stringResource(R.string.players_unarchive), color = Brand.Green) }
        } else if (!archived) {
            TextButton(onClick = onArchive) { Text(stringResource(R.string.players_archive), color = Brand.Red) }
        }
    }
}
