# X01 Game Mode (Android)

## 1. Canonical behavior

All scoring rules, checkout logic, leg/set formats, and bust handling: **[`dart-buddy-ios/specs/game-modes/implemented/X01GameSpec.md`](../../dart-buddy-ios/specs/game-modes/implemented/X01GameSpec.md)**.

---

## 2. Android implementation

| Piece | Location |
|-------|----------|
| Engine | `domain/engine/X01Engine.kt` |
| Checkout suggestions | `domain/engine/CheckoutSuggester.kt` |
| Match UI | `ui/play/match/X01MatchScreen.kt` |
| ViewModel | `ui/play/match/X01MatchViewModel.kt` |
| Setup options | `ui/play/PlayScreen.kt` (start scores 101–601, check-in/out, legs/sets) |
| Unit tests | `X01EngineTest.kt`, `X01VisitScoringTest.kt`, `CheckoutSuggesterTest.kt` |

---

## 3. Payload / persistence

Match config and snapshot serialized in `MatchEntity.configJson` / `snapshotJson`. Field names and `payloadVersion` must stay compatible with iOS engine payloads where history parity matters — see [`DataSchemaSpec.md`](../DataSchemaSpec.md).

---

## 4. Lean 1.0 gaps vs iOS

| Item | Status |
|------|--------|
| Core scoring + checkout | ✅ |
| Double-in check-in UX | ✅ |
| Leg/set win banners | ✅ |
| Tablet wide scorecard | 🟡 partial |

Track in [`PARITY.md`](../../PARITY.md).

---

## 5. Verification

```bash
./gradlew test --tests "com.jacobrozell.dartbuddy.domain.engine.X01*"
```

Mirror iOS `X01EngineTests` scenarios when adding cases.
