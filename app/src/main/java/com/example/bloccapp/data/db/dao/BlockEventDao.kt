package com.example.bloccapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.bloccapp.data.db.entity.BlockEvent

@Dao
interface BlockEventDao {

    @Insert
    suspend fun insert(event: BlockEvent)

    /** Numero di blocchi automatici (app intercettata) in un intervallo di tempo. */
    @Query(
        "SELECT COUNT(*) FROM block_events " +
        "WHERE eventType = 'APP_BLOCKED' AND timestamp BETWEEN :from AND :to"
    )
    suspend fun countBlocked(from: Long, to: Long): Int

    /** Numero di sblocchi temporanei (pause) in un intervallo di tempo. */
    @Query(
        "SELECT COUNT(*) FROM block_events " +
        "WHERE eventType = 'PAUSED' AND timestamp BETWEEN :from AND :to"
    )
    suspend fun countPaused(from: Long, to: Long): Int
}
