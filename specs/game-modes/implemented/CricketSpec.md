# Cricket Game Mode (Android)

## 1. Canonical behavior

Rules (standard vs cut-throat, points on/off, mark scoring): **[game-modes/implemented/CricketSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/game-modes/implemented/CricketSpec.md)**.

---

## 2. Android implementation

| Piece | Location |
|-------|----------|
| Engine | `domain/engine/CricketEngine.kt` |
| Match UI | `ui/play/match/CricketMatchScreen.kt` |
| ViewModel | `ui/play/match/CricketMatchViewModel.kt` |
| Board components | `ui/components/CricketBoard.kt` (and related) |
| Setup | `PlayScreen.kt` — scoring mode, points enabled |
| Unit tests | `CricketEngineTest.kt` |

---

## 3. Payload / persistence

Same JSON blob strategy as X01 — [`DataSchemaSpec.md`](../DataSchemaSpec.md).

---

## 4. Lean 1.0 gaps vs iOS

| Item | Status |
|------|--------|
| Standard + cut-throat | ✅ |
| Bot play | ✅ |
| Wide/tablet board layout | 🟡 partial |

---

## 5. Verification

```bash
./gradlew test --tests "com.jacobrozell.dartbuddy.domain.engine.CricketEngineTest"
```
