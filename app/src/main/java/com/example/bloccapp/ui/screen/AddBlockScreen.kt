package com.example.bloccapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.PermissionManager
import com.example.bloccapp.data.model.ScheduleConfig
import com.example.bloccapp.data.model.ScheduleType
import com.example.bloccapp.data.model.UnlockConfig
import com.example.bloccapp.data.model.WhatConfig
import com.example.bloccapp.ui.viewmodel.AddBlockViewModel
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
                title = { Text("Configura Blocco", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Nome blocco con icona
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                OutlinedTextField(
                    value         = blockName,
                    onValueChange = { vm.setBlockName(it) },
                    label = { Text("Nome del blocco") },
                    placeholder   = { Text("Esempio: Social Network") },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    shape    = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Sezione Selezione
            FormSection(title = "Impostazioni", icon = Icons.Default.Android) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormRowNavigable(
                        label   = "App da bloccare",
                        value   = if (selectedPackages.isEmpty()) "" else "${selectedPackages.size} app selezionate",
                        icon    = Icons.Default.Android,
                        onClick = { onSelectApps(selectedPackages) }
                    )

                    FormRowNavigable(
                        label   = "Quando bloccare",
                        value   = schedule.displayText(),
                        icon    = when (schedule.type) {
                            ScheduleType.TIME_SLOT -> Icons.Default.Schedule
                            ScheduleType.LOCATION -> Icons.Default.Place
                            ScheduleType.DAILY_USAGE -> Icons.Default.Timer
                            ScheduleType.DAILY_OPENS -> Icons.Default.Update
                            else -> Icons.Default.NotificationsActive
                        },
                        onClick = {
                            if (schedule.type == ScheduleType.LOCATION) {
                                onSelectLocation(schedule.lat, schedule.lng, schedule.radius)
                            } else {
                                showWhenSheet = true
                            }
                        }
                    )
                }
            }

            // ── Reset to generic if needed ───────────────────────────────────
            if (schedule.type == ScheduleType.LOCATION) {
                TextButton(
                    onClick = { showWhenSheet = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Update, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cambia tipo di restrizione")
                }
            }

            // ── What to block ─────────────────────────────────────────────────
            FormSection(title = "Cosa bloccare", icon = Icons.Default.Block) {
                WhatToBlockSection(config = whatConfig, onUpdate = { vm.setWhatConfig(it) })
            }

            // Configurazione sblocco
            FormSection(title = "Metodi di sblocco", icon = Icons.Default.Fingerprint) {
                HowToUnblockSection(config = unlockConfig, onUpdate = { vm.setUnlockConfig(it) })
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick  = { vm.saveBlock() },
                enabled  = blockName.isNotBlank() && selectedPackages.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Salva Blocco", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected     = config.appStart,
                onClick      = { onUpdate(config.copy(appStart = !config.appStart)) },
                label        = { Text("Avvio app") },
                leadingIcon  = if (config.appStart) {
                    { Icon(Icons.Default.Block, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected     = config.notifications,
                onClick      = { onUpdate(config.copy(notifications = !config.notifications)) },
                label        = { Text("Notifiche") },
                leadingIcon  = if (config.notifications) {
                    { Icon(Icons.Default.NotificationsOff, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Timer ────────────────────────────────────────────────────────────
        UnlockCard(
            icon    = Icons.Default.Timer,
            label   = "Timer di attesa",
            checked = config.timer,
            onToggle = { isChecked ->
                onUpdate(if (isChecked) {
                    config.copy(timer = true, qrCode = false, pin = false, biometric = false)
                } else {
                    config.copy(timer = false)
                })
            }
        ) {
            if (config.timer) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
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
                        steps         = 22,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── QR Code ──────────────────────────────────────────────────────────
        UnlockCard(
            icon    = Icons.Default.QrCode,
            label   = "Scansione QR",
            checked = config.qrCode,
            onToggle = { isChecked ->
                onUpdate(if (isChecked) {
                    config.copy(qrCode = true, timer = false, pin = false, biometric = false)
                } else {
                    config.copy(qrCode = false)
                })
            }
        ) {
            if (config.qrCode) {
                Text(
                    "Un codice QR verrà generato al salvataggio. Dovrai scansionarlo per sbloccare.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // ── PIN ──────────────────────────────────────────────────────────────
        UnlockCard(
            icon    = Icons.Default.Password,
            label   = "Codice PIN",
            checked = config.pin,
            onToggle = { isChecked ->
                onUpdate(if (isChecked) {
                    config.copy(pin = true, timer = false, qrCode = false, biometric = false)
                } else {
                    config.copy(pin = false)
                })
            }
        ) {
            if (config.pin) {
                OutlinedTextField(
                    value         = config.pinRaw,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() })
                            onUpdate(config.copy(pinRaw = it))
                    },
                    label               = { Text("Inserisci PIN a 4 cifre") },
                    keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine          = true
                )
            }
        }

        // ── Biometric ────────────────────────────────────────────────────────
        UnlockCard(
            icon    = Icons.Default.Fingerprint,
            label   = "Biometrico",
            checked = config.biometric,
            onToggle = { isChecked ->
                onUpdate(if (isChecked) {
                    config.copy(biometric = true, timer = false, qrCode = false, pin = false)
                } else {
                    config.copy(biometric = false)
                })
            }
        ) {
            if (config.biometric) {
                Text(
                    "Usa l'impronta digitale o il volto configurati sul dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

// Componenti interni

@Composable
private fun FormSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

/** Riga cliccabile con freccia, usata per "Selected apps" e "When to block". */
@Composable
private fun FormRowNavigable(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = value.ifBlank { "Seleziona…" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value.isBlank())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
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
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment  = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            modifier = Modifier.size(18.dp),
                            tint = if (checked) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        label,
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (checked) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = checked, 
                    onCheckedChange = onToggle,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            content()
        }
    }
}
