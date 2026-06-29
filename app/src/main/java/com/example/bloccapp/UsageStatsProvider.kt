package com.example.bloccapp

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long,   // ms
    val launchCount: Int = 0,
    val notificationCount: Int = 0
)

/**
 * Dati di utilizzo aggregati per la giornata corrente.
 *
 * @param apps                Lista di tutte le app usate oggi, ordinate per [AppUsageInfo.totalTimeInForeground] decrescente
 * @param hourlyScreenTimeMs  Millisecondi di schermo per ciascuna delle 24 ore
 * @param hourlyTimesOpened   Numero di aperture per ciascuna delle 24 ore
 * @param hourlyNotifications Numero di notifiche per ciascuna delle 24 ore
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

// ─────────────────────────────────────────────────────────────────────────────
// Provider
// ─────────────────────────────────────────────────────────────────────────────

object UsageStatsProvider {

    // Valori interi delle costanti UsageEvents.Event rilevanti
    // (usati direttamente per evitare problemi di accesso/deprecazione dell'SDK)
    private const val EVT_FOREGROUND          = 1   // MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
    private const val EVT_BACKGROUND          = 2   // MOVE_TO_BACKGROUND / ACTIVITY_PAUSED
    private const val EVT_NOTIFICATION_SEEN   = 10  // NOTIFICATION_SEEN (API 23+)
    private const val EVT_NOTIFICATION_INTR   = 12  // NOTIFICATION_INTERRUPTION (API 28+)

    /**
     * Restituisce i dati di utilizzo aggregati per la giornata corrente usando
     * [UsageStatsManager.queryEvents], che permette di ricavare:
     * - tempo in primo piano per-app e per-ora
     * - numero di aperture per-app e per-ora
     * - numero di notifiche viste per-app e per-ora
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

        // Per-app accumulatori
        val screenTimeMs      = mutableMapOf<String, Long>()
        val launchCount       = mutableMapOf<String, Int>()
        val notificationCount = mutableMapOf<String, Int>()

        // Hourly buckets (24 ore)
        val hourlyScreenTimeMs  = LongArray(24)
        val hourlyTimesOpened   = IntArray(24)
        val hourlyNotifications = IntArray(24)

        // Traccia l'inizio della sessione in foreground per ogni app
        val foregroundStart = mutableMapOf<String, Long>()

        val events = usm.queryEvents(startOfDay, now)
        val event  = UsageEvents.Event()

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

        // App ancora in foreground: chiude la sessione con "now"
        for ((pkg, start) in foregroundStart) {
            val duration = now - start
            screenTimeMs[pkg] = (screenTimeMs[pkg] ?: 0L) + duration
            distributeScreenTime(start, now, startOfDay, hourlyScreenTimeMs)
        }

        // Costruisce la lista app
        val apps = screenTimeMs.keys.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name    = pm.getApplicationLabel(appInfo).toString()
                AppUsageInfo(
                    packageName           = pkg,
                    appName               = name,
                    totalTimeInForeground = screenTimeMs[pkg] ?: 0L,
                    launchCount           = launchCount[pkg] ?: 0,
                    notificationCount     = notificationCount[pkg] ?: 0
                )
            } catch (_: PackageManager.NameNotFoundException) { null }
        }.sortedByDescending { it.totalTimeInForeground }

        Log.d("UsageStatsProvider", "getDailyUsageData: ${apps.size} apps")

        return DailyUsageData(
            apps                = apps,
            hourlyScreenTimeMs  = hourlyScreenTimeMs,
            hourlyTimesOpened   = hourlyTimesOpened,
            hourlyNotifications = hourlyNotifications
        )
    }

    // ── Legacy helper mantenuto per retrocompatibilità ────────────────────────
    fun getInstalledAppsUsage(context: Context): List<AppUsageInfo> =
        getDailyUsageData(context).apps

    /**
     * Restituisce i dati di utilizzo per-app su un range arbitrario usando
     * [UsageStatsManager.queryEvents]. Popola [AppUsageInfo.totalTimeInForeground],
     * [AppUsageInfo.launchCount] e [AppUsageInfo.notificationCount].
     * Utile per aggregazioni settimanali dove [UsageStatsManager.queryUsageStats]
     * non fornisce launch count né notification count.
     */
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

        // App ancora in foreground alla fine del range
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
                    notificationCount     = notificationCount[pkg] ?: 0
                )
            } catch (_: PackageManager.NameNotFoundException) { null }
        }.sortedByDescending { it.totalTimeInForeground }
    }

    // ── Utility: distribuisce la durata di una sessione sulle ore coinvolte ──

    private fun distributeScreenTime(
        startMs: Long,
        endMs: Long,
        dayStartMs: Long,
        hourlyMs: LongArray
    ) {
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
