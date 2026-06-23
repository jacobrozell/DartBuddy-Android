package com.jacobrozell.dartbuddy.data.repository

import com.jacobrozell.dartbuddy.data.local.dao.MatchDao
import com.jacobrozell.dartbuddy.data.local.dao.PlayerDao
import com.jacobrozell.dartbuddy.data.local.dao.SettingsDao
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.local.entity.PlayerEntity
import com.jacobrozell.dartbuddy.data.local.entity.SettingsEntity
import com.jacobrozell.dartbuddy.data.prefs.OnboardingStore
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.model.MatchConfigPayload
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.bots.CustomBotConfiguration
import com.jacobrozell.dartbuddy.domain.bots.CustomBotConfigurationCodec
import com.jacobrozell.dartbuddy.domain.bots.BotKind
import com.jacobrozell.dartbuddy.domain.bots.BotSkillProfile
import com.jacobrozell.dartbuddy.domain.bots.BotTurnCoordinator
import com.jacobrozell.dartbuddy.domain.bots.TrainingBotEligibilityService
import com.jacobrozell.dartbuddy.domain.bots.TrainingBotException
import com.jacobrozell.dartbuddy.domain.bots.TrainingBotSkillResolver
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.service.MatchSessionService
import com.jacobrozell.dartbuddy.domain.service.MatchStatsCalculator
import com.jacobrozell.dartbuddy.domain.service.PlayerStatsAggregator
import kotlinx.coroutines.flow.first
import com.jacobrozell.dartbuddy.domain.bots.CustomBotMetrics
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import com.jacobrozell.dartbuddy.support.HistoryListRow
import com.jacobrozell.dartbuddy.support.MatchHistoryCardBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerSummary(
    val id: String,
    val name: String,
    val isArchived: Boolean,
    val isBot: Boolean = false,
    val botDifficultyRaw: String? = null,
    val botKindRaw: String? = null,
    val avatarStyleRaw: String? = null,
    val colorToken: String? = null,
    val notes: String? = null,
    val linkedPlayerId: String? = null,
    val isPrimary: Boolean = false,
    val createdAtEpochMillis: Long = 0,
    val updatedAtEpochMillis: Long = 0,
) {
    val isTrainingBot: Boolean get() = botKindRaw == BotKind.TRAINING
}

data class MatchSummary(
    val id: String,
    val matchType: MatchType,
    val status: MatchStatus,
    val participantNames: List<String>,
    val participantIds: List<String>,
    val winnerPlayerId: String?,
    val forfeitedByPlayerId: String?,
    val startedAt: Instant,
    val updatedAt: Instant,
)

data class AppSettings(
    val appearanceMode: String = "system",
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val turnTotalCallerEnabled: Boolean = false,
    val defaultMatchType: MatchType = MatchType.X01,
    val defaultX01StartScore: Int = 501,
    val defaultX01CheckoutMode: X01CheckoutMode = X01CheckoutMode.DoubleOut,
    val defaultX01CheckInMode: X01CheckInMode = X01CheckInMode.StraightIn,
    val defaultLegFormat: X01LegFormat = X01LegFormat.FirstTo,
    val defaultLegsToWin: Int = 1,
    val defaultSetsEnabled: Boolean = false,
    val defaultSetsToWin: Int = 1,
    val defaultCricketPointsEnabled: Boolean = true,
    val defaultCricketScoringMode: CricketScoringMode = CricketScoringMode.Standard,
    val botStaggerEnabled: Boolean = true,
    val botDartHapticsEnabled: Boolean = true,
)

@Singleton
class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao,
    private val matchRepository: MatchRepository,
    private val matchSessionService: MatchSessionService,
    private val l10n: com.jacobrozell.dartbuddy.support.L10n,
) {
    fun observeActivePlayers(): Flow<List<PlayerSummary>> =
        playerDao.observeActivePlayers().map { list -> list.map { it.toSummary() } }

    fun observeAllPlayers(): Flow<List<PlayerSummary>> =
        playerDao.observeAllPlayers().map { list -> list.map { it.toSummary() } }

    suspend fun getById(id: String): PlayerSummary? = playerDao.getById(id)?.toSummary()

    suspend fun getPrimaryPlayer(): PlayerSummary? = playerDao.getPrimaryPlayer()?.toSummary()

    suspend fun create(
        name: String,
        colorToken: String? = null,
        avatarStyleRaw: String? = null,
        isPrimary: Boolean = false,
    ): PlayerSummary {
        val now = Instant.now().toEpochMilli()
        val id = UUID.randomUUID().toString()
        val entity = PlayerEntity(
            id = id,
            name = name.trim(),
            colorToken = colorToken ?: com.jacobrozell.dartbuddy.domain.model.PlayerColorToken.defaultForPlayer(id).raw,
            avatarStyleRaw = avatarStyleRaw ?: PlayerAvatarStyle.defaultForPlayer(id, isBot = false).raw,
            isPrimary = isPrimary,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        playerDao.upsert(entity)
        return entity.toSummary()
    }

    suspend fun createPresetBot(difficulty: BotDifficulty): PlayerSummary {
        val now = Instant.now().toEpochMilli()
        val id = UUID.randomUUID().toString()
        val entity = PlayerEntity(
            id = id,
            name = difficulty.rosterName(l10n),
            isBot = true,
            botDifficultyRaw = difficulty.name,
            botKindRaw = BotKind.PRESET,
            colorToken = com.jacobrozell.dartbuddy.domain.model.PlayerColorToken.Purple.raw,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        playerDao.upsert(entity)
        return entity.toSummary()
    }

    suspend fun createCustomBot(name: String, metrics: CustomBotMetrics): PlayerSummary {
        val now = Instant.now().toEpochMilli()
        val id = UUID.randomUUID().toString()
        val configuration = CustomBotConfiguration.from(metrics)
        val trimmed = name.trim()
        val resolvedName = trimmed.ifBlank { l10n.get(com.jacobrozell.dartbuddy.R.string.custom_bot_default_name) }
        val entity = PlayerEntity(
            id = id,
            name = resolvedName,
            isBot = true,
            botDifficultyRaw = CustomBotConfigurationCodec.encode(configuration),
            botKindRaw = BotKind.CUSTOM,
            colorToken = com.jacobrozell.dartbuddy.domain.model.PlayerColorToken.Purple.raw,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        playerDao.upsert(entity)
        return entity.toSummary()
    }

    suspend fun updateCustomBotMetrics(id: String, metrics: CustomBotMetrics) {
        val existing = playerDao.getById(id) ?: return
        if (!existing.isBot || existing.botKindRaw != BotKind.CUSTOM) return
        val configuration = (CustomBotConfigurationCodec.decode(existing.botDifficultyRaw) ?: CustomBotConfiguration.from(metrics))
            .withMetrics(metrics.x01Average, metrics.cricketMPR)
            .resetToSimpleTargets()
        playerDao.upsert(
            existing.copy(
                botDifficultyRaw = CustomBotConfigurationCodec.encode(configuration),
                updatedAtEpochMillis = Instant.now().toEpochMilli(),
            ),
        )
    }

    suspend fun updateCustomBotConfiguration(id: String, configuration: CustomBotConfiguration) {
        val existing = playerDao.getById(id) ?: return
        if (!existing.isBot || existing.botKindRaw != BotKind.CUSTOM) return
        playerDao.upsert(
            existing.copy(
                botDifficultyRaw = CustomBotConfigurationCodec.encode(configuration),
                updatedAtEpochMillis = Instant.now().toEpochMilli(),
            ),
        )
    }

    suspend fun update(
        id: String,
        name: String,
        colorToken: String?,
        avatarStyleRaw: String?,
        notes: String?,
        isPrimary: Boolean,
    ) {
        val existing = playerDao.getById(id) ?: return
        playerDao.upsert(
            existing.copy(
                name = name.trim(),
                colorToken = colorToken,
                avatarStyleRaw = avatarStyleRaw,
                notes = notes?.trim()?.ifBlank { null },
                isPrimary = isPrimary,
                updatedAtEpochMillis = Instant.now().toEpochMilli(),
            ),
        )
    }

    suspend fun fetchTrainingBot(linkedTo: String): PlayerSummary? =
        playerDao.getTrainingBotFor(linkedTo)?.toSummary()

    suspend fun createTrainingBot(humanPlayerId: String): PlayerSummary {
        val human = playerDao.getById(humanPlayerId)
            ?: throw TrainingBotException.InvalidPlayer
        if (human.isBot || human.isArchived) throw TrainingBotException.InvalidPlayer
        if (playerDao.getTrainingBotFor(humanPlayerId) != null) throw TrainingBotException.Duplicate
        val now = Instant.now().toEpochMilli()
        val id = UUID.randomUUID().toString()
        val name = l10n.format(com.jacobrozell.dartbuddy.R.string.training_bot_name_format, human.name)
        val entity = PlayerEntity(
            id = id,
            name = name,
            isBot = true,
            botKindRaw = BotKind.TRAINING,
            linkedPlayerId = humanPlayerId,
            avatarStyleRaw = human.avatarStyleRaw ?: PlayerAvatarStyle.defaultForPlayer(id, isBot = true).raw,
            colorToken = human.colorToken ?: com.jacobrozell.dartbuddy.domain.model.PlayerColorToken.defaultForPlayer(id).raw,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        playerDao.upsert(entity)
        return entity.toSummary()
    }

    suspend fun resolveBotSkillProfile(playerId: String, mode: MatchType): BotSkillProfile? {
        val player = getById(playerId) ?: return null
        if (!player.isBot) return null
        return when (player.botKindRaw) {
            BotKind.CUSTOM -> BotTurnCoordinator.customConfigurationFor(player)?.resolvedCanonicalProfile()
            BotKind.TRAINING -> resolveTrainingBotSkill(player.id, mode)
            else -> BotTurnCoordinator.difficultyFor(player)?.skillProfile
        }
    }

    suspend fun resolveTrainingBotSkill(botId: String, mode: MatchType): BotSkillProfile {
        val bot = getById(botId) ?: throw TrainingBotException.NotTrainingBot
        if (!bot.isTrainingBot) throw TrainingBotException.NotTrainingBot
        val linkedId = bot.linkedPlayerId ?: throw TrainingBotException.NotTrainingBot
        val linked = getById(linkedId) ?: throw TrainingBotException.Ineligible
        val modeStats = loadModeStatsForPlayer(linkedId, linked.name, mode)
            ?: throw TrainingBotException.Ineligible
        val eligibility = TrainingBotEligibilityService.eligibility(modeStats, mode)
        if (!eligibility.isEligible) throw TrainingBotException.Ineligible
        return TrainingBotSkillResolver.resolve(modeStats, mode)
    }

    private suspend fun loadModeStatsForPlayer(
        playerId: String,
        playerName: String,
        mode: MatchType,
    ) = PlayerStatsAggregator.aggregate(
        matches = matchRepository.observeHistory().first().filter {
            it.participantIds.contains(playerId) &&
                (it.status == MatchStatus.Completed || it.status == MatchStatus.Forfeited)
        },
        playerId = playerId,
        playerName = playerName,
        matchType = mode,
        statsForMatch = statsForMatch@{ summary ->
            when (summary.matchType) {
                MatchType.X01 -> {
                    val session = matchSessionService.loadX01Session(summary.id) ?: return@statsForMatch null
                    MatchStatsCalculator.x01Stats(
                        summary.participantIds,
                        summary.participantNames,
                        session.events,
                        session.state.players.associate { it.playerId to it.legsWon },
                        session.state.players.associate { it.playerId to it.setsWon },
                        summary.winnerPlayerId,
                    )
                }
                MatchType.Cricket -> {
                    val session = matchSessionService.loadCricketSession(summary.id) ?: return@statsForMatch null
                    MatchStatsCalculator.cricketStats(
                        summary.participantIds,
                        summary.participantNames,
                        session.events,
                        session.state.players.associate { it.playerId to it.score },
                        session.state.players.associate { it.playerId to it.legsWon },
                        session.state.players.associate { it.playerId to it.setsWon },
                        summary.winnerPlayerId,
                    )
                }
            }
        },
    )

    suspend fun rename(id: String, name: String) {
        val existing = playerDao.getById(id) ?: return
        playerDao.upsert(existing.copy(name = name.trim(), updatedAtEpochMillis = Instant.now().toEpochMilli()))
    }

    suspend fun archive(id: String) {
        playerDao.archive(id, Instant.now().toEpochMilli())
    }

    suspend fun unarchive(id: String) {
        playerDao.unarchive(id, Instant.now().toEpochMilli())
    }

    suspend fun delete(id: String) {
        playerDao.deleteById(id)
    }

    private fun PlayerEntity.toSummary() = PlayerSummary(
        id = id,
        name = name,
        isArchived = isArchived,
        isBot = isBot,
        botDifficultyRaw = botDifficultyRaw,
        botKindRaw = botKindRaw,
        avatarStyleRaw = avatarStyleRaw,
        colorToken = colorToken,
        notes = notes,
        linkedPlayerId = linkedPlayerId,
        isPrimary = isPrimary,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

@Singleton
class MatchRepository @Inject constructor(
    private val matchDao: MatchDao,
    private val json: Json,
    private val l10n: com.jacobrozell.dartbuddy.support.L10n,
) {
    fun observeHistory(): Flow<List<MatchSummary>> =
        matchDao.observeAll().map { list -> list.map { it.toSummary() } }

    fun observeHistoryRows(): Flow<List<HistoryListRow>> =
        matchDao.observeAll().map { list ->
            list
                .filter { it.status != MatchStatus.InProgress.name }
                .map { MatchHistoryCardBuilder.build(it, json, l10n) }
        }

    fun observeActiveMatch(): Flow<MatchEntity?> = matchDao.observeActiveMatch()

    suspend fun getMatch(id: String): MatchEntity? = matchDao.getById(id)

    suspend fun matchesForPlayer(playerId: String): List<MatchEntity> =
        matchDao.observeAll().first().filter { entity ->
            json.decodeFromString<List<String>>(entity.participantIdsJson).contains(playerId)
        }

    suspend fun saveMatch(entity: MatchEntity) = matchDao.upsert(entity)

    suspend fun deleteMatch(id: String) = matchDao.deleteById(id)

    suspend fun startMatch(
        matchType: MatchType,
        config: MatchConfigPayload,
        participantIds: List<String>,
        participantNames: List<String>,
        snapshotJson: String,
    ): MatchEntity {
        val now = Instant.now().toEpochMilli()
        val entity = MatchEntity(
            id = UUID.randomUUID().toString(),
            matchType = matchType.name,
            status = MatchStatus.InProgress.name,
            configJson = json.encodeToString(config),
            participantIdsJson = json.encodeToString(participantIds),
            participantNamesJson = json.encodeToString(participantNames),
            snapshotJson = snapshotJson,
            startedAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        matchDao.upsert(entity)
        return entity
    }

    private fun MatchEntity.toSummary() = MatchSummary(
        id = id,
        matchType = MatchType.valueOf(matchType),
        status = MatchStatus.valueOf(status),
        participantNames = json.decodeFromString(participantNamesJson),
        participantIds = json.decodeFromString(participantIdsJson),
        winnerPlayerId = winnerPlayerId,
        forfeitedByPlayerId = forfeitedByPlayerId,
        startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    )
}

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao,
) {
    fun observe(): Flow<AppSettings> =
        settingsDao.observe().map { entity -> entity?.toSettings() ?: AppSettings() }

    suspend fun get(): AppSettings = settingsDao.get()?.toSettings() ?: AppSettings().also { save(it) }

    suspend fun save(settings: AppSettings) {
        settingsDao.upsert(settings.toEntity())
    }

    private fun SettingsEntity.toSettings() = AppSettings(
        appearanceMode = appearanceMode,
        hapticsEnabled = hapticsEnabled,
        soundEnabled = soundEnabled,
        turnTotalCallerEnabled = turnTotalCallerEnabled,
        defaultMatchType = runCatching { MatchType.valueOf(defaultMatchType) }.getOrDefault(MatchType.X01),
        defaultX01StartScore = defaultX01StartScore,
        defaultX01CheckoutMode = runCatching { X01CheckoutMode.valueOf(defaultX01CheckoutMode) }
            .getOrDefault(X01CheckoutMode.DoubleOut),
        defaultX01CheckInMode = runCatching { X01CheckInMode.valueOf(defaultX01CheckInMode) }
            .getOrDefault(X01CheckInMode.StraightIn),
        defaultLegFormat = runCatching { X01LegFormat.valueOf(defaultLegFormat) }
            .getOrDefault(X01LegFormat.FirstTo),
        defaultLegsToWin = defaultLegsToWin,
        defaultSetsEnabled = defaultSetsEnabled,
        defaultSetsToWin = defaultSetsToWin,
        defaultCricketPointsEnabled = defaultCricketPointsEnabled,
        defaultCricketScoringMode = runCatching { CricketScoringMode.valueOf(defaultCricketScoringMode) }
            .getOrDefault(CricketScoringMode.Standard),
        botStaggerEnabled = botStaggerEnabled,
        botDartHapticsEnabled = botDartHapticsEnabled,
    )

    private fun AppSettings.toEntity() = SettingsEntity(
        appearanceMode = appearanceMode,
        hapticsEnabled = hapticsEnabled,
        soundEnabled = soundEnabled,
        turnTotalCallerEnabled = turnTotalCallerEnabled,
        defaultMatchType = defaultMatchType.name,
        defaultX01StartScore = defaultX01StartScore,
        defaultX01CheckoutMode = defaultX01CheckoutMode.name,
        defaultX01CheckInMode = defaultX01CheckInMode.name,
        defaultLegFormat = defaultLegFormat.name,
        defaultLegsToWin = defaultLegsToWin,
        defaultSetsEnabled = defaultSetsEnabled,
        defaultSetsToWin = defaultSetsToWin,
        defaultCricketPointsEnabled = defaultCricketPointsEnabled,
        defaultCricketScoringMode = defaultCricketScoringMode.name,
        botStaggerEnabled = botStaggerEnabled,
        botDartHapticsEnabled = botDartHapticsEnabled,
    )
}

@Singleton
class LocalDataResetService @Inject constructor(
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao,
    private val settingsDao: SettingsDao,
    private val settingsRepository: SettingsRepository,
    private val onboardingStore: OnboardingStore,
    private val rematchStore: com.jacobrozell.dartbuddy.data.prefs.RematchStore,
    private val practiceSetupStore: com.jacobrozell.dartbuddy.data.prefs.PracticeSetupStore,
    private val playStoreUpdateStore: com.jacobrozell.dartbuddy.data.prefs.PlayStoreUpdateStore,
) {
    suspend fun resetAllLocalData() {
        matchDao.deleteAll()
        playerDao.deleteAll()
        settingsDao.deleteAll()
        onboardingStore.clear()
        rematchStore.clear()
        practiceSetupStore.clear()
        playStoreUpdateStore.clear()
        settingsRepository.save(AppSettings())
    }
}
