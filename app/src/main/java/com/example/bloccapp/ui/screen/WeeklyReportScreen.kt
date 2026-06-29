package com.example.bloccapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.ui.viewmodel.ReportsViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportScreen(
    weekLabel: String,
    onBack: () -> Unit,
    vm: ReportsViewModel = viewModel()
) {
    val weeks by vm.weeks.collectAsStateWithLifecycle()
    val week  = weeks.firstOrNull { it.label == weekLabel }

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

            // ── Card totale settimana ─────────────────────────────────────────
            if (week != null) {
                val totalMs = week.topApps.sumOf { it.totalTimeInForeground }
                val hours   = TimeUnit.MILLISECONDS.toHours(totalMs)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60

                Card(
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    Column(
                        modifier          = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Utilizzo totale settimanale",
                            style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${hours}h ${minutes}m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Top 5 used apps ───────────────────────────────────────────────
            if (week != null && week.topApps.isNotEmpty()) {
                Text(
                    "Top 5 used apps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TopAppsBarChart(apps = week.topApps)

                Button(
                    onClick = { /* TODO: espandi lista */ },
                    colors  = ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("View more")
                }
            }

            // ── Block stats ───────────────────────────────────────────────────
            Text(
                "Block stats",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title   = "Blocchi attivi",
                    value   = "—",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title   = "Tempo risparmiato",
                    value   = "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Componenti interni ────────────────────────────────────────────────────────

@Composable
private fun TopAppsBarChart(apps: List<AppUsageInfo>) {
    val maxTime = apps.maxOf { it.totalTimeInForeground }.coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        apps.forEach { app ->
            val fraction  = (app.totalTimeInForeground.toFloat() / maxTime).coerceIn(0.01f, 0.99f)
            val remainder = (1f - fraction).coerceAtLeast(0.01f)
            val minutes   = TimeUnit.MILLISECONDS.toMinutes(app.totalTimeInForeground)

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = app.appName,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(90.dp),
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                Card(
                    shape    = RoundedCornerShape(4.dp),
                    colors   = CardDefaults.cardColors(containerColor = barColor),
                    modifier = Modifier
                        .weight(fraction)
                        .height(24.dp)
                ) {}
                Spacer(Modifier.weight(remainder))
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "${minutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.height(90.dp)
    ) {
        Column(
            modifier          = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
