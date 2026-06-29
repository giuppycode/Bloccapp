package com.example.bloccapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.bloccapp.data.db.BlockWithApps
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.data.db.entity.BlockApp
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {

    @Transaction
    @Query("SELECT * FROM blocks ORDER BY name ASC")
    fun getAllBlocksWithApps(): Flow<List<BlockWithApps>>

    @Transaction
    @Query("SELECT * FROM blocks WHERE isEnabled = 1 ORDER BY name ASC")
    fun getEnabledBlocksWithApps(): Flow<List<BlockWithApps>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: Block): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockApp(app: BlockApp)

    @Update
    suspend fun updateBlock(block: Block)

    @Delete
    suspend fun deleteBlock(block: Block)

    @Query("DELETE FROM block_apps WHERE blockId = :blockId")
    suspend fun deleteAppsForBlock(blockId: Long)
}
