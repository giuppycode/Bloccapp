package com.example.bloccapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bloccapp.data.db.entity.GamificationHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationHistoryDao {

    /** Emette tutta la storia in ordine cronologico inverso (più recente prima). */
    @Query("SELECT * FROM gamification_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<GamificationHistory>>

    /** Emette la somma totale dei punti guadagnati. */
    @Query("SELECT COALESCE(SUM(points), 0) FROM gamification_history")
    fun getTotalPoints(): Flow<Int>

    /** Restituisce il totale attuale dei punti (non come flusso). */
    @Query("SELECT COALESCE(SUM(points), 0) FROM gamification_history")
    suspend fun getCurrentTotal(): Int

    /** Restituisce gli eventi per un giorno specifico (range timestamp in ms). */
    @Query("SELECT * FROM gamification_history WHERE timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp DESC")
    fun getHistoryByDay(startOfDay: Long, endOfDay: Long): Flow<List<GamificationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: GamificationHistory)
}
