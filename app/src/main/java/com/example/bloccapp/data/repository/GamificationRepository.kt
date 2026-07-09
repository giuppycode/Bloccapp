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
     * Impedisce che il saldo totale scenda sotto lo zero.
     * @param points      Valore positivo per guadagnare, negativo per decurtare.
     * @param description Descrizione human-readable dell'evento.
     */
    suspend fun addPoints(points: Int, description: String) {
        val currentTotal = dao.getCurrentTotal()
        
        // Se stiamo togliendo punti e il totale andrebbe sotto zero, 
        // limitiamo la perdita a quanto basta per arrivare a zero.
        val adjustedPoints = if (points < 0) {
            if (currentTotal == 0) return // Non aggiungiamo nemmeno il log se siamo già a 0
            if (currentTotal + points < 0) -currentTotal else points
        } else {
            points
        }

        dao.insert(
            GamificationHistory(
                points = adjustedPoints,
                timestamp = System.currentTimeMillis(),
                description = description
            )
        )
    }
}
