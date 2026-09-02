package me.ranko.autodark.ui.compose

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.ranko.autodark.R
import me.ranko.autodark.core.ShizukuStatus
import me.ranko.autodark.data.CityReference
import me.ranko.autodark.ui.DarkSwitch
import me.ranko.autodark.ui.MainViewModel
import me.ranko.autodark.ui.compose.theme.AutoDarkTheme
import java.time.format.DateTimeFormatter

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScreen(
    viewModel: MainViewModel,
    hasSecurePermission: Boolean,
    shizukuStatus: ShizukuStatus,
    onPermissionClicked: () -> Unit,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
    onAutoModeClicked: () -> Unit,
    onAdvancedSettingsClicked: () -> Unit,
) {
    val context = LocalContext.current
    val autoMode by viewModel.autoMode.observeAsState(false)
    val switchState by viewModel.switch.observeAsState(DarkSwitch.OFF)
    val summary by viewModel.summaryText.observeAsState()
    val manualCity by viewModel.manualCity.observeAsState()
    val citySearchResults by viewModel.citySearchResults.observeAsState(emptyList())
    val citySearchInProgress by viewModel.citySearchInProgress.observeAsState(false)
    var showCityPicker by rememberSaveable { mutableStateOf(false) }

    if (showCityPicker) {
        CityPickerDialog(
            selectedCity = manualCity,
            results = citySearchResults,
            searching = citySearchInProgress,
            onQueryChanged = viewModel::searchCities,
            onCitySelected = { city ->
                showCityPicker = false
                viewModel.selectManualCity(city)
            },
            onRestoreAutomatic = {
                showCityPicker = false
                viewModel.restoreAutomaticLocation()
            },
            onDismiss = { showCityPicker = false }
        )
    }

    val switchEnabled = switchState == DarkSwitch.ON
    val startTime = viewModel.darkSettings.getStartTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    val endTime = viewModel.darkSettings.getEndTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    val locationValue = manualCity?.let { "${it.name} · ${it.countryCode}" }
        ?: stringResource(R.string.home_location_automatic)

    AutoDarkTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = onAdvancedSettingsClicked) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.advanced_settings_title)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { scaffoldPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "hero") {
                    DashboardHero(
                        enabled = switchEnabled,
                        automatic = autoMode,
                        summary = summary?.message,
                        onToggle = viewModel::onFabClicked
                    )
                }

                item(key = "permission") {
                    AnimatedVisibility(
                        visible = PermissionCardPolicy.shouldShow(
                            hasSecurePermission = hasSecurePermission,
                            shizukuStatus = shizukuStatus
                        ),
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = spring(
                                dampingRatio = 0.84f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(180)),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) + fadeOut(tween(140))
                    ) {
                        PermissionSetupCard(
                            shizukuStatus = shizukuStatus,
                            onClick = onPermissionClicked
                        )
                    }
                }

                item(key = "automation") {
                    AutomationPanel(
                        enabled = switchEnabled,
                        automatic = autoMode,
                        startTime = startTime,
                        endTime = endTime,
                        onAutomaticChanged = onAutoModeClicked,
                        onStartTimeClick = onPickStartTime,
                        onEndTimeClick = onPickEndTime
                    )
                }

                item(key = "feature_mosaic") {
                    FeatureMosaic(
                        locationValue = locationValue,
                        onLocationClick = { showCityPicker = true },
                        onWallpaperClick = {
                            context.startActivity(
                                Intent(context, DualWallpaperComposeActivity::class.java)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CityPickerDialog(
    selectedCity: CityReference?,
    results: List<CityReference>,
    searching: Boolean,
    onQueryChanged: (String) -> Unit,
    onCitySelected: (CityReference) -> Unit,
    onRestoreAutomatic: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(query) { onQueryChanged(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.city_picker_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.city_search_hint)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                if (searching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    if (!searching && results.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.city_search_empty),
                                modifier = Modifier.padding(vertical = 24.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    items(results, key = CityReference::id) { city ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCitySelected(city) },
                            headlineContent = { Text(city.name) },
                            supportingContent = { Text(city.displayTimeZone) },
                            trailingContent = { Text(city.countryCode) },
                            colors = ListItemDefaults.colors(
                                containerColor = if (selectedCity?.id == city.id) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        )
                        HorizontalDivider()
                    }
                }
                if (selectedCity != null) {
                    TextButton(
                        onClick = onRestoreAutomatic,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.restore_automatic_location))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.app_confirm))
            }
        }
    )
}
