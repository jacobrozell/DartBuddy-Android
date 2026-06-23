# App Shell Specification (Android)

## 1. Purpose

Root shell for lean 1.0 Android. Behavior aligns with [`dart-buddy-ios/specs/AppShellSpec.md`](../dart-buddy-ios/specs/AppShellSpec.md) and [`ProductSurfaceSpec.md`](ProductSurfaceSpec.md).

| Field | Value |
|-------|-------|
| **Application ID** | `com.jacobrozell.dartbuddy` |
| **Entry activity** | `MainActivity` |

---

## 2. Lean 1.0 tabs

| Tab | Route | iOS tab |
|-----|-------|---------|
| Play | `play` | Play |
| Players | `players` | Players |
| Activity | `activity` | Activity |
| Settings | `settings` | Settings |

**Modes tab:** hidden (not in lean 1.0 surface).

Implementation: `ui/navigation/NavHost.kt`, `ShellViewModel.kt`.

---

## 3. Bootstrap flow

1. `DartBuddyApp` — Hilt application class.
2. `MainActivity` — edge-to-edge, theme from settings (`AppearanceMode`).
3. **Onboarding gate** — if not complete, show `OnboardingScreen` (7 steps); else `NavHost`.
4. **Splash** — `Theme.DartBuddy.Splash` on activity; transition to main theme after first frame.

---

## 4. Active match chrome

- **Play tab badge** when match `InProgress`.
- **Resume banner** on Play and Activity when active match exists.
- Deep link `dartbuddy://v1/play/resume` selects Play and resumes session.

iOS parity: [`dart-buddy-ios/specs/PlayHomeSpec.md`](../dart-buddy-ios/specs/PlayHomeSpec.md).

---

## 5. External links

`support/AppLinks.kt` — FAQ, privacy, accessibility point at iOS repo GitHub Pages (`jacobrozell.github.io/Dart-Buddy/`). No Android-specific support site for lean 1.0.

---

## 6. Update prompts

iOS uses App Store lookup API. Android **lean 1.0:** no in-app Play update prompt (P3 in PARITY). Add Play In-App Updates API on a future release if needed.

---

## 7. Tablet / foldable / adaptive layout

Lean 1.0: phone-first with partial tablet support. **Canonical plan:** [`AdaptiveLayoutSpec.md`](AdaptiveLayoutSpec.md) — edge-to-edge insets (P0), `LayoutContext` breakpoints, XS/XL/foldable matrix, iOS `GameplayLayout` parity.

Track implementation in [`PARITY.md`](../PARITY.md) and [`ongoing/README.md`](../ongoing/README.md).
