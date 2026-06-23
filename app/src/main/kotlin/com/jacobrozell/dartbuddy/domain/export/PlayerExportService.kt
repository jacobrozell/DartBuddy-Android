package com.jacobrozell.dartbuddy.domain.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.jacobrozell.dartbuddy.BuildConfig
import com.jacobrozell.dartbuddy.data.local.entity.MatchEntity
import com.jacobrozell.dartbuddy.data.repository.PlayerSummary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

object PlayerExportService {
    fun buildBundle(
        anchor: PlayerSummary,
        allPlayers: List<PlayerSummary>,
        matches: List<MatchEntity>,
    ): PlayerExportBundle {
        val referencedIds = matches
            .flatMap { jsonDecodeIds(it.participantIdsJson) }
            .filter { it != anchor.id }
            .toSet()
        val referencedPlayers = allPlayers
            .filter { it.id in referencedIds }
            .map { it.toExportRecord() }
            .sortedBy { it.name.lowercase() }

        return PlayerExportBundle(
            producer = BuildConfig.APPLICATION_ID,
            producerVersion = BuildConfig.VERSION_NAME,
            exportedAtEpochMillis = Instant.now().toEpochMilli(),
            anchorPlayerId = anchor.id,
            player = anchor.toExportRecord(),
            referencedPlayers = referencedPlayers,
            matches = matches.map { it.toExportBundle() },
        )
    }

    fun writeExportFile(context: Context, bundle: PlayerExportBundle, playerName: String, json: Json): File {
        val sanitized = playerName.trim().ifBlank { "player" }
            .replace(Regex("[/\\\\?%*|\"<>:]"), "-")
            .replace(" ", "-")
        val fileName = "$sanitized-dartbuddy-export.dartbuddy.json"
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(json.encodeToString(bundle))
        return file
    }

    fun shareExport(context: Context, file: File, playerName: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$playerName — Dart Buddy export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun PlayerSummary.toExportRecord() = PlayerExportRecord(
        id = id,
        name = name,
        isArchived = isArchived,
        isBot = isBot,
        botDifficultyRaw = botDifficultyRaw,
        botKindRaw = botKindRaw,
        linkedPlayerId = linkedPlayerId,
        avatarStyleRaw = avatarStyleRaw,
        preferredColorToken = colorToken,
        notes = notes,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    private fun MatchEntity.toExportBundle() = MatchExportBundle(
        match = MatchExportRecord(
            id = id,
            matchType = matchType,
            status = status,
            winnerPlayerId = winnerPlayerId,
            forfeitedByPlayerId = forfeitedByPlayerId,
            startedAtEpochMillis = startedAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
            completedAtEpochMillis = completedAtEpochMillis,
        ),
        configJson = configJson,
        participantIdsJson = participantIdsJson,
        participantNamesJson = participantNamesJson,
        eventsJson = eventsJson,
        snapshotJson = snapshotJson,
    )

    private fun jsonDecodeIds(raw: String): List<String> =
        runCatching { kotlinx.serialization.json.Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}
