# Pixvi - Android Pixiv Client

**⚠️ Early Development Stage - Educational Purpose Only**

A Android application built with Kotlin and Jetpack Compose that provides a simple interface for browsing Pixiv content. This project is in very early development with incomplete features and known bugs, created primarily for learning Android development concepts.

## Current Status

🚧 **This app is incomplete and contains bugs** 🚧

- The Screen Displaying Novels is laggy and is recommdation is repeated 
- Authentication flow may be unstable
- Content loading is basic and may fail
- UI components are not fully polished
- Many basic features show "TODO" placeholders
- Error handling is minimal

## Screenshots & Features
This is how the starting screen looks like.

<img src="https://github.com/user-attachments/assets/c9699edc-eaad-4ffd-8865-b1d8c36efa21" alt="Illustration Screen" width="300">
<img src="https://github.com/user-attachments/assets/5e1c4c1e-937b-4fe1-a331-bb77b04a70d1" alt="Novel Screen" width="300"> 

| Screenshot (Before) | Feature Description | Screenshot (After) |
|---|---|---|
| <img src="https://github.com/user-attachments/assets/1414f60c-3795-41c8-ac61-4e59946e1464" alt="Manga screen" width="300"> | **Interactive Elements** <br><br> This screen includes several interactive UI components: <br><br> - **Page Indicator:** At the top right, a circle shows the current page number (e.g., "1"). Tapping it reveals the page remaining and the indicator color changes to pink. <br><br> - **Info Button:** Tapping this button displays post metadata, including total views, likes/bookmarks, and the original post date. <br><br> - **Like Button:** A simple heart icon to like the post. It's an outline when not liked and becomes a filled heart when the post is liked. <br><br> - Pressing the text displaying the title and author name doesn't do anything now, but when long pressed. It would copy them to the clipboard | <img src="https://github.com/user-attachments/assets/ac95fac2-2873-4158-a745-2abb07acb11a" alt="Manga screen after clicking the clickable" width="300"> |
|<img src="https://github.com/user-attachments/assets/6910b0f2-b2bd-42f6-85b6-c0093f9ba140" alt="Bottom Sheet" width="300">|**Bottom Sheet** <br><br> Upon long pressing a image this would show up, different option maybe be showed based on the number of image, if multiple image. <br><br> You can download all the images as a pdf. After pressing it you can open the notfication screen to see the download progress and upon complemntion save it directly to device or lanuch the print dialog box. <br><br> The "Save All as Images" is not implemnted. <br><br> User can click the icon besides the hamburger menu icon to navigate to different sections. | <img src="https://github.com/user-attachments/assets/56a42389-f67e-4087-96d0-b781de3e493d" alt="Notification->Notifcation Page" width="300"> |

### Future Features
- Audio book for the novels
- Pdf exporting for novels
- Automatic filtering with the help of Activity Recognition API
- All the normal features in the orginal Pixiv apk
- Any many others...


## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3 and Material 3 Expressive
- **Architecture**: MVVM pattern with ViewModels
- **Navigation**: Navigation Compose
- **Networking**: Retrofit for API calls
- **Image Loading**: Coil
- **State Management**: StateFlow and Compose State

## Project Structure

```
app/src/main/java/com/example/pixvi/
├── MainActivity.kt                 # Main entry point
├── screens/
│   ├── MainAppShell.kt            # Main navigation shell
│   ├── LoginScreen.kt             # Authentication
│   └── homeScreen/                # Content screens
├── network/                       # API and networking
├── viewModels/                    # Business logic
└── preview/                       # UI previews and testing
```

## Educational Goals

This project demonstrates:
- Modern Android development with Jetpack Compose
- MVVM architecture implementation
- REST API integration with OAuth
- Material Design 3 components
- Navigation between multiple screens
- State management in Compose applications

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Build and run on device/emulator

**Note**: You'll need valid Pixiv account for full functionality.

## Disclaimer

This is an educational project and is not affiliated with Pixiv Inc. The app is incomplete, contains bugs, and should not be used in production. It's designed for learning Android development concepts and experimenting with modern Android technologies.

## Development Status

- ✅ Basic project structure
- ✅ Authentication framework
- ✅ Navigation setup
- 🚧 Content loading (partial)
- 🚧 Error handling (minimal)
- ❌ Offline support
- ❌ Production-ready features
- ❌ Comprehensive testing

---

*This project is a work in progress and serves as a learning exercise for Android development with Kotlin and Jetpack Compose.*
