package com.judson.wallora.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.judson.wallora.data.MediaItem
import com.judson.wallora.data.MediaKind
import com.judson.wallora.data.MediaRepository
import com.judson.wallora.wallpaper.StaticWallpaperController
import com.judson.wallora.wallpaper.VideoWallpaperService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalloraApp() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var status by remember { mutableStateOf("Your wallpaper library, under your control.") }
    var showSettings by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            MediaRepository.persistReadPermission(context.contentResolver, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selected = MediaRepository.fromDocument(context.contentResolver, uri)
            status = "Imported: ${selected?.displayName}"
        }
    }

    MaterialTheme(colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Wallora") },
                    actions = {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Wallpaper studio", style = MaterialTheme.typography.headlineMedium)
                Text("Images, videos and live scenes from your own storage.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Collections, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Your local library", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(selected?.displayName ?: "Choose a photo, GIF or video to begin.")
                        Button(onClick = { picker.launch(arrayOf("image/*", "video/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse device storage")
                        }
                    }
                }

                selected?.let { item ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Selected media", style = MaterialTheme.typography.titleMedium)
                            Text("${item.kind} • ${item.mimeType ?: "Unknown format"}")
                            if (item.kind == MediaKind.IMAGE) {
                                Text("Static wallpaper")
                                Button(onClick = {
                                    val result = StaticWallpaperController.apply(context, item.uri, WallpaperManager.FLAG_SYSTEM)
                                    status = result.fold({ "Home wallpaper applied." }, { "Could not apply: ${it.message}" })
                                }, modifier = Modifier.fillMaxWidth()) { Text("Set as Home wallpaper") }
                                OutlinedButton(onClick = {
                                    val result = StaticWallpaperController.apply(context, item.uri, WallpaperManager.FLAG_LOCK)
                                    status = result.fold({ "Lock-screen wallpaper applied." }, { "Could not apply: ${it.message}" })
                                }, modifier = Modifier.fillMaxWidth()) { Text("Set as Lock screen") }
                                OutlinedButton(onClick = {
                                    val result = StaticWallpaperController.apply(context, item.uri, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                                    status = result.fold({ "Home and Lock wallpaper applied." }, { "Could not apply: ${it.message}" })
                                }, modifier = Modifier.fillMaxWidth()) { Text("Set both") }
                            } else if (item.kind == MediaKind.VIDEO) {
                                Button(onClick = {
                                    context.getSharedPreferences("wallora", 0).edit()
                                        .putString("active_video_uri", item.uri.toString()).apply()
                                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                            ComponentName(context, VideoWallpaperService::class.java))
                                    }
                                    runCatching { context.startActivity(intent) }.onFailure {
                                        status = "Live preview unavailable. Try the system wallpaper chooser."
                                    }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Rounded.Movie, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Preview and set live wallpaper")
                                }
                                OutlinedButton(onClick = {
                                    runCatching { context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
                                        .onFailure { status = "This device does not expose a live-wallpaper chooser." }
                                }, modifier = Modifier.fillMaxWidth()) { Text("Open system live wallpaper chooser") }
                            } else {
                                Text("This format is recognized, but its renderer is not implemented yet.")
                            }
                        }
                    }
                }

                if (showSettings) {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Storage access", style = MaterialTheme.typography.titleLarge)
                            Text("Wallora works with files you select. Advanced mode can request Android's special All files access, where allowed.")
                            OutlinedButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    runCatching { context.startActivity(intent) }
                                }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager())
                                    "All files access: enabled" else "Manage All files access")
                            }
                            Text("This special permission is optional, restricted by Google Play policy, and does not unlock Android/data or every protected folder.")
                        }
                    }
                }
                Text(status, color = MaterialTheme.colorScheme.primary)
                Text("More tools are planned: crop studio, parallax, color tuning, playlists, schedules, collections and battery profiles.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
