# Architecture Specification (Android)

## 1. Purpose

Define the production architecture for Dart Buddy on Android: clean boundaries between Compose UI, domain engines, and Room persistence. **Behavior** matches iOS; see [ArchitectureSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/ArchitectureSpec.md) for product-level module intent.

| Field | Value |
|-------|-------|
| **Application ID** | `com.jacobrozell.dartbuddy` |
| **Kotlin package root** | `com.jacobrozell.dartbuddy` |

---

## 2. Architectural style

- Single-module Gradle app (`:app`)
- Jetpack Compose presentation
- MVVM per screen (`ViewModel` + `StateFlow` / `mutableStateOf`)
- Pure Kotlin domain engines (no Android imports in `domain/engine`, `domain/bots`)
- Repository pattern over Room DAOs
- Hilt for dependency injection
- Unidirectional state: UI events → ViewModel → domain/repository → state

---

## 3. Package boundaries

```
com.jacobrozell.dartbuddy/
├── MainActivity.kt, DartBuddyApp.kt     # Entry, theme, onboarding gate
├── di/AppModule.kt                      # Hilt: database, repositories
├── ui/
│   ├── navigation/                      # NavHost, ShellViewModel, routes
│   ├── play/, players/, activity/, settings/, onboarding/
│   ├── designsystem/, components/, theme/
├── domain/
│   ├── engine/                          # X01Engine, CricketEngine, CheckoutSuggester
│   ├── bots/                            # DartBotEngine, custom bot codec
│   ├── service/                         # MatchSessionService, forfeit, stats
│   ├── rules/, scoring/, model/, feedback/
├── data/
│   ├── local/                           # Room entities, DAOs, database
│   ├── repository/, prefs/
└── support/                             # AppLinks, L10n, motion prefs
```

| Package | Role | iOS equivalent |
|---------|------|----------------|
| `ui/` | Compose screens + ViewModels | `Features/` |
| `domain/` | Rules, engines, services | `Domain/` |
| `data/` | Room + repositories | `Data/`, `Persistence/` |
| `ui/designsystem/` | Brand tokens, shared chrome | `DesignSystem/` |
| `support/` | Links, prefs helpers | `Support/` |

---

## 4. Dependency rules

- `ui` may depend on `domain`, `data` (via repository interfaces where possible), `designsystem`, `support`.
- `domain` must not import `android.*`, `androidx.*`, or Room.
- `data` may depend on `domain` models and Room only.
- `ui/designsystem` has no feature or domain knowledge.
- No circular dependencies.
- Domain DTOs use `String` IDs (not `UUID`) — maps to iOS `UUID` strings in JSON payloads.

---

## 5. State management

- ViewModels expose `StateFlow` or Compose-friendly state holders.
- `MatchSessionService` owns in-memory match state during play; persists via repositories on checkpoint.
- Repositories perform IO only; map entities ↔ domain models.
- Composables remain stateless where possible; side effects in `LaunchedEffect` / ViewModel coroutines.

---

## 6. Error handling

- Domain returns sealed results / typed failures (bust, illegal checkout, invalid roster).
- ViewModels map failures to user-visible snackbars or inline validation.
- Room migration failures: log and offer settings reset (see [`RoomMigrationsSpec.md`](RoomMigrationsSpec.md)).

---

## 7. Definition of done

- Business rules live in `domain/`, not in `@Composable` functions.
- No direct DAO calls from Composables.
- Unit tests for every domain engine and forfeit/stats resolver (mirror iOS test names where applicable).
- [`PARITY.md`](../PARITY.md) updated for the release surface.

---

## 8. Release alignment

Android release branches track iOS marketing versions. See [`docs/releases/README.md`](../docs/releases/README.md). Port domain changes from the matching [Dart-Buddy](https://github.com/jacobrozell/Dart-Buddy) branch before UI polish.
