package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.DailyUsageData
import com.example.bloccapp.UsageStatsProvider
import com.example.bloccapp.data.db.AppDatabase
import com.example.bloccapp.data.db.entity.GamificationHistory
import com.example.bloccapp.data.repository.GamificationRepository
import com.example.bloccapp.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db               = AppDatabase.getInstance(application)
    private val gamificationRepo = GamificationRepository(db.gamificationHistoryDao())
    private val userRepo         = UserRepository(db.userDao())

    /** Informazioni sull'utente. */
    val userInfo = userRepo.user
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Punteggio totale gamification. */
    val totalPoints: StateFlow<Int> = gamificationRepo.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Ultimi 5 eventi gamification. */
    val recentHistory: StateFlow<List<GamificationHistory>> = gamificationRepo.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _dailyData = MutableStateFlow(DailyUsageData(emptyList()))
    /** Tutti i dati di utilizzo della giornata (app + bucket orari). */
    val dailyData: StateFlow<DailyUsageData> = _dailyData.asStateFlow()

    /** Top 5 app per screen time — mantenuto per retrocompatibilità. */
    val topApps: StateFlow<List<AppUsageInfo>> = _dailyData
        .map { it.apps.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
                val data = withContext(Dispatchers.IO) {
                    UsageStatsProvider.getDailyUsageData(ctx)
                }
                _dailyData.value = data
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
