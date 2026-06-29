package com.example.bloccapp.data.repository

import com.example.bloccapp.data.db.dao.GamificationHistoryDao
import com.example.bloccapp.data.db.entity.GamificationHistory
import kotlinx.coroutines.flow.Flow

/**
 * Repository per la gamification.
 * Astrae le operazioni Room e fornisce funzioni di alto livello per aggiungere punti.
 */
class GamificationRepository(private val dao: GamificationHistoryDao) {

    /** Flow che emette tutta la storia in ordine cronologico inverso. */
    val allHistory: Flow<List<GamificationHistory>> = dao.getAllHistory()

    /** Flow che emette la somma totale aggiornata dei punti. */
    val totalPoints: Flow<Int> = dao.getTotalPoints()

    fun getHistoryByDay(startOfDay: Long, endOfDay: Long): Flow<List<GamificationHistory>> =
        dao.getHistoryByDay(startOfDay, endOfDay)

    /**
     * Aggiunge punti con una descrizione all'evento.
     * @param points      Valore positivo per guadagnare, negativo per decurtare.
     * @param description Descrizione human-readable dell'evento.
     */
    suspend fun addPoints(points: Int, description: String) {
        dao.insert(
            GamificationHistory(
                points = points,
                timestamp = System.currentTimeMillis(),
                description = description
            )
        )
    }
}
