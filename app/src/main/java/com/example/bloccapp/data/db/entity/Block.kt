package com.example.bloccapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rappresenta un blocco nominato che raggruppa più app.
 *
 * Versione DB 3: i tre campi stringa generici (scheduleDescription, whatToBlock, howToUnblock)
 * sono stati sostituiti da campi tipizzati per una gestione strutturata delle regole.
 */
@Entity(tableName = "blocks")
data class Block(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,

    // Regole orarie
    /** Tipo di vincolo: "NONE" | "TIME_SLOT" | "DAILY_USAGE" | "DAILY_OPENS" | "LOCATION" */
    val scheduleType: String = "NONE",
    val scheduleStartTime: String = "09:00",
    val scheduleEndTime: String = "17:00",
    val dailyUsageLimitMinutes: Int = 60,
    val dailyOpenCountLimit: Int = 5,

    // Geofencing
    val geofenceLat: Double? = null,
    val geofenceLng: Double? = null,
    val geofenceRadius: Float? = null,

    // Cosa bloccare
    val blockAppStart: Boolean = true,
    val blockNotifications: Boolean = false,

    // Sblocco
    val unlockTimer: Boolean = false,
    @androidx.room.ColumnInfo(name = "unlockTimerMinutes")
    val unlockTimerSeconds: Int = 30,
    val unlockQrCode: Boolean = false,
    /** UUID generato una volta sola al salvataggio; usato per generare il QR. */
    val unlockQrSecret: String = "",
    val unlockPin: Boolean = false,
    /** Hash SHA-256 del PIN a 4 cifre impostato dall'utente. */
    val unlockPinHash: String = "",
    val unlockBiometric: Boolean = false
) {
    /** Riepilogo leggibile della configurazione "quando bloccare". */
    fun scheduleDisplay(): String = when (scheduleType) {
        "TIME_SLOT"   -> "$scheduleStartTime – $scheduleEndTime"
        "DAILY_USAGE" -> "Max $dailyUsageLimitMinutes min/giorno"
        "DAILY_OPENS" -> "Max $dailyOpenCountLimit avvii/giorno"
        "LOCATION"    -> "Blocco in area"
        else          -> ""
    }

    /** Riepilogo leggibile dei metodi di sblocco configurati. */
    fun unlockMethodsDisplay(): String = buildString {
        if (unlockTimer)     append("⏱ ${unlockTimerSeconds}s")
        if (unlockQrCode)    { if (isNotEmpty()) append("  "); append("📷") }
        if (unlockPin)       { if (isNotEmpty()) append("  "); append("🔢") }
        if (unlockBiometric) { if (isNotEmpty()) append("  "); append("🪪") }
    }
}
