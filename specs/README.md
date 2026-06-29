# Dart Buddy Android — Specs Index

Android implementation specs for **Dart Buddy**. **Product behavior** (rules, UX intent, invariants) remains canonical in the iOS repo [Dart-Buddy/specs/](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/). Files here describe **how Android implements** that behavior.

| Field | Value |
|-------|-------|
| **Application ID** | `com.jacobrozell.dartbuddy` |
| **Lean 1.0 surface** | [`ProductSurfaceSpec.md`](ProductSurfaceSpec.md) |
| **iOS behavior specs** | [Dart-Buddy/specs/](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/) |
| **Parity tracker** | [`../PARITY.md`](../PARITY.md) |

---

## How to use these specs

1. Read the **iOS behavior spec** for the feature (rules, copy, edge cases).
2. Read the matching **Android spec** below for Room schema, Compose routes, Kotlin classes, and platform gaps.
3. Update [`PARITY.md`](../PARITY.md) when shipping or deferring.

---

## System specs (Android)

| Spec | Covers |
|------|--------|
| [`ArchitectureSpec.md`](ArchitectureSpec.md) | Layers, packages, dependency rules |
| [`TechStackSpec.md`](TechStackSpec.md) | Kotlin, Compose, Hilt, Room, CI |
| [`DataSchemaSpec.md`](DataSchemaSpec.md) | Room entities, JSON blobs, invariants |
| [`RoomMigrationsSpec.md`](RoomMigrationsSpec.md) | DB versioning and migration policy |
| [`DeepLinkSpec.md`](DeepLinkSpec.md) | `dartbuddy://v1` intent filters, routing |
| [`AppShellSpec.md`](AppShellSpec.md) | 4-tab shell, onboarding gate, splash |
| [`AdaptiveLayoutSpec.md`](AdaptiveLayoutSpec.md) | Edge-to-edge, breakpoints, tablet/XS/XL/foldable plan |
| [`ProductSurfaceSpec.md`](ProductSurfaceSpec.md) | Lean 1.0 feature gate (Android) |
| [`PlayConsoleSpec.md`](PlayConsoleSpec.md) | Play Store listing, signing, tracks |
| [`SpecGovernance.md`](SpecGovernance.md) | When to update which spec |

---

## Feature specs (lean 1.0)

| Spec | iOS behavior (canonical) | Android implementation |
|------|------------------------|------------------------|
| Match lifecycle | [MatchSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/MatchSpec.md) | [`MatchSpec.md`](MatchSpec.md) |
| Forfeit | [MatchForfeitSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/MatchForfeitSpec.md) | § in [`MatchSpec.md`](MatchSpec.md) |
| Match summary | [MatchSummarySpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/MatchSummarySpec.md) | § in [`MatchSpec.md`](MatchSpec.md) |
| Play home | [PlayHomeSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/PlayHomeSpec.md) | § in [`AppShellSpec.md`](AppShellSpec.md) |
| Setup flow | [SetupFlowSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/SetupFlowSpec.md) | `ui/play/PlayScreen.kt`, `PlayViewModel.kt` |
| Players | [PlayerSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/PlayerSpec.md) | `ui/players/` |
| Settings | [SettingsSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/SettingsSpec.md) | `ui/settings/` |
| Custom bots | [CustomBotSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/CustomBotSpec.md) | `domain/bots/`, `ui/players/` |
| Scoring input | [ScoringInputSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/ScoringInputSpec.md) | `ui/components/ScoringPad.kt` |

---

## Game modes — [`game-modes/`](game-modes/)

| Android spec | iOS behavior | Status |
|--------------|--------------|--------|
| [`game-modes/implemented/X01GameSpec.md`](game-modes/implemented/X01GameSpec.md) | [game-modes/implemented/X01GameSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/game-modes/implemented/X01GameSpec.md) | Shipped lean 1.0 |
| [`game-modes/implemented/CricketSpec.md`](game-modes/implemented/CricketSpec.md) | [game-modes/implemented/CricketSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/game-modes/implemented/CricketSpec.md) | Shipped lean 1.0 |

Party Pack (1.1.0): port iOS specs on `release/1.1.0` when that Android branch opens — add `game-modes/implemented/{Baseball,Killer,Shanghai}GameSpec.md` per release.
