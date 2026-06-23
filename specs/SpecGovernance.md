# Spec Governance (Android)

## 1. Two-layer model

| Layer | Location | Owns |
|-------|----------|------|
| **Behavior** | `dart-buddy-ios/specs/` | Rules, UX intent, copy, edge cases |
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

## 3. Submodule

`dart-buddy-ios` pins a commit of the iOS repo. Bump submodule when starting a port from a new iOS release tag or branch.

```bash
cd dart-buddy-ios && git fetch && git checkout release/1.1.0
cd .. && git add dart-buddy-ios && git commit -m "Bump iOS reference for 1.1.0 port"
```

---

## 4. Verification

- Domain: unit tests (`./gradlew test`)
- Parity: [`PARITY.md`](../PARITY.md) checklist before Play track promotion
- CI: `.github/workflows/ci.yml` on every PR
