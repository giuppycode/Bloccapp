package com.example.bloccapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.service.BlockingService
import com.example.bloccapp.ui.navigation.AppNavGraph
import com.example.bloccapp.ui.theme.BloccappTheme
import com.example.bloccapp.ui.viewmodel.ThemeViewModel
import org.osmdroid.config.Configuration

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurazione OpenStreetMap
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

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

    override fun onResume() {
        super.onResume()
        maybeStartBlockingService()
    }

    /**
     * Avvia [BlockingService] come foreground service se entrambi i permessi
     * (PACKAGE_USAGE_STATS e SYSTEM_ALERT_WINDOW) sono stati concessi.
     */
    private fun maybeStartBlockingService() {
        val hasUsage = PermissionManager.hasUsageStatsPermission(this)
        val hasOverlay = PermissionManager.hasOverlayPermission(this)
        android.util.Log.d("MainActivity", "maybeStartBlockingService: hasUsage=$hasUsage, hasOverlay=$hasOverlay")
        if (hasUsage && hasOverlay) {
            android.util.Log.d("MainActivity", "Starting BlockingService...")
            ContextCompat.startForegroundService(
                this,
                Intent(this, BlockingService::class.java)
            )
        }
    }
}
