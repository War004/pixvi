# PiYek

A privacy-focused Pixiv client, built to explore secure token handling on Android.

<div align="center">
  <video src="https://github.com/user-attachments/assets/38116576-ea1c-415d-957d-86cdc0d89843" width="280" controls></video>
  <br/>
  <sub>Some frame drops due to screen recording.</sub>
</div>

## Why I built this

Pixiv's official app ships with trackers and ads baked in, there were many client that I could try. All of them were great. But I wanted to build one from scratch as a fun excerise to know more about android app development in depth. 

## What the app does

Browse illustrations and artwork from Pixiv — no ads, no trackers, no telemetry.

## Technical Highlights

- **PKCE OAuth with CSRF protection** — login opens in a Chrome Custom Tab with a SHA-256 code challenge. On redirect, the saved `codeVerifier` acts as a CSRF guard — intents without a matching verifier are rejected, so rogue apps can't inject auth callbacks.

- **Encrypted refresh tokens (Tink AES-256-GCM)** — refresh tokens are encrypted via Google Tink AEAD, backed by a master key in Android Keystore. The encrypted blob is Base64-encoded and stored in Room. The raw token only exists briefly in memory between API response and encryption.

- **In-memory access tokens with dual refresh** — access tokens live in an `AtomicReference` (never persisted to disk). Refreshed proactively at startup by `SessionManager`, and reactively on 401s by an OkHttp `Authenticator` with `Mutex`-guarded thundering herd prevention.

- **Single-pass dominant color extraction** — a custom algorithm uses a pruned 15-bit histogram (32K buckets, ~131 KB) with epoch-stamped O(1) clears. Drives the dynamic background tinting and contrast-aware text colors on the onboarding screen.

- **Reverse-engineered X-Client-Hash** — every API request includes a time-stamped MD5 hash derived from a salt, matching what the official Pixiv app sends. Image requests go through a separate OkHttp client with the required `Referer` header for Pixiv's CDN.

- **Type-safe network layer** — a custom `NetworkResultCallAdapterFactory` wraps every Retrofit call into a sealed `NetworkResult<T>` (Success / Error / Exception), eliminating scattered try-catch blocks.

## Tech Stack

| Layer | Library |
|-------|---------|
| **Language** | Kotlin 2.4 |
| **UI** | Jetpack Compose (Material 3, BOM 2026.05) |
| **Networking** | Retrofit 3 + OkHttp, Kotlinx Serialization |
| **Image Loading** | Coil 3 (with OkHttp backend) |
| **Database** | Room 2.8 |
| **Preferences** | DataStore Preferences |
| **Encryption** | Google Tink 1.21 (AEAD / Android Keystore) |
| **Async** | Kotlin Coroutines + Flow |
| **Navigation** | AndroidX Navigation3 |
| **Background Work** | WorkManager |
| **Build** | AGP 9.2, KSP, Version Catalog (TOML) |
| **Testing** | JUnit 5, MockK, Coroutines Test, Room Testing |
| **DI** | Manual (`AppContainer` with `lazy`) |

## Known Limitations / In Progress

The app is in a prototype phase, right now it only has two screens, one for onboarding and the other for viewing the illustration page. 

There are many things which are not included in the app which include:
- Screens for Home, Manga, Novel.
- Downloading multiple page drawings and novels as PDF.
- Speech playback in novel.
- Multiple account selection.
- Notifications.

