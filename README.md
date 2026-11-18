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
- A Studio preview with Fill/Fit and preview dim controls.
- Static wallpaper application to Home, Lock, or both through WallpaperManager.
- Video live-wallpaper preview handoff through a registered WallpaperService.
- Folder access through SAF and an optional All files access settings handoff.
- Edge-to-edge activity window layout.
- Android 17 debug-build workflow in GitHub Actions.

The Studio's Fill/Fit and dim controls currently affect the preview only. They are not yet rendered into an exported image. GIF rendering, folder indexing, and advanced editing are not implemented yet.

## Design plan

The product direction, screen layouts, interaction flows, visual system, accessibility requirements, storage model, and phased implementation roadmap are documented in [docs/DESIGN_PLAN.md](docs/DESIGN_PLAN.md).

## Storage and privacy

Wallora uses Android's document picker by default. Imported media is represented by persisted content URIs and basic metadata; originals are not copied or modified by library import. A selected folder URI can be retained through SAF, but recursive folder indexing is a later milestone.

Optional **All files access** is a special Android permission. It is not required for core wallpaper use, is subject to Google Play policy, and does not grant unrestricted access to protected Android directories.

## Live wallpaper limitations

Android owns the final wallpaper preview and confirmation. Wallora cannot universally force a live wallpaper onto a device whose OEM firmware, launcher, policy, or system wallpaper service blocks it. The app registers its own WallpaperService and launches the supported system preview/chooser flows.

The current video renderer is an early baseline. Device/codec compatibility, lifecycle behaviour, battery impact, and persistent source access still require broader testing.

## Release signing

The release application ID is \`app.wallora.premium\`, with signing alias \`wallora_release\`.

Private signing files are intentionally excluded from Git. See [SIGNING.md](SIGNING.md). Never commit the keystore, signing ZIP, or passwords. Keep secure backups of the same signing identity for every future update.

## Build locally

Requirements:
- Android Studio with Android SDK Platform 37
- JDK 17
- Gradle 9.5.0 (required by the API 37-compatible Android Gradle Plugin 9.3.1)

Open the repository in Android Studio and sync, or install Gradle 9.5.0 and run:

\`\`\`bash
gradle :app:assembleDebug
\`\`\`

The repository CI workflow uses Gradle 8.13 and builds the debug APK on pushes and pull requests. It uploads the APK as a workflow artifact when the build succeeds.

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
      VideoWallpaperService.kt
    ui/WalloraApp.kt
  res/xml/wallpaper.xml
docs/DESIGN_PLAN.md
.github/workflows/android-ci.yml
SIGNING.md
\`\`\`

## Roadmap

1. Complete media library: SAF folder indexing, thumbnail caching, metadata, sorting, robust permission recovery.
2. Studio renderer: real crop/position/rotation/colour processing, undo/redo, and export.
3. Live wallpaper: persistent URI grants, playback settings, error recovery, and OEM/device compatibility tests.
4. Animated media: GIF and image-sequence rendering.
5. Release hardening: automated tests, accessibility, performance, battery profiling, privacy and store-policy review.

## License

No license has been applied yet. Choose one before public distribution.
