# Wallora

**A local-first Android wallpaper studio** for still images and video live wallpapers.

- Android application ID: `app.wallora.premium`
- Gradle namespace: `com.judson.wallora`
- Target SDK: Android 17 / API 37
- Minimum SDK: Android 8.0 / API 26
- Language and UI: Kotlin, Jetpack Compose, Material 3

## Current app experience

Wallora currently includes:

- Three-section navigation: Library, Studio, and Settings.
- Multi-select media import through Android's system document picker.
- A local library with image/video labels, thumbnails, search, filters, favourites, and remove-from-library.
- Persisted Storage Access Framework (SAF) permissions for individually selected media.
- Recursive SAF folder indexing with bounded traversal. A scan imports up to 2,500 supported files and visits at most 10,000 entries.
- A still-image editor with Fill/Fit composition, quarter-turn rotation, and a dim overlay.
- Screen-shaped JPEG rendering, export through Android's Create Document flow, and direct application of the rendered result to Home, Lock, or both.
- A video live-wallpaper service with looping, mute/unmute, Fill/Fit framing, and 0.5×–2× playback speed.
- Optional All files access settings handoff. Core import and folder scanning do not require it.
- Edge-to-edge activity layout and an Android 17 GitHub Actions debug-build workflow.

The still-image editor renders the current crop mode, rotation, and dim level into a new JPEG. Precision focal-point positioning, arbitrary crop handles, brightness/contrast/saturation, undo/redo, and non-destructive edit history remain planned. GIF files are recognised in the library but are not yet animated as live wallpapers.

## Design plan

The product direction, screen layouts, interaction flows, visual system, accessibility requirements, storage model, and phased roadmap are documented in [docs/DESIGN_PLAN.md](docs/DESIGN_PLAN.md).

## Storage and privacy

Wallora uses Android's document picker by default. Imported media is represented by persisted content URIs and basic metadata; library import does not copy or modify the original file. Users can retain a folder grant through SAF and scan it for supported images and videos. Folder traversal has explicit depth and entry limits. Edited images are saved as new JPEG files through the system document picker.

Optional **All files access** is a special Android permission. It is not required for core wallpaper use, is subject to Google Play policy, and does not grant unrestricted access to protected Android or app-private directories.

## Live wallpaper limitations

Android owns the final wallpaper preview and confirmation. Wallora cannot universally force a live wallpaper onto a device whose OEM firmware, launcher, policy, or system wallpaper service blocks it. The app registers its own WallpaperService and uses the supported system preview/chooser flows.

The video renderer loops the selected source and reads the saved mute, framing, and playback-speed preferences when the system preview starts. Codec support, lifecycle behaviour, battery impact, and OEM-specific preview/apply flows still require broader physical-device testing.

## Release signing

The release application ID is `app.wallora.premium`, with signing alias `wallora_release`.

Private signing files are intentionally excluded from Git. See [SIGNING.md](SIGNING.md). Never commit the keystore, signing ZIP, or passwords. Keep secure backups of the same signing identity for every future update.

## Build locally

Requirements:

- Android Studio with Android SDK Platform 37
- JDK 17
- Gradle 9.5.0

Open the repository in Android Studio and sync, or run:

```bash
gradle :app:assembleDebug
```

The GitHub Actions workflow uses Gradle 9.5.0 and builds a debug APK on pushes and pull requests. Successful runs upload the APK as a workflow artifact.

## Repository structure

```text
app/src/main/
  AndroidManifest.xml
  java/com/judson/wallora/
    MainActivity.kt
    data/
      MediaItem.kt
      MediaRepository.kt
      MediaLibraryStore.kt
    wallpaper/
      StaticWallpaperController.kt
      WallpaperImageRenderer.kt
      VideoWallpaperService.kt
    ui/WalloraApp.kt
  res/xml/wallpaper.xml
docs/DESIGN_PLAN.md
.github/workflows/android-ci.yml
SIGNING.md
```

## Roadmap

1. Media-library polish: thumbnail caching, sorting, metadata detail, stale-permission recovery, and large-library performance.
2. Studio expansion: draggable focal positioning, freeform crop, colour controls, undo/redo, and edit history.
3. Live-wallpaper hardening: decoder error recovery, lifecycle and battery profiling, and OEM/device compatibility tests.
4. Animated media: GIF and image-sequence live-wallpaper rendering.
5. Release hardening: automated tests, accessibility, performance profiling, privacy review, and store-policy compliance.

## License

No license has been applied yet. Choose one before public distribution.
