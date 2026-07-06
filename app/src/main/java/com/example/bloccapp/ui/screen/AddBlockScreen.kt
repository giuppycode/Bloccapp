package com.example.bloccapp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.PermissionManager
import com.example.bloccapp.data.model.ScheduleConfig
import com.example.bloccapp.data.model.ScheduleType
import com.example.bloccapp.data.model.UnlockConfig
import com.example.bloccapp.data.model.WhatConfig
import com.example.bloccapp.ui.viewmodel.AddBlockViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBlockScreen(
    onBack: () -> Unit,
    onSelectApps: (currentPackages: List<String>) -> Unit,
    onSelectLocation: (lat: Double?, lng: Double?, radius: Float) -> Unit,
    vm: AddBlockViewModel = viewModel()
) {
    val blockName        by vm.blockName.collectAsStateWithLifecycle()
    val selectedPackages by vm.selectedPackages.collectAsStateWithLifecycle()
    val schedule         by vm.schedule.collectAsStateWithLifecycle()
    val whatConfig       by vm.whatConfig.collectAsStateWithLifecycle()
    val unlockConfig     by vm.unlockConfig.collectAsStateWithLifecycle()
    val saved            by vm.saved.collectAsStateWithLifecycle()

    var showWhenSheet by remember { mutableStateOf(false) }

    // Launcher per i permessi di posizione
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineGranted) {
            onSelectLocation(schedule.lat, schedule.lng, schedule.radius)
        }
    }

    // Leggi posizione tornata dal MapSelectionScreen tramite SavedStateHandle
    val backStackEntry = (androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current as? androidx.navigation.NavBackStackEntry)
    val returnedLat = backStackEntry?.savedStateHandle?.get<Double>("selected_lat")
    val returnedLng = backStackEntry?.savedStateHandle?.get<Double>("selected_lng")
    val returnedRadius = backStackEntry?.savedStateHandle?.get<Float>("selected_radius")

    LaunchedEffect(returnedLat, returnedLng, returnedRadius) {
        if (returnedLat != null && returnedLng != null && returnedRadius != null) {
            vm.setSchedule(schedule.copy(
                lat = returnedLat,
                lng = returnedLng,
                radius = returnedRadius,
                type = ScheduleType.LOCATION
            ))
            // Pulisci per non ri-triggerare al recompose
            backStackEntry.savedStateHandle.remove<Double>("selected_lat")
            backStackEntry.savedStateHandle.remove<Double>("selected_lng")
            backStackEntry.savedStateHandle.remove<Float>("selected_radius")
        }
    }

    LaunchedEffect(saved) { if (saved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configura blocco", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Nome blocco
            OutlinedTextField(
                value         = blockName,
                onValueChange = { vm.setBlockName(it) },
                placeholder   = {
                    Text(
                        "Nome del blocco",
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                ),
                shape    = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // App selezionate
            FormRowNavigable(
                label   = "App da bloccare",
                value   = if (selectedPackages.isEmpty()) "" else "${selectedPackages.size} app selezionate",
                onClick = { onSelectApps(selectedPackages) }
            )

            // Regole orarie/limiti
            FormRowNavigable(
                label   = "Quando bloccare",
                value   = schedule.displayText(),
                onClick = {
                    if (schedule.type == ScheduleType.LOCATION) {
                        onSelectLocation(schedule.lat, schedule.lng, schedule.radius)
                    } else {
                        showWhenSheet = true
                    }
                }
            )

            // ── Reset to generic if needed ───────────────────────────────────
            if (schedule.type == ScheduleType.LOCATION) {
                Button(
                    onClick = { showWhenSheet = true },
                    colors = ButtonDefaults.textButtonColors(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cambia tipo di restrizione")
                }
            }

            // ── What to block ─────────────────────────────────────────────────
            WhatToBlockSection(config = whatConfig, onUpdate = { vm.setWhatConfig(it) })

            // Configurazione sblocco
            HowToUnblockSection(config = unlockConfig, onUpdate = { vm.setUnlockConfig(it) })

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { vm.saveBlock() },
                enabled  = blockName.isNotBlank() && selectedPackages.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salva blocco")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── ModalBottomSheet "When to block" ──────────────────────────────────────
    if (showWhenSheet) {
        val currentContext = LocalContext.current
        WhenToBlockSheet(
            schedule  = schedule,
            onDismiss = { showWhenSheet = false },
            onConfirm = { vm.setSchedule(it); showWhenSheet = false },
            onSelectLocation = {
                showWhenSheet = false
                if (PermissionManager.hasLocationPermission(currentContext)) {
                    onSelectLocation(schedule.lat, schedule.lng, schedule.radius)
                } else {
                    locationLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            },
            locationPermissionLauncher = locationLauncher
        )
    }
}

// When to block BottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhenToBlockSheet(
    schedule: ScheduleConfig,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleConfig) -> Unit,
    onSelectLocation: () -> Unit,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    var local by remember { mutableStateOf(schedule) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Quando bloccare",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Radio: No restriction
            ScheduleRadioRow(
                label    = "Senza restrizioni",
                selected = local.type == ScheduleType.NONE,
                onClick  = { local = local.copy(type = ScheduleType.NONE) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Radio: Time slot
            ScheduleRadioRow(
                label    = "Fascia oraria",
                selected = local.type == ScheduleType.TIME_SLOT,
                onClick  = { local = local.copy(type = ScheduleType.TIME_SLOT) }
            )
            if (local.type == ScheduleType.TIME_SLOT) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 40.dp, bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value         = local.startTime,
                        onValueChange = { local = local.copy(startTime = it) },
                        label         = { Text("Dalle") },
                        placeholder   = { Text("09:00") },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true
                    )
                    OutlinedTextField(
                        value         = local.endTime,
                        onValueChange = { local = local.copy(endTime = it) },
                        label         = { Text("Alle") },
                        placeholder   = { Text("17:00") },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Radio: Daily usage limit
            ScheduleRadioRow(
                label    = "Limite utilizzo giornaliero",
                selected = local.type == ScheduleType.DAILY_USAGE,
                onClick  = { local = local.copy(type = ScheduleType.DAILY_USAGE) }
            )
            if (local.type == ScheduleType.DAILY_USAGE) {
                // Genera i valori selezionabili con sensibilità variabile
                val selectableValues = remember {
                    val list = mutableListOf<Int>()
                    for (i in 1..10) list.add(i)               // 1..10 step 1
                    for (i in 15..30 step 5) list.add(i)       // 15..30 step 5
                    for (i in 40..60 step 10) list.add(i)      // 40..60 step 10
                    for (i in 75..480 step 15) list.add(i)     // 75..480 step 15
                    list
                }
                val currentIndex = selectableValues.indexOfFirst { it >= local.dailyUsageLimitMinutes }
                    .coerceAtLeast(0)

                Column(
                    modifier = Modifier.padding(start = 40.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Max ${local.dailyUsageLimitMinutes} min/giorno",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value         = currentIndex.toFloat(),
                        onValueChange = { index ->
                            val newValue = selectableValues[index.toInt()]
                            local = local.copy(dailyUsageLimitMinutes = newValue)
                        },
                        valueRange    = 0f..(selectableValues.size - 1).toFloat(),
                        steps         = selectableValues.size - 2,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Radio: Daily open count
            ScheduleRadioRow(
                label    = "Numero avvii giornalieri",
                selected = local.type == ScheduleType.DAILY_OPENS,
                onClick  = { local = local.copy(type = ScheduleType.DAILY_OPENS) }
            )
            if (local.type == ScheduleType.DAILY_OPENS) {
                Column(
                    modifier = Modifier.padding(start = 40.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Max ${local.dailyOpenCountLimit} avvvii/giorno",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value         = local.dailyOpenCountLimit.toFloat(),
                        onValueChange = { local = local.copy(dailyOpenCountLimit = it.toInt()) },
                        valueRange    = 1f..50f,
                        steps         = 48,   // 1..50 passo 1 → 48 step intermedi
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Radio: Location (Geofencing)
            val currentContext = LocalContext.current
            ScheduleRadioRow(
                label    = "Blocco in un'area (GPS)",
                selected = local.type == ScheduleType.LOCATION,
                onClick  = {
                    if (PermissionManager.hasLocationPermission(currentContext)) {
                        onConfirm(local.copy(type = ScheduleType.LOCATION))
                        onSelectLocation()
                    } else {
                        locationPermissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick  = { onConfirm(local) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// What to block

@Composable
private fun WhatToBlockSection(
    config: WhatConfig,
    onUpdate: (WhatConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Cosa bloccare",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected     = config.appStart,
                onClick      = { onUpdate(config.copy(appStart = !config.appStart)) },
                label        = { Text("Avvio app") },
                leadingIcon  = if (config.appStart) {
                    { Icon(Icons.Default.Block, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null
            )
            FilterChip(
                selected     = config.notifications,
                onClick      = { onUpdate(config.copy(notifications = !config.notifications)) },
                label        = { Text("Notifiche") },
                leadingIcon  = if (config.notifications) {
                    { Icon(Icons.Default.NotificationsOff, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null
            )
        }
    }
}

// How to unblock

@Composable
private fun HowToUnblockSection(
    config: UnlockConfig,
    onUpdate: (UnlockConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Come sbloccare",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Timer ────────────────────────────────────────────────────────────
        UnlockCard(
            icon    = Icons.Default.Timer,
            label   = "Timer",
            checked = config.timer,
            onToggle = { onUpdate(config.copy(timer = it)) }
        ) {
            if (config.timer) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Attendi ${config.timerMinutes} secondi prima dello sblocco",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value         = config.timerMinutes.toFloat(),
                        onValueChange = { onUpdate(config.copy(timerMinutes = it.toInt())) },
                        valueRange    = 5f..120f,
                        steps         = 22,   // (120-5)/5 - 1 = 22
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── QR Code ──────────────────────────────────────────────────────────
        UnlockCard(
            icon    = Icons.Default.QrCode,
            label   = "Codice QR",
            checked = config.qrCode,
            onToggle = { onUpdate(config.copy(qrCode = it)) }
        ) {
            if (config.qrCode) {
                Text(
                    "Un codice QR verrà generato al salvataggio. Scansionalo per sbloccare.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // ── PIN ──────────────────────────────────────────────────────────────
        UnlockCard(
            icon    = Icons.Default.Password,
            label   = "PIN",
            checked = config.pin,
            onToggle = { onUpdate(config.copy(pin = it)) }
        ) {
            if (config.pin) {
                OutlinedTextField(
                    value         = config.pinRaw,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() })
                            onUpdate(config.copy(pinRaw = it))
                    },
                    label               = { Text("PIN a 4 cifre") },
                    keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine          = true
                )
            }
        }

        // ── Biometric ────────────────────────────────────────────────────────
        UnlockCard(
            icon    = Icons.Default.Fingerprint,
            label   = "Biometrico",
            checked = config.biometric,
            onToggle = { onUpdate(config.copy(biometric = it)) }
        ) {
            if (config.biometric) {
                Text(
                    "Usa impronta digitale o riconoscimento facciale.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

// Componenti interni

/** Riga cliccabile con freccia, usata per "Selected apps" e "When to block". */
@Composable
private fun FormRowNavigable(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = value.ifBlank { "Seleziona…" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Riga radio per la selezione del tipo di orario. */
@Composable
private fun ScheduleRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Card toggle per i metodi di sblocco. [content] viene mostrato sotto quando abilitato. */
@Composable
private fun UnlockCard(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment  = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (checked) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        label,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Switch(checked = checked, onCheckedChange = onToggle)
            }
            content()
        }
    }
}
