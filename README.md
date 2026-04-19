# Pixvi - Android Pixiv Client

**⚠️ Early Development Stage - Educational Purpose Only**

A Android application built with Kotlin and Jetpack Compose that provides a simple interface for browsing Pixiv content. This project is in very early development with incomplete features and known bugs, created primarily for learning Android development concepts.

## Current Status

🚧 **This app is incomplete and contains bugs** 🚧

- Only ILLURASATION PAGES SCREEN ARE MADE FOR NOW.
- User get's logout if app is opened without internet connect
- PDF formation process is silent.
- Only logout via the phone's inbuilt account setting.
- Error handling is minimal.

## Screenshots & Features
Home page

<img width="300" alt="image" src="https://github.com/user-attachments/assets/02cd1f17-7591-4a7c-b548-e041507fdebc" />

### Future Plans
-Restore all the previous versions feature(novel playback, full screen image viewing, novel screen)
- Pdf exporting for novels
- Pixiv animation effects
- All the normal features in the orginal Pixiv apk
- Any many others...

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3 (including Material 3 Expressive via material3:1.5.0-alpha13)
- **Architecture**: MVVM pattern with ViewModels (Manual DI via AppContainer)
- **Navigation**: Navigation 3
- **Networking**: Retrofit with custom NetworkResultCallAdapter and Kotlinx Serialization converter
- **Image Loading**: Coil 3 (with OkHttp network integration and GIF support)
- **Encryption**: Google Tink
- **Serialization**: Kotlinx Serialization
- **State Management**: StateFlow and Compose State
- **Local Storage**: Room Database + DataStore Preferences
- **Background Work**: WorkManager

## Project Structure

<details>
<summary>📁 Click to expand</summary>

- [AppContainer.kt](cci:7://file:///c:/Users/91991/AndroidStudioProjects/pixvi2/app/src/main/java/com/cryptic/pixvi/AppContainer.kt:0:0-0:0) — Manual DI container
- [MainActivity.kt](cci:7://file:///c:/Users/91991/AndroidStudioProjects/pixvi2/app/src/main/java/com/cryptic/pixvi/MainActivity.kt:0:0-0:0) — Single Activity entry point
- **appShell/** — Main scaffold, nav bar, shell
- **artwork/** — Artwork viewer feature
    - **data/** — Data models
    - **ui/** — Screens & components
    - **viewmodel/** — State management
- **auth/** — Authentication (AccountManager + PKCE OAuth)
    - **account/** — Custom Authenticator
    - **data/** — Token management
    - **util/** — Code challenge/verifier generators
- **core/** — Shared infrastructure
    - **network/** — Retrofit client, interceptors, API services, repos
    - **downloader/** — Image & PDF download logic
    - **storage/** — DataStore preferences
    - **tink/** — Google Tink encryption
- **database/** — Room entities, DAOs, repos
- **login/** — OAuth login screen & ViewModel
- **notification/** — Notification list UI & ViewModel
- **printer/** — PDF generation service
- **worker/** — WorkManager download workers
- **ui/theme/** — Material 3 colors, typography, theme
- **experimental/** — Sliding window box blur

</details>

## Experimental

### **Custom Blur Engine**

A CPU-based box blur that runs entirely on the CPU without needing RenderScript or GPU shaders. It downscales the image first, applies a sliding-window box blur (horizontal + vertical passes), and scales back up. Multiple passes can be applied to approximate a Gaussian blur. It reuses internal buffers across calls to reduce memory allocations.

> This code was largely AI-generated and has not been extensively tested in production.

**Location:** `com.cryptic.pixvi.experimental.SlidingWindowBoxBlur`

**Usage:**

```kotlin
// In a Composable — automatically cleans up when leaving the screen
val blurrer = rememberBoxBlurState()

// Apply blur with default settings (25% scale, 9×9 kernel, 2 passes)
val blurredBitmap = blurrer.blur(bitmap)

// Or with custom settings
val blurredBitmap = blurrer.blur(bitmap, SlidingWindowBoxBlur.BlurConfig(
    scaleFactor = 0.1f,  // 10% resolution for speed
    kernelSize = 15,     // larger = more blur
    passes = 3           // more passes = smoother
))
```

> **Note:** The input bitmap is recycled after blurring. Pass a copy if you need to keep the original.

#### Benchmark and Visual Examples

<img width="600" alt="blur_benchmark_results" src="https://github.com/user-attachments/assets/660ff172-3d8f-40e4-989b-158d4d6c2667" />

<img width="900" alt="blur_comparison_collage" src="https://github.com/user-attachments/assets/9071226d-6712-4cb2-b679-059ee82facf2" />
