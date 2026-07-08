package com.example.bloccapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rappresenta l'unico utente dell'app.
 * Poiché l'app è locale, avremo sempre un solo record con id=1.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val email: String = "" // Inutilizzato ma mantenuto per compatibilità se necessario
)
