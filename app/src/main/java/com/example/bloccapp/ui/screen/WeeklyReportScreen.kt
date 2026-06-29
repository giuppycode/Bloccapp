package com.example.bloccapp.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.ui.viewmodel.ReportsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportScreen(
    weekLabel: String,
    onBack: () -> Unit,
    vm: ReportsViewModel = viewModel()
) {
    val weeks by vm.weeks.collectAsStateWithLifecycle()
    val week  = weeks.firstOrNull { it.label == weekLabel }

    var activeFilter by remember { mutableStateOf(UsageFilter.SCREEN_TIME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        weekLabel,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Filter chips ──────────────────────────────────────────────────
            WeeklyFilterRow(
                activeFilter     = activeFilter,
                onFilterSelected = { activeFilter = it }
            )

            // ── Totale + Media giornaliera ────────────────────────────────────
            if (week != null) {
                val totalMs   = week.topApps.sumOf { it.totalTimeInForeground }
                val dailyAvgMs = totalMs / 7L
                val totalHours   = TimeUnit.MILLISECONDS.toHours(totalMs)
                val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60
                val avgHours     = TimeUnit.MILLISECONDS.toHours(dailyAvgMs)
                val avgMinutes   = TimeUnit.MILLISECONDS.toMinutes(dailyAvgMs) % 60

                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Totale
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Utilizzo totale",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${totalHours}h ${totalMinutes}m",
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Separatore verticale
                        HorizontalDivider(
                            modifier  = Modifier
                                .height(48.dp)
                                .width(1.dp),
                            color     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
                            thickness = 1.dp
                        )

                        // Media giornaliera
                        Column(
                            modifier       = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "Media giornaliera",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${avgHours}h ${avgMinutes}m",
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ── Top 5 apps ────────────────────────────────────────────────────
            if (week != null && week.topApps.isNotEmpty()) {
                Text(
                    "Top 5 apps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val sortedApps = remember(week.topApps, activeFilter) {
                    when (activeFilter) {
                        UsageFilter.SCREEN_TIME   -> week.topApps.sortedByDescending { it.totalTimeInForeground }
                        UsageFilter.TIMES_OPENED  -> week.topApps.sortedByDescending { it.launchCount }
                        UsageFilter.NOTIFICATIONS -> week.topApps.sortedByDescending { it.notificationCount }
                    }
                }

                val maxValue = remember(sortedApps, activeFilter) {
                    when (activeFilter) {
                        UsageFilter.SCREEN_TIME   -> sortedApps.maxOf { it.totalTimeInForeground }.coerceAtLeast(1L).toFloat()
                        UsageFilter.TIMES_OPENED  -> sortedApps.maxOf { it.launchCount }.coerceAtLeast(1).toFloat()
                        UsageFilter.NOTIFICATIONS -> sortedApps.maxOf { it.notificationCount }.coerceAtLeast(1).toFloat()
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    sortedApps.forEach { app ->
                        WeeklyAppRow(
                            app         = app,
                            filter      = activeFilter,
                            maxValue    = maxValue
                        )
                    }
                }
            }

            // ── Blocking stats ────────────────────────────────────────────────
            Text(
                "Blocking stats",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BlockingStatCard(
                    title    = "App opens\nblocked",
                    value    = week?.appsBlocked?.toString() ?: "\u2014",
                    modifier = Modifier.weight(1f)
                )
                BlockingStatCard(
                    title    = "Notifications\nblocked",
                    value    = "\u2014",
                    modifier = Modifier.weight(1f)
                )
                BlockingStatCard(
                    title    = "Times\npaused",
                    value    = week?.timesPaused?.toString() ?: "\u2014",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter chips row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyFilterRow(
    activeFilter: UsageFilter,
    onFilterSelected: (UsageFilter) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UsageFilter.entries.forEach { filter ->
            FilterChip(
                selected = activeFilter == filter,
                onClick  = { onFilterSelected(filter) },
                label    = {
                    Text(
                        text     = filter.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style    = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Single app row (icon + bar + value)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyAppRow(
    app: AppUsageInfo,
    filter: UsageFilter,
    maxValue: Float
) {
    val context = LocalContext.current

    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(app.packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }
    }

    val (fraction, valueLabel) = when (filter) {
        UsageFilter.SCREEN_TIME   -> {
            val f = (app.totalTimeInForeground.toFloat() / maxValue).coerceIn(0f, 1f)
            f to formatWeeklyDuration(app.totalTimeInForeground)
        }
        UsageFilter.TIMES_OPENED  -> {
            val f = (app.launchCount.toFloat() / maxValue).coerceIn(0f, 1f)
            f to "${app.launchCount}x"
        }
        UsageFilter.NOTIFICATIONS -> {
            val f = (app.notificationCount.toFloat() / maxValue).coerceIn(0f, 1f)
            f to "${app.notificationCount}"
        }
    }

    Column {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Box(
                modifier         = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        bitmap             = icon!!,
                        contentDescription = app.appName,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.Android,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Name + progress bar
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = app.appName,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress   = { fraction },
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color      = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            Spacer(Modifier.width(12.dp))

            // Value label
            Text(
                text  = valueLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Blocking stat card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BlockingStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.height(90.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utility
// ─────────────────────────────────────────────────────────────────────────────

/** Formatta una durata in ms come h:mm o mm:ss per valori settimanali */
private fun formatWeeklyDuration(ms: Long): String {
    val h = ms / 3_600_000L
    val m = (ms % 3_600_000L) / 60_000L
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
