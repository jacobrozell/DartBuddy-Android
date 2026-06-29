# Spec Governance (Android)

## 1. Two-layer model

| Layer | Location | Owns |
|-------|----------|------|
| **Behavior** | [Dart-Buddy/specs/](https://github.com/jacobrozell/Dart-Buddy/tree/master/specs) (iOS repo) | Rules, UX intent, copy, edge cases |
| **Android implementation** | `specs/` (this repo) | Room, Compose, manifest, Kotlin classes, platform gaps |

When behavior changes on iOS, update the iOS spec first (or in the same PR on the paired release branch), then update Android spec + code.

---

## 2. When to update

| Event | Update |
|-------|--------|
| New Android release branch | `ProductSurfaceSpec.md`, `docs/releases/README.md`, `ongoing/README.md` |
| Room schema bump | `DataSchemaSpec.md`, `RoomMigrationsSpec.md` |
| New game mode shipped | `specs/game-modes/implemented/<Mode>GameSpec.md`, `PARITY.md` |
| Deep link added | `DeepLinkSpec.md`, `AndroidManifest.xml` |
| Play Store submit | `PlayConsoleSpec.md`, workspace `workspace/projects/dart-buddy-android.md` |

---

## 3. iOS reference repo

Behavior specs live in the separate iOS repo: [jacobrozell/Dart-Buddy](https://github.com/jacobrozell/Dart-Buddy).

When starting a port from a new iOS release, check out the matching branch or tag there (e.g. `release/1.1.0`, tag `1.0.0`) and link release notes in the Android PR. No submodule — clone both repos side by side if needed.

---

## 4. Verification

- Domain: unit tests (`./gradlew test`)
- Parity: [`PARITY.md`](../PARITY.md) checklist before Play track promotion
- CI: `.github/workflows/ci.yml` on every PR
