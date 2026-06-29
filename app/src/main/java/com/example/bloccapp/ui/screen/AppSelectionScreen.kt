package com.example.bloccapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.ui.viewmodel.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    initialSelectedPackages: List<String> = emptyList(),
    onBack: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    vm: AppListViewModel = viewModel()
) {
    val apps             by vm.filteredApps.collectAsStateWithLifecycle()
    val searchQuery      by vm.searchQuery.collectAsStateWithLifecycle()
    val selectedPackages by vm.selectedPackages.collectAsStateWithLifecycle()

    var searchActive by remember { mutableStateOf(false) }

    // Pre-seleziona i package già scelti
    LaunchedEffect(Unit) { vm.setInitialSelection(initialSelectedPackages) }

    // Categorizza: Social vs Others
    val socialKeywords = listOf(
        "instagram", "facebook", "tiktok", "twitter", "snapchat",
        "youtube", "whatsapp", "telegram", "reddit", "linkedin", "pinterest"
    )
    val socialApps = apps.filter { a ->
        socialKeywords.any { kw -> a.packageName.contains(kw, ignoreCase = true) }
    }
    val otherApps = apps.filter { a ->
        socialKeywords.none { kw -> a.packageName.contains(kw, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select apps to block", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick  = { onConfirm(selectedPackages.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val n = selectedPackages.size
                Text(if (n == 0) "Conferma" else "Conferma ($n selezionate)")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding  = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Search bar ───────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                @Suppress("DEPRECATION")
                SearchBar(
                    query         = searchQuery,
                    onQueryChange = { vm.setSearchQuery(it) },
                    onSearch      = { searchActive = false },
                    active        = false,
                    onActiveChange = { searchActive = it },
                    placeholder   = { Text("Search bar") },
                    modifier      = Modifier.fillMaxWidth()
                ) {}
            }

            // ── Social category ──────────────────────────────────────────────
            if (socialApps.isNotEmpty()) {
                item {
                    Text(
                        "Social",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    AppIconGrid(
                        apps             = socialApps,
                        selectedPackages = selectedPackages,
                        onToggle         = { vm.toggleSelection(it) }
                    )
                }
            }

            // ── Others category ──────────────────────────────────────────────
            if (otherApps.isNotEmpty()) {
                item {
                    Text(
                        "Others",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    AppIconGrid(
                        apps             = otherApps,
                        selectedPackages = selectedPackages,
                        onToggle         = { vm.toggleSelection(it) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AppIconGrid(
    apps: List<AppUsageInfo>,
    selectedPackages: Set<String>,
    onToggle: (String) -> Unit
) {
    // Griglia 4 colonne
    val rows    = apps.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowApps ->
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowApps.forEach { app ->
                    AppIconItem(
                        app      = app,
                        selected = app.packageName in selectedPackages,
                        onToggle = { onToggle(app.packageName) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining empty slots
                repeat(4 - rowApps.size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AppIconItem(
    app: AppUsageInfo,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor      = MaterialTheme.colorScheme.secondaryContainer
    val initial      = app.appName.firstOrNull()?.uppercaseChar() ?: '?'

    Column(
        modifier            = modifier.clickable { onToggle() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .then(
                    if (selected) Modifier.border(3.dp, primaryColor, RoundedCornerShape(12.dp))
                    else Modifier
                )
        ) {
            Text(
                text  = initial.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selezionato",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Text(
            text      = app.appName,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
