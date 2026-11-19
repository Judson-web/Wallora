package com.judson.wallora.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter as ComposeColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import com.judson.wallora.data.MediaItem
import com.judson.wallora.data.MediaKind
import com.judson.wallora.data.MediaLibraryStore
import com.judson.wallora.data.MediaRepository
import com.judson.wallora.wallpaper.StaticWallpaperController
import com.judson.wallora.wallpaper.VideoWallpaperService
import com.judson.wallora.wallpaper.GifWallpaperService
import com.judson.wallora.wallpaper.WallpaperImageRenderer

private enum class WalloraTab { LIBRARY, STUDIO, SETTINGS }

private val FOCAL_ALIGNMENTS = listOf(
    Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd,
    Alignment.CenterStart, Alignment.Center, Alignment.CenterEnd,
    Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd
)
private val FOCAL_COORDINATES = listOf(
    0f to 0f, 0.5f to 0f, 1f to 0f,
    0f to 0.5f, 0.5f to 0.5f, 1f to 0.5f,
    0f to 1f, 0.5f to 1f, 1f to 1f
)
private val FOCAL_LABELS = listOf("↖", "↑", "↗", "←", "•", "→", "↙", "↓", "↘")
private val FOCAL_ROTATION_MAPS = mapOf(
    90 to listOf(2, 5, 8, 1, 4, 7, 0, 3, 6),
    180 to listOf(8, 7, 6, 5, 4, 3, 2, 1, 0),
    270 to listOf(6, 3, 0, 7, 4, 1, 8, 5, 2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalloraApp() {
    val context = LocalContext.current
    var items by remember { mutableStateOf(MediaLibraryStore.load(context)) }
    var selected by remember { mutableStateOf(items.firstOrNull()) }
    var tab by remember { mutableStateOf(WalloraTab.LIBRARY) }
    var status by remember { mutableStateOf("Your wallpapers stay on your device.") }
    val appearancePreferences = remember(context) { context.getSharedPreferences("wallora", Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(appearancePreferences.getString("theme_mode", "system") ?: "system") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            MediaRepository.persistReadPermission(context.contentResolver, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            MediaLibraryStore.add(context, MediaRepository.fromDocument(context.contentResolver, uri))
        }
        items = MediaLibraryStore.load(context)
        selected = items.lastOrNull() ?: selected
        if (uris.isNotEmpty()) status = "Added ${uris.size} item(s) to your library."
    }

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) { "dark" -> true; "light" -> false; else -> systemDark }
    val walloraColorScheme = if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    MaterialTheme(colorScheme = walloraColorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Wallora", style = MaterialTheme.typography.titleLarge)
                            Text("Wallpaper studio", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = { tab = WalloraTab.SETTINGS }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == WalloraTab.LIBRARY, { tab = WalloraTab.LIBRARY },
                        icon = { Icon(Icons.Rounded.Collections, contentDescription = null) }, label = { Text("Library") })
                    NavigationBarItem(tab == WalloraTab.STUDIO, { tab = WalloraTab.STUDIO },
                        icon = { Icon(Icons.Rounded.Tune, contentDescription = null) }, label = { Text("Studio") })
                    NavigationBarItem(tab == WalloraTab.SETTINGS, { tab = WalloraTab.SETTINGS },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            when (tab) {
                WalloraTab.LIBRARY -> LibraryScreen(
                    modifier = Modifier.padding(padding), items = items,
                    onImport = { importLauncher.launch(arrayOf("image/*", "video/*")) },
                    onSelect = { selected = it; tab = WalloraTab.STUDIO },
                    onFavorite = {
                        MediaLibraryStore.toggleFavorite(context, it.uri)
                        items = MediaLibraryStore.load(context)
                        selected = items.firstOrNull { value -> value.uri == it.uri } ?: selected
                    },
                    onRemove = {
                        MediaLibraryStore.remove(context, it.uri)
                        items = MediaLibraryStore.load(context)
                        if (selected?.uri == it.uri) selected = items.firstOrNull()
                        status = "Removed from Wallora. The original file was not deleted."
                    }
                )
                WalloraTab.STUDIO -> StudioScreen(
                    modifier = Modifier.padding(padding), item = selected,
                    onImport = { importLauncher.launch(arrayOf("image/*", "video/*")) },
                    onStatus = { status = it },
                    onExportComplete = { exported ->
                        MediaLibraryStore.add(context, exported)
                        items = MediaLibraryStore.load(context)
                        selected = exported
                    },
                    status = status,
                    context = context
                )
                WalloraTab.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(padding), context = context,
                    status = status,
                    themeMode = themeMode,
                    onThemeModeChanged = { mode ->
                        themeMode = mode
                        appearancePreferences.edit().putString("theme_mode", mode).apply()
                    },
                    onStatus = { status = it },
                    onMediaIndexed = { found ->
                        MediaLibraryStore.addAll(context, found)
                        items = MediaLibraryStore.load(context)
                        status = "Added or refreshed ${found.size} media item(s) from the selected folder."
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    modifier: Modifier = Modifier,
    items: List<MediaItem>,
    onImport: () -> Unit,
    onSelect: (MediaItem) -> Unit,
    onFavorite: (MediaItem) -> Unit,
    onRemove: (MediaItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf("Recently added") }
    var sortExpanded by remember { mutableStateOf(false) }
    val filters = listOf("All", "Images", "Videos", "GIFs", "Favourites")
    val sortOrders = listOf("Recently added", "Name A–Z", "Name Z–A", "Media type", "Favourites first")
    val visibleItems = remember(items, query, filter, sortOrder) {
        val filtered = items.filter { item ->
            val matchesQuery = item.displayName.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "Images" -> item.kind == MediaKind.IMAGE || item.kind == MediaKind.GIF
                "Videos" -> item.kind == MediaKind.VIDEO
                "GIFs" -> item.kind == MediaKind.GIF
                "Favourites" -> item.isFavorite
                else -> true
            }
            matchesQuery && matchesFilter
        }
        when (sortOrder) {
            "Name A–Z" -> filtered.sortedBy { it.displayName.lowercase() }
            "Name Z–A" -> filtered.sortedByDescending { it.displayName.lowercase() }
            "Media type" -> filtered.sortedWith(compareBy<MediaItem> { it.kind.ordinal }.thenBy { it.displayName.lowercase() })
            "Favourites first" -> filtered.sortedWith(compareByDescending<MediaItem> { it.isFavorite }.thenBy { it.displayName.lowercase() })
            else -> filtered.asReversed()
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Your collection", style = MaterialTheme.typography.headlineSmall)
        Text("${items.size} imported item(s)", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, placeholder = { Text("Search your wallpapers") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { label ->
                FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${visibleItems.size} wallpapers", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                TextButton(onClick = { sortExpanded = true }) {
                    Text("Sort: $sortOrder")
                }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    sortOrders.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { sortOrder = option; sortExpanded = false },
                            trailingIcon = {
                                if (sortOrder == option) Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        )
                    }
                }
            }
        }
        if (visibleItems.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Collections, contentDescription = null,
                        modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (items.isEmpty()) "Your library is waiting." else "No matching wallpapers.")
                    Text(if (items.isEmpty()) "Import images or videos from your device to get started."
                        else "Try another search or filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (items.isEmpty()) Button(onClick = onImport) { Text("Import media") }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleItems, key = { it.uri.toString() }) { item ->
                    MediaCard(item, onClick = { onSelect(item) },
                        onFavorite = { onFavorite(item) }, onRemove = { onRemove(item) })
                }
            }
        }
        if (items.isNotEmpty()) {
            Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import more media")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem, onClick: () -> Unit, onFavorite: () -> Unit, onRemove: () -> Unit
) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(20.dp)) {
        Column {
            Box {
                AsyncImage(
                    model = item.uri, contentDescription = item.displayName,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.78f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.kind == MediaKind.VIDEO) Icons.Rounded.Movie else Icons.Rounded.Image,
                            contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(item.kind.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (item.isFavorite) "Remove favourite" else "Add favourite",
                        tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else Color.White)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(item.displayName, modifier = Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onRemove) {
                    Text("×", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioScreen(
    modifier: Modifier = Modifier,
    item: MediaItem?,
    onImport: () -> Unit,
    onStatus: (String) -> Unit,
    onExportComplete: (MediaItem) -> Unit,
    status: String,
    context: Context
) {
    val editorPreferences = remember(context) { context.getSharedPreferences("wallora_editor", Context.MODE_PRIVATE) }
    val editorKey = remember(item?.uri) { "item_${item?.uri?.toString()?.hashCode() ?: 0}_" }
    var fill by remember(item?.uri) { mutableStateOf(editorPreferences.getBoolean(editorKey + "fill", true)) }
    var dim by remember(item?.uri) { mutableFloatStateOf(editorPreferences.getFloat(editorKey + "dim", 0f)) }
    var rotation by remember(item?.uri) { mutableIntStateOf(editorPreferences.getInt(editorKey + "rotation", 0)) }
    var studioPage by remember(item?.uri) { mutableStateOf("Adjust") }
    val videoPreferences = remember(context) {
        context.getSharedPreferences("wallora", Context.MODE_PRIVATE)
    }
    val mediaPreferenceKey = remember(item?.uri) { "media_${item?.uri?.toString()?.hashCode() ?: 0}_" }
    var gifFill by remember(item?.uri) {
        mutableStateOf(videoPreferences.getBoolean(mediaPreferenceKey + "gif_fill", true))
    }
    var videoMuted by remember(item?.uri) {
        mutableStateOf(videoPreferences.getBoolean(mediaPreferenceKey + "video_muted", true))
    }
    var videoLoop by remember(item?.uri) {
        mutableStateOf(videoPreferences.getBoolean(mediaPreferenceKey + "video_loop", true))
    }
    var videoFill by remember(item?.uri) {
        mutableStateOf(videoPreferences.getBoolean(mediaPreferenceKey + "video_fill", true))
    }
    var videoSpeed by remember(item?.uri) {
        mutableFloatStateOf(videoPreferences.getFloat(mediaPreferenceKey + "video_speed", 1f).coerceIn(0.5f, 2f))
    }
    var focusIndex by remember(item?.uri) { mutableIntStateOf(editorPreferences.getInt(editorKey + "focus", 4)) }
    var brightness by remember(item?.uri) { mutableFloatStateOf(editorPreferences.getFloat(editorKey + "brightness", 0f)) }
    var contrast by remember(item?.uri) { mutableFloatStateOf(editorPreferences.getFloat(editorKey + "contrast", 1f)) }
    var saturation by remember(item?.uri) { mutableFloatStateOf(editorPreferences.getFloat(editorKey + "saturation", 1f)) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(editorKey, fill, dim, rotation, focusIndex, brightness, contrast, saturation) {
        editorPreferences.edit()
            .putBoolean(editorKey + "fill", fill)
            .putFloat(editorKey + "dim", dim)
            .putInt(editorKey + "rotation", rotation)
            .putInt(editorKey + "focus", focusIndex)
            .putFloat(editorKey + "brightness", brightness)
            .putFloat(editorKey + "contrast", contrast)
            .putFloat(editorKey + "saturation", saturation)
            .apply()
    }
    val normalizedRotation = ((rotation % 360) + 360) % 360
    val rotatedFocusIndex = FOCAL_ROTATION_MAPS[normalizedRotation]?.get(focusIndex) ?: focusIndex
    val renderFocusPoint = FOCAL_COORDINATES[rotatedFocusIndex]
    val previewColorFilter = remember(brightness, contrast, saturation) {
        val s = saturation.coerceIn(0f, 2f)
        val c = contrast.coerceIn(0.5f, 1.5f)
        val offset = ((1f - c) * 128f) + (brightness.coerceIn(-0.3f, 0.3f) * 255f)
        val red = 0.213f + 0.787f * s
        val green = 0.715f - 0.715f * s
        val blue = 0.072f - 0.072f * s
        val matrix = ComposeColorMatrix(
            floatArrayOf(
                red * c, green * c, blue * c, 0f, offset,
                (0.213f - 0.213f * s) * c, (0.715f + 0.285f * s) * c,
                (0.072f - 0.072f * s) * c, 0f, offset,
                (0.213f - 0.213f * s) * c, (0.715f - 0.715f * s) * c,
                (0.072f + 0.928f * s) * c, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        ComposeColorFilter.colorMatrix(matrix)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destination ->
        val activeItem = item
        if (destination == null) {
            onStatus("Export cancelled.")
        } else if (activeItem == null) {
            onStatus("Choose an image before exporting.")
        } else {
            onStatus("Rendering edited image…")
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val rendered = WallpaperImageRenderer.render(
                            context, activeItem.uri, fill, dim, rotation,
                            renderFocusPoint.first, renderFocusPoint.second, brightness, contrast, saturation
                        )
                        try {
                            val output = requireNotNull(context.contentResolver.openOutputStream(destination)) {
                                "The selected destination could not be opened."
                            }
                            output.use { WallpaperImageRenderer.writeJpeg(rendered, it) }
                        } finally {
                            rendered.recycle()
                        }
                        MediaRepository.persistReadPermission(
                            context.contentResolver,
                            destination,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        MediaRepository.fromDocument(context.contentResolver, destination)
                    }
                }
                result.fold(
                    { saved ->
                        onExportComplete(saved)
                        onStatus("Edited copy saved to your selected location.")
                    },
                    { error -> onStatus("Export failed: ${error.message ?: "unknown error"}") }
                )
            }
        }
    }

    if (item == null) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(52.dp))
                Text("Studio", style = MaterialTheme.typography.headlineMedium)
                Text("Choose something from your Library, or import a new wallpaper.")
                Button(onClick = onImport) { Text("Choose media") }
            }
        }
        return
    }

    val selectedItem = item
    fun applyEdited(target: Int) {
        onStatus("Rendering wallpaper…")
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val rendered = WallpaperImageRenderer.render(
                        context, selectedItem.uri, fill, dim, rotation,
                        renderFocusPoint.first, renderFocusPoint.second, brightness, contrast, saturation
                    )
                    try {
                        StaticWallpaperController.apply(context, rendered, target).getOrThrow()
                    } finally {
                        rendered.recycle()
                    }
                }
            }
            onStatus(
                result.fold(
                    { "Rendered wallpaper applied successfully." },
                    { "Could not apply wallpaper: ${it.message ?: "unknown error"}" }
                )
            )
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Studio", style = MaterialTheme.typography.headlineSmall)
        Text(
            selectedItem.displayName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier.fillMaxWidth().aspectRatio(
                (context.resources.displayMetrics.widthPixels.toFloat() /
                    context.resources.displayMetrics.heightPixels.toFloat()).coerceIn(0.3f, 1.8f)
            ).clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (selectedItem.kind == MediaKind.VIDEO) {
                AsyncImage(
                    model = selectedItem.uri,
                    contentDescription = "Video wallpaper first-frame preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (videoFill) ContentScale.Crop else ContentScale.Fit
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Movie, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Live video wallpaper")
                        }
                    }
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val imageModifier = if (normalizedRotation == 90 || normalizedRotation == 270) {
                        Modifier.align(Alignment.Center)
                            .size(width = maxHeight, height = maxWidth)
                            .graphicsLayer { rotationZ = rotation.toFloat() }
                    } else {
                        Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.toFloat() }
                    }
                    AsyncImage(
                        model = selectedItem.uri,
                        contentDescription = "Wallpaper preview",
                        modifier = imageModifier,
                        alignment = if (fill) FOCAL_ALIGNMENTS[rotatedFocusIndex] else Alignment.Center,
                        colorFilter = previewColorFilter,
                        contentScale = if (fill) ContentScale.Crop else ContentScale.Fit
                    )
                }
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "PREVIEW  •  ${rotation}°",
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        val studioPages = if (selectedItem.kind == MediaKind.IMAGE) {
            listOf("Placement", "Adjust", "Apply")
        } else {
            listOf("Playback", "Apply")
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            studioPages.forEachIndexed { index, page ->
                SegmentedButton(
                    selected = studioPage == page,
                    onClick = { studioPage = page },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = studioPages.size)
                ) { Text(page) }
            }
        }

        if (selectedItem.kind == MediaKind.IMAGE) {
        if (studioPage == "Placement") {
        Text("Composition", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = fill,
                onClick = { fill = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Fill") }
            SegmentedButton(
                selected = !fill,
                onClick = { fill = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Fit") }
        }
        Text("Focal position", style = MaterialTheme.typography.titleMedium)
        Text(
            "Choose which part of the image stays visible when Fill crops it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { columnIndex ->
                        val index = rowIndex * 3 + columnIndex
                        FilterChip(
                            selected = focusIndex == index,
                            onClick = { focusIndex = index },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    FOCAL_LABELS[index],
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { rotation = (rotation + 90) % 360 },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rotate 90°  •  Current: ${rotation}°")
        }
        }
        if (studioPage == "Adjust") {
        Text("Brightness  •  ${(brightness * 100).toInt()}%")
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = -0.3f..0.3f,
            steps = 11
        )
        Text("Contrast  •  ${(contrast * 100).toInt()}%")
        Slider(
            value = contrast,
            onValueChange = { contrast = it },
            valueRange = 0.5f..1.5f,
            steps = 9
        )
        Text("Saturation  •  ${(saturation * 100).toInt()}%")
        Slider(
            value = saturation,
            onValueChange = { saturation = it },
            valueRange = 0f..2f,
            steps = 9
        )
        Text("Dim overlay  •  ${(dim * 100).toInt()}%")
        Slider(value = dim, onValueChange = { dim = it }, valueRange = 0f..0.8f)
        OutlinedButton(
            onClick = {
                fill = true
                dim = 0f
                rotation = 0
                focusIndex = 4
                brightness = 0f
                contrast = 1f
                saturation = 1f
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reset image edits") }

        }
        }

        if (selectedItem.kind == MediaKind.IMAGE && studioPage == "Apply") {
            Text(
                "The editor renders a screen-shaped image. Fill crops to the screen; Fit keeps the whole image with a dark canvas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { exportLauncher.launch("Wallora-edited-wallpaper.jpg") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save edited copy")
            }
            Text("Apply wallpaper", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { applyEdited(WallpaperManager.FLAG_SYSTEM) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Set Home screen") }
            OutlinedButton(
                onClick = { applyEdited(WallpaperManager.FLAG_LOCK) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Set Lock screen") }
            OutlinedButton(
                onClick = {
                    applyEdited(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Set both screens") }
        } else if (selectedItem.kind == MediaKind.VIDEO && studioPage == "Playback") {
            Text("Live video controls", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Mute audio", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Muted by default for silent wallpapers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = videoMuted,
                    onCheckedChange = {
                        videoMuted = it
                        videoPreferences.edit().putBoolean(mediaPreferenceKey + "video_muted", it).apply()
                    }
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Loop video", style = MaterialTheme.typography.bodyLarge)
                    Text("Repeat continuously or stop at the final frame.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = videoLoop, onCheckedChange = {
                    videoLoop = it
                    videoPreferences.edit().putBoolean(mediaPreferenceKey + "video_loop", it).apply()
                })
            }
            Text("Video framing", style = MaterialTheme.typography.bodyMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = videoFill,
                    onClick = {
                        videoFill = true
                        videoPreferences.edit().putBoolean(mediaPreferenceKey + "video_fill", true).apply()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Fill") }
                SegmentedButton(
                    selected = !videoFill,
                    onClick = {
                        videoFill = false
                        videoPreferences.edit().putBoolean(mediaPreferenceKey + "video_fill", false).apply()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Fit") }
            }
            Text("Playback speed  •  ${"%.2f".format(videoSpeed)}×")
            Slider(
                value = videoSpeed,
                onValueChange = { videoSpeed = it },
                onValueChangeFinished = {
                    videoPreferences.edit().putFloat(mediaPreferenceKey + "video_speed", videoSpeed).apply()
                },
                valueRange = 0.5f..2f,
                steps = 5
            )
            Text(
                "Playback settings are saved for this wallpaper and applied when you open the Apply page.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (selectedItem.kind == MediaKind.GIF && studioPage == "Playback") {
            Text("Animated GIF wallpaper", style = MaterialTheme.typography.titleMedium)
            Text(
                "The animation loops silently. Choose how it fills the screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = gifFill,
                    onClick = {
                        gifFill = true
                        videoPreferences.edit().putBoolean(mediaPreferenceKey + "gif_fill", true).apply()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Fill") }
                SegmentedButton(
                    selected = !gifFill,
                    onClick = {
                        gifFill = false
                        videoPreferences.edit().putBoolean(mediaPreferenceKey + "gif_fill", false).apply()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Fit") }
            }
            Text(
                "Large or high-frame-rate GIFs may consume more memory and battery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (studioPage == "Apply" && selectedItem.kind == MediaKind.VIDEO) {
            Text("Ready to apply", style = MaterialTheme.typography.titleMedium)
            Text("Android will show its live-wallpaper preview so you can confirm the video wallpaper.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                videoPreferences.edit()
                    .putString("active_video_uri", selectedItem.uri.toString())
                    .putBoolean("video_muted", videoMuted)
                    .putBoolean("video_loop", videoLoop)
                    .putBoolean("video_fill", videoFill)
                    .putFloat("video_speed", videoSpeed)
                    .apply()
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, VideoWallpaperService::class.java))
                }
                runCatching { context.startActivity(intent) }
                    .onFailure { onStatus("Android live-wallpaper preview could not be opened.") }
            }, modifier = Modifier.fillMaxWidth()) { Text("Preview and apply video wallpaper") }
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
                    .onFailure { onStatus("Android live wallpaper chooser is unavailable.") }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open Android live wallpaper chooser") }
        } else if (studioPage == "Apply" && selectedItem.kind == MediaKind.GIF) {
            Text("Ready to apply", style = MaterialTheme.typography.titleMedium)
            Text("Android will show its live-wallpaper preview so you can confirm the animated wallpaper.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                videoPreferences.edit().putString("active_gif_uri", selectedItem.uri.toString())
                    .putBoolean("gif_fill", gifFill).apply()
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, GifWallpaperService::class.java))
                }
                runCatching { context.startActivity(intent) }
                    .onFailure { onStatus("Android live-wallpaper preview could not be opened.") }
            }, modifier = Modifier.fillMaxWidth()) { Text("Preview and apply GIF wallpaper") }
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
                    .onFailure { onStatus("Android live wallpaper chooser is unavailable.") }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open Android live wallpaper chooser") }
        } else if (studioPage == "Playback" && selectedItem.kind !in listOf(MediaKind.VIDEO, MediaKind.GIF)) {
            Text("No playback controls are available for this media.")
        }

        Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    context: Context,
    status: String,
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    onStatus: (String) -> Unit,
    onMediaIndexed: (List<MediaItem>) -> Unit
) {
    var page by remember { mutableStateOf("Home") }
    BackHandler(enabled = page != "Home") { page = "Home" }
    var folderUri by remember {
        mutableStateOf(
            context.getSharedPreferences("wallora_library", Context.MODE_PRIVATE)
                .getString("library_folder_uri", null)?.let(Uri::parse)
        )
    }
    var scanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableIntStateOf(0) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var hasAllFiles by remember { mutableStateOf(hasFullStorageAccess(context)) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasAllFiles = hasFullStorageAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                context.getSharedPreferences("wallora_library", Context.MODE_PRIVATE)
                    .edit().putString("library_folder_uri", uri.toString()).apply()
                folderUri = uri
                onStatus("Folder access saved. Scan it to add supported wallpapers to your library.")
            }.onFailure { onStatus("Could not retain folder access: ${it.message}") }
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (page != "Home") {
                TextButton(onClick = { page = "Home" }) { Text("‹ Settings") }
            }
            Text(if (page == "Home") "Settings" else page,
                style = MaterialTheme.typography.headlineSmall)
        }
        if (page == "Home") {
            Text("Personalise your wallpaper experience and manage how Wallora works.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                Triple("Storage", "Folders, full-device indexing and media permissions", "Manage your media sources"),
                Triple("Appearance", "Theme, colour and interface preferences", "Personalise Wallora"),
                Triple("About", "App information, privacy and platform details", "Wallora Premium")
            ).forEach { entry ->
                ElevatedCard(onClick = { page = entry.first }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.first, style = MaterialTheme.typography.titleMedium)
                        Text(entry.second, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (page == "Storage") {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Storage and privacy", style = MaterialTheme.typography.titleMedium)
                Text("Wallora stores selected file references locally. It does not upload your wallpaper collection.")
                OutlinedButton(
                    onClick = { folderLauncher.launch(folderUri) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (folderUri == null) "Grant access to a folder" else "Change indexed folder")
                }
                if (folderUri != null) {
                    Text(
                        "A folder is connected. Wallora scans nested folders for supported images, GIFs, and videos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val selectedTree = folderUri
                            if (selectedTree != null) {
                                scanJob = scope.launch {
                                    scanning = true
                                    try {
                                        val found = withContext(Dispatchers.IO) {
                                            MediaRepository.scanTree(context, selectedTree)
                                        }
                                        onMediaIndexed(found)
                                        onStatus("Folder scan complete: ${found.size} supported media item(s) found.")
                                    } catch (cancelled: CancellationException) {
                                        onStatus("Folder scan cancelled.")
                                    } catch (error: Exception) {
                                        onStatus("Folder scan failed: ${error.message ?: "unknown error"}")
                                    } finally {
                                        scanning = false
                                        scanJob = null
                                    }
                                }
                            }
                        },
                        enabled = !scanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (scanning) "Scanning folder…" else "Scan folder for wallpapers")
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val appSettings = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(appSettings) }.onFailure {
                                runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
                                    .onFailure { onStatus("Android storage settings could not be opened.") }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (hasAllFiles) "Manage shared-storage access" else "Grant full shared-storage access")
                }
                Button(
                    onClick = {
                        scanJob = scope.launch {
                            scanning = true
                            scanProgress = 0
                            try {
                                val found = withContext(Dispatchers.IO) {
                                    MediaRepository.scanAllSharedStorage(context) { count -> scanProgress = count }
                                }
                                onMediaIndexed(found)
                                onStatus("Storage scan complete: ${found.size} supported media item(s) indexed.")
                            } catch (cancelled: CancellationException) {
                                onStatus("Storage scan cancelled.")
                            } catch (error: Exception) {
                                onStatus("Storage scan failed: ${error.message ?: "unknown error"}")
                            } finally {
                                scanning = false
                                scanJob = null
                            }
                        }
                    },
                    enabled = hasAllFiles && !scanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (scanning) "Scanning… $scanProgress found" else "Scan all accessible storage")
                }
                if (scanning) {
                    OutlinedButton(onClick = { scanJob?.cancel() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel scan")
                    }
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    "This Android 17 build can index shared storage after you enable All files access. Android itself still blocks protected app-private Android/data and Android/obb folders. Your selected folders remain available as a separate access method.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
        if (page == "Appearance") {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Text("Choose how Wallora follows your device.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = themeMode == option.first,
                            onClick = { onThemeModeChanged(option.first) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) { Text(option.second) }
                    }
                }
            }
        }
        }
        if (page == "About") {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About Wallora", style = MaterialTheme.typography.titleMedium)
                Text("Wallora Premium • app.wallora.premium")
                Text("Version 0.4.0 • Android 17 / API 37 only")
                Text("Live wallpaper support depends on Android, the system wallpaper service, and the device launcher.")
                Divider()
                Text("Privacy", style = MaterialTheme.typography.titleSmall)
                Text("Your media stays on your device. Wallora stores local media references and editor preferences; it does not require an account or upload your wallpaper files.")
                Text("Compatibility", style = MaterialTheme.typography.titleSmall)
                Text("This build is designed exclusively for Android 17. Live wallpaper confirmation and codec support can vary by device firmware.")
            }
        }
        }
        Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
}

private fun hasFullStorageAccess(context: Context): Boolean = Environment.isExternalStorageManager()
