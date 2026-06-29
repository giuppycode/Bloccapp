package com.example.bloccapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.bloccapp.data.db.dao.BlockDao
import com.example.bloccapp.data.db.dao.BlockRuleDao
import com.example.bloccapp.data.db.dao.GamificationHistoryDao
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.data.db.entity.BlockApp
import com.example.bloccapp.data.db.entity.BlockRule
import com.example.bloccapp.data.db.entity.GamificationHistory

/**
 * Database Room principale dell'applicazione.
 *
 * v1 → schema iniziale (BlockRule, GamificationHistory).
 * v2 → aggiunta tabelle Block e BlockApp (campi stringa generici).
 * v3 → refactoring tabella Block: rimosse colonne stringa generiche,
 *       aggiunte colonne tipizzate per schedule, whatToBlock e howToUnblock.
 */
@Database(
    entities = [
        BlockRule::class,
        GamificationHistory::class,
        Block::class,
        BlockApp::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun gamificationHistoryDao(): GamificationHistoryDao
    abstract fun blockDao(): BlockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Migrazione 1→2: aggiunge le tabelle blocks e block_apps (schema originale). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `scheduleDescription` TEXT NOT NULL DEFAULT '',
                        `whatToBlock` TEXT NOT NULL DEFAULT '',
                        `howToUnblock` TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `block_apps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `blockId` INTEGER NOT NULL,
                        `packageName` TEXT NOT NULL,
                        FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_block_apps_blockId` ON `block_apps` (`blockId`)"
                )
            }
        }

        /**
         * Migrazione 2→3: rifacimento della tabella `blocks`.
         * Rimuove le colonne stringa generiche (scheduleDescription, whatToBlock, howToUnblock)
         * e aggiunge campi tipizzati per schedule, what-to-block e how-to-unblock.
         * La tabella block_apps non viene modificata.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Crea la nuova tabella con lo schema aggiornato
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blocks_new` (
                        `id`                      INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name`                    TEXT    NOT NULL,
                        `isEnabled`               INTEGER NOT NULL DEFAULT 1,
                        `scheduleType`            TEXT    NOT NULL DEFAULT 'NONE',
                        `scheduleStartTime`       TEXT    NOT NULL DEFAULT '09:00',
                        `scheduleEndTime`         TEXT    NOT NULL DEFAULT '17:00',
                        `dailyUsageLimitMinutes`  INTEGER NOT NULL DEFAULT 60,
                        `dailyOpenCountLimit`     INTEGER NOT NULL DEFAULT 5,
                        `blockAppStart`           INTEGER NOT NULL DEFAULT 1,
                        `blockNotifications`      INTEGER NOT NULL DEFAULT 0,
                        `unlockTimer`             INTEGER NOT NULL DEFAULT 0,
                        `unlockTimerMinutes`      INTEGER NOT NULL DEFAULT 30,
                        `unlockQrCode`            INTEGER NOT NULL DEFAULT 0,
                        `unlockQrSecret`          TEXT    NOT NULL DEFAULT '',
                        `unlockPin`               INTEGER NOT NULL DEFAULT 0,
                        `unlockPinHash`           TEXT    NOT NULL DEFAULT '',
                        `unlockBiometric`         INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // 2. Copia i dati essenziali (id, name, isEnabled) dalla vecchia tabella
                db.execSQL(
                    """
                    INSERT INTO `blocks_new` (`id`, `name`, `isEnabled`)
                    SELECT `id`, `name`, `isEnabled` FROM `blocks`
                    """.trimIndent()
                )

                // 3. Elimina la vecchia tabella e rinomina la nuova
                db.execSQL("DROP TABLE `blocks`")
                db.execSQL("ALTER TABLE `blocks_new` RENAME TO `blocks`")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bloccapp.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
