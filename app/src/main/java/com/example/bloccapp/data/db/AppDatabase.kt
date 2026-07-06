package com.example.bloccapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.bloccapp.data.db.dao.BlockDao
import com.example.bloccapp.data.db.dao.BlockEventDao
import com.example.bloccapp.data.db.dao.BlockRuleDao
import com.example.bloccapp.data.db.dao.GamificationHistoryDao
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.data.db.entity.BlockApp
import com.example.bloccapp.data.db.entity.BlockEvent
import com.example.bloccapp.data.db.entity.BlockRule
import com.example.bloccapp.data.db.entity.GamificationHistory

/**
 * Database Room
 *
 * v1: init
 * v2: aggiunta Block e BlockApp
 * v3: refactoring tabella Block (campi tipizzati)
 * v4: tabella block_events
 */
@Database(
    entities = [
        BlockRule::class,
        GamificationHistory::class,
        Block::class,
        BlockApp::class,
        BlockEvent::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun gamificationHistoryDao(): GamificationHistoryDao
    abstract fun blockDao(): BlockDao
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Migrazione 4→5: aggiunge campi geofence alla tabella blocks. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `blocks` ADD COLUMN `geofenceLat` REAL")
                db.execSQL("ALTER TABLE `blocks` ADD COLUMN `geofenceLng` REAL")
                db.execSQL("ALTER TABLE `blocks` ADD COLUMN `geofenceRadius` REAL")
            }
        }

        /** Migrazione 1->2 */
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

        /** Migrazione 2->3 */
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

        /** Migrazione 3→4: aggiunge la tabella block_events. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `block_events` (
                        `id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `blockId`     INTEGER NOT NULL,
                        `packageName` TEXT    NOT NULL,
                        `eventType`   TEXT    NOT NULL,
                        `timestamp`   INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bloccapp.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
