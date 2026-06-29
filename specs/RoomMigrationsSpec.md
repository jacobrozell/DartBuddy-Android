# Room Migrations Specification

## 1. Purpose

Versioning and migration policy for `DartBuddyDatabase`. iOS equivalent: [SwiftData.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/SwiftData.md).

---

## 2. Current version

| Room `version` | Release | Notes |
|----------------|---------|-------|
| `2` | Lean 1.0 dogfood | `PlayerEntity`, `MatchEntity`, `SettingsEntity` |

`exportSchema = false` in lean 1.0; enable `room.schemaLocation` before Play production if team wants generated JSON schemas in CI.

---

## 3. Migration rules

1. **Never drop user match history** without explicit Settings → Reset all data.
2. Prefer **additive** migrations: new nullable columns, new tables.
3. JSON payload evolution uses `payloadVersion` inside blobs — Room schema can stay stable while payloads bump.
4. Implement `Migration(old, new)` in `AppModule` / database builder; bump `version` in `@Database`.
5. On migration failure: log to console (Crashlytics when wired); surface recoverable error in Settings with reset option.

---

## 4. Cross-platform schema drift

Android JSON-in-row layout may differ from iOS normalized SwiftData tables. **Behavioral parity** is required; **byte-identical DB files** are not. Player export (DBPE) is out of lean 1.0 scope on Android.

When iOS `SchemaV*` changes affect shared payload shapes, update Kotlin serializers and add compatibility tests on the Android release branch that tracks that iOS version.

---

## 5. Testing

- Migration instrumented tests (`androidTest`) when `version` increments.
- Unit tests for payload decoders across `payloadVersion` values.
