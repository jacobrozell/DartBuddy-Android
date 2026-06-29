package com.jacobrozell.dartbuddy.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TextButton
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.ui.activity.ActivityScreen
import com.jacobrozell.dartbuddy.ui.activity.HistoryDetailScreen
import com.jacobrozell.dartbuddy.ui.play.PlayScreen
import com.jacobrozell.dartbuddy.ui.play.match.CricketMatchScreen
import com.jacobrozell.dartbuddy.ui.play.match.X01MatchScreen
import com.jacobrozell.dartbuddy.ui.play.summary.MatchSummaryScreen
import com.jacobrozell.dartbuddy.ui.players.PlayerDetailScreen
import com.jacobrozell.dartbuddy.ui.players.PlayersScreen
import com.jacobrozell.dartbuddy.ui.settings.SettingsScreen
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.jacobrozell.dartbuddy.support.accessibility.a11yId
import com.jacobrozell.dartbuddy.support.motion.TabContentReveal
import com.jacobrozell.dartbuddy.ui.theme.Brand

enum class RootTab(val route: String, val labelRes: Int, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    Play("play", R.string.tab_play, Icons.Filled.Home, Icons.Outlined.Home),
    Players("players", R.string.tab_players, Icons.Filled.People, Icons.Outlined.People),
    Activity("activity", R.string.tab_activity, Icons.Filled.History, Icons.Outlined.History),
    Settings("settings", R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}

private val tabRoutes = RootTab.entries.map { it.route }.toSet()

@Composable
fun DartBuddyNavHost(
    deepLinkPath: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabRoutes
    val activeMatch by shellViewModel.activeMatch.collectAsStateWithLifecycle()
    val updateOffer by shellViewModel.updateOffer.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val navBg = if (dark) Brand.CardDark else Brand.CardLight

    LaunchedEffect(Unit) {
        shellViewModel.checkForPlayStoreUpdate()
    }

    var pendingResumeDeepLink by remember { mutableStateOf(false) }

    LaunchedEffect(deepLinkPath) {
        when (deepLinkPath?.trim('/')) {
            "play", "play/setup" -> {
                shellViewModel.logDeepLinkReceived()
                navController.navigate(RootTab.Play.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                shellViewModel.logDeepLinkApplied()
                onDeepLinkConsumed()
            }
            "play/resume" -> pendingResumeDeepLink = true
            "players" -> {
                shellViewModel.logDeepLinkReceived()
                navController.navigate(RootTab.Players.route)
                shellViewModel.logDeepLinkApplied()
                onDeepLinkConsumed()
            }
            "activity" -> {
                shellViewModel.logDeepLinkReceived()
                navController.navigate(RootTab.Activity.route)
                shellViewModel.logDeepLinkApplied()
                onDeepLinkConsumed()
            }
            "settings" -> {
                shellViewModel.logDeepLinkReceived()
                navController.navigate(RootTab.Settings.route)
                shellViewModel.logDeepLinkApplied()
                onDeepLinkConsumed()
            }
            else -> when {
                deepLinkPath?.startsWith("play/match/") == true -> {
                    shellViewModel.logDeepLinkReceived()
                    val matchId = deepLinkPath.removePrefix("play/match/")
                    val route = shellViewModel.resolveActiveMatchRoute(matchId)
                    if (route != null) {
                        navController.navigate(route) {
                            popUpTo(RootTab.Play.route) { saveState = true }
                        }
                    } else {
                        navController.navigate(RootTab.Play.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    shellViewModel.logDeepLinkApplied()
                    onDeepLinkConsumed()
                }
                deepLinkPath?.startsWith("activity/history/match/") == true -> {
                    shellViewModel.logDeepLinkReceived()
                    val matchId = deepLinkPath.removePrefix("activity/history/match/")
                    navController.navigate(RootTab.Activity.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    navController.navigate("history/$matchId")
                    shellViewModel.logDeepLinkApplied()
                    onDeepLinkConsumed()
                }
                deepLinkPath?.startsWith("players/") == true -> {
                    shellViewModel.logDeepLinkReceived()
                    val playerId = deepLinkPath.removePrefix("players/")
                    val route = shellViewModel.playerDetailRoute(playerId)
                    if (route != null) {
                        navController.navigate(route)
                    } else {
                        navController.navigate(RootTab.Players.route)
                    }
                    shellViewModel.logDeepLinkApplied()
                    onDeepLinkConsumed()
                }
                deepLinkPath != null -> onDeepLinkConsumed()
            }
        }
    }

    LaunchedEffect(pendingResumeDeepLink) {
        if (!pendingResumeDeepLink) return@LaunchedEffect
        shellViewModel.logDeepLinkReceived()
        val match = withTimeoutOrNull(1_000) {
            shellViewModel.activeMatch.filterNotNull().first()
        }
        if (match != null) {
            shellViewModel.logMatchResumed(match, startSource = "deepLink")
            navController.navigate("match/${match.matchType.lowercase()}/${match.id}") {
                popUpTo(RootTab.Play.route) { saveState = true }
            }
        } else {
            navController.navigate(RootTab.Play.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        shellViewModel.logDeepLinkApplied()
        pendingResumeDeepLink = false
        onDeepLinkConsumed()
    }

    updateOffer?.let {
        AlertDialog(
            onDismissRequest = { shellViewModel.dismissPlayStoreUpdate() },
            title = { Text(stringResource(R.string.app_update_title)) },
            text = { Text(stringResource(R.string.app_update_message)) },
            confirmButton = {
                TextButton(onClick = {
                    shellViewModel.playStoreUpdateIntent()?.let { intent ->
                        context.startActivity(intent)
                    }
                }) {
                    Text(stringResource(R.string.app_update_update))
                }
            },
            dismissButton = {
                TextButton(onClick = { shellViewModel.dismissPlayStoreUpdate() }) {
                    Text(stringResource(R.string.app_update_not_now))
                }
            },
        )
    }

    Scaffold(
        containerColor = if (dark) Brand.BackgroundDark else Brand.BackgroundLight,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = navBg) {
                    RootTab.entries.forEach { tab ->
                        val label = stringResource(tab.labelRes)
                        val selected = currentRoute == tab.route
                        val showBadge = tab == RootTab.Play && activeMatch != null
                        val tabTestTag = when (tab) {
                            RootTab.Play -> AccessibilityIds.TAB_PLAY
                            RootTab.Players -> AccessibilityIds.TAB_PLAYERS
                            RootTab.Activity -> AccessibilityIds.TAB_ACTIVITY
                            RootTab.Settings -> AccessibilityIds.TAB_SETTINGS
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                shellViewModel.logTabPresented(tab.route)
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.a11yId(tabTestTag),
                            icon = {
                                BadgedBox(badge = { if (showBadge) Badge() }) {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                    )
                                }
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Brand.Green,
                                selectedTextColor = Brand.Green,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Brand.Green.copy(alpha = 0.18f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = RootTab.Play.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(RootTab.Play.route) {
                TabContentReveal {
                    PlayScreen(
                        onStartMatch = { type, matchId ->
                            navController.navigate("match/${type.name.lowercase()}/$matchId")
                        },
                        onResumeMatch = { type, matchId ->
                            navController.navigate("match/${type.lowercase()}/$matchId")
                        },
                    )
                }
            }
            composable(RootTab.Players.route) {
                TabContentReveal {
                    PlayersScreen(onOpenPlayer = { id -> navController.navigate("players/$id") })
                }
            }
            composable("players/{playerId}") { entry ->
                PlayerDetailScreen(
                    playerId = entry.arguments?.getString("playerId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenMatch = { matchId -> navController.navigate("history/$matchId") },
                    onPractice = { humanId, botId, mode ->
                        shellViewModel.practiceSetupStore.enqueuePractice(humanId, botId, mode)
                        navController.navigate(RootTab.Play.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(RootTab.Activity.route) {
                TabContentReveal {
                    ActivityScreen(
                        onOpenMatch = { matchId ->
                            navController.navigate("history/$matchId")
                        },
                        onResumeMatch = { type, matchId ->
                            shellViewModel.logMatchResumedById(matchId, "resume")
                            navController.navigate("match/${type.lowercase()}/$matchId")
                        },
                        onStartMatch = {
                            navController.navigate(RootTab.Play.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
            composable(RootTab.Settings.route) {
                TabContentReveal {
                    SettingsScreen()
                }
            }
            composable("match/x01/{matchId}") { entry ->
                X01MatchScreen(
                    matchId = entry.arguments?.getString("matchId").orEmpty(),
                    onComplete = { id -> navController.navigate("summary/$id") { popUpTo(RootTab.Play.route) } },
                    onExit = { navController.popBackStack() },
                )
            }
            composable("match/cricket/{matchId}") { entry ->
                CricketMatchScreen(
                    matchId = entry.arguments?.getString("matchId").orEmpty(),
                    onComplete = { id -> navController.navigate("summary/$id") { popUpTo(RootTab.Play.route) } },
                    onExit = { navController.popBackStack() },
                )
            }
            composable("summary/{matchId}") { entry ->
                val matchId = entry.arguments?.getString("matchId").orEmpty()
                MatchSummaryScreen(
                    matchId = matchId,
                    onDone = { navController.popBackStack(RootTab.Play.route, inclusive = false) },
                    onRematch = {
                        navController.popBackStack(RootTab.Play.route, inclusive = false)
                    },
                    onViewStatistics = { navController.navigate("history/$matchId") },
                    onResumeMatch = { type, id ->
                        navController.navigate("match/${type.name.lowercase()}/$id") {
                            popUpTo(RootTab.Play.route)
                        }
                    },
                )
            }
            composable("history/{matchId}") { entry ->
                HistoryDetailScreen(
                    matchId = entry.arguments?.getString("matchId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onResume = { type, id -> navController.navigate("match/${type.lowercase()}/$id") },
                    onViewSummary = { id -> navController.navigate("summary/$id") },
                )
            }
        }
    }
}
