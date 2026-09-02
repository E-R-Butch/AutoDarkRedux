package me.ranko.autodark.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.ranko.autodark.R
import me.ranko.autodark.core.ShizukuStatus

@Composable
internal fun DashboardHero(
    enabled: Boolean,
    automatic: Boolean,
    summary: String?,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_hero"),
        shape = RoundedCornerShape(
            topStart = 32.dp,
            topEnd = 32.dp,
            bottomEnd = 32.dp,
            bottomStart = 12.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (enabled) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.pref_master_switch),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() }
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(
                    if (enabled) R.string.home_state_enabled else R.string.home_state_disabled
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = summary ?: stringResource(
                    if (automatic) R.string.schedule_summary_automatic
                    else R.string.schedule_summary_custom
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun PermissionSetupCard(
    shizukuStatus: ShizukuStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 28.dp,
            bottomEnd = 12.dp,
            bottomStart = 28.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.shizuku_permission_entry_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(
                            when (shizukuStatus) {
                                ShizukuStatus.AVAILABLE -> R.string.secure_permission_status_missing
                                ShizukuStatus.DEAD -> R.string.shizuku_status_dead_short
                                ShizukuStatus.UNAUTHORIZED -> R.string.shizuku_status_unauthorized_short
                                ShizukuStatus.NOT_INSTALL -> R.string.shizuku_status_not_installed_short
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.home_permission_action))
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun AutomationPanel(
    enabled: Boolean,
    automatic: Boolean,
    startTime: String,
    endTime: String,
    onAutomaticChanged: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("automation_panel"),
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 12.dp,
            bottomEnd = 28.dp,
            bottomStart = 28.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.auto_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            if (automatic) R.string.schedule_summary_automatic
                            else R.string.schedule_summary_custom
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = automatic,
                    onCheckedChange = { onAutomaticChanged() }
                )
            }

            AnimatedVisibility(
                visible = enabled && !automatic,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(180, delayMillis = 45)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { -it / 10 }
                    ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(140))
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactTimeCard(
                            modifier = Modifier
                                .weight(1f)
                                .animateEnterExit(
                                    enter = fadeIn(tween(180, delayMillis = 55)) +
                                        slideInVertically(tween(220)) { -it / 10 },
                                    exit = fadeOut(tween(100))
                                ),
                            label = stringResource(R.string.schedule_start_time),
                            time = startTime,
                            onClick = onStartTimeClick
                        )
                        CompactTimeCard(
                            modifier = Modifier
                                .weight(1f)
                                .animateEnterExit(
                                    enter = fadeIn(tween(190, delayMillis = 110)) +
                                        slideInVertically(tween(250)) { -it / 8 },
                                    exit = fadeOut(tween(90))
                                ),
                            label = stringResource(R.string.schedule_end_time),
                            time = endTime,
                            onClick = onEndTimeClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTimeCard(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun FeatureMosaic(
    locationValue: String,
    onLocationClick: () -> Unit,
    onWallpaperClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureTile(
            modifier = Modifier
                .weight(1.08f)
                .fillMaxSize()
                .testTag("location_tile"),
            title = stringResource(R.string.home_location_title),
            value = locationValue,
            icon = Icons.Rounded.LocationOn,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 12.dp,
                bottomEnd = 28.dp,
                bottomStart = 28.dp
            ),
            onClick = onLocationClick
        )
        FeatureTile(
            modifier = Modifier
                .weight(0.92f)
                .fillMaxSize()
                .testTag("wallpaper_tile"),
            title = stringResource(R.string.home_wallpaper_title),
            value = stringResource(R.string.home_wallpaper_summary),
            icon = Icons.Rounded.Wallpaper,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 28.dp,
                bottomEnd = 12.dp,
                bottomStart = 28.dp
            ),
            onClick = onWallpaperClick
        )
    }
}

@Composable
private fun FeatureTile(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.10f),
                    contentColor = contentColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.76f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
