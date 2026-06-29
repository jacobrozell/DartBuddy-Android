# Data Schema Specification (Android)

## 1. Purpose

Room persistence for Dart Buddy Android. **Entity semantics and invariants** align with [DataSchemaSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/DataSchemaSpec.md); this spec defines the Android storage shape.

| Field | Value |
|-------|-------|
| **Database** | `DartBuddyDatabase` |
| **Room version** | `2` (see [`RoomMigrationsSpec.md`](RoomMigrationsSpec.md)) |
| **Application ID** | `com.jacobrozell.dartbuddy` |

---

## 2. Storage model

Android lean 1.0 uses **three Room tables** plus **JSON columns** for match config, snapshot, and events (simpler than iOS normalized `MatchParticipantRecord` / `MatchEventRecord` tables).

Implementation: `data/local/entity/Entities.kt`, `data/local/DartBuddyDatabase.kt`.

---

## 3. Tables

### `players` → `PlayerEntity`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `TEXT` PK | UUID string |
| `name` | `TEXT` | Display name |
| `isArchived` | `BOOLEAN` | Soft hide from default lists |
| `isBot` | `BOOLEAN` | Preset or custom bot |
| `botDifficultyRaw` | `TEXT?` | Preset skill |
| `botKindRaw` | `TEXT?` | Bot kind enum raw |
| `avatarStyleRaw` | `TEXT?` | Simplified vs iOS avatars |
| `colorToken` | `TEXT?` | Roster color |
| `notes` | `TEXT?` | Optional |
| `isPrimary` | `BOOLEAN` | Default “me” player |
| `createdAtEpochMillis` | `LONG` | |
| `updatedAtEpochMillis` | `LONG` | |

iOS `PlayerRecord` bot/training fields: map `botKindRaw`; training partner not in lean 1.0 Android surface.

### `matches` → `MatchEntity`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `TEXT` PK | UUID string |
| `matchType` | `TEXT` | `X01`, `Cricket`, … |
| `status` | `TEXT` | `InProgress`, `Completed`, … |
| `configJson` | `TEXT` | Serialized setup (`payloadVersion` inside) |
| `participantIdsJson` | `TEXT` | JSON array of player IDs |
| `participantNamesJson` | `TEXT` | Denormalized names for history |
| `snapshotJson` | `TEXT?` | Engine snapshot blob |
| `eventsJson` | `TEXT` | Default `[]`; append-only event log |
| `winnerPlayerId` | `TEXT?` | |
| `forfeitedByPlayerId` | `TEXT?` | |
| `startedAtEpochMillis` | `LONG` | |
| `updatedAtEpochMillis` | `LONG` | |
| `completedAtEpochMillis` | `LONG?` | |

### `settings` → `SettingsEntity`

Singleton row `id = 1`. Columns mirror iOS `SettingsRecord` defaults for lean 1.0: appearance, haptics, sound, X01/Cricket defaults, turn-total caller, bot stagger/haptics.

---

## 4. Invariants (same as iOS)

- Event order in `eventsJson` is contiguous and monotonic per match.
- Completed match has `completedAtEpochMillis` and winner/forfeit metadata.
- In-progress match has valid snapshot and turn pointers inside `snapshotJson`.
- History renders with `participantNamesJson` when player rows are archived/deleted.
- Player hard delete blocked when referenced by match history (repository enforces).

---

## 5. Payload rules

- `configJson`, `snapshotJson`, and event entries include `payloadVersion` (kotlinx.serialization).
- Decoders must tolerate previous version where iOS spec requires backward compatibility.
- Reserve optional keys for future multi-device: `originDeviceType` (`android`), `originDeviceSessionId`.

---

## 6. Indexing

Lean 1.0: Room queries filter by `status`, `matchType`, `startedAtEpochMillis` / `completedAtEpochMillis` via DAO SQL. Add `@Index` when history list profiling requires it.

---

## 7. Testing

Mirror iOS invariant tests where applicable:

| Android test | iOS analogue |
|--------------|--------------|
| `X01EngineTest` | `X01EngineTests` |
| `CricketEngineTest` | `CricketEngineTests` |
| `MatchForfeitResolverTest` | forfeit resolver tests |

Repository round-trip tests: add when schema migrations ship.
