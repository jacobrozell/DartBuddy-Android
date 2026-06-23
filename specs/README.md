# Dart Buddy Android — Specs Index

Android implementation specs for **Dart Buddy**. **Product behavior** (rules, UX intent, invariants) remains canonical in the iOS repo submodule [`dart-buddy-ios/specs/`](../dart-buddy-ios/specs/). Files here describe **how Android implements** that behavior.

| Field | Value |
|-------|-------|
| **Application ID** | `com.jacobrozell.dartbuddy` |
| **Lean 1.0 surface** | [`ProductSurfaceSpec.md`](ProductSurfaceSpec.md) |
| **iOS behavior specs** | [`dart-buddy-ios/specs/`](../dart-buddy-ios/specs/) |
| **Parity tracker** | [`../PARITY.md`](../PARITY.md) |

After clone: `git submodule update --init --recursive`

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
| Match lifecycle | [`dart-buddy-ios/specs/MatchSpec.md`](../dart-buddy-ios/specs/MatchSpec.md) | [`MatchSpec.md`](MatchSpec.md) |
| Forfeit | [`dart-buddy-ios/specs/MatchForfeitSpec.md`](../dart-buddy-ios/specs/MatchForfeitSpec.md) | § in [`MatchSpec.md`](MatchSpec.md) |
| Match summary | [`dart-buddy-ios/specs/MatchSummarySpec.md`](../dart-buddy-ios/specs/MatchSummarySpec.md) | § in [`MatchSpec.md`](MatchSpec.md) |
| Play home | [`dart-buddy-ios/specs/PlayHomeSpec.md`](../dart-buddy-ios/specs/PlayHomeSpec.md) | § in [`AppShellSpec.md`](AppShellSpec.md) |
| Setup flow | [`dart-buddy-ios/specs/SetupFlowSpec.md`](../dart-buddy-ios/specs/SetupFlowSpec.md) | `ui/play/PlayScreen.kt`, `PlayViewModel.kt` |
| Players | [`dart-buddy-ios/specs/PlayerSpec.md`](../dart-buddy-ios/specs/PlayerSpec.md) | `ui/players/` |
| Settings | [`dart-buddy-ios/specs/SettingsSpec.md`](../dart-buddy-ios/specs/SettingsSpec.md) | `ui/settings/` |
| Custom bots | [`dart-buddy-ios/specs/CustomBotSpec.md`](../dart-buddy-ios/specs/CustomBotSpec.md) | `domain/bots/`, `ui/players/` |
| Scoring input | [`dart-buddy-ios/specs/ScoringInputSpec.md`](../dart-buddy-ios/specs/ScoringInputSpec.md) | `ui/components/ScoringPad.kt` |

---

## Game modes — [`game-modes/`](game-modes/)

| Android spec | iOS behavior | Status |
|--------------|--------------|--------|
| [`game-modes/implemented/X01GameSpec.md`](game-modes/implemented/X01GameSpec.md) | [`dart-buddy-ios/specs/game-modes/implemented/X01GameSpec.md`](../dart-buddy-ios/specs/game-modes/implemented/X01GameSpec.md) | Shipped lean 1.0 |
| [`game-modes/implemented/CricketSpec.md`](game-modes/implemented/CricketSpec.md) | [`dart-buddy-ios/specs/game-modes/implemented/CricketSpec.md`](../dart-buddy-ios/specs/game-modes/implemented/CricketSpec.md) | Shipped lean 1.0 |

Party Pack (1.1.0): port iOS specs on `release/1.1.0` when that Android branch opens — add `game-modes/implemented/{Baseball,Killer,Shanghai}GameSpec.md` per release.
