package com.example.bloccapp.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.ui.viewmodel.DashboardViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyUsageScreen(
    vm: DashboardViewModel = viewModel()
) {
    val topApps by vm.topApps.collectAsStateWithLifecycle()
    val hasPerm by vm.hasUsagePermission.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Daily app usage", fontWeight = FontWeight.Bold) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Grafico grande ────────────────────────────────────────────────
            UsageBarChartCard(
                apps     = topApps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // ── Due card medie ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TotalTimeCard(apps = topApps, modifier = Modifier.weight(1f))
                TopAppCard(apps   = topApps, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componenti interni
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UsageBarChartCard(
    apps: List<AppUsageInfo>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary

    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun dato disponibile", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val maxTime = apps.maxOf { it.totalTimeInForeground }.coerceAtLeast(1L)
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val barCount  = apps.size
                val gap       = 8.dp.toPx()
                val barWidth  = (size.width - gap * (barCount - 1)) / barCount
                val maxHeight = size.height - 24.dp.toPx()

                apps.forEachIndexed { i, app ->
                    val fraction = app.totalTimeInForeground.toFloat() / maxTime
                    val barH     = maxHeight * fraction
                    val x        = i * (barWidth + gap)
                    val y        = size.height - barH
                    drawRoundRect(
                        color       = barColor,
                        topLeft     = Offset(x, y),
                        size        = Size(barWidth, barH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalTimeCard(apps: List<AppUsageInfo>, modifier: Modifier = Modifier) {
    val totalMs  = apps.sumOf { it.totalTimeInForeground }
    val hours    = TimeUnit.MILLISECONDS.toHours(totalMs)
    val minutes  = TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60

    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.height(120.dp)
    ) {
        Column(
            modifier          = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tempo totale", style = MaterialTheme.typography.labelMedium)
            Text(
                text  = "${hours}h ${minutes}m",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TopAppCard(apps: List<AppUsageInfo>, modifier: Modifier = Modifier) {
    val top = apps.firstOrNull()

    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier.height(120.dp)
    ) {
        Column(
            modifier          = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("App più usata", style = MaterialTheme.typography.labelMedium)
            Text(
                text  = top?.appName ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            if (top != null) {
                val m = TimeUnit.MILLISECONDS.toMinutes(top.totalTimeInForeground)
                Text("${m}m", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
