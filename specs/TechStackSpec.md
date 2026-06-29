# Tech Stack and Dependencies (Android)

## 1. Purpose

Concrete stack for Dart Buddy Android lean 1.0. iOS stack reference: [TechStackSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/TechStackSpec.md).

| Field | Value |
|-------|-------|
| **Application ID** | `com.jacobrozell.dartbuddy` |
| **minSdk / targetSdk** | 36 (Android 16+) |

---

## 2. Core stack

| Layer | Choice | Version source |
|-------|--------|----------------|
| Language | Kotlin | `gradle/libs.versions.toml` |
| UI | Jetpack Compose + Material 3 | Compose BOM |
| Navigation | Navigation Compose | catalog |
| DI | Hilt + KSP | catalog |
| Persistence | Room + KSP | catalog |
| Serialization | kotlinx.serialization (JSON blobs) | catalog |
| Async | Kotlin Coroutines | catalog |
| Testing | JUnit 4, coroutines-test | unit tests in `app/src/test/` |

---

## 3. Dependency policy

- **No unnecessary runtime libraries** — prefer AndroidX and Kotlin stdlib.
- Version catalog (`gradle/libs.versions.toml`) is the single source for dependency versions.
- Firebase (Analytics + Crashlytics): **approved for release builds** when `google-services.json` is configured — see [`PlayConsoleSpec.md`](PlayConsoleSpec.md). Not required for debug dogfood.

---

## 4. Build

```bash
./gradlew assembleDebug test
```

CI: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) — Ubuntu, JDK 17, standard checkout.

---

## 5. IDE and agent tooling

- **Android Studio** — open repo root (not a parent monorepo path).
- **Cursor + JetBrains MCP** — `build_project` with `projectPath` = repo root.
- **Emulator** — Android 16+ system image; workspace `android-emulator` MCP optional.

---

## 6. Out of scope (lean 1.0)

- Kotlin Multiplatform shared module with iOS
- Wear OS companion
- App Widgets / Glance
- Play Feature Delivery / dynamic modules
