package me.ranko.autodark.ui.compose

import android.app.WallpaperManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import me.ranko.autodark.data.WallpaperRepository
import androidx.compose.material3.ExperimentalMaterial3Api
import me.ranko.autodark.ui.compose.theme.AutoDarkTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualWallpaperScreen(
    onDone: () -> Unit = {}
) {
    val context = LocalContext.current
    val repo = remember { WallpaperRepository(context) }
    val scope = rememberCoroutineScope()
    var lightPath by remember { mutableStateOf(repo.getWallpaperPath(false, WallpaperManager.FLAG_SYSTEM)) }
    var darkPath by remember { mutableStateOf(repo.getWallpaperPath(true, WallpaperManager.FLAG_SYSTEM)) }
    var saving by remember { mutableStateOf(false) }

    val pickLight = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            saving = true
            scope.launch {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, 1)
                } catch (_: Exception) {}
                repo.saveWallpaper(uri, false, WallpaperManager.FLAG_SYSTEM)
                repo.saveWallpaper(uri, false, WallpaperManager.FLAG_LOCK)
                lightPath = repo.getWallpaperPath(false, WallpaperManager.FLAG_SYSTEM)
            }.invokeOnCompletion { saving = false }
        }
    }
    val pickDark = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            saving = true
            scope.launch {
                try { context.contentResolver.takePersistableUriPermission(uri, 1) } catch (_: Exception) {}
                repo.saveWallpaper(uri, true, WallpaperManager.FLAG_SYSTEM)
                repo.saveWallpaper(uri, true, WallpaperManager.FLAG_LOCK)
                darkPath = repo.getWallpaperPath(true, WallpaperManager.FLAG_SYSTEM)
            }.invokeOnCompletion { saving = false }
        }
    }

    AutoDarkTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("日夜壁纸") },
                    navigationIcon = { TextButton(onClick = onDone) { Text("完成") } }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "像 iOS 一样，日间/夜间各一张，调度时自动切换并跟随 Monet 变色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                WallpaperCard(
                    title = "日间壁纸",
                    subtitle = "浅色模式时显示",
                    path = lightPath,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { pickLight.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
                WallpaperCard(
                    title = "夜间壁纸",
                    subtitle = "深色模式时显示",
                    path = darkPath,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = { pickDark.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )

                if (saving) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("保存中...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                FilledButtonRow(
                    lightPath = lightPath,
                    darkPath = darkPath,
                    onClear = {
                        scope.launch {
                            repo.clearAll()
                            lightPath = null
                            darkPath = null
                        }
                    },
                    onPreview = {
                        scope.launch {
                            // 立即预览：根据当前深色模式应用对应壁纸
                            val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                            repo.apply(isDark)
                        }
                    }
                )

                if (lightPath == null && darkPath == null) {
                    Text("提示：未设置时将保留系统壁纸，调度仅切换深色模式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun WallpaperCard(
    title: String,
    subtitle: String,
    path: String?,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (path != null && File(path).exists()) {
                AsyncImage(
                    model = File(path),
                    contentDescription = title,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("无") }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onClick) { Text("更换") }
        }
    }
}

@Composable
private fun FilledButtonRow(lightPath: String?, darkPath: String?, onClear: () -> Unit, onPreview: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f), enabled = lightPath != null || darkPath != null) { Text("清除") }
        Button(onClick = onPreview, modifier = Modifier.weight(1f), enabled = lightPath != null || darkPath != null) { Text("立即预览") }
    }
}
