package com.example.bloccapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateListOf

class MainActivity : ComponentActivity() {
    private val appUsageList = mutableStateListOf<AppUsageInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DigitalWellbeingScreen()
                }
            }
        }
    }

    @Composable
    fun DigitalWellbeingScreen() {
        val hasUsagePermission = PermissionManager.hasUsageStatsPermission(this)
        val hasOverlayPermission = PermissionManager.hasOverlayPermission(this)

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Digital Wellbeing Permissions", style = MaterialTheme.typography.headlineMedium)

            PermissionStatus(
                label = "Usage Stats Permission",
                hasPermission = hasUsagePermission,
                onRequest = { PermissionManager.requestUsageStatsPermission(this@MainActivity) }
            )

            PermissionStatus(
                label = "Overlay Permission",
                hasPermission = hasOverlayPermission,
                onRequest = { PermissionManager.requestOverlayPermission(this@MainActivity) }
            )

            if (hasUsagePermission) {
                Button(
                    onClick = {
                        val usage = UsageStatsProvider.getInstalledAppsUsage(this@MainActivity)
                        appUsageList.clear()
                        appUsageList.addAll(usage)
                        if (usage.isEmpty()) {
                            Log.d("UsageStats", "No usage data found for today.")
                        } else {
                            usage.forEach { info ->
                                Log.d("UsageStats", "App: ${info.appName}, Package: ${info.packageName}, Time: ${info.totalTimeInForeground / 1000}s")
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Log & Show App Usage")
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                items(appUsageList) { info ->
                    AppUsageItem(info)
                    HorizontalDivider()
                }
            }
        }
    }

    @Composable
    fun AppUsageItem(info: AppUsageInfo) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = info.appName, style = MaterialTheme.typography.titleMedium)
            Text(text = info.packageName, style = MaterialTheme.typography.bodySmall)
            Text(text = "Usage: ${info.totalTimeInForeground / 1000}s", style = MaterialTheme.typography.bodyMedium)
        }
    }

    @Composable
    fun PermissionStatus(label: String, hasPermission: Boolean, onRequest: () -> Unit) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = "$label: ${if (hasPermission) "GRANTED" else "DENIED"}")
            if (!hasPermission) {
                Button(onClick = onRequest) {
                    Text("Grant $label")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-trigger setContent to refresh permission states accurately in UI
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DigitalWellbeingScreen()
                }
            }
        }
    }
}
