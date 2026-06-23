# Product Surface Specification (Android)

## 1. Purpose

Feature gate for Android releases. Mirrors iOS `ProductSurface.lean1_0` on `dart-buddy-ios` `master`. Each release branch sets **one** active surface constant in code (future: `domain/surface/ProductSurface.kt`).

---

## 2. Lean 1.0 (`release/1.0.0`)

| Gate | Value |
|------|-------|
| **Application ID** | `com.jacobrozell.dartbuddy` |
| **versionName** | `1.0.0` |
| **Tabs** | Play · Players · Activity · Settings (4) |
| **Modes tab** | Hidden |
| **Gameplay** | X01 + Cricket only |
| **Preset bots** | On |
| **Custom bots** | On |
| **Training Partner bots** | Off |
| **Party modes** | Off |
| **Player export (DBPE)** | Off |
| **Locales** | English only (lean 1.0) |
| **Firebase Analytics + Crashlytics** | Target for release; not blocking dogfood |
| **minSdk** | 36 |

Parity detail: [`PARITY.md`](../PARITY.md).

---

## 3. Party Pack 1.1.0 (planned — `release/1.1.0`)

When branching from iOS `release/1.1.0`:

| Gate | Value |
|------|-------|
| **versionName** | `1.1.0` |
| **Added modes** | Baseball, Killer, Shanghai |
| **Surface** | Extend lean 1.0; still no Modes catalog tab unless iOS ships it |

Add Android game-mode specs under `specs/game-modes/implemented/` as each mode ports.

---

## 4. Enforcement

- Navigation: do not register routes for out-of-surface modes.
- Play setup: only show mode chips in active surface.
- Settings / help: do not link to unreleased features.
- Unit tests: assert `ProductSurface` gates for the active release branch.

---

## 5. iOS reference

iOS source of truth for **what** ships: `dart-buddy-ios` `ProductSurface` / release checklists. Android implements **when** on the paired release branch — see [`docs/releases/README.md`](../docs/releases/README.md).
