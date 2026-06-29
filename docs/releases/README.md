# Android release branches

Android trails iOS by **marketing version**. Each Android release branch ports the matching iOS `release/X.Y.Z` branch (or `master` for lean 1.0).

## Branch map

| Android branch | Tracks iOS | `versionName` | Surface gate |
|----------------|------------|---------------|--------------|
| `release/1.0.0` | `Dart-Buddy` `master` lean 1.0 | `1.0.0` | X01 + Cricket, custom bots |
| `release/1.1.0` | `Dart-Buddy` `release/1.1.0` | `1.1.0` | Party Pack (+ Baseball, Killer, Shanghai) |
| `main` | integration / ahead | `*-dev` | WIP only |

## Workflow

1. iOS merges spec or behavior changes on `release/X.Y.Z` in [Dart-Buddy](https://github.com/jacobrozell/Dart-Buddy).
2. On the matching Android branch: read the iOS specs at that tag/branch (clone or browse on GitHub).
3. Port domain engines first (mirror iOS unit test names).
4. Update [`PARITY.md`](../../PARITY.md) (or `docs/parity/X.Y.Z.md` when split).
5. CI green → Play internal track → tag `X.Y.Z` on ship.

## Rules

- **Do not port from iOS `main` while a release branch is active** — use the paired `release/X.Y.Z`.
- **`versionCode`** increments on every Play upload; **`versionName`** matches iOS marketing version.
- **PARITY.md** is the ship gate — feature matrix must match iOS surface for that release.

## Per-release checklists

| Release | Checklist | Parity |
|---------|-----------|--------|
| 1.0.0 lean | [`docs/release/1.0.0-ship-checklist.md`](docs/release/1.0.0-ship-checklist.md) | [`PARITY.md`](../../PARITY.md) |
| 1.1.0 Party Pack | TBD on `release/1.1.0` | TBD |
