package com.judson.wallora.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.judson.wallora.data.MediaItem
import com.judson.wallora.data.MediaKind
import com.judson.wallora.data.MediaLibraryStore
import com.judson.wallora.data.MediaRepository
import com.judson.wallora.wallpaper.StaticWallpaperController
import com.judson.wallora.wallpaper.VideoWallpaperService
import com.judson.wallora.wallpaper.WallpaperImageRenderer

private enum class WalloraTab { LIBRARY, STUDIO, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalloraApp() {
    val context = LocalContext.current
    var items by remember { mutableStateOf(MediaLibraryStore.load(context)) }
    var selected by remember { mutableStateOf(items.firstOrNull()) }
    var tab by remember { mutableStateOf(WalloraTab.LIBRARY) }
    var status by remember { mutableStateOf("Your wallpapers stay on your device.") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            MediaRepository.persistReadPermission(context.contentResolver, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            MediaLibraryStore.add(context, MediaRepository.fromDocument(context.contentResolver, uri))
        }
        items = MediaLibraryStore.load(context)
        selected = items.lastOrNull() ?: selected
        if (uris.isNotEmpty()) status = "Added ${uris.size} item(s) to your library."
    }

    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
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
                    status = status, onStatus = { status = it }
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
    val filters = listOf("All", "Images", "Videos", "Favourites")
    val visibleItems = remember(items, query, filter) {
        items.filter { item ->
            val matchesQuery = item.displayName.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "Images" -> item.kind == MediaKind.IMAGE
                "Videos" -> item.kind == MediaKind.VIDEO
                "Favourites" -> item.isFavorite
                else -> true
            }
            matchesQuery && matchesFilter
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { label ->
                FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
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
    var fill by remember(item?.uri) { mutableStateOf(true) }
    var dim by remember(item?.uri) { mutableFloatStateOf(0f) }
    var rotation by remember(item?.uri) { mutableIntStateOf(0) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destination ->
        val activeItem = item
        if (destination == null) {
            onStatus("Export cancelled.")
        } else if (activeItem == null) {
            onStatus("Choose an image before exporting.")
        } else {
            val result = runCatching {
                val rendered = WallpaperImageRenderer.render(
                    context, activeItem.uri, fill, dim, rotation
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
            result.fold(
                { saved ->
                    onExportComplete(saved)
                    onStatus("Edited copy saved to your selected location.")
                },
                { error -> onStatus("Export failed: ${error.message ?: "unknown error"}") }
            )
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
        val result = runCatching {
            val rendered = WallpaperImageRenderer.render(
                context, selectedItem.uri, fill, dim, rotation
            )
            try {
                StaticWallpaperController.apply(context, rendered, target).getOrThrow()
            } finally {
                rendered.recycle()
            }
        }
        onStatus(
            result.fold(
                { "Rendered wallpaper applied successfully." },
                { "Could not apply wallpaper: ${it.message ?: "unknown error"}" }
            )
        )
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
            Modifier.fillMaxWidth().aspectRatio(0.62f).clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = selectedItem.uri,
                contentDescription = "Wallpaper preview",
                modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.toFloat() },
                contentScale = if (fill) ContentScale.Crop else ContentScale.Fit
            )
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
        OutlinedButton(
            onClick = { rotation = (rotation + 90) % 360 },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rotate 90°  •  Current: ${rotation}°")
        }

        Text("Dim overlay  •  ${(dim * 100).toInt()}%")
        Slider(value = dim, onValueChange = { dim = it }, valueRange = 0f..0.8f)

        if (selectedItem.kind == MediaKind.IMAGE) {
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
        } else if (selectedItem.kind == MediaKind.VIDEO) {
            Button(onClick = {
                context.getSharedPreferences("wallora", Context.MODE_PRIVATE).edit()
                    .putString("active_video_uri", selectedItem.uri.toString()).apply()
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, VideoWallpaperService::class.java)
                    )
                }
                runCatching { context.startActivity(intent) }
                    .onFailure {
                        onStatus("Live wallpaper preview is unavailable on this device. Try the system chooser.")
                    }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Movie, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Preview live wallpaper in Android")
            }
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                }.onFailure { onStatus("This device does not expose the live wallpaper chooser.") }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open system wallpaper chooser") }
            Text(
                "Image composition controls do not alter video files. Video playback settings are managed separately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "GIF rendering is not available yet. Choose a still image or video for the current editor.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    context: Context,
    status: String,
    onStatus: (String) -> Unit
) {
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.getSharedPreferences("wallora_library", Context.MODE_PRIVATE)
                    .edit().putString("library_folder_uri", uri.toString()).apply()
                onStatus("Folder access saved. Folder indexing is a follow-up milestone.")
            }.onFailure { onStatus("Could not retain folder access: ${it.message}") }
        }
    }
    val hasAllFiles = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text("Control how Wallora looks and accesses your media.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Storage and privacy", style = MaterialTheme.typography.titleMedium)
                Text("Wallora stores selected file references locally. It does not upload your wallpaper collection.")
                OutlinedButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant access to a folder")
                }
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(intent) }
                            .onFailure { onStatus("Could not open All files access settings.") }
                    } else onStatus("All files access is not available on this Android version.")
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (hasAllFiles) "All files access: enabled" else "Manage All files access")
                }
                Text("All files access is optional and subject to Google Play policy. Android still protects some system and app-private folders.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About Wallora", style = MaterialTheme.typography.titleMedium)
                Text("Wallora Premium • app.wallora.premium")
                Text("Version 0.1.0 • Android 8.0+ baseline • Android 17 target")
                Text("Live wallpaper support depends on Android, the system wallpaper service, and the device launcher.")
            }
        }
        Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
}
