# Deep Link Specification (Android)

## 1. Purpose

Android implementation of [`dart-buddy-ios/specs/DeepLinkSpec.md`](../dart-buddy-ios/specs/DeepLinkSpec.md). Same URL registry and versioning policy; routing via `MainActivity` intent + `NavHost`.

| Field | Value |
|-------|-------|
| **Application ID** | `com.jacobrozell.dartbuddy` |
| **Scheme** | `dartbuddy` |
| **Host** | `v1` |

---

## 2. Manifest

`AndroidManifest.xml` on `MainActivity`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="dartbuddy" android:host="v1" />
</intent-filter>
```

Matches `dartbuddy://v1/...` (path in `Uri.path`).

---

## 3. URL registry — v1 (lean 1.0)

Base: `dartbuddy://v1/{path}`

| Path | Android route | Status |
|------|---------------|--------|
| `/tab/play` | Play tab | ✅ |
| `/tab/players` | Players tab | ✅ |
| `/tab/activity` | Activity tab | ✅ |
| `/tab/settings` | Settings tab | ✅ |
| `/play` | Play tab home | ✅ |
| `/play/resume` | Play tab + resume active match | ✅ |
| `/play/setup` | Play setup | ✅ |
| `/play/match/{id}` | Active match | ✅ |
| `/activity/history/match/{id}` | History detail | ✅ |
| `/players/{id}` | Player detail | ✅ |

**Aliases:** `dartbuddy://play` should normalize to `dartbuddy://v1/play` (implement in parser if not already).

Lean 1.0 hides **Modes** tab — `/tab/modes` logs unsupported or maps to Play (match iOS lean surface).

---

## 4. Routing implementation

| Component | Path |
|-----------|------|
| Intent capture | `MainActivity.kt` — `intent.data`, `onNewIntent` |
| Deferred delivery | Hold `pendingDeepLink` until onboarding completes |
| Consumption | `NavHost.kt` — `deepLinkPath`, `onDeepLinkConsumed` |

Behavior matches iOS §5: links during onboarding queue until onboarding finishes.

---

## 5. App Links (future)

HTTPS `https://jacobrozell.github.io/Dart-Buddy/...` remains web/support only. **Digital Asset Links** for `dartbuddy.com` or GitHub Pages host are post–lean 1.0.

---

## 6. Testing

- Manual: `adb shell am start -a android.intent.action.VIEW -d "dartbuddy://v1/tab/players" com.jacobrozell.dartbuddy`
- Unit: URI parser tests when extracted to `support/DeepLinkParser.kt`
