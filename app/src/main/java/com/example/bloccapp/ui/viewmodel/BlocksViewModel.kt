package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.data.db.AppDatabase
import com.example.bloccapp.data.db.BlockWithApps
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.data.repository.BlockRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlocksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BlockRepository(
        AppDatabase.getInstance(application).blockDao()
    )

    /** Tutti i blocchi con le app associate. */
    val allBlocks: StateFlow<List<BlockWithApps>> = repository.allBlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleBlock(block: Block) {
        viewModelScope.launch { repository.toggleBlock(block) }
    }

    fun deleteBlock(block: Block) {
        viewModelScope.launch { repository.deleteBlock(block) }
    }
}
