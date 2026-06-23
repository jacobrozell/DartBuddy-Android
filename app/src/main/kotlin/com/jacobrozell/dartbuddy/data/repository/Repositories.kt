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
import com.jacobrozell.dartbuddy.domain.bots.CustomBotMetrics
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
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
    val isPrimary: Boolean = false,
)

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
    private val l10n: com.jacobrozell.dartbuddy.support.L10n,
) {
    fun observeActivePlayers(): Flow<List<PlayerSummary>> =
        playerDao.observeActivePlayers().map { list -> list.map { it.toSummary() } }

    fun observeAllPlayers(): Flow<List<PlayerSummary>> =
        playerDao.observeAllPlayers().map { list -> list.map { it.toSummary() } }

    suspend fun getById(id: String): PlayerSummary? = playerDao.getById(id)?.toSummary()

    suspend fun getPrimaryPlayer(): PlayerSummary? = playerDao.getPrimaryPlayer()?.toSummary()

    suspend fun create(name: String, colorToken: String? = null, isPrimary: Boolean = false): PlayerSummary {
        val now = Instant.now().toEpochMilli()
        val id = UUID.randomUUID().toString()
        val entity = PlayerEntity(
            id = id,
            name = name.trim(),
            colorToken = colorToken ?: com.jacobrozell.dartbuddy.domain.model.PlayerColorToken.defaultForPlayer(id).raw,
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
            botKindRaw = "preset",
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
            botKindRaw = "custom",
            colorToken = com.jacobrozell.dartbuddy.domain.model.PlayerColorToken.Purple.raw,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        playerDao.upsert(entity)
        return entity.toSummary()
    }

    suspend fun updateCustomBotMetrics(id: String, metrics: CustomBotMetrics) {
        val existing = playerDao.getById(id) ?: return
        if (!existing.isBot || existing.botKindRaw != "custom") return
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
        if (!existing.isBot || existing.botKindRaw != "custom") return
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
        notes: String?,
        isPrimary: Boolean,
    ) {
        val existing = playerDao.getById(id) ?: return
        playerDao.upsert(
            existing.copy(
                name = name.trim(),
                colorToken = colorToken,
                notes = notes?.trim()?.ifBlank { null },
                isPrimary = isPrimary,
                updatedAtEpochMillis = Instant.now().toEpochMilli(),
            ),
        )
    }

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
        isPrimary = isPrimary,
    )
}

@Singleton
class MatchRepository @Inject constructor(
    private val matchDao: MatchDao,
    private val json: Json,
) {
    fun observeHistory(): Flow<List<MatchSummary>> =
        matchDao.observeAll().map { list -> list.map { it.toSummary() } }

    fun observeActiveMatch(): Flow<MatchEntity?> = matchDao.observeActiveMatch()

    suspend fun getMatch(id: String): MatchEntity? = matchDao.getById(id)

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
) {
    suspend fun resetAllLocalData() {
        matchDao.deleteAll()
        playerDao.deleteAll()
        settingsDao.deleteAll()
        onboardingStore.clear()
        settingsRepository.save(AppSettings())
    }
}
