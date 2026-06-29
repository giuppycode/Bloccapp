package com.example.bloccapp.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.data.db.entity.BlockApp

/**
 * Relazione Room: [Block] con la lista dei suoi [BlockApp].
 * Usata per query che restituiscono un blocco con tutte le app associate.
 */
data class BlockWithApps(
    @Embedded val block: Block,
    @Relation(
        parentColumn = "id",
        entityColumn = "blockId"
    )
    val apps: List<BlockApp>
)
