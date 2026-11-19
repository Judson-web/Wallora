# Wallora

**A local-first Android wallpaper studio** for still images, animated GIFs, and video live wallpapers.

- Android application ID: `app.wallora.premium`
- Gradle namespace: `com.judson.wallora`
- Target SDK: Android 17 / API 37
- Minimum SDK: Android 8.0 / API 26
- Language and UI: Kotlin, Jetpack Compose, Material 3

## Current app experience

Wallora currently includes:

- Three-section navigation: Library, Studio, and Settings.
- Multi-select media import through Android's system document picker.
- A local library with image/video labels, image previews, extracted video-frame thumbnails, search, filters, favourites, and remove-from-library.
- Persisted Storage Access Framework (SAF) permissions for individually selected media.
- Recursive SAF folder indexing without an artificial file-count ceiling.
- Full shared-storage indexing across readable internal and removable volumes after Android All files access is granted. The scan is cancellable, de-duplicates mounted paths, and includes nested media folders while respecting Android-protected app-private directories.
- Library sorting by recently added, name in either direction, media type, and favourites first.
- A still-image editor with Fill/Fit composition, a 3×3 focal-position grid, quarter-turn rotation, brightness, contrast, saturation, and a dim overlay.
- Screen-shaped JPEG rendering, export through Android's Create Document flow, and direct application of the rendered result to Home, Lock, or both.
- A video live-wallpaper service with looping, mute/unmute, Fill/Fit framing, and 0.5×–2× playback speed.
- An animated GIF library preview and a looping GIF live-wallpaper service with Fill/Fit framing. GIF decoding is capped at 32 MB and 16 megapixels.
- A sideload-oriented All files access flow with a live permission-state refresh and a full-storage scan action. Core document-picker import and SAF folder scanning still work without this special permission.
- Persistent System, Light, and Dark appearance modes using Material You dynamic colours where supported.
- Edge-to-edge activity layout and an Android 17 GitHub Actions debug-build workflow.

The still-image editor renders composition, focal position, rotation, brightness, contrast, saturation, and dim level into a new JPEG. The 3×3 focal grid is the current positioning control; draggable freeform crop, undo/redo, and non-destructive edit history remain planned. GIF files are animated in library previews and can be launched into the dedicated GIF WallpaperService. GIF playback is silent and uses Android's legacy Movie decoder, with a 32 MB encoded-file cap and a 16-megapixel decoded-size cap. High-frame-rate files and device-specific performance still need testing.

## Design plan

The product direction, screen layouts, interaction flows, visual system, accessibility requirements, storage model, and phased roadmap are documented in [docs/DESIGN_PLAN.md](docs/DESIGN_PLAN.md).

## Storage and privacy

Wallora uses Android's document picker by default. Imported media is represented by persisted content URIs and basic metadata; library import does not copy or modify the original file. Users can retain a folder grant through SAF and scan it for supported images and videos. SAF folder traversal is depth-bounded to avoid provider cycles. The full-storage scanner has no arbitrary media-count limit, can be cancelled, and skips Android/data and Android/obb because those paths are protected by Android itself. Edited images are saved as new JPEG files through the system document picker.

**All files access** is presented for this sideloaded build, not as a Play Store distribution workflow. It must still be explicitly enabled in Android Settings. It provides broad access to shared storage, but Android itself continues to protect app-private `Android/data` and `Android/obb` directories. Users can also grant individual files or folders through SAF.

## Live wallpaper limitations

Android owns the final wallpaper preview and confirmation. Wallora cannot universally force a live wallpaper onto a device whose OEM firmware, launcher, policy, or system wallpaper service blocks it. The app registers its own WallpaperService and uses the supported system preview/chooser flows.

The video renderer reads the saved loop, mute, framing, and playback-speed preferences when the system preview starts. Codec support, lifecycle behaviour, battery impact, and OEM-specific preview/apply flows still require broader physical-device testing.

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

1. Media-library polish: thumbnail caching, metadata detail, stale-permission recovery, and large-library performance.
2. Studio expansion: draggable focal positioning, freeform crop, colour controls, undo/redo, and edit history.
3. Live-wallpaper hardening: decoder error recovery, lifecycle and battery profiling, and OEM/device compatibility tests.
4. Animated media: improve GIF frame pacing, memory bounds, and animated image-sequence support.
5. Release hardening: automated tests, accessibility, performance profiling, privacy review, and store-policy compliance.

## License

No license has been applied yet. Choose one before public distribution.
