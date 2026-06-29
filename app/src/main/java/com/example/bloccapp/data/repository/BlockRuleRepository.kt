package com.example.bloccapp.data.repository

import com.example.bloccapp.data.db.dao.BlockRuleDao
import com.example.bloccapp.data.db.entity.BlockRule
import kotlinx.coroutines.flow.Flow

/**
 * Repository per le regole di blocco.
 * Fornisce un'unica sorgente di verità ai ViewModel, astraendo la sorgente dati (Room).
 */
class BlockRuleRepository(private val dao: BlockRuleDao) {

    /** Flow che emette la lista completa di regole ad ogni modifica del DB. */
    val allRules: Flow<List<BlockRule>> = dao.getAllRules()

    /** Flow che emette solo le regole attualmente abilitate. */
    val activeRules: Flow<List<BlockRule>> = dao.getActiveRules()

    suspend fun getRuleByPackage(packageName: String): BlockRule? =
        dao.getRuleByPackage(packageName)

    suspend fun insert(rule: BlockRule) = dao.insert(rule)

    suspend fun update(rule: BlockRule) = dao.update(rule)

    suspend fun delete(rule: BlockRule) = dao.delete(rule)

    suspend fun deleteByPackage(packageName: String) = dao.deleteByPackage(packageName)

    /** Abilita o disabilita una regola esistente senza eliminarla. */
    suspend fun setEnabled(rule: BlockRule, enabled: Boolean) =
        dao.update(rule.copy(isEnabled = enabled))
}
