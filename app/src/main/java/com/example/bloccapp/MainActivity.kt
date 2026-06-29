package com.example.bloccapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.service.BlockingService
import com.example.bloccapp.ui.navigation.AppNavGraph
import com.example.bloccapp.ui.theme.BloccappTheme
import com.example.bloccapp.ui.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

            BloccappTheme(themeMode = themeMode) {
                AppNavGraph()
            }
        }

        // Avvia il servizio di blocco se i permessi necessari sono già stati concessi.
        // Se mancano, la schermata Account → Permessi guiderà l'utente.
        maybeStartBlockingService()
    }

    /**
     * Avvia [BlockingService] come foreground service se entrambi i permessi
     * (PACKAGE_USAGE_STATS e SYSTEM_ALERT_WINDOW) sono stati concessi.
     */
    private fun maybeStartBlockingService() {
        if (PermissionManager.hasUsageStatsPermission(this) &&
            PermissionManager.hasOverlayPermission(this)
        ) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, BlockingService::class.java)
            )
        }
    }
}
