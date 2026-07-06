package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.data.db.AppDatabase
import com.example.bloccapp.data.model.ScheduleConfig
import com.example.bloccapp.data.model.UnlockConfig
import com.example.bloccapp.data.model.WhatConfig
import com.example.bloccapp.data.repository.BlockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class AddBlockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BlockRepository(
        AppDatabase.getInstance(application).blockDao()
    )

    // Form

    private val _blockName = MutableStateFlow("")
    val blockName: StateFlow<String> = _blockName.asStateFlow()

    private val _selectedPackages = MutableStateFlow<List<String>>(emptyList())
    val selectedPackages: StateFlow<List<String>> = _selectedPackages.asStateFlow()

    private val _schedule = MutableStateFlow(ScheduleConfig())
    val schedule: StateFlow<ScheduleConfig> = _schedule.asStateFlow()

    private val _whatConfig = MutableStateFlow(WhatConfig())
    val whatConfig: StateFlow<WhatConfig> = _whatConfig.asStateFlow()

    /**
     * Configurazione di sblocco.
     * Durante l'editing il campo [UnlockConfig.pinRaw] contiene il PIN in chiaro (4 cifre).
     * Al salvataggio viene hashato in [UnlockConfig.pinHash] prima di persistere su DB.
     */
    private val _unlockConfig = MutableStateFlow(UnlockConfig())
    val unlockConfig: StateFlow<UnlockConfig> = _unlockConfig.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    // Funzioni set

    fun setBlockName(name: String)            { _blockName.value = name }
    fun setSelectedPackages(pkgs: List<String>) { _selectedPackages.value = pkgs }
    fun setSchedule(config: ScheduleConfig)   { _schedule.value = config }
    fun setWhatConfig(config: WhatConfig)     { _whatConfig.value = config }
    fun setUnlockConfig(config: UnlockConfig) { _unlockConfig.value = config }

    // Salvataggio

    fun saveBlock() {
        if (_blockName.value.isBlank()) return
        viewModelScope.launch {
            val rawUnlock = _unlockConfig.value

            // Genera il QR secret se non già presente
            val qrSecret = if (rawUnlock.qrCode && rawUnlock.qrSecret.isBlank())
                UUID.randomUUID().toString()
            else
                rawUnlock.qrSecret

            // Hasha il PIN se presente
            val pinHash = if (rawUnlock.pin && rawUnlock.pinRaw.isNotBlank())
                sha256(rawUnlock.pinRaw)
            else
                rawUnlock.pinHash

            val finalUnlock = rawUnlock.copy(qrSecret = qrSecret, pinHash = pinHash)

            repository.saveBlock(
                name      = _blockName.value.trim(),
                packages  = _selectedPackages.value,
                schedule  = _schedule.value,
                what      = _whatConfig.value,
                unlock    = finalUnlock
            )
            _saved.value = true
        }
    }

    // Utils

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
