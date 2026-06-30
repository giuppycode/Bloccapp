package com.example.bloccapp.ui.screen

import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.AppUsageInfo
import com.example.bloccapp.ui.viewmodel.AppListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    // Pre-seleziona i package già scelti
    LaunchedEffect(Unit) { vm.setInitialSelection(initialSelectedPackages) }

    // Raggruppamento dinamico
    val groupedApps = remember(apps) {
        apps.groupBy { app -> getCategoryName(app) }
            .toSortedMap(compareBy { categorySortOrder(it) })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select apps", fontWeight = FontWeight.Bold) },
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
            contentPadding  = PaddingValues(bottom = 80.dp), // Spazio per il bottone
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Search bar custom ────────────────────────────────────────────
            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { vm.setSearchQuery(it) },
                        placeholder   = { Text("Cerca app…") },
                        modifier      = Modifier.fillMaxWidth(),
                        leadingIcon   = { Icon(Icons.Default.Search, null) },
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // ── Categories ───────────────────────────────────────────────────
            groupedApps.forEach { (categoryName, categoryApps) ->
                item {
                    Text(
                        text  = categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                item {
                    AppIconGrid(
                        apps             = categoryApps,
                        selectedPackages = selectedPackages,
                        onToggle         = { vm.toggleSelection(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIconGrid(
    apps: List<AppUsageInfo>,
    selectedPackages: Set<String>,
    onToggle: (String) -> Unit
) {
    val rows = apps.chunked(4)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier            = Modifier.padding(horizontal = 16.dp)
    ) {
        rows.forEach { rowApps ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowApps.forEach { app ->
                    AppIconItem(
                        app      = app,
                        selected = app.packageName in selectedPackages,
                        onToggle = { onToggle(app.packageName) },
                        modifier = Modifier.weight(1f)
                    )
                }
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
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor      = MaterialTheme.colorScheme.secondaryContainer

    // Caricamento asincrono dell'icona
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(app.packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }
    }

    Column(
        modifier            = modifier.clickable { onToggle() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .then(
                    if (selected) Modifier.border(3.dp, primaryColor, RoundedCornerShape(14.dp))
                    else Modifier
                )
        ) {
            if (icon != null) {
                Image(
                    bitmap             = icon!!,
                    contentDescription = app.appName,
                    modifier           = Modifier.fillMaxSize().padding(8.dp)
                )
            } else {
                Icon(
                    imageVector        = Icons.Default.Android,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                    modifier           = Modifier.size(32.dp)
                )
            }

            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(
            text      = app.appName,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Logic helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun getCategoryName(app: AppUsageInfo): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && app.category != ApplicationInfo.CATEGORY_UNDEFINED) {
        return when (app.category) {
            ApplicationInfo.CATEGORY_SOCIAL         -> "Social"
            ApplicationInfo.CATEGORY_GAME           -> "Games"
            ApplicationInfo.CATEGORY_PRODUCTIVITY   -> "Productivity"
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_AUDIO          -> "Entertainment"
            ApplicationInfo.CATEGORY_MAPS           -> "Navigation"
            ApplicationInfo.CATEGORY_NEWS           -> "News"
            ApplicationInfo.CATEGORY_IMAGE          -> "Photo & Video"
            else -> if (app.isSystemApp) "System" else fallbackCategory(app)
        }
    }
    if (app.isSystemApp) return "System"
    return fallbackCategory(app)
}

private fun fallbackCategory(app: AppUsageInfo): String {
    val socialKeywords = listOf(
        "instagram", "facebook", "tiktok", "twitter", "snapchat",
        "youtube", "whatsapp", "telegram", "reddit", "linkedin", "pinterest", "x.android"
    )
    if (socialKeywords.any { app.packageName.contains(it, ignoreCase = true) }) return "Social"
    return "Others"
}

private fun categorySortOrder(name: String): Int = when (name) {
    "Social"         -> 1
    "Entertainment"  -> 2
    "Games"          -> 3
    "Productivity"   -> 4
    "Navigation"     -> 5
    "News"           -> 6
    "Photo & Video"  -> 7
    "Others"         -> 8
    "System"         -> 9
    else             -> 10
}
