# Wallora

**A local-first Android wallpaper studio** for images, animated media, and video/live wallpapers.

- Android application ID: \`app.wallora.premium\`
- Gradle namespace: \`com.judson.wallora\`
- Target SDK: Android 17 / API 37
- Minimum SDK: Android 8.0 / API 26
- Language and UI: Kotlin, Jetpack Compose, Material 3

## Current app experience

The current implementation includes:

- Three-section navigation: Library, Studio, and Settings.
- Multi-select media import using Android's document picker.
- A local library that persists imported URI references and metadata.
- Image/video type labels, thumbnail grid, search, filters, favourites, and remove-from-library action.
- A local library with image/video labels, thumbnails, search, filters, favourites, and remove-from-library.
- Multi-select import with persisted Storage Access Framework read permissions.
- Recursive SAF folder indexing with bounded depth and scan limits. The default scan imports up to 2,500 supported items per pass.
- A still-image editor with Fill/Fit composition, 90-degree rotation, and dim overlay.
- Actual screen-shaped JPEG rendering, export through Android's Create Document flow, and direct application of the rendered edit to Home, Lock, or both.
- A video live-wallpaper service with looping, mute/unmute, Fill/Fit scaling, and 0.5×–2× playback speed controls.
- An optional All files access settings handoff. Core import and folder scanning do not require it.
- Edge-to-edge activity window layout.
- Android 17 debug-build workflow in GitHub Actions.

The still-image editor currently exports a rendered JPEG and applies that rendered bitmap to the selected wallpaper target. The first editing pass supports Fill/Fit, quarter-turn rotation, and a dim overlay. Precision focal-point positioning, arbitrary crop handles, colour grading, undo/redo, and non-destructive edit history remain planned. GIF animation is not implemented yet; GIF entries are recognised in the library but not yet animated as live wallpapers.

## Design plan

The product direction, screen layouts, interaction flows, visual system, accessibility requirements, storage model, and phased implementation roadmap are documented in [docs/DESIGN_PLAN.md](docs/DESIGN_PLAN.md).

## Storage and privacy

Wallora uses Android's document picker by default. Imported media is represented by persisted content URIs and basic metadata; originals are not copied or modified by library import. Users can retain a folder grant through SAF and scan it for supported images and videos. Scanning is bounded by a depth limit and a maximum number of visited entries to keep unusually large trees from becoming an unbounded operation. Exported edits are saved as new JPEG files through the system document picker.

Optional **All files access** is a special Android permission. It is not required for core wallpaper use, is subject to Google Play policy, and does not grant unrestricted access to protected Android directories.

## Live wallpaper limitations

Android owns the final wallpaper preview and confirmation. Wallora cannot universally force a live wallpaper onto a device whose OEM firmware, launcher, policy, or system wallpaper service blocks it. The app registers its own WallpaperService and launches the supported system preview/chooser flows.

The video renderer loops the selected source and reads the saved mute, Fill/Fit, and playback-speed preferences when the system preview starts. Device/codec compatibility, lifecycle behaviour, battery impact, and OEM-specific preview/apply flows still require broader testing on physical devices.

## Release signing

The release application ID is \`app.wallora.premium\`, with signing alias \`wallora_release\`.

Private signing files are intentionally excluded from Git. See [SIGNING.md](SIGNING.md). Never commit the keystore, signing ZIP, or passwords. Keep secure backups of the same signing identity for every future update.

## Build locally

Requirements:
- Android Studio with Android SDK Platform 37
- JDK 17
- Gradle 9.5.0 (used by the Android 17 CI workflow)

Open the repository in Android Studio and sync, or install Gradle 9.5.0 and run:

\`\`\`bash
gradle :app:assembleDebug
\`\`\`

The repository CI workflow uses Gradle 9.5.0 and builds the debug APK on pushes and pull requests. It uploads the APK as a workflow artifact when the build succeeds.

## Repository structure

\`\`\`text
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
\`\`\`

## Roadmap

1. Media library polish: thumbnail caching, sorting, metadata detail, stale-permission recovery, and large-library performance.
2. Studio expansion: draggable focal positioning, freeform crop, brightness/contrast/saturation, undo/redo, and edit history.
3. Live wallpaper hardening: decoder error recovery, lifecycle and battery profiling, and OEM/device compatibility tests.
4. Animated media: GIF and image-sequence live-wallpaper rendering.
5. Release hardening: automated tests, accessibility, performance profiling, privacy review, and store-policy compliance.

## License

No license has been applied yet. Choose one before public distribution.
