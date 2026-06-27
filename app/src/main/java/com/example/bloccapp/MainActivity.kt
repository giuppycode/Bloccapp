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

class MainActivity : ComponentActivity() {
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
        var hasUsagePermission by remember { mutableStateOf(PermissionManager.hasUsageStatsPermission(this)) }
        var hasOverlayPermission by remember { mutableStateOf(PermissionManager.hasOverlayPermission(this)) }

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
                        usage.forEach { info ->
                            Log.d("UsageStats", "App: ${info.appName}, Package: ${info.packageName}, Time: ${info.totalTimeInForeground / 1000}s")
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Log App Usage")
                }
            }
        }

        // Re-check permissions when activity resumes
        LaunchedEffect(Unit) {
            // Simplified: in a real app we'd use a LifecycleEventObserver
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
        // Force recomposition to update permission status
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
