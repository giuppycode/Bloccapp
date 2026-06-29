package com.example.bloccapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rappresenta un evento nello storico della gamification.
 *
 * @param points      Punti aggiunti (o sottratti, se negativi) in questo evento.
 * @param timestamp   Data/ora dell'evento in millisecondi epoch.
 * @param description Breve descrizione dell'evento (es. "Giornata senza sblocchi: +10 pt").
 */
@Entity(tableName = "gamification_history")
data class GamificationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val points: Int,
    val timestamp: Long,
    val description: String
)
