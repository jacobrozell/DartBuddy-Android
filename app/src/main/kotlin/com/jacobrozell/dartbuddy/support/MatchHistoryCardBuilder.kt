package com.jacobrozell.dartbuddy.support

import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.domain.engine.CricketEngine
import com.jacobrozell.dartbuddy.domain.engine.CricketState
import com.jacobrozell.dartbuddy.domain.engine.X01Engine
import com.jacobrozell.dartbuddy.domain.engine.X01State
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import kotlinx.serialization.json.Json

data class HistoryStanding(
    val playerId: String,
    val name: String,
    val isWinner: Boolean,
    val sets: Int,
    val legs: Int,
    val score: Int,
)

data class HistoryListRow(
    val id: String,
    val matchType: MatchType,
    val status: MatchStatus,
    val updatedAtEpochMillis: Long,
    val configText: String,
    val standings: List<HistoryStanding>,
    val isForfeited: Boolean,
    val isFinished: Boolean,
)

object MatchHistoryCardBuilder {
    fun build(entity: MatchEntity, json: Json, l10n: L10n): HistoryListRow {
        val names: List<String> = json.decodeFromString(entity.participantNamesJson)
        val ids: List<String> = json.decodeFromString(entity.participantIdsJson)
        val nameById = ids.zip(names).toMap()
        val type = MatchType.valueOf(entity.matchType)
        val status = MatchStatus.valueOf(entity.status)
        val config = runCatching { json.decodeFromString<MatchConfigPayload>(entity.configJson) }.getOrNull()

        val (configText, standings) = when (type) {
            MatchType.X01 -> {
                val x01Config = (config as? MatchConfigPayload.X01)?.config
                val state: X01State = entity.snapshotJson?.let { json.decodeFromString(it) }
                    ?: x01Config?.let { X01Engine.makeInitialState(it, ids) }
                    ?: X01State(
                        config = (config as MatchConfigPayload.X01).config,
                        players = emptyList(),
                        currentPlayerIndex = 0,
                        legIndex = 0,
                        setIndex = 0,
                        turnIndex = 0,
                    )
                val text = x01Config?.let { MatchConfigText.x01InlineConfig(it, l10n) }
                    ?: l10n.get(com.jacobrozell.dartbuddy.R.string.play_mode_x01)
                val rows = state.players.map { player ->
                    HistoryStanding(
                        playerId = player.playerId,
                        name = nameById[player.playerId] ?: player.playerId,
                        isWinner = player.playerId == entity.winnerPlayerId,
                        sets = player.setsWon,
                        legs = player.legsWon,
                        score = player.remainingScore,
                    )
                }
                text to sortStandings(rows)
            }
            MatchType.Cricket -> {
                val cricketConfig = (config as? MatchConfigPayload.Cricket)?.config
                val state: CricketState = entity.snapshotJson?.let { json.decodeFromString(it) }
                    ?: cricketConfig?.let { CricketEngine.makeInitialState(it, ids) }
                    ?: CricketState(
                        config = (config as MatchConfigPayload.Cricket).config,
                        players = emptyList(),
                        currentPlayerIndex = 0,
                        roundIndex = 0,
                        turnIndex = 0,
                    )
                val text = cricketConfig?.let { MatchConfigText.cricketInlineConfig(it, l10n) }
                    ?: l10n.get(com.jacobrozell.dartbuddy.R.string.play_mode_cricket)
                val rows = state.players.map { player ->
                    HistoryStanding(
                        playerId = player.playerId,
                        name = nameById[player.playerId] ?: player.playerId,
                        isWinner = player.playerId == entity.winnerPlayerId,
                        sets = player.setsWon,
                        legs = player.legsWon,
                        score = player.score,
                    )
                }
                text to sortStandings(rows)
            }
        }

        return HistoryListRow(
            id = entity.id,
            matchType = type,
            status = status,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            configText = configText,
            standings = standings.ifEmpty {
                ids.zip(names).map { (id, name) ->
                    HistoryStanding(
                        playerId = id,
                        name = name,
                        isWinner = id == entity.winnerPlayerId,
                        sets = 0,
                        legs = 0,
                        score = 0,
                    )
                }
            },
            isForfeited = status == MatchStatus.Forfeited,
            isFinished = status == MatchStatus.Completed || status == MatchStatus.Forfeited,
        )
    }

    private fun sortStandings(standings: List<HistoryStanding>): List<HistoryStanding> =
        standings.sortedWith(
            compareByDescending<HistoryStanding> { it.isWinner }
                .thenBy { it.score },
        )
}
