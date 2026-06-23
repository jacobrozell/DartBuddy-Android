package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.jacobrozell.dartbuddy.domain.bots.CustomBotMetrics
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.ui.players.displayNameComposable
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import com.jacobrozell.dartbuddy.ui.designsystem.BrandRootScreenTitle
import com.jacobrozell.dartbuddy.ui.designsystem.DS
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

    fun addPlayer(name: String, colorToken: String? = null, avatarStyleRaw: String? = null) {
        viewModelScope.launch { playerRepository.create(name, colorToken, avatarStyleRaw) }
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

@Composable
fun PlayersScreen(
    onOpenPlayer: (String) -> Unit,
    viewModel: PlayersViewModel = hiltViewModel(),
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    var newName by rememberSaveable { mutableStateOf("") }
    var newAvatar by rememberSaveable { mutableStateOf(PlayerAvatarStyle.Dart.raw) }
    var newColor by rememberSaveable { mutableStateOf(PlayerColorToken.Green.raw) }
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
            PlayerCreateFields(
                name = newName,
                onNameChange = { newName = it },
                avatarStyle = PlayerAvatarStyle.resolve(newAvatar),
                onAvatarStyleChange = { newAvatar = it.raw },
                colorToken = PlayerColorToken.resolve(newColor),
                onColorChange = { newColor = it.raw },
            )
            PrimaryActionButton(
                title = stringResource(R.string.players_add),
                onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.addPlayer(newName.trim(), newColor, newAvatar)
                        newName = ""
                        newAvatar = PlayerAvatarStyle.Dart.raw
                        newColor = PlayerColorToken.Green.raw
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
                SwipeablePlayerRow(
                    player = player,
                    onOpen = { onOpenPlayer(player.id) },
                    onArchive = { viewModel.archivePlayer(player.id) },
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePlayerRow(
    player: PlayerSummary,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onArchive()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DS.radiusSm))
                    .background(Brand.RedAccent)
                    .padding(horizontal = DS.s4),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    stringResource(R.string.players_archive),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        content = {
            PlayerRow(player = player, onOpen = onOpen, onArchive = onArchive)
        },
    )
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
        PlayerAvatar(
            name = player.name,
            colorToken = player.colorToken,
            playerId = player.id,
            avatarStyleRaw = player.avatarStyleRaw,
            isBot = player.isBot,
            size = 36.dp,
        )
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
