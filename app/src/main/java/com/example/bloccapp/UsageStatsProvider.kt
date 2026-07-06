package com.example.bloccapp

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.util.Calendar

// Data models

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long,   // ms
    val launchCount: Int = 0,
    val notificationCount: Int = 0,
    val category: Int = -1,
    val isSystemApp: Boolean = false
)

/**
 * Stats di utilizzo per la giornata.
 */
data class DailyUsageData(
    val apps: List<AppUsageInfo>,
    val hourlyScreenTimeMs: LongArray = LongArray(24),
    val hourlyTimesOpened: IntArray   = IntArray(24),
    val hourlyNotifications: IntArray = IntArray(24)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DailyUsageData) return false
        return apps == other.apps &&
                hourlyScreenTimeMs.contentEquals(other.hourlyScreenTimeMs) &&
                hourlyTimesOpened.contentEquals(other.hourlyTimesOpened) &&
                hourlyNotifications.contentEquals(other.hourlyNotifications)
    }

    override fun hashCode(): Int {
        var result = apps.hashCode()
        result = 31 * result + hourlyScreenTimeMs.contentHashCode()
        result = 31 * result + hourlyTimesOpened.contentHashCode()
        result = 31 * result + hourlyNotifications.contentHashCode()
        return result
    }
}

// Provider

object UsageStatsProvider {

    private const val EVT_FOREGROUND          = 1
    private const val EVT_BACKGROUND          = 2
    private const val EVT_NOTIFICATION_SEEN   = 10
    private const val EVT_NOTIFICATION_INTR   = 12

    /**
     * Restituisce i dati di utilizzo aggregati per la giornata corrente.
     */
    fun getDailyUsageData(context: Context): DailyUsageData {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm  = context.packageManager

        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val screenTimeMs      = mutableMapOf<String, Long>()
        val launchCount       = mutableMapOf<String, Int>()
        val notificationCount = mutableMapOf<String, Int>()
        val hourlyScreenTimeMs  = LongArray(24)
        val hourlyTimesOpened   = IntArray(24)
        val hourlyNotifications = IntArray(24)
        val foregroundStart = mutableMapOf<String, Long>()

        val events = usm.queryEvents(startOfDay, now)
        val event  = UsageEvents.Event()

    // Fix per app già aperta a mezzanotte
        // Cerchiamo l'ultimo evento prima dello startOfDay negli ultimi 24h
        val preEvents = usm.queryEvents(startOfDay - 86_400_000L, startOfDay)
        var pkgActiveAtStart: String? = null
        while (preEvents.hasNextEvent()) {
            preEvents.getNextEvent(event)
            if (event.eventType == EVT_FOREGROUND) pkgActiveAtStart = event.packageName
            else if (event.eventType == EVT_BACKGROUND) pkgActiveAtStart = null
        }
        if (pkgActiveAtStart != null) {
            foregroundStart[pkgActiveAtStart] = startOfDay
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg  = event.packageName
            val time = event.timeStamp
            val hour = ((time - startOfDay) / 3_600_000L).toInt().coerceIn(0, 23)

            when (event.eventType) {
                EVT_FOREGROUND -> {
                    foregroundStart[pkg] = time
                    launchCount[pkg] = (launchCount[pkg] ?: 0) + 1
                    hourlyTimesOpened[hour]++
                }
                EVT_BACKGROUND -> {
                    val start = foregroundStart.remove(pkg) ?: continue
                    val duration = time - start
                    screenTimeMs[pkg] = (screenTimeMs[pkg] ?: 0L) + duration
                    distributeScreenTime(start, time, startOfDay, hourlyScreenTimeMs)
                }
                EVT_NOTIFICATION_SEEN,
                EVT_NOTIFICATION_INTR -> {
                    notificationCount[pkg] = (notificationCount[pkg] ?: 0) + 1
                    hourlyNotifications[hour]++
                }
            }
        }

        for ((pkg, start) in foregroundStart) {
            val duration = now - start
            screenTimeMs[pkg] = (screenTimeMs[pkg] ?: 0L) + duration
            distributeScreenTime(start, now, startOfDay, hourlyScreenTimeMs)
        }

        val apps = screenTimeMs.keys.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name    = pm.getApplicationLabel(appInfo).toString()
                AppUsageInfo(
                    packageName           = pkg,
                    appName               = name,
                    totalTimeInForeground = screenTimeMs[pkg] ?: 0L,
                    launchCount           = launchCount[pkg] ?: 0,
                    notificationCount     = notificationCount[pkg] ?: 0,
                    category              = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appInfo.category else -1,
                    isSystemApp           = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            } catch (_: PackageManager.NameNotFoundException) { null }
        }.sortedByDescending { it.totalTimeInForeground }

        return DailyUsageData(apps, hourlyScreenTimeMs, hourlyTimesOpened, hourlyNotifications)
    }

    /**
     * Ritorna le app installate con stats di oggi.
     */
    fun getAllInstalledApps(context: Context): List<AppUsageInfo> {
        val pm = context.packageManager
        val dailyData = try {
            getDailyUsageData(context)
        } catch (e: Exception) {
            null
        }

        val usageMap = dailyData?.apps?.associateBy { it.packageName } ?: emptyMap()

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }

        return installedApps.map { info ->
            val pkg = info.packageName
            val usage = usageMap[pkg]
            AppUsageInfo(
                packageName           = pkg,
                appName               = pm.getApplicationLabel(info).toString(),
                totalTimeInForeground = usage?.totalTimeInForeground ?: 0L,
                launchCount           = usage?.launchCount ?: 0,
                notificationCount     = usage?.notificationCount ?: 0,
                category              = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) info.category else -1,
                isSystemApp           = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }.sortedWith(compareByDescending<AppUsageInfo> { it.totalTimeInForeground }.thenBy { it.appName })
    }

    @Deprecated("Usare getAllInstalledApps o getDailyUsageData().apps", ReplaceWith("getDailyUsageData(context).apps"))
    fun getInstalledAppsUsage(context: Context): List<AppUsageInfo> =
        getDailyUsageData(context).apps

    fun getAppStatsForRange(context: Context, startMs: Long, endMs: Long): List<AppUsageInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm  = context.packageManager

        val screenTimeMs      = mutableMapOf<String, Long>()
        val launchCount       = mutableMapOf<String, Int>()
        val notificationCount = mutableMapOf<String, Int>()
        val foregroundStart   = mutableMapOf<String, Long>()

        val events = usm.queryEvents(startMs, endMs)
        val event  = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg  = event.packageName
            val time = event.timeStamp

            when (event.eventType) {
                EVT_FOREGROUND -> {
                    foregroundStart[pkg] = time
                    launchCount[pkg] = (launchCount[pkg] ?: 0) + 1
                }
                EVT_BACKGROUND -> {
                    val start = foregroundStart.remove(pkg) ?: continue
                    screenTimeMs[pkg] = (screenTimeMs[pkg] ?: 0L) + (time - start)
                }
                EVT_NOTIFICATION_SEEN,
                EVT_NOTIFICATION_INTR -> {
                    notificationCount[pkg] = (notificationCount[pkg] ?: 0) + 1
                }
            }
        }

        for ((pkg, start) in foregroundStart) {
            screenTimeMs[pkg] = (screenTimeMs[pkg] ?: 0L) + (endMs - start)
        }

        val allPkgs = (screenTimeMs.keys + launchCount.keys + notificationCount.keys).toSet()
        return allPkgs.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name    = pm.getApplicationLabel(appInfo).toString()
                AppUsageInfo(
                    packageName           = pkg,
                    appName               = name,
                    totalTimeInForeground = screenTimeMs[pkg] ?: 0L,
                    launchCount           = launchCount[pkg] ?: 0,
                    notificationCount     = notificationCount[pkg] ?: 0,
                    category              = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appInfo.category else -1,
                    isSystemApp           = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            } catch (_: PackageManager.NameNotFoundException) { null }
        }.sortedByDescending { it.totalTimeInForeground }
    }

    private fun distributeScreenTime(startMs: Long, endMs: Long, dayStartMs: Long, hourlyMs: LongArray) {
        var current = startMs
        while (current < endMs) {
            val hour    = ((current - dayStartMs) / 3_600_000L).toInt().coerceIn(0, 23)
            val hourEnd = dayStartMs + (hour + 1) * 3_600_000L
            val sliceEnd = minOf(endMs, hourEnd)
            hourlyMs[hour] += sliceEnd - current
            current = sliceEnd
        }
    }
}
