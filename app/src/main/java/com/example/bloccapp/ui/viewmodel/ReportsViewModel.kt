package com.example.bloccapp.ui.viewmodel

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.AppUsageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Dati di utilizzo aggregati per una settimana. */
data class WeeklyUsage(
    val label: String,          // es. "22 giu - 28 giu"
    val startMillis: Long,
    val endMillis: Long,
    val topApps: List<AppUsageInfo>   // top 5 app ordinate per uso
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val _weeks = MutableStateFlow<List<WeeklyUsage>>(emptyList())
    val weeks: StateFlow<List<WeeklyUsage>> = _weeks.asStateFlow()

    init { loadWeeks() }

    private fun loadWeeks() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx  = getApplication<Application>()
            val usm  = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val fmt  = SimpleDateFormat("d MMM", Locale.ITALIAN)
            val list = mutableListOf<WeeklyUsage>()

            // Carica le ultime 4 settimane
            repeat(4) { weekOffset ->
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                    add(Calendar.WEEK_OF_YEAR, -weekOffset)
                }
                val startCal = (endCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = startCal.timeInMillis
                val end   = endCal.timeInMillis
                val label = "${fmt.format(startCal.time)} - ${fmt.format(endCal.time)}"

                val stats = usm?.queryUsageStats(
                    UsageStatsManager.INTERVAL_WEEKLY, start, end
                ) ?: emptyList()

                val topApps: List<AppUsageInfo> = stats
                    .filter { it.totalTimeInForeground > 0 }
                    .sortedByDescending { it.totalTimeInForeground }
                    .take(5)
                    .map { s ->
                        val name = try {
                            ctx.packageManager.getApplicationLabel(
                                ctx.packageManager.getApplicationInfo(s.packageName, 0)
                            ).toString()
                        } catch (_: Exception) { s.packageName }
                        AppUsageInfo(s.packageName, name, s.totalTimeInForeground)
                    }

                list.add(WeeklyUsage(label, start, end, topApps))
            }
            _weeks.value = list
        }
    }
}
