package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.UsageStatsProvider
import com.example.bloccapp.data.db.AppDatabase
import com.example.bloccapp.data.db.entity.GamificationHistory
import com.example.bloccapp.data.preferences.AuthPreferencesManager
import com.example.bloccapp.data.preferences.UserInfo
import com.example.bloccapp.data.repository.GamificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db               = AppDatabase.getInstance(application)
    private val gamificationRepo = GamificationRepository(db.gamificationHistoryDao())
    private val authPrefs        = AuthPreferencesManager(application.applicationContext)

    /** Informazioni sull'utente loggato. */
    val userInfo: StateFlow<UserInfo?> = authPrefs.userInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Punteggio totale gamification. */
    val totalPoints: StateFlow<Int> = gamificationRepo.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Ultimi 5 eventi gamification. */
    val recentHistory: StateFlow<List<GamificationHistory>> = gamificationRepo.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _topApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    /** Top 5 app per tempo di utilizzo giornaliero. */
    val topApps: StateFlow<List<AppUsageInfo>> = _topApps.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    init {
        loadUsageData()
    }

    fun loadUsageData() {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val hasPermission = com.example.bloccapp.PermissionManager.hasUsageStatsPermission(ctx)
            _hasUsagePermission.value = hasPermission
            if (hasPermission) {
                val usage = UsageStatsProvider.getInstalledAppsUsage(ctx)
                _topApps.value = usage.take(5)
            }
        }
    }

    /** Aggiunge punti gamification (es. per test o ricompense). */
    fun addPoints(points: Int, description: String) {
        viewModelScope.launch {
            gamificationRepo.addPoints(points, description)
        }
    }
}
