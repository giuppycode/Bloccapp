package com.example.bloccapp

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        Log.d("UsageStatsProvider", "Querying from ${java.util.Date(startTime)} to ${java.util.Date(endTime)}")

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        Log.d("UsageStatsProvider", "Found ${stats?.size ?: 0} raw usage stats entries")

        val appUsageList = mutableListOf<AppUsageInfo>()
        if (stats != null) {
            for (usageStats in stats) {
                try {
                    if (usageStats.totalTimeInForeground > 0) {
                        val appInfo = pm.getApplicationInfo(usageStats.packageName, 0)
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        appUsageList.add(
                            AppUsageInfo(
                                usageStats.packageName,
                                appName,
                                usageStats.totalTimeInForeground
                            )
                        )
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // App might have been uninstalled or is a system component without label
                }
            }
        }
        Log.d("UsageStatsProvider", "Returning ${appUsageList.size} processed entries")
        return appUsageList.sortedByDescending { it.totalTimeInForeground }
    }
}
