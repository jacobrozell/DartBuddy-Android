package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.repository.MatchRepository
import com.jacobrozell.dartbuddy.domain.engine.CricketEngine
import com.jacobrozell.dartbuddy.domain.engine.CricketState
import com.jacobrozell.dartbuddy.domain.engine.CricketTurnEvent
import com.jacobrozell.dartbuddy.domain.engine.X01Engine
import com.jacobrozell.dartbuddy.domain.engine.X01State
import com.jacobrozell.dartbuddy.domain.engine.X01TurnEvent
import com.jacobrozell.dartbuddy.domain.model.MatchConfigCricket
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchSessionService @Inject constructor(
    private val matchRepository: MatchRepository,
    private val json: Json,
) {
    suspend fun loadX01Session(matchId: String): X01Session? {
        val entity = matchRepository.getMatch(matchId) ?: return null
        val config = decodeConfig(entity) as? MatchConfigPayload.X01 ?: return null
        val events = decodeX01Events(entity.eventsJson)
        val state = if (entity.snapshotJson != null) {
            json.decodeFromString<X01State>(entity.snapshotJson)
        } else {
            val playerIds = json.decodeFromString<List<String>>(entity.participantIdsJson)
            X01Engine.makeInitialState(config.config, playerIds)
        }
        return X01Session(entity, config.config, state, events)
    }

    suspend fun loadCricketSession(matchId: String): CricketSession? {
        val entity = matchRepository.getMatch(matchId) ?: return null
        val config = decodeConfig(entity) as? MatchConfigPayload.Cricket ?: return null
        val events = decodeCricketEvents(entity.eventsJson)
        val state = if (entity.snapshotJson != null) {
            json.decodeFromString<CricketState>(entity.snapshotJson)
        } else {
            val playerIds = json.decodeFromString<List<String>>(entity.participantIdsJson)
            CricketEngine.makeInitialState(config.config, playerIds)
        }
        return CricketSession(entity, config.config, state, events)
    }

    suspend fun submitX01Turn(session: X01Session, enteredTotal: Int?, darts: List<DartInput>?): X01Session {
        val outcome = X01Engine.submitTurn(session.state, enteredTotal, darts)
        val events = session.events + outcome.event
        val status = if (outcome.updatedState.isComplete) MatchStatus.Completed else MatchStatus.InProgress
        val now = Instant.now().toEpochMilli()
        val updatedEntity = session.entity.copy(
            snapshotJson = json.encodeToString(outcome.updatedState),
            eventsJson = json.encodeToString(events),
            status = status.name,
            winnerPlayerId = outcome.updatedState.winnerPlayerId,
            updatedAtEpochMillis = now,
            completedAtEpochMillis = if (status == MatchStatus.Completed) now else null,
        )
        matchRepository.saveMatch(updatedEntity)
        return session.copy(state = outcome.updatedState, events = events, entity = updatedEntity)
    }

    suspend fun submitCricketTurn(session: CricketSession, darts: List<DartInput>): CricketSession {
        val outcome = CricketEngine.submitTurn(session.state, darts)
        val events = session.events + outcome.event
        val status = if (outcome.updatedState.isComplete) MatchStatus.Completed else MatchStatus.InProgress
        val now = Instant.now().toEpochMilli()
        val updatedEntity = session.entity.copy(
            snapshotJson = json.encodeToString(outcome.updatedState),
            eventsJson = json.encodeToString(events),
            status = status.name,
            winnerPlayerId = outcome.updatedState.winnerPlayerId,
            updatedAtEpochMillis = now,
            completedAtEpochMillis = if (status == MatchStatus.Completed) now else null,
        )
        matchRepository.saveMatch(updatedEntity)
        return session.copy(state = outcome.updatedState, events = events, entity = updatedEntity)
    }

    suspend fun undoLastX01Turn(session: X01Session): X01Session? {
        if (session.events.isEmpty()) return null
        val config = session.config
        val playerIds = json.decodeFromString<List<String>>(session.entity.participantIdsJson)
        val remainingEvents = session.events.dropLast(1)
        val state = if (remainingEvents.isEmpty()) {
            X01Engine.makeInitialState(config, playerIds)
        } else {
            X01Engine.replay(config, playerIds, remainingEvents)
        }
        val updatedEntity = session.entity.copy(
            snapshotJson = json.encodeToString(state),
            eventsJson = json.encodeToString(remainingEvents),
            status = MatchStatus.InProgress.name,
            winnerPlayerId = null,
            completedAtEpochMillis = null,
            updatedAtEpochMillis = Instant.now().toEpochMilli(),
        )
        matchRepository.saveMatch(updatedEntity)
        return session.copy(state = state, events = remainingEvents, entity = updatedEntity)
    }

    suspend fun undoLastCricketTurn(session: CricketSession): CricketSession? {
        if (session.events.isEmpty()) return null
        val config = session.config
        val playerIds = json.decodeFromString<List<String>>(session.entity.participantIdsJson)
        val remainingEvents = session.events.dropLast(1)
        val state = if (remainingEvents.isEmpty()) {
            CricketEngine.makeInitialState(config, playerIds)
        } else {
            CricketEngine.replay(config, playerIds, remainingEvents)
        }
        val updatedEntity = session.entity.copy(
            snapshotJson = json.encodeToString(state),
            eventsJson = json.encodeToString(remainingEvents),
            status = MatchStatus.InProgress.name,
            winnerPlayerId = null,
            completedAtEpochMillis = null,
            updatedAtEpochMillis = Instant.now().toEpochMilli(),
        )
        matchRepository.saveMatch(updatedEntity)
        return session.copy(state = state, events = remainingEvents, entity = updatedEntity)
    }

    suspend fun forfeitMatch(
        session: X01Session,
        forfeitingPlayerId: String,
        winnerPlayerId: String?,
    ): X01Session = session.copy(entity = forfeitEntity(session.entity, session.events, forfeitingPlayerId, winnerPlayerId))

    suspend fun forfeitMatch(
        session: CricketSession,
        forfeitingPlayerId: String,
        winnerPlayerId: String?,
    ): CricketSession = session.copy(entity = forfeitEntity(session.entity, session.events, forfeitingPlayerId, winnerPlayerId))

    private suspend fun forfeitEntity(
        entity: MatchEntity,
        events: List<*>,
        forfeitingPlayerId: String,
        winnerPlayerId: String?,
    ): MatchEntity {
        require(events.isNotEmpty()) { "Cannot forfeit without scored events" }
        val participantIds = json.decodeFromString<List<String>>(entity.participantIdsJson)
        require(forfeitingPlayerId in participantIds) { "Invalid forfeiting player" }
        if (participantIds.size > 1) {
            require(winnerPlayerId != null && winnerPlayerId in participantIds && winnerPlayerId != forfeitingPlayerId)
        }
        val now = Instant.now().toEpochMilli()
        val updatedEntity = entity.copy(
            status = MatchStatus.Forfeited.name,
            winnerPlayerId = winnerPlayerId,
            forfeitedByPlayerId = forfeitingPlayerId,
            updatedAtEpochMillis = now,
            completedAtEpochMillis = now,
        )
        matchRepository.saveMatch(updatedEntity)
        return updatedEntity
    }

    suspend fun getMatchEntity(matchId: String): MatchEntity? = matchRepository.getMatch(matchId)

    private fun decodeConfig(entity: MatchEntity): MatchConfigPayload =
        json.decodeFromString(entity.configJson)

    private fun decodeX01Events(raw: String): List<X01TurnEvent> =
        if (raw.isBlank() || raw == "[]") emptyList() else json.decodeFromString(raw)

    private fun decodeCricketEvents(raw: String): List<CricketTurnEvent> =
        if (raw.isBlank() || raw == "[]") emptyList() else json.decodeFromString(raw)
}

data class X01Session(
    val entity: MatchEntity,
    val config: MatchConfigX01,
    val state: X01State,
    val events: List<X01TurnEvent>,
)

data class CricketSession(
    val entity: MatchEntity,
    val config: MatchConfigCricket,
    val state: CricketState,
    val events: List<CricketTurnEvent>,
)
