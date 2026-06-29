package com.example.bloccapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rappresenta una regola di blocco per una specifica app.
 *
 * @param packageName  Nome del pacchetto dell'app (es. "com.instagram.android")
 * @param dailyTimeLimitMs Limite giornaliero di utilizzo in millisecondi. 0 = nessun limite temporale.
 * @param locationId   ID opzionale della geofence associata (FK verso future tabelle geofence).
 *                     null = la regola non è legata a una posizione GPS.
 * @param isEnabled    Se false, la regola è disabilitata senza doverla eliminare.
 */
@Entity(tableName = "block_rules")
data class BlockRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val dailyTimeLimitMs: Long = 0L,
    val locationId: Long? = null,
    val isEnabled: Boolean = true
)
