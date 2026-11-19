# Wallora — Product & UI/UX Design Plan

**Status:** Product and design blueprint  
**Platform:** Android 17 / API 37 only, minimum and target SDK  
**Implementation direction:** Kotlin, Jetpack Compose, Material 3, local-first storage

---

## 1. Product vision

Wallora is a local-first wallpaper studio for images, animated images, and videos. It should feel like a refined visual workspace: browse a personal media library, inspect a wallpaper, adjust its composition, configure live playback, preview the result, and hand off to Android's wallpaper confirmation UI.

The product should be powerful without being intimidating. The default path is short: choose media, preview it, optionally edit it, choose a target, and apply. Advanced tools belong in contextual Studio panels and advanced settings rather than being forced into every screen.

### Product principles

- **Media first:** artwork is the main content; controls recede during preview and editing.
- **Local by default:** no account, cloud upload, or analytics is required for core use.
- **User-controlled access:** request storage access only in context and explain special access before opening system settings.
- **Honest system integration:** use Android's supported wallpaper APIs and confirmation screens; never claim to bypass OEM or launcher restrictions.
- **Progressive disclosure:** keep basic actions obvious and expert controls one level deeper.
- **Recoverable editing:** preview changes before applying; support undo, redo, reset, and clear failure states.
- **Adaptive layouts:** support compact phones, large phones, tablets, foldables, and landscape.

## 2. Brand and visual direction

Wallora should feel modern, calm, precise, and visual. Avoid a loud gaming aesthetic, excessive gradients, and dense control panels. The wallpaper itself supplies most of the colour and personality.

| Area | Direction |
|---|---|
| Default appearance | Dark-first, with a complete light theme |
| Brand accent | Aurora violet / periwinkle, used sparingly |
| Secondary accent | Cool mint or cyan for playback/live indicators |
| Surfaces | Deep neutral backgrounds with layered tonal surfaces |
| Typography | Android system typography / Material 3 type scale |
| Shapes | Rounded but disciplined; larger media surfaces, medium controls |
| Imagery | Large edge-to-edge previews, clean thumbnails |
| Density | Comfortable touch targets and generous spacing |

Support System Dynamic Colour, Wallora colour, and neutral/monochrome options. Dynamic colour must not compromise contrast or make status indicators ambiguous.

### Layout tokens

- Base spacing rhythm: 4 dp.
- Main phone content inset: 16–20 dp.
- Section spacing: 24–32 dp.
- Compact control spacing: 8–12 dp.
- Minimum interactive target: 48 dp.
- Media surface radius: 20–28 dp.
- Standard sheet top radius: 28 dp.
- Typical transition duration: 160–240 ms.

These are starting tokens, not hard-coded rules. Verify on small screens, landscape, large font scale, and large displays.

## 3. Information architecture

**Implemented navigation direction:** The app targets API 37 exclusively. Compact navigation keeps Library, Studio, and Settings as primary destinations. Studio uses dedicated Placement, Adjust, and Apply pages for still images, and Playback and Apply pages for video/GIF. Settings is a hub that opens separate Storage, Appearance, and About screens. Editor adjustments are saved per media item so moving between pages or reopening a wallpaper preserves the draft.

### Primary navigation

Use three destinations on compact devices:

1. **Library:** local media, imports, recents, favourites, collections, search, filters.
2. **Studio:** editor workspace and tools. With no media selected, show a useful import/open state.
3. **Settings:** appearance, storage access, playback defaults, library behaviour, accessibility, and app information.

On expanded layouts, replace the bottom bar with a navigation rail or pane when it improves reachability. Library browsing can use a list-detail layout.

### Secondary surfaces

- Media detail / immersive preview
- Static wallpaper editor
- Live wallpaper setup
- Apply-target sheet
- Collection manager
- Search and filter results
- File and folder import
- Storage access explainer
- Compatibility / troubleshooting
- Tool panels: crop, transform, effects, colour, metadata, export

### Navigation rules

- Opening an item presents a focused preview/detail surface.
- Edit opens Studio with that item loaded.
- Set wallpaper opens a target-specific preview/apply flow.
- Video or animated media opens Live setup, then Android's system preview/confirmation.
- Back navigation preserves scroll position, filters, and edit drafts where practical.
- Use predictive back where supported and keep controls clear of gesture insets.

## 4. Screen-by-screen design

### 4.1 First launch and access setup

**Goal:** Explain the product and let users begin without granting broad permissions.

Proposed hierarchy:
- Wallora wordmark/title and a large wallpaper preview.
- Headline: “Your wallpapers. Your way.”
- Supporting text: “Keep your media on-device.”
- Primary action: “Choose photos or videos”.
- Secondary action: “Browse files and folders”.
- Quiet optional link: “Advanced storage access”.
- Short note: advanced access can be enabled later.

Use Photo Picker or SAF as the default path. Explain that selected files work without access to the entire device library. Do not show a permission prompt immediately on launch without context.

### 4.2 Library home

**Goal:** Make the user's wallpapers easy to find and revisit.

Suggested layout:
- Top app bar with Wallora identity, search, and profile/settings affordance (no account required).
- Featured/recent wallpaper preview.
- Filter chips: All, Images, Videos, Animated, Favourites.
- Sort: Recent, Name, Size, Date added.
- Responsive media grid with consistent thumbnail ratios.
- Floating or prominent import action.
- Empty state with a clear import CTA and a concise privacy note.

Media cards should show a preview, filename/title, media-type marker, and optional favourite state. Avoid stacking unnecessary metadata on top of artwork.

### 4.3 Media detail and preview

**Goal:** Let the artwork take centre stage.

- Immersive preview canvas, edge-to-edge where appropriate.
- Minimal overlay controls: back, favourite, more actions.
- Bottom information panel: name, type, dimensions/duration, file size when available.
- Primary actions: Edit and Set wallpaper.
- Overflow actions: share, export copy, add to collection, inspect metadata.

Support pinch zoom and pan for inspection. Provide a visible reset/fit action. Do not crop or modify the original source merely by previewing it.

### 4.4 Studio editor

**Goal:** Provide a precise, reversible editing workspace.

The canvas occupies most of the screen. A collapsible bottom tool sheet or side panel contains grouped tools:

- **Frame:** crop presets, aspect ratio, rotation, flip, fit/fill.
- **Position:** pan, zoom, alignment, focal point.
- **Look:** brightness, contrast, saturation, temperature, tint, blur, dim, vignette.
- **Overlay:** gradient tint, subtle colour overlay, safe-area guides.
- **Output:** resolution, quality, format, export destination.

Include undo, redo, reset, and before/after comparison. Show changes non-destructively until the user exports or applies. Safe-area guides are advisory only because clocks, widgets, and launcher layouts vary by OEM.

### 4.5 Live wallpaper setup

**Goal:** Make video wallpaper configuration understandable and battery-aware.

- Large playback preview.
- Controls: mute, loop, fit/fill, playback speed, start offset, frame-rate cap.
- Performance presets: Battery saver, Balanced, Smooth.
- Pause when not visible and resume behaviour.
- Compatibility information: codec/container, duration, resolution, estimated resource use where measurable.
- Primary action: “Preview in Android”.
- Fallback: “Open system live wallpaper chooser”.
- Static fallback action for unsupported formats/devices.

Android must own final confirmation. Wallora cannot guarantee live wallpaper installation on every launcher or OEM build.

### 4.6 Apply target sheet

For static images, show Home screen, Lock screen, and Both only where supported. Explain any device-specific limitation. Show a final preview and only report success after the wallpaper API succeeds.

For live media, hand off to the registered WallpaperService's system preview/confirmation. Do not pretend the static apply API can install a live wallpaper.

### 4.7 Settings

Sections:
- Appearance: System / Dark / Light; Dynamic / Wallora / Neutral palette.
- Storage: selected folders, access status, optional All files access explanation and settings handoff.
- Playback: mute by default, loop, performance preset, pause when hidden.
- Library: sort order, grid density, metadata visibility, duplicate handling.
- Accessibility: motion reduction, haptics, contrast support.
- About: version, open-source notices, privacy, support, known limitations.

## 5. Responsive behaviour

### Compact width
- Three-item bottom navigation.
- Two or three column media grid depending on width and font scale.
- Full-screen editor with collapsible tool sheet.
- Full-screen image/video preview.

### Medium and expanded width
- Navigation rail or pane.
- Library list/grid beside a media detail pane.
- Studio canvas alongside persistent tool controls when space permits.
- Keep action locations stable when resizing or folding/unfolding.

### Landscape
- Wide preview canvas with side control panel.
- Essential controls remain reachable without covering the artwork.
- Avoid squeezing a portrait-only layout into landscape.

## 6. Storage, privacy, and platform behaviour

Distinguish three access models:

1. **Photo Picker / document picker:** user selects individual files. Default import path.
2. **SAF folder access:** user grants access to a chosen folder tree. Persist URI access where supported and handle revoked/moved sources.
3. **All files access:** optional advanced special access. Explain what it enables and what Android still protects. Do not repeatedly nag users who decline.

Do not assume MANAGE_EXTERNAL_STORAGE unlocks every path. It does not provide unrestricted access to Android/data and other protected directories, and Play distribution requires a separate policy review.

Keep indexing and edits on-device. Do not copy originals unless required for a user-requested export or documented cache. Avoid collecting filenames, paths, thumbnails, or media contents in diagnostics by default.

Static wallpaper application uses WallpaperManager and SET_WALLPAPER. Live wallpaper uses a declared WallpaperService and Android's system preview/confirmation flow. Device/OEM restrictions must be explained honestly.

## 7. Design system component inventory

Reusable Compose components to build and test:

- WalloraTopBar
- WalloraBottomNavigation / WalloraNavigationRail
- MediaGrid, MediaGridItem, MediaTypeBadge
- FeaturedWallpaperCard
- MediaPreviewCanvas
- MediaMetadataRow
- FilterChipRow and SortMenu
- EmptyLibraryState, PermissionExplainer, UnsupportedState
- StudioToolGroup, EditorSlider, ValueStepper, SegmentedChoice
- CropPresetSelector, SafeAreaOverlay, ZoomControl
- PlaybackPreviewControls, PerformancePresetCard
- ApplyTargetSheet, ApplyProgressState, ApplyResultBanner
- StorageAccessStatusCard

Preview reusable components in light/dark themes, with long text, large font scale, loading, empty, disabled, and error states.

## 8. Architecture and state model

Keep UI state separate from file access, rendering, and Android wallpaper calls.

Suggested feature boundaries:
- feature/library — grid, search, sorting, filters, multi-select.
- feature/preview — media details and immersive preview.
- feature/studio — editor state, transforms, adjustments, undo/redo.
- feature/livewallpaper — playback options, preview handoff, compatibility.
- feature/settings — theme, storage, playback preferences.
- core/media — models, metadata, URI handling, thumbnail pipeline.
- core/storage — SAF, Photo Picker, optional advanced storage adapter.
- core/wallpaper — static controller and live service contracts.
- core/designsystem — theme, tokens, icons, reusable components.

Represent states explicitly: AccessNotRequested, SelectedUrisAvailable, FolderAccessGranted, AllFilesAccessGranted, AccessRevoked, PreparingPreview, AwaitingSystemConfirmation, Applied, Unsupported, and Failed.

Long-running work such as scanning, metadata extraction, thumbnails, and video preparation must be cancellable and expose progress. Avoid large media operations on the main thread.

## 9. Implementation roadmap

### Phase 0 — Foundation and visual system
- Apply Material 3 theme and design tokens.
- Set up edge-to-edge and insets correctly.
- Establish navigation and adaptive layout rules.
- Build reusable media cards, preview surfaces, empty states, and permission explanations.
- Add accessibility semantics from the beginning.

**Exit criteria:** core screens have consistent hierarchy, no clipped content, and usable compact/expanded layouts.

### Phase 1 — Reliable media library and static wallpapers
- Photo Picker / SAF import.
- Persisted URI permission and missing-source handling.
- Media metadata and thumbnails.
- Library grid, search, filters, favourites, and recents.
- Static image preview and crop/fit/fill positioning.
- Apply to Home / Lock / Both where supported.

**Exit criteria:** users can select, preview, position, and apply an image without All files access.

### Phase 2 — Studio editing
- Crop presets and gesture controls.
- Rotation, flip, blur, dim, colour adjustments, overlays.
- Undo/redo, reset, draft recovery, export edited copy.
- Safe-area guides and before/after comparison.

**Exit criteria:** edits are reversible and never unexpectedly modify originals.

### Phase 3 — Video live wallpapers
- Harden WallpaperService lifecycle and playback.
- Mute, loop, scaling, speed, battery-aware options.
- System preview and confirmation handoff.
- Static fallback and compatibility troubleshooting.

**Exit criteria:** test across Android versions, launchers, OEM devices, screen on/off, and media formats. No universal compatibility claim.

### Phase 4 — Advanced library and animated media
- Folder indexing with SAF.
- Optional All files access after policy review.
- GIF/animated-image renderer.
- Collections, duplicate detection, metadata tools, richer search.
- Video trim and export pipeline.

**Exit criteria:** large libraries remain responsive; revoked permissions and unsupported files are handled cleanly.

### Phase 5 — Release readiness
- Unit, UI, and instrumentation tests for critical flows.
- Accessibility audit and large-font testing.
- Performance and battery profiling.
- Privacy review and crash/error reporting review.
- OEM compatibility matrix and release notes.
- Signing, versioning, Play policy review, and store assets.

**Exit criteria:** reproducible signed release, tested install/update/uninstall paths, transparent permissions, and documented limitations.

## 10. Quality gates

Before a feature is considered complete, verify:
- Happy path works on a clean install.
- Denied/revoked permissions do not crash the app.
- Unsupported files show useful errors and do not hang.
- UI remains responsive during scanning/rendering.
- Back navigation preserves expected state.
- Workflow is operable with TalkBack and large font scaling.
- Light and dark themes have correct contrast.
- Success is reported only after the underlying operation succeeds.
- Original files are not altered or deleted unexpectedly.
- Battery behaviour and lifecycle cleanup are tested for live wallpapers.

## 11. Known limitations and non-goals

- Wallora cannot force live wallpaper onto a device whose system wallpaper service, launcher, policy, or OEM implementation blocks it.
- All files access does not grant unrestricted access to protected Android directories.
- Lock-screen clocks, widgets, and launcher overlays cannot be previewed identically across all OEMs.
- Refresh rates, codecs, decoder limits, and power management affect live playback.
- Cloud discovery, social feeds, accounts, and cloud sync are outside the initial product direction.

## 12. References

- [Android 17 release notes](https://developer.android.com/about/versions/17/release-notes)
- [Material 3 in Jetpack Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Edge-to-edge design](https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge)
- [Predictive back design](https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back)
- [Build adaptive apps with Jetpack Compose](https://developer.android.com/develop/ui/compose/build-adaptive-apps)
- [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Manage all files access](https://developer.android.com/training/data-storage/manage-all-files)
- [WallpaperManager API](https://developer.android.com/reference/android/app/WallpaperManager)

This is a product/design blueprint, not evidence that every listed feature is implemented. Implement and test each feature before describing it as complete.
