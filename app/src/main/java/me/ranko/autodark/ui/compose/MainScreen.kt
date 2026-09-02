package me.ranko.autodark.ui.compose

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.ranko.autodark.R
import me.ranko.autodark.ui.BlockListActivity
import me.ranko.autodark.ui.DarkSwitch
import me.ranko.autodark.ui.compose.DualWallpaperComposeActivity
import me.ranko.autodark.ui.MainViewModel
import me.ranko.autodark.ui.compose.theme.AutoDarkTheme
import java.time.format.DateTimeFormatter

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScreen(
    viewModel: MainViewModel,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
) {
    val context = LocalContext.current
    val autoMode by viewModel.autoMode.observeAsState(false)
    // Bridge ObservableField to Compose State
    var switchState by remember { mutableStateOf(viewModel.switch.get() as? DarkSwitch ?: DarkSwitch.OFF) }
    DisposableEffect(viewModel) {
        val callback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                switchState = viewModel.switch.get() as? DarkSwitch ?: DarkSwitch.OFF
            }
        }
        viewModel.switch.addOnPropertyChangedCallback(callback)
        onDispose { viewModel.switch.removeOnPropertyChangedCallback(callback) }
    }

    val summary = remember { mutableStateOf(viewModel.summaryText.get()?.message) }
    DisposableEffect(viewModel) {
        val cb = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                summary.value = viewModel.summaryText.get()?.message
            }
        }
        viewModel.summaryText.addOnPropertyChangedCallback(cb)
        onDispose { viewModel.summaryText.removeOnPropertyChangedCallback(cb) }
    }

    AutoDarkTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Master Switch Card
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.pref_master_switch),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (autoMode) "跟随日出日落" else "自定义时间",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Switch(
                                checked = switchState == DarkSwitch.ON,
                                onCheckedChange = { viewModel.onFabClicked() }
                            )
                        }
                    }
                }

                // Time Range Cards (show only when not auto and switch ON)
                if (autoMode == false && switchState == DarkSwitch.ON) {
                    item {
                        TimeCard(
                            title = "开启时间",
                            timeStr = viewModel.darkSettings.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                            onClick = onPickStartTime
                        )
                    }
                    item {
                        TimeCard(
                            title = "关闭时间",
                            timeStr = viewModel.darkSettings.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                            onClick = onPickEndTime
                        )
                    }
                }

                // Auto Mode Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "自动模式",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "根据位置自动计算日出日落",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Switch(
                                checked = autoMode,
                                onCheckedChange = { viewModel.onAutoModeClicked() }
                            )
                        }
                    }
                }

                // Dark Wallpaper Dual Card (iOS style)
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            context.startActivity(Intent(context, DualWallpaperComposeActivity::class.java))
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text("日夜壁纸") },
                            supportingContent = { Text("像 iOS 一样，日间/夜间自动切换壁纸 + 跟色") },
                            trailingContent = { Text("→", style = MaterialTheme.typography.headlineSmall) }
                        )
                    }
                }

                // Block List
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            context.startActivity(Intent(context, BlockListActivity::class.java))
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text("应用排除") },
                            supportingContent = { Text("选择不跟随深色的应用 (需 Xposed)") }
                        )
                    }
                }

                summary.value?.let {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeCard(title: String, timeStr: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(onClick = onClick) {
                Text(timeStr)
            }
        }
    }
}
