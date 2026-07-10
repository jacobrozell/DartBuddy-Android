package com.jacobrozell.dartbuddy.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.data.repository.PlayerRepository
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.domain.service.PlayerListStatsLoader
import com.jacobrozell.dartbuddy.domain.service.PlayerListSummary
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator
import com.jacobrozell.dartbuddy.domain.bots.CustomBotMetrics
import com.jacobrozell.dartbuddy.domain.surface.ProductSurface
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken
import com.jacobrozell.dartbuddy.ui.players.displayNameComposable
import com.jacobrozell.dartbuddy.ui.components.PlayerAvatar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.ui.components.AddPlayerSheet
import com.jacobrozell.dartbuddy.ui.designsystem.ScoreboardEmptyState
import com.jacobrozell.dartbuddy.ui.designsystem.BrandRootScreenTitle
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.designsystem.PrimaryActionButton
import com.jacobrozell.dartbuddy.ui.designsystem.TabRootScaffold
import com.jacobrozell.dartbuddy.ui.theme.Brand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlayersLoadState {
    Loading,
    Ready,
    Error,
}

@HiltViewModel
class PlayersViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
) : ViewModel() {
    private val query = kotlinx.coroutines.flow.MutableStateFlow("")
    private val _loadState = MutableStateFlow(PlayersLoadState.Loading)
    val loadState: StateFlow<PlayersLoadState> = _loadState.asStateFlow()

    val allPlayers = playerRepository.observeAllPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val players = combine(
        allPlayers,
        query,
    ) { list, q ->
        val needle = q.trim().lowercase()
        if (needle.isBlank()) list else list.filter { it.name.lowercase().contains(needle) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val listSummaries = matchRepository.observeHistory()
        .map { PlayerListStatsLoader.build(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loadState.value = PlayersLoadState.Loading
            try {
                playerRepository.observeAllPlayers().first()
                matchRepository.observeHistory().first()
                _loadState.value = PlayersLoadState.Ready
            } catch (_: Exception) {
                _loadState.value = PlayersLoadState.Error
            }
        }
    }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    onOpenPlayer: (String) -> Unit,
    viewModel: PlayersViewModel = hiltViewModel(),
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    val allPlayers by viewModel.allPlayers.collectAsStateWithLifecycle()
    val listSummaries by viewModel.listSummaries.collectAsStateWithLifecycle()
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    var newName by rememberSaveable { mutableStateOf("") }
    var newAvatar by rememberSaveable { mutableStateOf(PlayerAvatarStyle.Dart.raw) }
    var newColor by rememberSaveable { mutableStateOf(PlayerColorToken.Green.raw) }
    var showAddPlayer by rememberSaveable { mutableStateOf(false) }
    var showAddBot by rememberSaveable { mutableStateOf(false) }
    var showCustomBot by rememberSaveable { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    val active = players.filter { !it.isArchived }
    val archived = players.filter { it.isArchived }

    val searchA11y = stringResource(R.string.players_search_accessibility)
    val addFabA11y = stringResource(R.string.players_add)
    val activeHumans = active.filter { !it.isBot }.sortedBy { it.name.lowercase() }
    val activeBots = active.filter { it.isBot }.sortedBy { it.name.lowercase() }
    val isDatabaseEmpty = allPlayers.isEmpty()
    val isSearchNoResults = !isDatabaseEmpty && players.isEmpty() && search.isNotBlank()

    when (loadState) {
        PlayersLoadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .a11yId(AccessibilityIds.PLAYERS_LOADING),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Brand.Green)
            }
            return
        }
        PlayersLoadState.Error -> {
            TabRootScaffold {
                ScoreboardEmptyState(
                    title = stringResource(R.string.error_title),
                    message = stringResource(R.string.players_error_load),
                    icon = Icons.Default.People,
                )
                PrimaryActionButton(
                    title = stringResource(R.string.common_retry),
                    onClick = viewModel::refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            return
        }
        PlayersLoadState.Ready -> Unit
    }

    TabRootScaffold(
        bottomBar = {
            if (isDatabaseEmpty) {
                PrimaryActionButton(
                    title = stringResource(R.string.players_add),
                    onClick = { showAddPlayer = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DS.s4, vertical = DS.s2),
                )
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { fabMenuExpanded = true },
                    containerColor = Brand.Green,
                    contentColor = Brand.InkOnBright,
                    modifier = Modifier
                        .a11yId(AccessibilityIds.PLAYERS_ADD_FAB)
                        .semantics { contentDescription = addFabA11y },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.players_add)) },
                        onClick = {
                            fabMenuExpanded = false
                            showAddPlayer = true
                        },
                        leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.play_setup_addBot)) },
                        onClick = {
                            fabMenuExpanded = false
                            showAddBot = true
                        },
                        leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) },
                    )
                }
            }
        },
    ) {
        BrandRootScreenTitle(title = stringResource(R.string.players_title))

        OutlinedTextField(
            value = search,
            onValueChange = {
                search = it
                viewModel.setQuery(it)
            },
            label = { Text(stringResource(R.string.players_search_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .a11yId(AccessibilityIds.PLAYERS_SEARCH)
                .semantics { contentDescription = searchA11y },
        )

        when {
            isDatabaseEmpty -> {
                ScoreboardEmptyState(
                    title = stringResource(R.string.players_empty_title),
                    message = stringResource(R.string.players_empty_description),
                    icon = Icons.Default.People,
                    modifier = Modifier.a11yId(AccessibilityIds.PLAYERS_EMPTY),
                )
            }
            isSearchNoResults -> {
                ScoreboardEmptyState(
                    title = stringResource(R.string.players_empty_title),
                    message = stringResource(R.string.players_search_no_results),
                    icon = Icons.Default.People,
                    modifier = Modifier.a11yId(AccessibilityIds.PLAYERS_SEARCH_EMPTY),
                )
            }
        }

        if (activeHumans.isNotEmpty()) {
            Text(stringResource(R.string.players_section_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            activeHumans.forEach { player ->
                SwipeablePlayerRow(
                    player = player,
                    listSummary = listSummaries[player.id],
                    onOpen = { onOpenPlayer(player.id) },
                    onArchive = { viewModel.archivePlayer(player.id) },
                )
            }
        }

        if (activeBots.isNotEmpty()) {
            Text(stringResource(R.string.players_bots_section_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            activeBots.forEach { player ->
                SwipeablePlayerRow(
                    player = player,
                    listSummary = null,
                    onOpen = { onOpenPlayer(player.id) },
                    onArchive = { viewModel.archivePlayer(player.id) },
                )
            }
        }

        if (archived.isNotEmpty()) {
            Text(stringResource(R.string.players_archived_section), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            archived.forEach { player ->
                PlayerRow(
                    player = player,
                    listSummary = listSummaries[player.id],
                    archived = true,
                    onOpen = { onOpenPlayer(player.id) },
                    onArchive = {},
                    onUnarchive = { viewModel.unarchivePlayer(player.id) },
                )
            }
        }
    }

    if (showAddPlayer) {
        AddPlayerSheet(
            name = newName,
            onNameChange = { newName = it },
            avatarStyle = PlayerAvatarStyle.resolve(newAvatar),
            onAvatarStyleChange = { newAvatar = it.raw },
            colorToken = PlayerColorToken.resolve(newColor),
            onColorChange = { newColor = it.raw },
            onConfirm = {
                if (newName.isNotBlank()) {
                    viewModel.addPlayer(newName.trim(), newColor, newAvatar)
                    newName = ""
                    newAvatar = PlayerAvatarStyle.Dart.raw
                    newColor = PlayerColorToken.Green.raw
                    showAddPlayer = false
                }
            },
            onDismiss = { showAddPlayer = false },
        )
    }

    if (showAddBot) {
        ModalBottomSheet(onDismissRequest = { showAddBot = false }) {
            Column(Modifier.padding(DS.s4), verticalArrangement = Arrangement.spacedBy(DS.s2)) {
                Text(
                    stringResource(R.string.play_setup_addBot_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                BotDifficulty.entries.forEach { difficulty ->
                    TextButton(
                        onClick = {
                            viewModel.addBot(difficulty)
                            showAddBot = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(difficulty.displayNameComposable(), color = Brand.Green, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (ProductSurface.showsCustomBots) {
                    TextButton(
                        onClick = { showAddBot = false; showCustomBot = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePlayerRow(
    player: PlayerSummary,
    listSummary: PlayerListSummary?,
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
            PlayerRow(
                player = player,
                listSummary = listSummary,
                onOpen = onOpen,
                onArchive = onArchive,
            )
        },
    )
}

@Composable
private fun PlayerRow(
    player: PlayerSummary,
    listSummary: PlayerListSummary? = null,
    archived: Boolean = false,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: (() -> Unit)? = null,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    val rowA11y = playerRowAccessibilityLabel(player, archived, listSummary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .clickable(onClick = onOpen)
            .semantics { contentDescription = rowA11y }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DS.s2),
            ) {
                Text(player.name, fontWeight = FontWeight.SemiBold)
                if (player.isPrimary) {
                    Surface(
                        color = Brand.Green,
                        shape = RoundedCornerShape(DS.radiusSm),
                    ) {
                        Text(
                            stringResource(R.string.players_primary_badge),
                            modifier = Modifier.padding(horizontal = DS.s2, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Brand.InkOnBright,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (player.isBot) {
                botBadgeText(player)?.let { badge ->
                    Text(
                        badge,
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.ProBot,
                    )
                }
            } else if (listSummary != null && listSummary.games > 0) {
                Text(
                    stringResource(R.string.players_list_record, listSummary.games, listSummary.wins),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun playerRowAccessibilityLabel(
    player: PlayerSummary,
    archived: Boolean,
    listSummary: PlayerListSummary? = null,
): String {
    val suffix = buildString {
        if (player.isBot) {
            val difficulty = BotTurnCoordinator.difficultyFor(player)
            if (player.botKindRaw == "custom") {
                BotTurnCoordinator.customMetricsFor(player)?.let { metrics ->
                    append(
                        stringResource(
                            R.string.custom_bot_row_accessibility_suffix,
                            metrics.x01Average,
                            metrics.cricketMPR,
                        ),
                    )
                } ?: append(stringResource(R.string.players_row_bot_suffix, stringResource(R.string.players_bot_label)))
            } else if (difficulty != null) {
                append(stringResource(R.string.players_row_bot_suffix, difficulty.displayNameComposable()))
            }
        } else if (listSummary != null && listSummary.games > 0) {
            append(
                stringResource(
                    R.string.players_list_record_accessibility,
                    listSummary.games,
                    listSummary.wins,
                ),
            )
        }
        if (player.isPrimary) {
            append(stringResource(R.string.players_row_primary_suffix))
        }
        if (archived || player.isArchived) {
            append(stringResource(R.string.players_row_archived_suffix))
        }
    }
    return stringResource(R.string.players_row_accessibility_format, player.name, suffix)
}
