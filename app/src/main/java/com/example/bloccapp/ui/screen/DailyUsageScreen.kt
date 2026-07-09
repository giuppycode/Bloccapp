package com.example.bloccapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.DailyUsageData
import com.example.bloccapp.ui.viewmodel.DashboardViewModel
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Filter enum
// ─────────────────────────────────────────────────────────────────────────────

enum class UsageFilter(val label: String) {
    SCREEN_TIME("Tempo schermo"),
    TIMES_OPENED("Aperture"),
    NOTIFICATIONS("Notifiche")
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyUsageScreen(
    vm: DashboardViewModel = viewModel()
) {
    val dailyData   by vm.dailyData.collectAsStateWithLifecycle()
    val hasPerm     by vm.hasUsagePermission.collectAsStateWithLifecycle()
    val userInfo    by vm.userInfo.collectAsStateWithLifecycle()
    val totalPoints by vm.totalPoints.collectAsStateWithLifecycle()

    // Rinfresca i dati ogni volta che l'utente torna nella schermata
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadUsageData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Aggiorna i dati all'apertura della schermata e ogni 30 secondi mentre è visibile
    LaunchedEffect(Unit) {
        while (true) {
            vm.loadUsageData()
            delay(30_000L)
        }
    }

    var activeFilter by remember { mutableStateOf(UsageFilter.SCREEN_TIME) }

    Scaffold(
        // TopAppBar rimossa per pulizia visiva, il saluto fa da intestazione
    ) { innerPadding ->
        if (!hasPerm) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Abilita il permesso di accesso all'utilizzo nelle Impostazioni di sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Calcolo app ordinate e valore massimo in base al filtro
            val sortedApps = remember(dailyData.apps, activeFilter) {
                when (activeFilter) {
                    UsageFilter.SCREEN_TIME   -> dailyData.apps.sortedByDescending { it.totalTimeInForeground }
                    UsageFilter.TIMES_OPENED  -> dailyData.apps.sortedByDescending { it.launchCount }
                    UsageFilter.NOTIFICATIONS -> dailyData.apps.sortedByDescending { it.notificationCount }
                }
            }

            val maxValue = remember(sortedApps, activeFilter) {
                when (activeFilter) {
                    UsageFilter.SCREEN_TIME   -> sortedApps.firstOrNull()?.totalTimeInForeground?.coerceAtLeast(1L)?.toFloat() ?: 1f
                    UsageFilter.TIMES_OPENED  -> sortedApps.firstOrNull()?.launchCount?.coerceAtLeast(1)?.toFloat() ?: 1f
                    UsageFilter.NOTIFICATIONS -> sortedApps.firstOrNull()?.notificationCount?.coerceAtLeast(1)?.toFloat() ?: 1f
                }
            }

            // Calcolo totale giornaliero in base al filtro
            val (totalValueLabel, totalLabel) = remember(dailyData.apps, activeFilter) {
                when (activeFilter) {
                    UsageFilter.SCREEN_TIME -> {
                        val total = dailyData.apps.sumOf { it.totalTimeInForeground }
                        formatDuration(total) to "Tempo totale oggi"
                    }
                    UsageFilter.TIMES_OPENED -> {
                        val total = dailyData.apps.sumOf { it.launchCount.toLong() }
                        "${total}x" to "Aperture totali oggi"
                    }
                    UsageFilter.NOTIFICATIONS -> {
                        val total = dailyData.apps.sumOf { it.notificationCount.toLong() }
                        "$total" to "Notifiche totali oggi"
                    }
                }
            }

            LazyColumn(
                modifier              = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val greeting = remember(userInfo?.displayName) {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val base = when {
                            hour in 5..12 -> "Buongiorno"
                            hour in 13..18 -> "Buon pomeriggio"
                            else -> "Buonasera"
                        }
                        "$base, ${userInfo?.displayName ?: "Utente"}!"
                    }
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ecco come stai usando il tuo tempo oggi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sezione Gamification (Spostata qui)
                item {
                    GamificationHeader(points = totalPoints)
                }

                // Chips filtri
                item {
                    UsageFilterRow(
                        activeFilter     = activeFilter,
                        onFilterSelected = { activeFilter = it }
                    )
                }

            // Sompmatio totale
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = totalLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalValueLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

            // Grafico a barre
                item {
                    HourlyChartCard(
                        data     = dailyData,
                        filter   = activeFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                }

            // Lista app
                if (dailyData.apps.isEmpty()) {
                    item {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(top = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = "Nessun dato disponibile per oggi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text  = "Utilizzo app",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(
                        items = sortedApps,
                        key   = { it.packageName }
                    ) { app ->
                        AppUsageRow(app = app, filter = activeFilter, maxValue = maxValue)
                    }
                }

                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter chips row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UsageFilterRow(
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

// Grafico

@Composable
private fun HourlyChartCard(
    data: DailyUsageData,
    filter: UsageFilter,
    modifier: Modifier = Modifier
) {
    val hourlyValues = remember(data, filter) {
        FloatArray(24) { h ->
            when (filter) {
                UsageFilter.SCREEN_TIME   -> data.hourlyScreenTimeMs[h].toFloat()
                UsageFilter.TIMES_OPENED  -> data.hourlyTimesOpened[h].toFloat()
                UsageFilter.NOTIFICATIONS -> data.hourlyNotifications[h].toFloat()
            }
        }
    }
    val maxValue = (hourlyValues.maxOrNull() ?: 0f).coerceAtLeast(1f)

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor   = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val labelStyle   = TextStyle(fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Medium)

    Card(
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Attività nelle 24 ore",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
            ) {
                val leftPad   = 40.dp.toPx()
                val bottomPad = 24.dp.toPx()
                val chartW    = size.width - leftPad
                val chartH    = size.height - bottomPad

                // Linee di griglia orizzontali (sfumate)
                val ySteps = 3
                for (i in 0..ySteps) {
                    val frac  = i.toFloat() / ySteps
                    val y     = chartH * (1f - frac)
                    drawLine(
                        color       = gridColor,
                        start       = Offset(leftPad, y),
                        end         = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // Label asse Y
                    val valAtStep = maxValue * frac
                    val label = if (filter == UsageFilter.SCREEN_TIME)
                        formatYLabelMs(valAtStep.toLong())
                    else
                        valAtStep.toInt().toString()

                    val measured = textMeasurer.measure(label, labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = leftPad - measured.size.width.toFloat() - 8.dp.toPx(),
                            y = y - measured.size.height / 2f
                        )
                    )
                }

                // Disegno dell'area sfumata (Gradients)
                val slotW = chartW / 23f
                val path = Path().apply {
                    moveTo(leftPad, chartH)
                    for (hour in 0..23) {
                        val x = leftPad + hour * slotW
                        val valFrac = (hourlyValues[hour] / maxValue)
                        val y = chartH - (chartH * valFrac)
                        if (hour == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                // Linea del grafico curva (o spezzata morbida)
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )

                // Riempimento sfumato sotto la linea
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(leftPad + chartW, chartH)
                    lineTo(leftPad, chartH)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                        startY = 0f,
                        endY = chartH
                    ),
                    style = Fill
                )

                // Punti sui picchi
                for (hour in 0..23) {
                    val value = hourlyValues[hour]
                    if (value > maxValue * 0.1f) { // Disegna punti solo per valori rilevanti
                        val x = leftPad + hour * slotW
                        val y = chartH - (chartH * (value / maxValue))
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                // Labels asse X (ore principali)
                listOf(0, 6, 12, 18, 23).forEach { hour ->
                    val x = leftPad + (hour.toFloat() / 23f) * chartW
                    val label = "${hour}h"
                    val measured = textMeasurer.measure(label, labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = (x - measured.size.width / 2f).coerceIn(leftPad, size.width - measured.size.width),
                            y = chartH + 8.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

// Riga app

@Composable
private fun AppUsageRow(app: AppUsageInfo, filter: UsageFilter, maxValue: Float) {
    val context = LocalContext.current

    // Carica l'icona dell'app in modo asincrono
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
        UsageFilter.SCREEN_TIME -> {
            val f = (app.totalTimeInForeground.toFloat() / maxValue).coerceIn(0f, 1f)
            f to formatDuration(app.totalTimeInForeground)
        }
        UsageFilter.TIMES_OPENED -> {
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
            // Icona
            Box(
                modifier         = Modifier.size(40.dp),
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

            // Nome app
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

            // Valore
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
// Utility
// ─────────────────────────────────────────────────────────────────────────────

/** Formatta una durata in millisecondi come hh:mm:ss */
private fun formatDuration(ms: Long): String {
    val h = ms / 3_600_000L
    val m = (ms % 3_600_000L) / 60_000L
    val s = (ms % 60_000L) / 1_000L
    return "%02d:%02d:%02d".format(h, m, s)
}

/** Etichetta breve per l'asse Y del grafico (millisecondi → testo) */
private fun formatYLabelMs(ms: Long): String = when {
    ms <= 0L         -> "0"
    ms >= 3_600_000L -> "${ms / 3_600_000L}h"
    ms >= 60_000L    -> "${ms / 60_000L}m"
    else             -> "${ms / 1_000L}s"
}
