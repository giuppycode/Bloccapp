package com.example.bloccapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabella di join tra [Block] e i package Android bloccati.
 * Ogni riga rappresenta un'app appartenente a un blocco.
 *
 * @param blockId     FK verso [Block.id]
 * @param packageName Nome del pacchetto Android (es. "com.instagram.android")
 */
@Entity(
    tableName = "block_apps",
    foreignKeys = [
        ForeignKey(
            entity        = Block::class,
            parentColumns = ["id"],
            childColumns  = ["blockId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("blockId")]
)
data class BlockApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockId: Long,
    val packageName: String
)
