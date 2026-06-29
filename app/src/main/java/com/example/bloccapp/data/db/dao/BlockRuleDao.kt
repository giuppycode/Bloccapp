package com.example.bloccapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bloccapp.data.db.entity.BlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {

    /** Emette la lista aggiornata ogni volta che cambia il DB. */
    @Query("SELECT * FROM block_rules ORDER BY packageName ASC")
    fun getAllRules(): Flow<List<BlockRule>>

    /** Restituisce la regola per un pacchetto specifico, o null se assente. */
    @Query("SELECT * FROM block_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleByPackage(packageName: String): BlockRule?

    /** Restituisce solo le regole attive. */
    @Query("SELECT * FROM block_rules WHERE isEnabled = 1")
    fun getActiveRules(): Flow<List<BlockRule>>

    /** Inserisce o sovrascrive una regola (basato su id). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: BlockRule)

    @Update
    suspend fun update(rule: BlockRule)

    @Delete
    suspend fun delete(rule: BlockRule)

    @Query("DELETE FROM block_rules WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
