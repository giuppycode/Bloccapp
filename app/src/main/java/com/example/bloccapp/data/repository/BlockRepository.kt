package com.example.bloccapp.data.repository

import com.example.bloccapp.data.db.BlockWithApps
import com.example.bloccapp.data.db.dao.BlockDao
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.data.db.entity.BlockApp
import com.example.bloccapp.data.model.ScheduleConfig
import com.example.bloccapp.data.model.ScheduleType
import com.example.bloccapp.data.model.UnlockConfig
import com.example.bloccapp.data.model.WhatConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository per i blocchi nominati multi-app ([Block] + [BlockApp]).
 *
 * Espone i dati come [Flow] per il layer ViewModel/UI e
 * fornisce operazioni suspend per le modifiche.
 */
class BlockRepository(private val dao: BlockDao) {

    /** Tutti i blocchi con le rispettive app. */
    val allBlocks: Flow<List<BlockWithApps>> = dao.getAllBlocksWithApps()

    /** Solo i blocchi attualmente abilitati. */
    val enabledBlocks: Flow<List<BlockWithApps>> = dao.getEnabledBlocksWithApps()

    /**
     * Salva un nuovo blocco con la configurazione strutturata.
     * @return l'id del blocco appena creato
     */
    suspend fun saveBlock(
        name: String,
        packages: List<String>,
        schedule: ScheduleConfig = ScheduleConfig(),
        what: WhatConfig = WhatConfig(),
        unlock: UnlockConfig = UnlockConfig()
    ): Long {
        val block = Block(
            name                    = name,
            scheduleType            = schedule.type.name,
            scheduleStartTime       = schedule.startTime,
            scheduleEndTime         = schedule.endTime,
            dailyUsageLimitMinutes  = schedule.dailyUsageLimitMinutes,
            dailyOpenCountLimit     = schedule.dailyOpenCountLimit,
            blockAppStart           = what.appStart,
            blockNotifications      = what.notifications,
            unlockTimer             = unlock.timer,
            unlockTimerSeconds      = unlock.timerSeconds,
            unlockQrCode            = unlock.qrCode,
            unlockQrSecret          = unlock.qrSecret,
            unlockPin               = unlock.pin,
            unlockPinHash           = unlock.pinHash,
            unlockBiometric         = unlock.biometric,
            geofenceLat             = schedule.lat,
            geofenceLng             = schedule.lng,
            geofenceRadius          = schedule.radius
        )
        val blockId = dao.insertBlock(block)
        packages.forEach { pkg ->
            dao.insertBlockApp(BlockApp(blockId = blockId, packageName = pkg))
        }
        return blockId
    }

    /**
     * Aggiorna un blocco esistente con la nuova configurazione strutturata.
     */
    suspend fun updateBlock(
        block: Block,
        packages: List<String>,
        schedule: ScheduleConfig,
        what: WhatConfig,
        unlock: UnlockConfig
    ) {
        val updated = block.copy(
            scheduleType            = schedule.type.name,
            scheduleStartTime       = schedule.startTime,
            scheduleEndTime         = schedule.endTime,
            dailyUsageLimitMinutes  = schedule.dailyUsageLimitMinutes,
            dailyOpenCountLimit     = schedule.dailyOpenCountLimit,
            blockAppStart           = what.appStart,
            blockNotifications      = what.notifications,
            unlockTimer             = unlock.timer,
            unlockTimerSeconds     = unlock.timerSeconds,
            unlockQrCode            = unlock.qrCode,
            unlockQrSecret          = unlock.qrSecret,
            unlockPin               = unlock.pin,
            unlockPinHash           = unlock.pinHash,
            unlockBiometric         = unlock.biometric,
            geofenceLat             = schedule.lat,
            geofenceLng             = schedule.lng,
            geofenceRadius          = schedule.radius
        )
        dao.updateBlock(updated)
        dao.deleteAppsForBlock(block.id)
        packages.forEach { pkg ->
            dao.insertBlockApp(BlockApp(blockId = block.id, packageName = pkg))
        }
    }

    /** Abilita o disabilita un blocco. */
    suspend fun toggleBlock(block: Block) {
        dao.updateBlock(block.copy(isEnabled = !block.isEnabled))
    }

    /** Elimina completamente un blocco (cascade sulle app). */
    suspend fun deleteBlock(block: Block) {
        dao.deleteBlock(block)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Ricostruisce un [ScheduleConfig] dai campi del [Block]. */
    fun Block.toScheduleConfig(): ScheduleConfig = ScheduleConfig(
        type                   = runCatching { ScheduleType.valueOf(scheduleType) }.getOrDefault(ScheduleType.NONE),
        startTime              = scheduleStartTime,
        endTime                = scheduleEndTime,
        dailyUsageLimitMinutes = dailyUsageLimitMinutes,
        dailyOpenCountLimit    = dailyOpenCountLimit,
        lat                    = geofenceLat,
        lng                    = geofenceLng,
        radius                 = geofenceRadius ?: 200f
    )

    /** Ricostruisce un [WhatConfig] dai campi del [Block]. */
    fun Block.toWhatConfig(): WhatConfig = WhatConfig(
        appStart      = blockAppStart,
        notifications = blockNotifications
    )

    /** Ricostruisce un [UnlockConfig] dai campi del [Block]. */
    fun Block.toUnlockConfig(): UnlockConfig = UnlockConfig(
        timer        = unlockTimer,
        timerMinutes = unlockTimerSeconds,
        qrCode       = unlockQrCode,
        qrSecret     = unlockQrSecret,
        pin          = unlockPin,
        pinHash      = unlockPinHash,
        biometric    = unlockBiometric
    )
}
