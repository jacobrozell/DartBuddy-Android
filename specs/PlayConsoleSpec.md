# Google Play Console Specification

## 1. Purpose

Play Store release metadata and pipeline for Dart Buddy Android. iOS analogue: [AppStoreConnectSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/AppStoreConnectSpec.md).

| Field | Value |
|-------|-------|
| **Application ID** | `com.jacobrozell.dartbuddy` |
| **Play listing name** | Dart Buddy |
| **Developer account** | Personal Apple team `7JT2JB89AV` has no Play equivalent — use personal Google Play developer account |

---

## 2. Lean 1.0 status

| Item | Status |
|------|--------|
| Play Console app record | ⬜ |
| Release signing key | ⬜ |
| Internal testing track | ⬜ |
| Production | ⬜ |

---

## 3. Signing

- **Upload key** in Play Console; store keystore outside repo (`keystore/` gitignored).
- `release` build type: enable minify when ProGuard rules validated; lean 1.0 currently `isMinifyEnabled = false`.
- Never commit keystore passwords or `google-services.json` secrets.

---

## 4. Versioning

| Field | Rule |
|-------|------|
| `versionName` | Match iOS marketing version (`1.0.0`, `1.1.0`) |
| `versionCode` | Monotonic integer per Play upload |

Configured in `app/build.gradle.kts` `defaultConfig`.

---

## 5. Store listing assets

Reuse iOS marketing copy where accurate; generate phone screenshots from Android emulator (16+). Feature graphic and icon from brand assets — full splash art gap noted in [`PARITY.md`](../PARITY.md).

**Privacy / support URLs:** same as iOS GitHub Pages (`AppLinks.kt`).

---

## 6. Firebase

When shipping release builds:

1. Add Android app in Firebase with package `com.jacobrozell.dartbuddy`.
2. Place `google-services.json` in `app/` (gitignored). Copy from [`google-services.json.example`](../app/google-services.json.example) for local/CI smoke only.
3. Apply Google Services + Crashlytics Gradle plugins per [FirebaseBackendAnalyticsSpec.md (iOS)](https://github.com/jacobrozell/Dart-Buddy/blob/master/specs/FirebaseBackendAnalyticsSpec.md) event names.

---

## 7. Tracks

| Track | Use |
|-------|-----|
| Internal | Dogfood, CI-signed builds |
| Closed | Trusted testers |
| Production | Public launch |

Branch alignment: [`docs/releases/README.md`](../docs/releases/README.md).
