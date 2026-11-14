# Memento

Think it. Write it. Keep it.

Memento is an offline-first Android notes app built with Kotlin, Jetpack Compose, Material 3, and Room.

## Current functionality

- Create and edit text notes
- Automatically save edits to a local Room database
- Search note titles and bodies
- Pin notes and sort them ahead of other notes
- Create and edit checklists, track completion, and search their items
- Organize notes into folders and archive or restore notes
- Choose note colors, grid/list layout, light/dark theme, and dynamic color
- Use Android Dynamic Color on Android 12 and newer, with Material light/dark fallback palettes
- Adaptive card grid, search, settings, and bottom navigation using Material 3 Expressive

## Download and install

[Download the debug APK](dist/memento-debug.apk) from this repository and install it on Android 8.0 or newer. On GitHub, open the link and select **Download raw file**. You may need to allow installations from your browser or file manager in Android settings.

This is a debug-signed test build, not a production release. An existing installation signed with a different key cannot be updated with this APK without uninstalling it first; uninstalling removes its local data.

## Build

Open this directory in Android Studio and install Android SDK Platform 35. The project uses Android Gradle Plugin 8.8.2, Gradle 8.10.2 or newer, and requires JDK 17 or newer. Material 3 is pinned to 1.5.0-alpha07 because its public Expressive APIs support the current Android build toolchain; those APIs are experimental.

To build a debug APK from a machine with Gradle and the Android SDK configured:

```powershell
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

Instrumented tests can be run with `gradle connectedDebugAndroidTest`. Run them on a dedicated test emulator: Gradle may uninstall the app after testing and remove its local data.

## Project status

Rich-text editing, attachments, tags, trash and backup/restore, the integrated home-screen widget, and release signing are not implemented yet. Deleting a note is permanent; deleting a folder keeps its notes.
