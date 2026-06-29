# Match Lifecycle Specification (Android)

## 1. Purpose

Android implementation of match create → play → complete/forfeit → history. **Canonical behavior:** [MatchSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/MatchSpec.md).

---

## 2. Key classes

| Concern | Kotlin |
|---------|--------|
| Session orchestration | `domain/service/MatchSessionService.kt` |
| Persistence | `data/repository/MatchRepository.kt`, `MatchDao` |
| X01 UI | `ui/play/match/X01MatchViewModel.kt`, `X01MatchScreen.kt` |
| Cricket UI | `ui/play/match/CricketMatchViewModel.kt`, `CricketMatchScreen.kt` |
| Summary | `ui/play/summary/MatchSummaryViewModel.kt` |
| Setup | `ui/play/PlayViewModel.kt`, `PlayScreen.kt` |

---

## 3. Lifecycle states

Align with iOS `MatchStatus`:

| Status | Android storage |
|--------|-----------------|
| In progress | `MatchEntity.status`, live `snapshotJson` |
| Completed | `completedAtEpochMillis`, `winnerPlayerId` |
| Forfeited | `forfeitedByPlayerId`, completed timestamp |

Checkpoint: service writes snapshot + `eventsJson` on turn submit, undo, leg/set advance.

---

## 4. Resume and abandon

- **Resume:** `MatchRepository` loads single `InProgress` match; shell shows banner + badge.
- **Exit sheet:** save and leave (persist snapshot) vs forfeit flow.
- **Forfeit:** multi-step sheet — see §5.

---

## 5. Forfeit (Save & Forfeit)

**Behavior:** [MatchForfeitSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/MatchForfeitSpec.md)

Implementation: `ui/components/ForfeitSheet.kt`, `domain/service/MatchForfeitResolver.kt`, tests in `MatchForfeitResolverTest.kt`.

---

## 6. Match summary

**Behavior:** [MatchSummarySpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/MatchSummarySpec.md)

Android lean 1.0: trophy, stats cards, **undo last throw**, rematch, view in Activity. Landscape split layout partial vs iOS.

---

## 7. Testing

- Engine tests independent of UI.
- Forfeit resolver unit tests.
- Manual smoke: [`STATUS.md`](../STATUS.md#manual-smoke-checklist).
