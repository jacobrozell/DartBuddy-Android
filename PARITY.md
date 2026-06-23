# iOS ↔ Android parity tracker

> **Start here:** [`STATUS.md`](STATUS.md) — current-state summary, architecture, routes, verification checklist.

**Target:** Dart Buddy **1.0.0** (App Store lean) on Android 16+  
**iOS baseline:** `master` branch · `ProductSurface.lean1_0`  
**Android path:** `DartBuddy-Android/` (repo root)  
**Last updated:** 2026-06-23

## Legend

| Status | Meaning |
|--------|---------|
| ✅ | Shipped on Android (matches iOS lean 1.0 intent) |
| 🟡 | Partial / simplified UI or behavior |
| ⬜ | Not started |
| 🚫 | Out of lean 1.0 scope on **both** platforms |

## iOS lean 1.0 surface (reference)

From `ProductSurface.lean1_0` on `master`:

| Gate | Value |
|------|-------|
| Tabs | Play · Players · Activity · Settings (4) |
| Modes tab | Hidden |
| Reachable gameplay | X01 + Cricket only |
| Preset bots + **custom bots** | **On** |
| Training Partner bots | Off |
| Party modes | Off |
| Player export (DBPE) | Off |
| Bundled locales | English only |
| Firebase Analytics + Crashlytics | On (Release) |

---

## Summary

| Area | Android status |
|------|----------------|
| 4-tab shell + brand tokens | ✅ |
| X01 + Cricket setup & match core | ✅ / ✅ (adaptive layouts) |
| Rules / learn to play | ✅ |
| Players list + detail / edit / delete | ✅ / ✅ (trend, training bot, export) |
| Activity history + Statistics segment | ✅ |
| Settings defaults + appearance + bots + help | ✅ |
| Onboarding | ✅ 7-step flow |
| **Bots (preset + gameplay)** | ✅ |
| **Custom bots** | ✅ simple + advanced editor |
| Firebase / bundled sounds / TTS caller | ✅ sounds + TTS + Firebase (release) |
| Visual polish (motion, trophy, splash) | 🟡 key motions ✅; full splash art 🟡 |

**~96% lean 1.0 intent** — see [`STATUS.md`](STATUS.md) for completion table and remaining work.

---

## App shell & navigation

| Feature | iOS lean 1.0 | Android |
|---------|--------------|---------|
| Four root tabs | ✅ | ✅ |
| No Modes tab | ✅ | ✅ |
| Green selected tab state + filled icons when selected | ✅ | ✅ outlined/filled + green indicator |
| Match routes hide tab bar | ✅ | ✅ |
| Brand background on tab roots | ✅ | 🟡 solid color only |
| Tab scroll bottom padding (AX Dynamic Type) | ✅ | ✅ extra scroll padding on tab roots |
| Play tab badge when active match | ✅ | `ShellViewModel` + `BadgedBox` |
| App Store update prompt | ✅ | ✅ Play Store lookup + dismiss persistence |
| Deep links (`dartbuddy://v1/...`) | ✅ | ✅ manifest + tab routes |
| Deep link deferral until onboarding done | ✅ | ✅ |
| Launch splash (dartboard backdrop + wordmark) | ✅ | 🟡 branded drawable + launcher foreground |
| Tab / screen reveal motion | ✅ | ✅ tab content fade-in |
| iPad main shell adaptations | ✅ | ✅ `LayoutContext` + readable 920dp column |

---

## Onboarding

iOS: **Welcome → Roster setup → Preferences → Learn to play (conditional) → App tour → Support → Ready** (full-screen cover).

| Feature | iOS | Android |
|---------|-----|---------|
| Welcome + skip to finish | ✅ | ✅ |
| Roster setup (name, avatar, color, experience tier) | ✅ | ✅ (name, color, experience slider) |
| Seed human + preset bot for first match | ✅ | ✅ tier from slider |
| Replay onboarding from Settings | ✅ | ✅ |
| Preferences step (default game settings) | ✅ | ✅ |
| Learn to play with mode picker | ✅ conditional | ✅ conditional |
| App tour cards (Play / Players / Activity / Settings) | ✅ | ✅ |
| Support step (FAQ, feedback, a11y, privacy links) | ✅ | ✅ |
| Ready step + roster summary | ✅ | ✅ |
| Progress chrome + back navigation | ✅ | ✅ |
| Skip roster if primary player exists (replay) | ✅ | ✅ |
| Onboarding analytics event | ✅ | ✅ `onboarding_completed` |
| Step transitions / reduce motion | ✅ | ✅ fade; skipped when animator scale is 0 |

Android today: **7-step full-screen flow** — Welcome → Roster → Preferences/Learn (branch) → App tour → Support → Ready.

---

## Play setup

| Feature | iOS | Android |
|---------|-----|---------|
| Sticky START footer + validation hints | ✅ | ✅ |
| Mode card + change-mode sheet (X01 / Cricket) | ✅ | ✅ |
| Collapsible edit options | ✅ | ✅ |
| Edit options chevron icon | ✅ | Up/down chevron on button |
| Dynamic mode config summary (selected options) | ✅ | `MatchConfigText` on setup card |
| Learn to play → rules sheet | ✅ | ✅ |
| Learn to play in selected-mode header row | ✅ | ✅ |
| Roster: Add to match → turn order → action buttons | ✅ | ✅ |
| Resume active match banner | ✅ | ✅ |
| Resume banner entrance motion | ✅ | ✅ scale + fade spring |
| iPad / wide two-column setup | ✅ | ✅ @600dp |
| Randomize throw order | ✅ | ✅ |
| Turn order list | ✅ drag-to-reorder | ✅ long-press drag handle + up/down |
| Add human player | ✅ sheet | ✅ bottom sheet |
| **Add Bot menu** (Very Easy → Pro) | ✅ | ✅ Play + Players |
| **Custom bot** create + add to roster | ✅ | ✅ Play + Players sheets |
| Bot badges in roster rows | ✅ | ✅ preset + custom MPR badge |
| AX layout: validation hints inline (not footer) | ✅ | ✅ empty roster inline; cricket min in footer |
| Localized option chip labels | ✅ L10n | ✅ `rememberOptionLabelMaps()` |
| AX grid layout for option chips | ✅ | ✅ `SetupOptionChipGrid` (2/3-col AX/tablet) |
| Persist setup as defaults on start | ✅ | ✅ |

### X01 setup options

| Option | iOS | Android |
|--------|-----|---------|
| Start scores 101–601 | ✅ | ✅ Play, Settings, onboarding |
| Checkout / check-in modes | ✅ localized | ✅ |
| Leg format, legs, sets | ✅ | ✅ |

### Cricket setup options

| Option | iOS | Android |
|--------|-----|---------|
| Points scoring toggle | ✅ | ✅ |
| Standard / Cut-throat | ✅ | ✅ |
| Leg format, legs, sets | ✅ | ✅ |

---

## Match gameplay — X01

| Feature | iOS | Android |
|---------|-----|---------|
| 7-column number pad | ✅ | ✅ |
| Auto-submit after 3 darts | ✅ | ✅ |
| Visit preview row above pad | ✅ portrait | ✅ hidden in landscape only |
| Landscape: hide visit preview | ✅ | ✅ |
| Landscape side-by-side (252dp pad) | ✅ | ✅ |
| Pinned active player card (3+ players) | ✅ | ✅ landscape or 3+ players |
| Wide player score card (iPad) | ✅ | ✅ @840dp+ |
| Checkout suggestion banner + route cycle | ✅ | ✅ |
| Bust feedback banner | ✅ | ✅ |
| **Leg-win banner** + VoiceOver announce | ✅ | ✅ animated banner + leg sound |
| Check-in / state banner | ✅ | ✅ mode banner + dimmed non-scoring darts |
| **Bot turn banner** | ✅ | ✅ |
| Player score cards (accent, visit, 3DA, sets/legs) | ✅ | ✅ |
| Match header (title, config summary, exit, undo) | ✅ | ✅ |
| Exit sheet (save / forfeit / abandon) | ✅ | ✅ |
| Forfeit flow | ✅ multi-step sheets | ✅ pick player / winner / confirm sheets |
| Undo last dart / undo turn | ✅ | ✅ |
| Haptics (dart, bust, checkout) | ✅ | ✅ |
| Bundled scoring sounds | ✅ | ✅ hit/miss/leg/match from iOS assets |
| Bot auto-play engine | ✅ | ✅ `DartBotEngine` + stagger/haptics |
| Turn total caller (TTS) | ✅ | ✅ |

---

## Match gameplay — Cricket

| Feature | iOS | Android |
|---------|-----|---------|
| Tap pad (segments + multipliers) | ✅ | ✅ |
| Manual submit turn | ✅ | ✅ |
| Column marks board | ✅ | ✅ |
| Landscape side-by-side layout | ✅ | ✅ |
| Landscape transposed board (stacked shell) | ✅ | ✅ phone landscape |
| iPad landscape board fill-height variant | ✅ | ⬜ iOS also disabled (`usesSideBySideBottomScoringRegion` false) |
| Forfeit / exit / undo | ✅ | ✅ |
| Bot turn banner | ✅ | ✅ |

---

## Match summary

| Feature | iOS | Android |
|---------|-----|---------|
| Winner / forfeit headline | ✅ | ✅ headline + game recorded banner |
| Trophy celebration + success haptic | ✅ | ✅ trophy spring + summary haptic |
| Per-player stat cards | ✅ rich | ✅ `StatChip` grid + winner amber bar |
| Sets / legs breakdown | ✅ | ✅ in stat cards |
| Landscape split summary | ✅ | ✅ |
| Side-by-side player grid (many players) | ✅ | ✅ |
| Rematch CTA | ✅ | ✅ |
| Done CTA | ✅ | ✅ |
| Undo last throw | ✅ | ✅ reopens in-progress match |
| View Game Statistics → history detail | ✅ | ✅ |
| Rematch error banner | ✅ | ✅ Play setup banner when roster invalid |

---

## Players tab

| Feature | iOS | Android |
|---------|-----|---------|
| Player list with avatars | ✅ SF Symbol styles | ✅ icon styles + color |
| Search / filter | ✅ | ✅ |
| Add player form | ✅ name, avatar, color, primary | ✅ name, avatar, color (primary on edit) |
| **Player detail** (stats, recent matches) | ✅ navigation | ✅ stats-first + trend chart + recent matches → history |
| Edit player sheet | ✅ | ✅ name, notes, color, avatar, primary |
| Archive (swipe + detail) | ✅ | ✅ swipe + archive button |
| Unarchive | ✅ | ✅ archived section |
| Delete player (with guards) | ✅ | ✅ AlertDialog |
| **Training Partner** bot | 🚫 lean gate | ✅ create + practice prefill |
| **Custom bot** create / detail / metrics edit | ✅ lean 1.0 | ✅ simple + advanced editor |
| Bot badges in list | ✅ | ✅ |
| Primary player flag | ✅ | ✅ detail + edit |
| Player notes field | ✅ | ✅ on detail |
| Player export (DBPE) | 🚫 lean gate | ✅ JSON share via FileProvider |

---

## Activity tab

| Feature | iOS | Android |
|---------|-----|---------|
| **History / Statistics segmented control** | ✅ | ✅ |
| Mode filter (All / X01 / Cricket) | ✅ | ✅ |
| Period filter | ✅ | ✅ |
| Player filter menu | ✅ | ✅ |
| Resume active match banner | ✅ | ✅ Play + Activity |
| Rich history row cards | ✅ | ✅ `MatchHistoryCard` (badge, config, standings) |
| Load-more pagination | ✅ | ✅ 30 per page |
| Filtered empty states + Start Match CTA | ✅ | ✅ |
| **Statistics tables + charts** | ✅ | ✅ tables + bar charts |
| Partial active match in stats | ✅ | ✅ banner |
| History detail: Game Statistics title | ✅ | ✅ |
| Stat tables (3DA, checkouts, throws, …) | ✅ | ✅ |
| Sector hit breakdown | ✅ | ✅ |
| Event timeline (expandable) | ✅ | ✅ |
| Delete match from detail | ✅ | ✅ |
| Error + retry on detail load | ✅ | ✅ |

---

## Settings

iOS sections: Appearance · Starting mode · Match defaults · X01 defaults · During play · Bot opponents · Data · Help · About.

| Feature | iOS | Android |
|---------|-----|---------|
| Appearance (System / Light / Dark) | ✅ | ✅ |
| Brand-themed settings chrome | ✅ | ✅ brand palette when not Light |
| Section icons + footer hints | ✅ | ✅ footer hints (no section icons) |
| Starting mode default | ✅ | ✅ |
| Match defaults (leg format, legs, sets) | ✅ | ✅ dedicated section |
| X01 defaults (full start scores, checkout, check-in) | ✅ | ✅ |
| Cricket defaults | ✅ | ✅ |
| Haptics / sound toggles | ✅ | ✅ |
| Turn total caller | ✅ | ✅ |
| Bot stagger + bot dart haptics | ✅ | ✅ |
| Reset all local data | ✅ | ✅ |
| Reset → replay onboarding | ✅ | ✅ via Settings “View onboarding again” |
| Help & feedback (FAQ, email, rate, a11y page, privacy) | ✅ | ✅ (no App Store rate link) |
| View onboarding again | ✅ | ✅ |
| About / version | ✅ | ✅ |
| Loading / error retry placeholder | ✅ | 🟡 history detail only |

---

## Rules / learn to play

| Feature | iOS | Android |
|---------|-----|---------|
| Learn to play on Play setup | ✅ | ✅ |
| Rules bottom sheet + Done | ✅ | ✅ |
| X01 guide (5 sections) | ✅ | ✅ |
| Cricket guide (4 sections) | ✅ | ✅ |
| Mode picker in rules | ✅ | ✅ |

---

## Localization & copy

| Feature | iOS | Android |
|---------|-----|---------|
| English-only bundled UI | ✅ | ✅ |
| Localized chip / enum labels | ✅ | 🟡 many hardcoded strings in Kotlin |
| Formatted accessibility strings | ✅ | 🟡 partial in `strings.xml` |
| de / es / nl in repo | 🚫 not in lean bundle | 🚫 |

---

## Platform & integrations

| Feature | iOS lean 1.0 | Android |
|---------|--------------|---------|
| Firebase Analytics | ✅ | ✅ release builds |
| Crashlytics | ✅ | ✅ release builds |
| Match / onboarding analytics events | ✅ | ✅ incl. turn_submitted / turn_undone / dart_undone |
| Custom URL scheme deep links | ✅ | ✅ `dartbuddy://v1` in manifest |
| App Intents / Siri resume | ✅ (lean gates some routes) | ✅ shortcuts + `dartbuddy://v1/play/resume` |
| Bundled scoring sound assets | ✅ | ✅ `res/raw` (iOS Media.xcassets) |

---

## Accessibility

| Feature | iOS | Android |
|---------|-----|---------|
| Scoring pad content descriptions | ✅ | ✅ |
| Checkout suggestion a11y | ✅ | ✅ |
| Turn total caller | ✅ | ✅ |
| Accessibility marketing link | ✅ | ✅ |
| Dedicated AX layouts (setup, match, tabs) | ✅ | 🟡 match scroll + 4-col pad + tab bottom padding |
| Reduce motion (splash, onboarding, summary) | ✅ | ✅ onboarding, summary trophy, resume banner |
| UI test accessibility identifiers | ✅ extensive | ✅ incl. scoring pad keys |

---

## Visual polish & motion

| Feature | iOS | Android |
|---------|-----|---------|
| Launch splash (dartboard image asset) | ✅ | 🟡 vector dartboard + wordmark |
| Resume banner motion | ✅ | ✅ |
| Onboarding step transitions | ✅ | 🟡 fade only |
| Leg-win banner animation | ✅ | ✅ |
| Match summary trophy spring | ✅ | ✅ |
| Error banner styling (hint vs destructive) | ✅ | ✅ `HintBanner` + `ErrorBanner` |

---

## Domain / data (non-UI but affects parity)

| Feature | iOS | Android |
|---------|-----|---------|
| X01 engine (bust, checkout, legs/sets, check-in) | ✅ | ✅ ported |
| Cricket engine (marks, cut-throat, no-score) | ✅ | ✅ ported |
| Checkout suggester | ✅ | ✅ |
| Match persistence (snapshot + event log) | ✅ | ✅ Room |
| Resume active match | ✅ | ✅ |
| Rematch store | ✅ | ✅ |
| Local data reset inventory | ✅ | ✅ Room + onboarding + rematch + practice + Play Store dismiss |
| Bot engine + skill profiles | ✅ | ✅ incl. custom interpolator |
| Stats repository / reducers for detail | ✅ | ⬜ minimal calculator only |

---

## Remaining work (ordered)

See [`STATUS.md`](STATUS.md) for the full prioritized list. Summary:

1. **Play Store** — release signing, listing, tracks (P0).
2. **Launch splash** — import iOS dartboard PNG export (P2).
3. **Accessibility** — more setup AX polish (P3).

### Completed major milestones

Adaptive layout (`LayoutContext`, edge-to-edge, cricket transposed board) · Bots · Activity statistics · Onboarding · Players · Settings · Match UI (forfeit, sounds, TTS) · Match summary · Deep links · Check-in UX · Firebase.

---

## Build & test

```bash
./gradlew assembleDebug test
```

**10 unit test classes** (engines, checkout, bots, custom bots, onboarding, activity filters, forfeit resolver, game rules).

**Android Studio MCP:** `build_project` with `projectPath` set to this repo root — see [`README.md`](README.md).

**Note:** Prior versions of this file incorrectly listed Activity Statistics and custom bots as out of scope. Both are **in** iOS lean 1.0 on `master`.
