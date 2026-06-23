# Dart Buddy — Android

Kotlin + Jetpack Compose port of **Dart Buddy iOS 1.0.0 lean** (X01 + Cricket, preset + custom bots).

> **Current state:** [`STATUS.md`](STATUS.md) · **Feature matrix:** [`PARITY.md`](PARITY.md) · **Releases:** [`docs/releases/README.md`](docs/releases/README.md)

## Status (2026-06-23)

**Lean 1.0 is feature-complete for core gameplay** (~85% parity intent). End-to-end: onboarding → play setup → X01/Cricket match (bots, sounds, TTS) → summary (undo, rematch) → activity history/stats → settings.

**Not yet:** Firebase, Play Store release pipeline, tablet-optimized layouts, rich activity detail (timelines/charts), drag-to-reorder turn order.

```bash
./gradlew assembleDebug test   # passing
```

**iOS repo:** [jacobrozell/Dart-Buddy](https://github.com/jacobrozell/Dart-Buddy) · **Specs submodule:** `dart-buddy-ios/specs/` (after `git submodule update --init`)

---

## Requirements

- **minSdk 36** (Android 16)
- Android Studio Ladybug or newer
- JDK 17

## Open in Android Studio

1. **File → Open** → select this repo root (`DartBuddy-Android/`)
2. Sync Gradle, run on an **Android 16+** emulator or device

## Cursor ↔ Android Studio (MCP)

Connect Cursor chat to Android Studio via the [JetBrains MCP Server plugin](https://plugins.jetbrains.com/plugin/26071-mcp-server) ([setup guide](https://github.com/muxi1998/android-studio-mcp-server)).

### Android Studio (one-time)

1. **Settings → Plugins → Marketplace** → search **MCP Server** (plugin ID 26071) → **Install**
2. Restart Android Studio
3. Open this repo and leave Android Studio running

### Cursor

`jetbrains` is configured in `.cursor/mcp.json`:

```json
"jetbrains": {
  "command": "npx",
  "args": ["-y", "@jetbrains/mcp-proxy"]
}
```

Reload Cursor MCP after editing. Use **`build_project`** with `projectPath` set to this repo root to validate edits.

**Multiple JetBrains IDEs:** set `IDE_PORT` in the `env` block to Android Studio’s built-in webserver port.

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

## Remaining (see PARITY.md)

- Firebase Analytics + Crashlytics
- iPad/tablet layout polish
- Activity sector breakdown + event timeline + charts
- Turn order drag-to-reorder
- Full launch splash artwork (iOS PNG export)
- Localized chip labels everywhere

---

## iOS reference

**Behavior specs (canonical):** git submodule `dart-buddy-ios/specs/`

**Android implementation specs:** [`specs/README.md`](specs/README.md) — Room, Compose, manifest, Kotlin map

After clone:

```bash
git submodule update --init --recursive
```

Key cross-links:

- Behavior: `dart-buddy-ios/specs/game-modes/implemented/X01GameSpec.md`
- Android: `specs/game-modes/implemented/X01GameSpec.md`
- Architecture: `specs/ArchitectureSpec.md`
- Package: `com.jacobrozell.dartbuddy`
