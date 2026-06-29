package com.example.bloccapp.data.model

// ─────────────────────────────────────────────────────────────────────────────
// Modelli del form di creazione/modifica di un blocco
// ─────────────────────────────────────────────────────────────────────────────

/** Tipo di vincolo orario per un blocco. */
enum class ScheduleType { NONE, TIME_SLOT, DAILY_USAGE, DAILY_OPENS }

/**
 * Configurazione di "quando bloccare".
 *
 * @param type                   Tipo di vincolo
 * @param startTime              Ora di inizio (formato "HH:mm"), usato solo per [ScheduleType.TIME_SLOT]
 * @param endTime                Ora di fine (formato "HH:mm"), usato solo per [ScheduleType.TIME_SLOT]
 * @param dailyUsageLimitMinutes Minuti massimi di utilizzo al giorno, per [ScheduleType.DAILY_USAGE]
 * @param dailyOpenCountLimit    Numero massimo di aperture al giorno, per [ScheduleType.DAILY_OPENS]
 */
data class ScheduleConfig(
    val type: ScheduleType = ScheduleType.NONE,
    val startTime: String = "09:00",
    val endTime: String = "17:00",
    val dailyUsageLimitMinutes: Int = 60,
    val dailyOpenCountLimit: Int = 5
) {
    /** Testo riepilogativo leggibile per la UI. */
    fun displayText(): String = when (type) {
        ScheduleType.NONE        -> ""
        ScheduleType.TIME_SLOT   -> "$startTime – $endTime"
        ScheduleType.DAILY_USAGE -> "Max $dailyUsageLimitMinutes min/day"
        ScheduleType.DAILY_OPENS -> "Max $dailyOpenCountLimit opens/day"
    }
}

/**
 * Configurazione di "cosa bloccare".
 */
data class WhatConfig(
    val appStart: Boolean = true,
    val notifications: Boolean = false
)

/**
 * Configurazione di "come sbloccare".
 *
 * @param timer        Se l'utente deve attendere prima di sbloccare
 * @param timerMinutes Minuti di attesa (visualizzati nella UI)
 * @param qrCode       Se il QR code è richiesto per sbloccare
 * @param qrSecret     UUID del QR secret (generato al primo salvataggio)
 * @param pin          Se il PIN è richiesto per sbloccare
 * @param pinRaw       PIN in chiaro (4 cifre), usato solo durante la compilazione del form
 * @param pinHash      Hash SHA-256 del PIN, persistito nel DB
 * @param biometric    Se la biometria è richiesta per sbloccare
 */
data class UnlockConfig(
    val timer: Boolean = false,
    val timerMinutes: Int = 5,
    val qrCode: Boolean = false,
    val qrSecret: String = "",
    val pin: Boolean = false,
    val pinRaw: String = "",
    val pinHash: String = "",
    val biometric: Boolean = false
) {
    /** Alias usato dal layer repository per la persistenza su DB. */
    val timerSeconds: Int get() = timerMinutes
}
