package com.example.bloccapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registra ogni evento di blocco per le statistiche settimanali.
 *
 * @param blockId     ID del blocco che ha scatenato l'evento
 * @param packageName Package dell'app bloccata
 * @param eventType   "APP_BLOCKED" quando il blocco intercetta l'apertura; "PAUSED" quando
 *                    l'utente sblocca temporaneamente
 * @param timestamp   Epoch ms dell'evento
 */
@Entity(tableName = "block_events")
data class BlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockId: Long,
    val packageName: String,
    val eventType: String,
    val timestamp: Long = System.currentTimeMillis()
)
