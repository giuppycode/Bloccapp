package com.example.bloccapp.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
 * Gestisce anche le notifiche di pre-attivazione (5 min, 1 min, 30 sec).
 */
class BlockingService : Service() {

    companion object {
        private const val TAG = "BlockingService"
        private const val NOTIFICATION_ID         = 1001
        private const val CHANNEL_ID               = "blocking_service"
        private const val CHANNEL_ALERTS_ID        = "blocking_alerts"
        private const val OUR_PACKAGE             = "com.example.bloccapp"

        /** Intervallo di refresh della cache usage stats. */
        private const val USAGE_CACHE_TTL_MS = 60_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private var db: AppDatabase? = null
    private var enabledBlocks: List<BlockWithApps> = emptyList()

    /** Package bloccato al tick precedente. */
    private var lastBlockedPackage: String? = null

    /** Cache usage stats. */
    private var usageCacheTimestamp = 0L
    private var usageCache: List<com.example.bloccapp.AppUsageInfo> = emptyList()

    /** Cache notifiche inviate. */
    private val sentNotifications = mutableMapOf<String, Boolean>()

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(applicationContext)
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        collectBlocks()
        startPollingLoop()
        Log.d(TAG, "Service started. Notification permission: ${hasNotificationPermission()}")
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
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
    // Blocks collection
    // ─────────────────────────────────────────────────────────────────────────

    private fun collectBlocks() {
        scope.launch {
            db?.blockDao()?.getEnabledBlocksWithApps()?.collect { blocks ->
                enabledBlocks = blocks
                Log.d(TAG, "Loaded ${blocks.size} enabled blocks:")
                blocks.forEach { bwa ->
                    Log.d(TAG, "  - Block: ${bwa.block.name}, Type: ${bwa.block.scheduleType}, Start: ${bwa.block.scheduleStartTime}")
                }
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
                    checkPreActivationNotifications()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling loop", e)
                }
                delay(1_000L)
            }
        }
    }

    private suspend fun checkForegroundApp() {
        val currentPkg = getForegroundPackage() ?: return

        if (currentPkg == OUR_PACKAGE) {
            lastBlockedPackage = null
            return
        }

        if (BlockingState.isTemporarilyUnlocked(currentPkg)) {
            lastBlockedPackage = null
            return
        }

        for (bwa in enabledBlocks) {
            if (!bwa.apps.any { it.packageName == currentPkg }) continue
            if (!isScheduleActive(bwa)) continue

            if (currentPkg != lastBlockedPackage) {
                lastBlockedPackage = currentPkg
                logBlockEvent(bwa.block.id, currentPkg, "APP_BLOCKED")
                launchBlockedActivity(currentPkg, bwa)
            }
            return
        }

        lastBlockedPackage = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pre-activation notifications logic
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkPreActivationNotifications() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val dateKey = "${cal.get(Calendar.YEAR)}${cal.get(Calendar.MONTH)}${cal.get(Calendar.DAY_OF_MONTH)}"

        if (enabledBlocks.isEmpty()) return

        for (bwa in enabledBlocks) {
            val block = bwa.block
            if (block.scheduleType != "TIME_SLOT") continue

            try {
                val (sh, sm) = block.scheduleStartTime.split(":").map { it.toInt() }
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, sh)
                    set(Calendar.MINUTE, sm)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Se l'orario è già passato oggi, consideriamo l'orario di domani
                if (targetCal.timeInMillis < now - 10_000L) { // tolleranza 10s per non saltare trigger istantanei
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val diffSeconds = (targetCal.timeInMillis - now) / 1000

                // Log periodico ogni 10 secondi per non intasare, se entro 1 ora
                if (diffSeconds in 0..3600 && (diffSeconds % 10 == 0L)) {
                    Log.d(TAG, "DEBUG: Block '${block.name}' (${block.scheduleStartTime}) starts in $diffSeconds seconds")
                }

                val thresholds = listOf(300, 60, 30)

                for (t in thresholds) {
                    // Finestra di 5 secondi per intercettare il tick
                    if (diffSeconds in (t - 5)..t) {
                        val notificationKey = "${block.id}_${dateKey}_$t"
                        if (sentNotifications[notificationKey] != true) {
                            Log.i(TAG, "!!! TRIGGER !!! Sending $t seconds notification for '${block.name}'")
                            sendPreActivationNotification(block.name, t)
                            sentNotifications[notificationKey] = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating pre-activation for block ${block.name}", e)
            }
        }
    }

    private fun sendPreActivationNotification(blockName: String, secondsRemaining: Int) {
        // Verifica permesso su Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot send notification: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val timeLabel = when (secondsRemaining) {
            300 -> "5 min"
            60  -> "1 min"
            else -> "$secondsRemaining secondi"
        }

        val text = "mancano $timeLabel all'attivazione del blocco $blockName"

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS_ID)
            .setContentTitle("Blocco in arrivo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Popup visibile
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(blockName.hashCode() + secondsRemaining, notification)
        Log.d(TAG, "Notification posted: $text")
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

    private fun isCurrentTimeInSlot(startTime: String, endTime: String): Boolean {
        return try {
            val cal  = Calendar.getInstance()
            val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val (sh, sm) = startTime.split(":").map { it.toInt() }
            val (eh, em) = endTime.split(":").map { it.toInt() }
            val startMin = sh * 60 + sm
            val endMin   = eh * 60 + em
            if (startMin <= endMin) nowMin in startMin until endMin
            else nowMin >= startMin || nowMin < endMin
        } catch (e: Exception) {
            false
        }
    }

    private fun isDailyUsageLimitReached(bwa: BlockWithApps): Boolean {
        val limitMs = bwa.block.dailyUsageLimitMinutes * 60_000L
        val stats   = getOrRefreshUsageCache()
        return bwa.apps.any { app ->
            val usage = stats.find { it.packageName == app.packageName }
            (usage?.totalTimeInForeground ?: 0L) >= limitMs
        }
    }

    private fun isDailyOpensLimitReached(bwa: BlockWithApps): Boolean {
        val limit = bwa.block.dailyOpenCountLimit
        val stats = getOrRefreshUsageCache()
        return bwa.apps.any { app ->
            val usage = stats.find { it.packageName == app.packageName }
            (usage?.launchCount ?: 0) >= limit
        }
    }

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
    // Notifications setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canale di servizio (silenzioso)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Bloccapp attivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifica persistente mentre il servizio di blocco è attivo"
            }
            nm.createNotificationChannel(serviceChannel)

            // Canale avvisi (suono/vibrazione/popup)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Avvisi blocco",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche pre-attivazione dei blocchi"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(alertChannel)
            Log.d(TAG, "Notification channels created/updated")
        }
    }

    private fun buildForegroundNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
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
