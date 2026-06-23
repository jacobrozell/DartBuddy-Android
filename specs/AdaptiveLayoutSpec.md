# Adaptive layout & edge-to-edge plan

> **Status:** Shipped (lean 1.0) — 2026-06-23  
> **iOS reference:** `dart-buddy-ios/DesignSystem/Components/GameplayLayout.swift`, [`docs/gameplay-layout-modes.md`](../dart-buddy-ios/docs/gameplay-layout-modes.md)  
> **Android code today:** `ui/layout/GameplayLayout.kt`, `ui/designsystem/BrandComponents.kt`, `MainActivity.kt`

## 1. Problem statement

Dart Buddy Android targets **API 36+** phones and tablets. Today:

| Area | State | Risk |
|------|-------|------|
| **Edge-to-edge** | `enableEdgeToEdge()` in `MainActivity` | **No `WindowInsets` consumption** — content can draw under status bar, cutout, gesture nav |
| **Breakpoints** | Width dp only (`600`, `840`) | **Large iPhone landscape** (regular width) misclassified as tablet |
| **Tablet** | Partial side-by-side @600dp+ | Missing iPad-only cricket board variants, 3+ player threshold, portrait sidebar pad |
| **Small screens** | No dedicated policy | `<360dp` width — chips/pad may clip; no reduced column count |
| **Huge screens** | `920dp` max width on tab roots | Match screens full-bleed OK; foldables/desktop untested |
| **Font scale** | Compose default scaling only | No AX layout switches (iOS has AX1–AX5 alternate shells) |
| **Multi-window** | Not specified | Split-screen uses **smallest width** — layout must follow that, not current width |

**Goal:** One predictable layout system for edge-to-edge safe areas and all width/height/idiom combinations, aligned with iOS lean 1.0 intent.

---

## 2. Design principles

1. **Safe areas first** — edge-to-edge is visual; interactive content stays in `safeDrawing` (+ IME when keyboard open).
2. **Smallest-width wins** — all breakpoint decisions use `smallestScreenWidthDp` (foldable outer, split-screen).
3. **Phone ≠ wide phone** — `screenWidthDp >= 600` alone must **not** imply tablet (iOS Pro Max landscape lesson). Require `smallestScreenWidthDp >= 600` **or** explicit tablet signal.
4. **Match screens bleed horizontally** — scoreboard/pad use full width; tab roots use readable column (`920dp` max).
5. **Player-count gates** — iPad stacked shell for 1–2 players; side-by-side sidebar pad at 3+ (iOS `sideBySidePlayerCountThreshold`).
6. **Font scale overrides width** — at large accessibility text, fall back to stacked scroll layouts (no side-by-side).
7. **Test matrix before ship** — every P0/P1 item has an emulator config + manual smoke step.

---

## 3. Device & window taxonomy

### 3.1 Signals (Compose / Android)

| Signal | Source | Use |
|--------|--------|-----|
| `smallestScreenWidthDp` | `LocalConfiguration` | **Primary** breakpoint input |
| `screenWidthDp` / `screenHeightDp` | `LocalConfiguration` | Orientation, landscape detection |
| `isLandscape` | `height < width` | Match side-by-side (phone landscape) |
| `fontScale` | `LocalDensity.current.fontScale` | AX layout switches |
| `WindowInsets.safeDrawing` | `Modifier.windowInsetsPadding` | Edge-to-edge safe areas |
| `WindowInsets.ime` | keyboard | Play setup text fields, add-player sheet |
| Tablet hint | `smallestScreenWidthDp >= 600` | Tablet idiom (covers most tablets; verify foldable inner) |
| Foldable | `WindowInfoTracker` / Jetpack WindowManager (optional P2) | Hinge-aware two-pane (future) |

### 3.2 Size buckets

| Bucket | Smallest width (dp) | Typical devices | Layout family |
|--------|---------------------|-----------------|---------------|
| **XS** | `< 360` | Small phones, cover displays, narrow split-screen | Compact chips (4-col pad), tighter spacing, scroll-heavy |
| **S** | `360 – 599` | Standard phones portrait | Default phone layouts |
| **M** | `600 – 839` | Tablets portrait, unfolded inner narrow, large split | Readable column, setup 2-col, match side-by-side if landscape or tablet |
| **L** | `840 – 1199` | Tablets landscape, desktop window | Wide score cards, wider pad sidebar (`300dp`) |
| **XL** | `≥ 1200` | Desktop / large tablet landscape | Same as L + verify max-width centering; optional wider pad |

**Height buckets** (orthogonal):

| Bucket | Condition | Effect |
|--------|-----------|--------|
| **Landscape** | `height < width` | Match: board \| pad; summary: split |
| **Short** | `height < 480` | Reduce vertical chrome; pin pad; hide non-critical preview rows |
| **Tall** | default portrait | Standard stacked pad below board |

---

## 4. Edge-to-edge plan (P0)

### 4.1 Current gap

```kotlin
// MainActivity.kt — enabled but no inset propagation
enableEdgeToEdge()
```

No `WindowInsets`, `systemBarsPadding`, or `safeDrawing` modifiers anywhere in the app.

### 4.2 Target architecture

```
MainActivity (enableEdgeToEdge)
  └─ DartBuddyTheme
       └─ AppInsetsProvider          // NEW: CompositionLocal<PaddingValues> or wrapper
            ├─ NavHost Scaffold       // contentWindowInsets = WindowInsets(0) if handling manually
            │    ├─ Tab routes       // top: safeDrawing.top, bottom: navBar + safeDrawing.bottom
            │    └─ Match routes     // top: safeDrawing.top; bottom: safeDrawing.bottom (no tab bar)
            └─ OnboardingFlow        // full-screen; all safeDrawing edges
```

### 4.2.1 Implementation tasks

| # | Task | Files |
|---|------|-------|
| E1 | Add `ui/layout/WindowInsets.kt` — `Modifier.tabRootInsets()`, `matchScreenInsets()`, `stickyFooterInsets()` | new |
| E2 | Wrap root content in `Box(Modifier.fillMaxSize().safeDrawingPadding())` **or** configure `Scaffold(contentWindowInsets = …)` explicitly | `NavHost.kt`, `MainActivity.kt` |
| E3 | Play setup sticky START footer — pad bottom by `max(navBar, safeDrawing.bottom)` | `SetupHomeChrome` in `BrandComponents.kt` |
| E4 | Status bar / nav bar icon contrast — `SystemBarStyle` light/dark from theme in `MainActivity` | `MainActivity.kt` |
| E5 | Splash theme — `android:statusBarColor` / `navigationBarColor` transparent for API 36 | `themes.xml` |
| E6 | Cutout — never place primary CTAs in top corners; use `displayCutout` padding on full-bleed match header | `MatchGameplayHeader.kt` |
| E7 | IME — `Modifier.imePadding()` on sheets with text fields (add player, onboarding name) | `AddPlayerSheet.kt`, `OnboardingFlow.kt` |

### 4.3 Verification

- [ ] Pixel 8 — gesture nav: tab labels not under home indicator
- [ ] Pixel 8 — punch-hole: Play title not under camera
- [ ] Emulator 3-button nav: bottom bar + system nav don't double-stack awkwardly
- [ ] Rotate during match — insets update without clipping pad
- [ ] Keyboard on Play add-player sheet — field stays visible

---

## 5. Gameplay layout plan (P1)

Extend `GameplayLayout.kt` to mirror iOS helpers. **All functions take `LayoutContext` data class** (smallestWidth, width, height, fontScale, playerCount) — not raw width alone.

### 5.1 `LayoutContext` (new)

```kotlin
data class LayoutContext(
    val smallestWidthDp: Int,
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float,
    val playerCount: Int = 0,
) {
    val isLandscape get() = heightDp < widthDp
    val isTablet get() = smallestWidthDp >= 600
    val isCompactWidth get() = smallestWidthDp < 360
    val isAccessibilityFont get() = fontScale >= 1.3f // tune to match iOS AX threshold
}
```

### 5.2 Helper matrix (parity with iOS)

| Helper | True when | Android today |
|--------|-----------|---------------|
| `usesAccessibilityMatchScoringLayout` | `fontScale` ≥ threshold | ⬜ |
| `usesSideBySideMatchScoringLayout` | landscape **or** (tablet **and** portrait) | 🟡 landscape OR width≥600 (wrong on Pro Max) |
| `usesLandscapePhoneMatchScoringLayout` | landscape **and not** tablet | 🟡 partial |
| `usesTabletPortraitMatchScoringLayout` | tablet **and** portrait | ⬜ |
| `usesSideBySideBottomScoringRegion` | tablet **and** `playerCount >= 3` | ⬜ |
| `usesWidePlayerScoreCardLayout` | tablet **and** width≥840 **and** not AX font | 🟡 width only |
| `usesPinnedActiveX01PlayerCard` | landscape OR `playerCount >= 3` OR AX font | ✅ close |
| `scoringPadColumnCount` | 7 default, 4 if AX | ⬜ (always 7) |
| `scoringPadFixedWidth` | 252 phone / 300 tablet landscape | 🟡 partial |
| `contentMaxWidth` | 920 if tablet else unlimited | 🟡 width≥600 |
| `usesLandscapeMatchSummaryLayout` | landscape **and not** AX | 🟡 |
| `usesMatchSummarySideBySideGrid` | tablet OR (2 players portrait phone) | 🟡 |
| `usesTransposedCricketBoardLayout` | tablet landscape | ⬜ |
| `usesCricketBoardFillHeight` | tablet landscape | ⬜ |

### 5.3 Per-screen layout policy

| Screen | XS/S phone | M tablet | L/XL | Landscape phone | Notes |
|--------|------------|----------|------|-----------------|-------|
| **Tab roots** | Full width, scroll | Centered `920dp` | Centered | Same | Bottom inset for tab bar |
| **Play setup** | Single column | 2-column `@600` | 2-column | Single column if height short | Sticky footer above insets |
| **X01 match** | Pad below board | Side-by-side if 3+ players | Wider pad | Side-by-side | AX: full scroll stack |
| **Cricket match** | Pad below board | Transposed board variant | Fill-height board | Side-by-side | iOS cricket iPad modes |
| **Match summary** | Stacked scroll | Side-by-side grid | Split landscape | Landscape split | |
| **Onboarding** | Full-screen scroll | Readable width centered | Same | Same | IME padding on inputs |
| **History detail** | Full width cards | `920dp` max | Same | — | Charts scroll horizontal OK |

### 5.4 Implementation phases

| Phase | Scope | Effort |
|-------|-------|--------|
| **P1a** | `LayoutContext` + fix tablet vs phone landscape misclassification | 1–2 days |
| **P1b** | Player-count sidebar threshold (3+) for match pad | 1 day |
| **P1c** | Cricket tablet board variants | 1–2 days |
| **P1d** | Font-scale AX layouts (pad 4-col, stacked cards, no side-by-side) | 2 days |

---

## 6. Extreme & weird sizes (P2)

### 6.1 Very small (XS, <360dp)

| Issue | Mitigation |
|-------|------------|
| Option chips wrap awkwardly | `FlowRow` already; reduce min chip width; allow 2-line labels |
| Number pad cramped | Reduce cell min size; consider 4 columns below 340dp |
| Turn order rows clip | Avatar 24dp; single-line names with ellipsis |
| Sticky START footer covers roster | Ensure footer only uses measured inset, not fixed 96dp |

**Test devices:** Emulator `Resizable` at 320×640; Galaxy Z Flip cover (if available).

### 6.2 Very large (XL, ≥1200dp)

| Issue | Mitigation |
|-------|------------|
| Tab content too wide | Keep `920dp` max — **done** |
| Match pad lost in empty space | Fixed-width pad column anchored trailing; scoreboard fills remainder |
| Summary cards stretch | Side-by-side grid with `weight(1f)` per card — **done** |
| Desktop freeform window | Treat as tablet; re-layout on `WindowSizeClass` changes |

### 6.3 Foldables & multi-window

| Scenario | Policy |
|----------|--------|
| **Split-screen (50%)** | Use **smallestScreenWidthDp** → likely phone layout |
| **Foldable outer** | Usually XS/S — phone layout |
| **Foldable inner** | M/L — tablet layout |
| **Hinge obscures center** | P3: Jetpack `WindowInfoTracker` — avoid primary CTAs in hinge zone |
| **Resize mid-match** | `LayoutContext` from `BoxWithConstraints` + recomposition; persist match state (no restart) |

**Manifest:** consider `android:resizeableActivity="true"` (default on API 36) — document in spec; no change needed unless disabled.

### 6.4 Density / font outliers

| Scenario | Policy |
|----------|--------|
| System font 200% | `usesAccessibilityMatchScoringLayout` → stacked layouts |
| Display size "Largest" | Combined with fontScale in `LayoutContext` |
| TalkBack | Semantic headings already partial; add test IDs in adaptive pass |

---

## 7. Compose implementation conventions

### 7.1 Single source of truth

```kotlin
@Composable
fun rememberLayoutContext(playerCount: Int = 0): LayoutContext {
    val cfg = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    return remember(cfg, fontScale, playerCount) {
        LayoutContext(
            smallestWidthDp = cfg.smallestScreenWidthDp,
            widthDp = cfg.screenWidthDp,
            heightDp = cfg.screenHeightDp,
            fontScale = fontScale,
            playerCount = playerCount,
        )
    }
}
```

Use in match screens instead of scattered `currentScreenWidthDp()` calls.

### 7.2 Modifiers (new file `ui/layout/AppInsets.kt`)

| Modifier | Applies |
|----------|---------|
| `tabRootPadding()` | safe top + bottom (includes nav bar when tab visible) |
| `matchScreenPadding()` | safe top + bottom (no tab bar) |
| `readableWidth()` | `widthIn(max = contentMaxWidth(ctx))` + center |
| `stickyBottomPadding()` | for SetupHomeChrome footer |

### 7.3 Avoid

- Hard-coded `padding(bottom = 96.dp)` without insets
- `screenWidthDp >= 600` without `smallestScreenWidthDp` check
- Separate breakpoint constants in `DS` and `GameplayLayoutMetrics` — **consolidate to one object**

---

## 8. Testing matrix

### 8.1 Emulator configs (manual + release checklist)

| ID | Config | smallestWidth | Validates |
|----|--------|---------------|-----------|
| T1 | Pixel 8 portrait | ~412 | Default phone, edge-to-edge |
| T2 | Pixel 8 landscape | ~915* | Phone landscape side-by-side (*width; smallest still ~412) |
| T3 | Pixel Tablet portrait | ~800 | 2-col setup, readable width |
| T4 | Pixel Tablet landscape | ~1280 | Wide cards, cricket board |
| T5 | Resizable 320×640 | 320 | XS chips/pad |
| T6 | Resizable 2200×1200 | 1200 | XL centering |
| T7 | Split-screen 50% | ~360 | Multi-window |
| T8 | Font scale 1.5× / 2.0× | any | AX stacked layout |
| T9 | Display cutout emulator | any | Punch-hole padding |

### 8.2 Unit tests (extend `GameplayLayoutTest.kt`)

- Tablet vs phone landscape classification (`smallestWidthDp = 412`, `widthDp = 915` → **not** tablet portrait side-by-side)
- Player count threshold for sidebar pad
- Font scale disables side-by-side
- `contentMaxWidth` only when `smallestWidthDp >= 600`

### 8.3 Screenshot / Macrobenchmark (optional P3)

- Paparazzi or Compose screenshot tests at T1/T3/T5 for Play setup + X01 match

---

## 9. Delivery roadmap

| Priority | Milestone | Blocks store? |
|----------|-----------|---------------|
| **P0** | Edge-to-edge insets (§4) | **Yes** — UI under system bars looks broken on API 36 |
| **P1a** | `LayoutContext` + tablet/phone fix | **Yes** — wrong layouts on large phones |
| **P1b–d** | Match/cricket/summary tablet parity | Quality |
| **P2** | XS + XL + split-screen hardening | Quality |
| **P3** | Foldable hinge + screenshot tests | Nice-to-have |
| **P3** | Material3 `WindowSizeClass` migration if needed | Optional |

Suggested branch commits:

1. `fix/insets-edge-to-edge` — P0 only, small diff
2. `feat/layout-context` — P1a helpers + tests
3. `feat/tablet-match-layouts` — P1b–d screen wiring
4. `chore/layout-qa-matrix` — checklist in `docs/release/`

---

## 10. Doc cross-references

| Doc | Update when |
|-----|-------------|
| [`AppShellSpec.md`](AppShellSpec.md) §7 | Link this spec; remove "deferred" |
| [`PARITY.md`](../PARITY.md) | Mark tablet/iPad rows when P1 done |
| [`STATUS.md`](../STATUS.md) | Remaining work → P0 insets |
| [`docs/release/`](../docs/releases/README.md) | Add layout smoke to release checklist |
| iOS [`gameplay-layout-modes.md`](../dart-buddy-ios/docs/gameplay-layout-modes.md) | Keep in sync when adding helpers |

---

## 11. Open decisions

| # | Question | Recommendation |
|---|----------|----------------|
| 1 | Use Material3 `androidx.compose.material3.adaptive` WindowSizeClass? | **Defer** — `smallestScreenWidthDp` + tablet bool is enough for lean 1.0 |
| 2 | Match screens under status bar (immersive) vs padded? | **Padded** — match header below status bar (iOS parity) |
| 3 | Navigation bar transparent vs themed? | **Transparent** with `NavigationBar` containerColor (current) + inset padding |
| 4 | Support ChromeOS desktop resize? | **Best-effort** via tablet rules; no dedicated desktop IA for 1.0 |
