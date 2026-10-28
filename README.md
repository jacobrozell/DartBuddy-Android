# Dart Buddy — Android

Kotlin + Jetpack Compose port of **Dart Buddy iOS 1.0.0 lean** (X01 + Cricket, preset + custom bots).

> **Current state:** [`STATUS.md`](STATUS.md) · **Feature matrix:** [`PARITY.md`](PARITY.md) · **Releases:** [`docs/releases/README.md`](docs/releases/README.md)

## Status (2026-10-28)

**Lean 1.0 is feature-complete for core use** (~98% parity). X01 and Cricket play end-to-end with bots, onboarding, persistence, history, settings, and match lifecycle. Firebase Analytics and Crashlytics are wired for release builds. Activity charts, sector hits, and turn-order drag are in. Safe to dogfood on an emulator or device.

**Not Play Store–ready.** Release signing, the Play Console listing, and a real `google-services.json` on CI are still open. See [`STATUS.md`](STATUS.md) and [`docs/release/1.0.0-ship-checklist.md`](docs/release/1.0.0-ship-checklist.md).

This checkout is `experiment/vision-tripod-test`. It allowlists the iOS camera-scoring outcome event so Firebase names stay paired. That experiment is not part of the lean 1.0 Play Store ship.

```bash
./gradlew assembleDebug test   # passing
```

**iOS repo:** [jacobrozell/Dart-Buddy](https://github.com/jacobrozell/Dart-Buddy) · **Behavior specs:** [Dart-Buddy/specs/](https://github.com/jacobrozell/Dart-Buddy/tree/master/specs)

---

## Requirements

- **minSdk 33** (Android 13) · **targetSdk 36**
- Android Studio Ladybug or newer
- JDK 17

## Open in Android Studio

1. **File → Open** → select this repo root (`DartBuddy-Android/`)
2. Sync Gradle, run on an **Android 13+** emulator or device

## Verify

UI changes use the `android-verify` skill. Package id and test commands are in [`AGENTS.md`](AGENTS.md).

## Build from CLI

```bash
./gradlew assembleDebug test
```

---

## Architecture

| Layer | Android | iOS equivalent |
|-------|---------|----------------|
| UI | Compose + ViewModels | SwiftUI + ViewModels |
| Domain | `domain/engine`, `domain/bots`, `domain/service` | `Domain/Engines`, `Domain/Services` |
| Data | Room + repositories | SwiftData + repositories |
| DI | Hilt | `AppDependencies` |

Package layout and navigation routes: [`STATUS.md`](STATUS.md#architecture).

---

## Lean 1.0 scope (shipped on Android)

| Area | Highlights |
|------|------------|
| **Shell** | Play · Players · Activity · Settings; deep links; active-match badge |
| **Play** | X01 + Cricket setup (101–601, check-in/out, legs/sets); bots; resume banner |
| **Match** | Scoring pads, checkout suggestions, check-in UX, bot engine, sounds/TTS/haptics |
| **Lifecycle** | Exit sheet, multi-step forfeit, summary + undo last throw, rematch |
| **Onboarding** | 7-step flow, roster seeding, replay from Settings |
| **Players** | CRUD, archive/unarchive, search, custom bot editor |
| **Activity** | History + statistics segments, filters, load-more, detail + delete |
| **Settings** | Appearance, defaults, bots, help (FAQ, email, a11y, privacy), reset |

Unit tests: X01/Cricket engines, checkout suggester, bots, custom bots, onboarding, activity filters, forfeit resolver.

---

## Remaining (see STATUS.md)

- Play Store pipeline: signing, listing, CI `google-services.json`
- Summary layout is phone-landscape split; tablet stays stacked
- Camera scoring stays an experiment branch, not the 1.0 ship

---

## iOS reference

**Behavior specs (canonical):** [Dart-Buddy/specs/](https://github.com/jacobrozell/Dart-Buddy/tree/master/specs) on GitHub

**Android implementation specs:** [`specs/README.md`](specs/README.md) — Room, Compose, manifest, Kotlin map

Key cross-links:

- Behavior: [X01GameSpec (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/game-modes/implemented/X01GameSpec.md)
- Android: [`specs/game-modes/implemented/X01GameSpec.md`](specs/game-modes/implemented/X01GameSpec.md)
- Architecture: [`specs/ArchitectureSpec.md`](specs/ArchitectureSpec.md)
- Package: `com.jacobrozell.dartbuddy`
