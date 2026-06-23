package com.jacobrozell.dartbuddy.support.navigation

import android.net.Uri

sealed interface DeepLinkDestination {
    data class Tab(val route: String) : DeepLinkDestination
    data object PlayHome : DeepLinkDestination
    data object PlaySetup : DeepLinkDestination
    data object PlayResume : DeepLinkDestination
    data class PlayMatch(val matchId: String) : DeepLinkDestination
    data class HistoryMatch(val matchId: String) : DeepLinkDestination
    data class PlayerDetail(val playerId: String) : DeepLinkDestination
}

object DeepLinkParser {
    fun parse(uri: Uri?): DeepLinkDestination? {
        if (uri == null) return null
        if (uri.scheme != "dartbuddy") return null

        val segments = buildList {
            uri.host?.takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(uri.pathSegments.filter { it.isNotBlank() })
        }
        return parseSegments(segments)
    }

    internal fun parseSegments(segments: List<String>): DeepLinkDestination? {
        if (segments.isEmpty()) return DeepLinkDestination.PlayHome

        val normalized = when {
            segments.first() == "v1" -> segments.drop(1)
            else -> segments
        }
        if (normalized.isEmpty()) return DeepLinkDestination.PlayHome

        return when (normalized.first()) {
            "play" -> when (normalized.getOrNull(1)) {
                "resume" -> DeepLinkDestination.PlayResume
                "setup" -> DeepLinkDestination.PlaySetup
                "match" -> normalized.getOrNull(2)?.let { DeepLinkDestination.PlayMatch(it) }
                null -> DeepLinkDestination.PlayHome
                else -> DeepLinkDestination.PlayHome
            }
            "tab" -> normalized.getOrNull(1)?.let { tab ->
                when (tab) {
                    "play" -> DeepLinkDestination.Tab("play")
                    "players" -> DeepLinkDestination.Tab("players")
                    "activity" -> DeepLinkDestination.Tab("activity")
                    "settings" -> DeepLinkDestination.Tab("settings")
                    else -> null
                }
            }
            "activity" -> when {
                normalized.getOrNull(1) == "history" &&
                    normalized.getOrNull(2) == "match" &&
                    normalized.getOrNull(3) != null ->
                    DeepLinkDestination.HistoryMatch(normalized[3])
                else -> DeepLinkDestination.Tab("activity")
            }
            "players" -> when (normalized.size) {
                1 -> DeepLinkDestination.Tab("players")
                2 -> DeepLinkDestination.PlayerDetail(normalized[1])
                else -> null
            }
            "settings" -> DeepLinkDestination.Tab("settings")
            else -> null
        }
    }

    fun pathFrom(uri: Uri?): String? = pathFrom(parse(uri))

    fun pathFrom(destination: DeepLinkDestination?): String? = when (destination) {
        is DeepLinkDestination.Tab -> destination.route
        DeepLinkDestination.PlayHome -> "play"
        DeepLinkDestination.PlaySetup -> "play/setup"
        DeepLinkDestination.PlayResume -> "play/resume"
        is DeepLinkDestination.PlayMatch -> "play/match/${destination.matchId}"
        is DeepLinkDestination.HistoryMatch -> "activity/history/match/${destination.matchId}"
        is DeepLinkDestination.PlayerDetail -> "players/${destination.playerId}"
        null -> null
    }
}
