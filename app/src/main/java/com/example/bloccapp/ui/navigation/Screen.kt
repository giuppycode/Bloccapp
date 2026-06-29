package com.example.bloccapp.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Definisce tutte le destinazioni di navigazione dell'app.
 */
sealed class Screen(val route: String) {

    // ──── Auth ────────────────────────────────────────────────────────────────
    object Auth : Screen("auth")

    // ──── Bottom-nav (tab principali) ─────────────────────────────────────────
    object Blocks          : Screen("blocks")
    object DailyUsage      : Screen("daily_usage")
    object Reports         : Screen("reports")
    object AccountSettings : Screen("account_settings")

    // ──── Sub-schermate (senza bottom nav) ────────────────────────────────────

    /** Creazione/modifica di un blocco. Parametro opzionale: blockId (-1 = nuovo). */
    object AddBlock : Screen("add_block/{blockId}") {
        const val ARG = "blockId"
        /** Nuova schermata "crea blocco". */
        fun createRoute(blockId: Long = -1L) = "add_block/$blockId"
    }

    /** Selezione app da aggiungere a un blocco. */
    object AppSelection : Screen("app_selection")

    /** Report dettagliato di una settimana. Parametro: weekLabel (URL-encoded). */
    object WeeklyReport : Screen("weekly_report/{weekLabel}") {
        const val ARG = "weekLabel"
        fun createRoute(label: String): String =
            "weekly_report/${URLEncoder.encode(label, "UTF-8")}"
        fun decodeLabel(encoded: String): String =
            URLDecoder.decode(encoded, "UTF-8")
    }
}
