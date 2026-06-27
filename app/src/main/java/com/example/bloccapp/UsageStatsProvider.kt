package com.example.bloccapp

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long
)

object UsageStatsProvider {

    fun getInstalledAppsUsage(context: Context): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val appUsageList = mutableListOf<AppUsageInfo>()
        if (stats != null) {
            for (usageStats in stats) {
                try {
                    val appInfo = pm.getApplicationInfo(usageStats.packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    if (usageStats.totalTimeInForeground > 0) {
                        appUsageList.add(
                            AppUsageInfo(
                                usageStats.packageName,
                                appName,
                                usageStats.totalTimeInForeground
                            )
                        )
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // App might have been uninstalled
                }
            }
        }
        return appUsageList.sortedByDescending { it.totalTimeInForeground }
    }
}
