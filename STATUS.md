# Dart Buddy Android — current state

> **Canonical detail:** [`PARITY.md`](PARITY.md) (line-by-line iOS lean 1.0 tracker)  
> **Last updated:** 2026-07-10  
> **Target:** iOS **1.0.0 lean** (`master` · `ProductSurface.lean1_0`) on **Android 13+** (`minSdk 33`)

---

## Verdict

**Lean 1.0 is feature-complete for core use.** X01 and Cricket can be played end-to-end with bots, onboarding, persistence, history, settings, and match lifecycle (resume, forfeit, summary, undo). Release APK builds; safe for emulator/device dogfooding.

**Not Play Store–ready yet.** Release signing, Play Console listing, and Firebase `google-services.json` on CI remain (Jacob). See [`docs/release/1.0.0-ship-checklist.md`](docs/release/1.0.0-ship-checklist.md).

---

## At a glance

| Item | Value |
|------|-------|
| **Package** | `com.jacobrozell.dartbuddy` |
| **Version** | `1.0.0` (versionCode 1) |
| **Stack** | Kotlin · Jetpack Compose · Hilt · Room · minSdk **33** · targetSdk **36** |
| **iOS reference** | `Dart-Buddy/` on `master`, lean 1.0 surface |
| **Build** | `./gradlew assembleDebug assembleRelease test connectedDebugAndroidTest` — **passing** (125 unit + 10 instrumented) |
| **Unit tests** | 29 JVM test classes (engines, ViewModels, repositories via fakes, stats, onboarding, activity) |
| **Instrumented tests** | Room repository contracts + tab/onboarding replay UI smoke |

---

## Completion estimate (lean 1.0 intent)

| Area | ~% | Status |
|------|-----|--------|
| App shell & navigation | 95% | 4 tabs, deep links, active-match badge, launch splash |
| Play setup | 92% | Wide layout, bots, validation, resume banner |
| X01 / Cricket match | 99% | Live scorecard 3DA, cricket column darts/MPR footer |
| Match summary | 94% | Phone-landscape split only; tablet stacked |
| Onboarding (7 steps) | 99% | Avatar picker on roster step; slide transitions |
| Players | 90% | Empty state, humans/bots sections, row a11y labels |
| Activity | 88% | History + stats segment, filters, charts, sector hits, timeline |
| Settings | 96% | Loading/error/retry, match defaults section |
| Platform (Firebase, etc.) | 75% | Analytics + Crashlytics wired (release builds) |
| Visual polish / motion | 82% | Splash dartboard backdrop, summary landscape split, drag reorder |

**Overall lean 1.0 parity: ~98%** — launch splash, onboarding motion, settings load states; Play Store pipeline remains.

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
| P0 | **Edge-to-edge insets** | ✅ `AppInsets.kt` |
| P0 | **Play Store** pipeline (signing, listing, tracks) | `specs/PlayConsoleSpec.md` — needs signing keys |
| P1 | **LayoutContext + adaptive layouts** | ✅ `GameplayLayout.kt`, cricket transposed board, AX pad |
| P2 | **Full launch splash art** | ✅ iOS PNG + `LaunchSplashScreen` |
| P3 | **Accessibility** | ✅ formatted labels + dedicated AX layouts |

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

### Verify

UI changes use the `android-verify` skill. Commands are in [`AGENTS.md`](AGENTS.md).

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
- [ ] Phone landscape X01 — side-by-side board + pad; tablet portrait — stacked full-width pad
- [ ] Cricket phone landscape — transposed active-player board
- [x] Large display font — accessibility match scroll layout; 4-column number pad; stacked score cards

Emulator screenshots (when captured): `.emulator-screenshots/`

---

## Related docs

| Doc | Purpose |
|-----|---------|
| [`PARITY.md`](PARITY.md) | Full feature matrix vs iOS lean 1.0 |
| [`README.md`](README.md) | Setup, MCP, architecture intro |
| [`specs/README.md`](specs/README.md) | Android implementation specs (behavior in [Dart-Buddy/specs/](https://github.com/jacobrozell/Dart-Buddy/tree/master/specs)) |
| [`docs/releases/README.md`](docs/releases/README.md) | Release branch model |
| [`docs/release/1.0.0-ship-checklist.md`](docs/release/1.0.0-ship-checklist.md) | Pre-ship checklist (Play/Firebase §5) |
| Workspace [`workspace/projects/dart-buddy-android.md`](../workspace/projects/dart-buddy-android.md) | Personal monorepo project status |

---

## Changelog (Android lean 1.0 port)

| Date | Milestone |
|------|-----------|
| 2026-06-22 | Forfeit multi-step sheets, Activity resume banner + load-more, summary undo, splash drawable, a11y link, leg-win animation |
| 2026-06-22 | Settings/onboarding 101–601 start scores, tab filled icons, resume motion, validation UX |
| 2026-06-22 | Play setup layout fix, check-in UX, bundled sounds + TTS |
| 2026-06-23 | Launch splash (iOS PNG + wordmark/dots), onboarding slide transitions, settings loading/error/retry |
| 2026-06-23 | Live match stats: `StatsService` (X01 live 3DA, cricket darts/MPR column footers), localized setup a11y strings |
| 2026-06-23 | `ProductSurface.kt` lean 1.0 gates (training/export off in release), Play Store rate link, dynamic version label |
| 2026-06-23 | Players tab empty state, humans/bots sections, formatted a11y strings (settings, onboarding, activity, rows) |
| 2026-06-23 | Secondary a11y: `FormattedAccessibilityLabels` (history, stats table, setup chips, activity filters, resume/partial banners) |
| 2026-06-29 | Fix flaky `ActivityFiltersTest`; verify `assembleRelease test` green; add [`docs/release/1.0.0-ship-checklist.md`](docs/release/1.0.0-ship-checklist.md) |
| 2026-07-10 | Lower `minSdk` 36 → 33 (Android 13+) for physical device testing (Galaxy A51) |
