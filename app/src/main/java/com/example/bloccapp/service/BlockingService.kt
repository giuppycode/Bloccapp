package com.example.bloccapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bloccapp.MainActivity
import com.example.bloccapp.data.db.AppDatabase
import com.example.bloccapp.data.db.BlockWithApps
import com.example.bloccapp.data.db.entity.BlockEvent
import com.example.bloccapp.ui.screen.BlockedAppActivity
import com.example.bloccapp.UsageStatsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Servizio in foreground che monitora quale app è in primo piano ogni secondo.
 *
 * Quando rileva un'app appartenente a un blocco attivo (e la cui schedule è attiva),
 * lancia [BlockedAppActivity] e registra un [BlockEvent] nel DB.
 *
 * Richiede:
 *  - android.permission.PACKAGE_USAGE_STATS  (per rilevare l'app in foreground)
 *  - android.permission.SYSTEM_ALERT_WINDOW  (per lanciare activity da background su API 29+)
 */
class BlockingService : Service() {

    companion object {
        private const val TAG = "BlockingService"
        private const val NOTIFICATION_ID   = 1001
        private const val CHANNEL_ID        = "blocking_service"
        private const val OUR_PACKAGE       = "com.example.bloccapp"

        /** Intervallo di refresh della cache usage stats (DAILY_USAGE/DAILY_OPENS). */
        private const val USAGE_CACHE_TTL_MS = 60_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private var db: AppDatabase? = null
    private var enabledBlocks: List<BlockWithApps> = emptyList()

    /** Package bloccato al tick precedente (evita di lanciare l'activity ad ogni tick). */
    private var lastBlockedPackage: String? = null

    /** Cache usage stats (per DAILY_USAGE / DAILY_OPENS). */
    private var usageCacheTimestamp = 0L
    private var usageCache: List<com.example.bloccapp.AppUsageInfo> = emptyList()

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        collectBlocks()
        startPollingLoop()
        Log.d(TAG, "Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onDestroy() {
        scope.cancel()
        Log.d(TAG, "Service stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Blocks collection (Flow → keep in sync)
    // ─────────────────────────────────────────────────────────────────────────

    private fun collectBlocks() {
        scope.launch {
            db?.blockDao()?.getEnabledBlocksWithApps()?.collect { blocks ->
                enabledBlocks = blocks
                Log.d(TAG, "Loaded ${blocks.size} enabled blocks")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Polling loop
    // ─────────────────────────────────────────────────────────────────────────

    private fun startPollingLoop() {
        scope.launch {
            while (true) {
                try {
                    checkForegroundApp()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling loop", e)
                }
                delay(1_000L)
            }
        }
    }

    private suspend fun checkForegroundApp() {
        val currentPkg = getForegroundPackage() ?: return

        // Non blocchiamo noi stessi
        if (currentPkg == OUR_PACKAGE) {
            lastBlockedPackage = null
            return
        }

        // Sblocco temporaneo attivo
        if (BlockingState.isTemporarilyUnlocked(currentPkg)) {
            lastBlockedPackage = null
            return
        }

        // Controlla ogni blocco attivo
        for (bwa in enabledBlocks) {
            if (!bwa.apps.any { it.packageName == currentPkg }) continue
            if (!isScheduleActive(bwa)) continue

            // App bloccata — lancia l'overlay solo se non già mostrata per questa app
            if (currentPkg != lastBlockedPackage) {
                lastBlockedPackage = currentPkg
                logBlockEvent(bwa.block.id, currentPkg, "APP_BLOCKED")
                launchBlockedActivity(currentPkg, bwa)
            }
            return
        }

        // Nessun blocco attivo per questo package
        lastBlockedPackage = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Foreground app detection
    // ─────────────────────────────────────────────────────────────────────────

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 3_000L, now)
        val event  = UsageEvents.Event()
        var lastPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Schedule evaluation
    // ─────────────────────────────────────────────────────────────────────────

    private fun isScheduleActive(bwa: BlockWithApps): Boolean {
        val block = bwa.block
        return when (block.scheduleType) {
            "NONE"        -> true
            "TIME_SLOT"   -> isCurrentTimeInSlot(block.scheduleStartTime, block.scheduleEndTime)
            "DAILY_USAGE" -> isDailyUsageLimitReached(bwa)
            "DAILY_OPENS" -> isDailyOpensLimitReached(bwa)
            else          -> false
        }
    }

    /** Controlla se l'ora corrente è nella fascia [startTime, endTime) in formato "HH:mm". */
    private fun isCurrentTimeInSlot(startTime: String, endTime: String): Boolean {
        return try {
            val cal  = Calendar.getInstance()
            val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val (sh, sm) = startTime.split(":").map { it.toInt() }
            val (eh, em) = endTime.split(":").map { it.toInt() }
            val startMin = sh * 60 + sm
            val endMin   = eh * 60 + em
            if (startMin <= endMin) nowMin in startMin until endMin
            else nowMin >= startMin || nowMin < endMin   // span mezzanotte
        } catch (e: Exception) {
            false
        }
    }

    /** True se almeno un'app del blocco ha superato il limite di minuti giornalieri. */
    private fun isDailyUsageLimitReached(bwa: BlockWithApps): Boolean {
        val limitMs = bwa.block.dailyUsageLimitMinutes * 60_000L
        val stats   = getOrRefreshUsageCache()
        return bwa.apps.any { app ->
            val usage = stats.find { it.packageName == app.packageName }
            (usage?.totalTimeInForeground ?: 0L) >= limitMs
        }
    }

    /** True se almeno un'app del blocco ha superato il limite di aperture giornaliere. */
    private fun isDailyOpensLimitReached(bwa: BlockWithApps): Boolean {
        val limit = bwa.block.dailyOpenCountLimit
        val stats = getOrRefreshUsageCache()
        return bwa.apps.any { app ->
            val usage = stats.find { it.packageName == app.packageName }
            (usage?.launchCount ?: 0) >= limit
        }
    }

    /** Restituisce la cache usage stats, aggiornandola se scaduta (TTL 60s). */
    private fun getOrRefreshUsageCache(): List<com.example.bloccapp.AppUsageInfo> {
        val now = System.currentTimeMillis()
        if (now - usageCacheTimestamp > USAGE_CACHE_TTL_MS) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            usageCache          = UsageStatsProvider.getAppStatsForRange(applicationContext, cal.timeInMillis, now)
            usageCacheTimestamp = now
        }
        return usageCache
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────────────

    private fun launchBlockedActivity(packageName: String, bwa: BlockWithApps) {
        val intent = BlockedAppActivity.buildIntent(applicationContext, packageName, bwa.block)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        Log.d(TAG, "Launched BlockedAppActivity for $packageName (block: ${bwa.block.name})")
    }

    private suspend fun logBlockEvent(blockId: Long, packageName: String, eventType: String) {
        try {
            db?.blockEventDao()?.insert(
                BlockEvent(blockId = blockId, packageName = packageName, eventType = eventType)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log block event", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bloccapp attivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifica persistente mentre il servizio di blocco è attivo"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Bloccapp attivo")
        .setContentText("Il blocco app è in esecuzione")
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
}
