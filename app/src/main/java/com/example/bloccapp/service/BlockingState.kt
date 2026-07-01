package com.example.bloccapp.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton thread-safe che traccia gli sblocchi temporanei.
 *
 * Quando l'utente passa con successo un metodo di sblocco (timer, PIN, QR),
 * il pacchetto viene aggiunto qui con un timestamp di scadenza.
 * Il [BlockingService] controlla [isTemporarilyUnlocked] prima di bloccare.
 */
object BlockingState {

    /** packageName → epoch ms di scadenza dell'unlock temporaneo */
    private val unlocks = ConcurrentHashMap<String, Long>()

    /** Restituisce true se il pacchetto è stato sbloccato temporaneamente e non è scaduto. */
    fun isTemporarilyUnlocked(packageName: String): Boolean =
        System.currentTimeMillis() < (unlocks[packageName] ?: 0L)

    /**
     * Concede un unlock temporaneo al pacchetto per [durationMs] millisecondi.
     * Default: 5 minuti.
     */
    fun grantTemporaryUnlock(packageName: String, durationMs: Long = 5 * 60_000L) {
        unlocks[packageName] = System.currentTimeMillis() + durationMs
    }

    /** Revoca immediatamente un unlock temporaneo (utile per i test). */
    fun revokeUnlock(packageName: String) {
        unlocks.remove(packageName)
    }

    /** Rimuove tutti gli sblocchi temporanei (es. al cambio di giorno). */
    fun resetAll() {
        unlocks.clear()
    }
}
