package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.data.db.AppDatabase
import com.example.bloccapp.data.db.BlockWithApps
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.data.repository.BlockRepository
import com.example.bloccapp.data.repository.GamificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlocksViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = BlockRepository(db.blockDao())
    private val gamificationRepo = GamificationRepository(db.gamificationHistoryDao())

    /** Tutti i blocchi con le app associate. */
    val allBlocks: StateFlow<List<BlockWithApps>> = repository.allBlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Punti totali gamification. */
    val totalPoints: StateFlow<Int> = gamificationRepo.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleBlock(block: Block) {
        viewModelScope.launch {
            if (block.isEnabled) {
                // Se viene disabilitato un blocco attivo, togliamo punti
                gamificationRepo.addPoints(-6, "Disattivazione blocco: ${block.name}")
            }
            repository.toggleBlock(block)
        }
    }

    fun deleteBlock(block: Block) {
        viewModelScope.launch {
            gamificationRepo.addPoints(-6, "Eliminazione blocco: ${block.name}")
            repository.deleteBlock(block)
        }
    }
}
