package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.PermissionManager
import com.example.bloccapp.UsageStatsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel per la selezione app (AppSelectionScreen).
 * Carica la lista delle app installate e gestisce ricerca + selezione multipla.
 */
class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val _allApps     = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** App filtrate per query di ricerca. */
    val filteredApps: StateFlow<List<AppUsageInfo>> = combine(
        _allApps, _searchQuery
    ) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter {
            it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Package selezionati dall'utente (multi-select). */
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    init { loadApps() }

    fun loadApps() {
        viewModelScope.launch {
            val ctx    = getApplication<Application>().applicationContext
            val hasPerm = PermissionManager.hasUsageStatsPermission(ctx)
            _hasUsagePermission.value = hasPerm
            if (hasPerm) {
                _allApps.value = UsageStatsProvider.getInstalledAppsUsage(ctx)
            } else {
                // Mostra comunque le app installate senza statistiche
                val pm    = ctx.packageManager
                val pkgs  = pm.getInstalledApplications(0)
                    .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                _allApps.value = pkgs.map { info ->
                    AppUsageInfo(
                        packageName          = info.packageName,
                        appName              = pm.getApplicationLabel(info).toString(),
                        totalTimeInForeground = 0L
                    )
                }.sortedBy { it.appName }
            }
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun toggleSelection(packageName: String) {
        _selectedPackages.value = _selectedPackages.value.toMutableSet().also { set ->
            if (packageName in set) set.remove(packageName) else set.add(packageName)
        }
    }

    fun setInitialSelection(packages: List<String>) {
        _selectedPackages.value = packages.toSet()
    }

    fun clearSelection() { _selectedPackages.value = emptySet() }
}
