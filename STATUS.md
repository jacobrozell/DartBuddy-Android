# Dart Buddy Android — current state

> **Canonical detail:** [`PARITY.md`](PARITY.md) (line-by-line iOS lean 1.0 tracker)  
> **Last updated:** 2026-06-23  
> **Target:** iOS **1.0.0 lean** (`master` · `ProductSurface.lean1_0`) on **Android 16+** (`minSdk 36`)

---

## Verdict

**Lean 1.0 is feature-complete for core use.** X01 and Cricket can be played end-to-end with bots, onboarding, persistence, history, settings, and match lifecycle (resume, forfeit, summary, undo). Safe for emulator/device dogfooding.

**Not Play Store–ready yet.** Firebase Analytics/Crashlytics, release signing, and several iOS polish items remain (see [Remaining work](#remaining-work)).

---

## At a glance

| Item | Value |
|------|-------|
| **Package** | `com.jacobrozell.dartbuddy` |
| **Version** | `1.0.0` (versionCode 1) |
| **Stack** | Kotlin · Jetpack Compose · Hilt · Room · minSdk **36** |
| **iOS reference** | `Dart-Buddy/` on `master`, lean 1.0 surface |
| **Build** | `./gradlew assembleDebug test` — **passing** |
| **Unit tests** | 10 test classes under `app/src/test/` (engines, bots, onboarding, activity filters, forfeit resolver) |

---

## Completion estimate (lean 1.0 intent)

| Area | ~% | Status |
|------|-----|--------|
| App shell & navigation | 90% | 4 tabs, deep links, active-match badge, splash drawable |
| Play setup | 92% | Wide layout, bots, validation, resume banner |
| X01 / Cricket match | 95% | Pad, bots, sounds, TTS, check-in, forfeit sheets |
| Match summary | 85% | Trophy, haptic, undo, rematch; no landscape split |
| Onboarding (7 steps) | 95% | Full flow + replay; no analytics |
| Players | 80% | Search, detail, archive, custom bots; simplified avatars |
| Activity | 75% | History + stats segment, filters, load-more; no charts/timeline |
| Settings | 88% | Defaults, appearance, bots, help + a11y link, reset |
| Platform (Firebase, etc.) | 15% | Deep links ✅; Firebase ⬜ |
| Visual polish / motion | 70% | Key motions shipped; iPad layouts partial |

**Overall lean 1.0 parity: ~85%** — functional gaps are small; most remaining work is polish, tablet layouts, rich stats, and platform integration.

---

## What works end-to-end

1. **First launch** → 7-step onboarding (welcome, roster, preferences, optional learn-to-play, tour, support, ready) → Play tab.
2. **Play setup** → X01 or Cricket, full options (101–601 start scores, check-in/out, legs/sets), roster with preset/custom bots, random order, START.
3. **Match** → Per-dart scoring, checkout suggestions, check-in banners, bot auto-play, sounds + haptics + TTS turn totals, undo, exit/forfeit multi-step sheets.
4. **Complete** → Summary (trophy, stats, undo last throw, rematch, view in Activity).
5. **Resume** → Active match banner on Play and Activity; tab badge on Play.
6. **Players** → Add/edit/archive/delete, custom bot editor (simple + advanced).
7. **Activity** → History list with filters + load-more; Statistics aggregates; detail with stats cards and delete.
8. **Settings** → Appearance, defaults, feedback links (FAQ, email, accessibility, privacy), replay onboarding, reset all data.
9. **Deep links** → `dartbuddy://v1/...` routes to tabs (deferred until onboarding completes).

---

## Remaining work

Ordered by impact for reaching “store-quality lean 1.0”:

| Priority | Item | Notes |
|----------|------|-------|
| P0 | **Firebase** Analytics + Crashlytics | Needs `google-services.json`, Gradle plugin, event parity |
| P1 | **Turn order drag-to-reorder** | Android uses up/down buttons |
| P1 | **Rich Activity detail** | Sector breakdown, expandable event timeline |
| P1 | **iPad / tablet layouts** | Wide score cards, split summary, cricket board variants |
| P2 | **Localized chip labels** | `OptionLabels.kt` exists; not wired everywhere |
| P2 | **Full launch splash art** | iOS PNG dartboard export; Android uses drawable + launcher foreground |
| P2 | **Statistics charts** | Aggregate table only, no charts |
| P3 | **App Store / Play update prompts** | iOS has update offer; Android N/A |
| P3 | **Dedicated AX layouts** | Compose scale + strings; no alternate layouts |
| P3 | **Analytics events** | Onboarding, match lifecycle (blocked on Firebase) |

---

## Architecture

```
android/app/src/main/kotlin/com/jacobrozell/dartbuddy/
├── MainActivity.kt              # Root: theme, onboarding gate, deep links
├── ui/
│   ├── navigation/NavHost.kt    # Tab shell + match/summary/history routes
│   ├── play/                    # Setup, X01/Cricket match, summary
│   ├── players/                 # List, detail, custom bots
│   ├── activity/                # History + statistics
│   ├── settings/                # Preferences + reset
│   ├── onboarding/              # 7-step flow
│   ├── designsystem/            # Brand tokens, setup chrome, chips
│   └── components/              # Pad, boards, banners, forfeit, exit
├── domain/
│   ├── engine/                  # X01Engine, CricketEngine, CheckoutSuggester
│   ├── bots/                    # DartBotEngine, custom bot codec
│   ├── service/                 # MatchSessionService, stats, forfeit resolver
│   └── feedback/                # SoundPool, TTS, haptics
└── data/
    ├── local/                   # Room entities + DAOs
    ├── repository/              # Players, matches, settings
    └── prefs/                   # Onboarding, rematch store
```

| Layer | Android | iOS equivalent |
|-------|---------|----------------|
| UI | Compose + ViewModels | SwiftUI + ViewModels |
| Domain | `domain/` | `Domain/` |
| Data | Room + repositories | SwiftData + repositories |
| DI | Hilt | `AppDependencies` |

---

## Navigation routes

| Route | Screen |
|-------|--------|
| `play` | Play setup (tab) |
| `players` | Player list (tab) |
| `players/{id}` | Player detail |
| `activity` | History / Statistics (tab) |
| `settings` | Settings (tab) |
| `match/x01/{matchId}` | X01 match (no tab bar) |
| `match/cricket/{matchId}` | Cricket match |
| `summary/{matchId}` | Match summary |
| `history/{matchId}` | Activity detail |

Deep link host: `dartbuddy://v1/<path>` → tab routes (`play`, `players`, `activity`, `settings`).

---

## Key files (recent parity work)

| Feature | Primary files |
|---------|----------------|
| Play setup | `ui/play/PlayScreen.kt`, `PlayViewModel.kt` |
| X01 match | `ui/play/match/X01MatchScreen.kt`, `X01MatchViewModel.kt` |
| Forfeit flow | `ui/components/ForfeitFlow.kt`, `domain/service/MatchForfeitResolver.kt` |
| Exit sheet | `ui/components/MatchExitDialog.kt` |
| Sounds / TTS | `domain/feedback/FeedbackServices.kt`, `res/raw/` |
| Check-in UX | `X01Engine.kt`, `X01MatchScreen.kt`, `MatchBanners.kt` |
| Resume banner | `ui/components/ResumeBanner.kt` (Play + Activity) |
| Match summary | `ui/play/summary/MatchSummaryScreen.kt`, `MatchSummaryViewModel.kt` |
| Onboarding | `ui/onboarding/OnboardingFlow.kt`, `OnboardingViewModel.kt` |
| Custom bots | `ui/players/CustomBotViews.kt`, `domain/bots/CustomBot.kt` |
| Activity | `ui/activity/ActivityScreen.kt`, `ActivityFilters.kt` |
| Shared start scores | `domain/model/X01StartScores.kt` |
| Parity tracker | `PARITY.md` |

---

## Build, test, and verify

### CLI

```bash
./gradlew assembleDebug test
```

### Android Studio MCP (Cursor)

1. Install [MCP Server plugin](https://plugins.jetbrains.com/plugin/26071-mcp-server) in Android Studio.
2. Open this repo, keep Studio running.
3. Cursor MCP config: `jetbrains` via `@jetbrains/mcp-proxy` (see [`README.md`](README.md)).
4. Use `build_project` with `projectPath` set to this repo root after edits.

### Manual smoke checklist

- [ ] Fresh install → onboarding → Play with seeded bot
- [ ] X01 501 double-out match through leg win (sounds, leg banner)
- [ ] Double-in check-in banner and dimmed darts
- [ ] Cricket cut-throat with bot
- [ ] Exit → Save & Forfeit → pick player → confirm → summary
- [ ] Summary → Undo last throw → match resumes
- [ ] Activity → history detail → delete match
- [ ] Settings → replay onboarding; reset all data
- [ ] Deep link `dartbuddy://v1/players` after onboarding

Emulator screenshots (when captured): `.emulator-screenshots/`

---

## Related docs

| Doc | Purpose |
|-----|---------|
| [`PARITY.md`](PARITY.md) | Full feature matrix vs iOS lean 1.0 |
| [`README.md`](README.md) | Setup, MCP, architecture intro |
| [`specs/README.md`](specs/README.md) | Android implementation specs (behavior in `dart-buddy-ios/specs/`) |
| [`docs/releases/README.md`](docs/releases/README.md) | Release branch model |
| Workspace `docs/projects/dart-buddy-android.md` | Personal monorepo project status (local) |

---

## Changelog (Android lean 1.0 port)

| Date | Milestone |
|------|-----------|
| 2026-06-22 | Forfeit multi-step sheets, Activity resume banner + load-more, summary undo, splash drawable, a11y link, leg-win animation |
| 2026-06-22 | Settings/onboarding 101–601 start scores, tab filled icons, resume motion, validation UX |
| 2026-06-22 | Play setup layout fix, check-in UX, bundled sounds + TTS |
| 2026-06-23 | Split to `DartBuddy-Android` repo; CI + release branch docs |
