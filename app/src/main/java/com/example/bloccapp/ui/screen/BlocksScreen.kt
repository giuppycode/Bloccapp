package com.example.bloccapp.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.data.db.BlockWithApps
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.ui.viewmodel.BlocksViewModel
import com.example.bloccapp.util.BiometricHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocksScreen(
    onAddBlock: () -> Unit,
    vm: BlocksViewModel = viewModel()
) {
    val allBlocks   by vm.allBlocks.collectAsStateWithLifecycle()
    
    val blocked    = allBlocks.filter { it.block.isEnabled }
    val notBlocked = allBlocks.filter { !it.block.isEnabled }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBlock) {
                Icon(Icons.Default.Add, contentDescription = "Nuovo blocco")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Titolo
            Text(
                text = "I tuoi Blocchi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (notBlocked.isNotEmpty()) {
                    item {
                        Text(
                            text  = "Non attivi",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(notBlocked) { bwa ->
                        BlockItem(
                            bwa      = bwa,
                            onToggle = { vm.toggleBlock(bwa.block) },
                            onDelete = { vm.deleteBlock(bwa.block) }
                        )
                    }
                }

                if (blocked.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = "Attivi",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(blocked) { bwa ->
                        BlockItem(
                            bwa      = bwa,
                            onToggle = { vm.toggleBlock(bwa.block) },
                            onDelete = { vm.deleteBlock(bwa.block) }
                        )
                    }
                }

                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
fun GamificationHeader(points: Int) {
    val safePoints = points.coerceAtLeast(0)
    val level = (safePoints / 100) + 1
    val expInLevel = safePoints % 100
    val progress = expInLevel / 100f

    val (nickname, levelIcon, colorScheme) = when {
        level >= 10 -> Triple("Leggenda", Icons.Default.WorkspacePremium, MaterialTheme.colorScheme.primary)
        level >= 8  -> Triple("Maestro", Icons.Default.MilitaryTech, MaterialTheme.colorScheme.secondary)
        level >= 5  -> Triple("Professionista", Icons.Default.Star, MaterialTheme.colorScheme.tertiary)
        level >= 3  -> Triple("Apprendista", Icons.Default.EmojiEvents, MaterialTheme.colorScheme.primary)
        else        -> Triple("Principiante", Icons.Default.EmojiEvents, MaterialTheme.colorScheme.outline)
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Esagono o Cerchio con l'icona del livello
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colorScheme.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = levelIcon,
                    contentDescription = null,
                    tint = colorScheme,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Livello $level",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme
                        )
                    }
                    
                    Text(
                        text = safePoints.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Barra di progresso stilizzata
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(colorScheme, colorScheme.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$expInLevel/100 XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Prossimo: LV ${level + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// App Icons

@Composable
private fun AppIconRow(packageNames: List<String>, modifier: Modifier = Modifier) {
    if (packageNames.isEmpty()) {
        Text(
            text  = "Nessuna app",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mostriamo al massimo le prime 6 icone
        packageNames.take(6).forEach { pkg ->
            AppSmallIcon(pkg)
        }
        if (packageNames.size > 6) {
            Text(
                text  = "+${packageNames.size - 6}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppSmallIcon(packageName: String) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap             = icon!!,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector        = Icons.Default.Android,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier           = Modifier.fillMaxSize().padding(2.dp)
            )
        }
    }
}

// Card Blocco

@Composable
private fun BlockItem(
    bwa: BlockWithApps,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val block: Block = bwa.block
    
    // Colori basati sullo stato
    val containerColor = if (block.isEnabled) 
        MaterialTheme.colorScheme.primaryContainer
    else 
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    
    val contentColor = if (block.isEnabled) 
        MaterialTheme.colorScheme.onPrimaryContainer 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant

    // Icona del tipo di blocco
    val scheduleIcon = when (block.scheduleType) {
        "TIME_SLOT"   -> Icons.Default.Schedule
        "LOCATION"    -> Icons.Default.Place
        "DAILY_USAGE" -> Icons.Default.Timer
        "DAILY_OPENS" -> Icons.Default.Update
        else          -> Icons.Default.NotificationsActive
    }

    // ── Stato sheet ──────────────────────────────────────────────────────────
    var showUnlockSheet  by remember { mutableStateOf(false) }
    var pendingAction: (() -> Unit)? by remember { mutableStateOf(null) }
    var pendingActionLabel by remember { mutableStateOf("") }

    // ── QR scanner launcher ──
    var qrVerifyCallback: ((String) -> Unit)? by remember { mutableStateOf(null) }
    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        qrVerifyCallback?.invoke(result.contents ?: "")
        qrVerifyCallback = null
    }

    fun launchQrScan(onResult: (String) -> Unit) {
        qrVerifyCallback = onResult
        qrLauncher.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scansiona il QR code di sblocco")
                setBeepEnabled(true)
                setOrientationLocked(true) // Locked to portrait from manifest
            }
        )
    }

    // Helper per sblocco
    fun requestVerified(label: String, blockToVerify: Block, action: () -> Unit) {
        if (blockToVerify.hasAnyUnlockMethod()) {
            pendingActionLabel = label
            pendingAction = action
            showUnlockSheet = true
        } else {
            action()
        }
    }

    Card(
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (block.isEnabled) 4.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                // Info blocco (Sinistra)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (block.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else contentColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = scheduleIcon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (block.isEnabled) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.6f)
                            )
                        }
                        Column {
                            Text(
                                text       = block.name,
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            val schedSummary = block.scheduleDisplay()
                            if (schedSummary.isNotBlank()) {
                                Text(
                                    text  = schedSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Azioni (Destra)
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked         = block.isEnabled,
                        onCheckedChange = { willBeEnabled ->
                            if (!willBeEnabled && block.isEnabled) {
                                requestVerified("Disabilita blocco", block, onToggle)
                            } else {
                                onToggle()
                            }
                        }
                    )
                    IconButton(
                        onClick = { requestVerified("Elimina blocco", block, onDelete) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconRow(
                    packageNames = bwa.apps.map { it.packageName },
                    modifier = Modifier.weight(1f)
                )
                
                AssistChip(
                    onClick = { /* Potrebbe mostrare i dettagli */ },
                    label = { 
                        Text(
                            "${bwa.apps.size} " + if (bwa.apps.size == 1) "app" else "apps",
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (block.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else contentColor.copy(alpha = 0.1f),
                        labelColor = contentColor
                    ),
                    border = null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            // Footer con i metodi di sblocco se presenti
            val unlockSummary = block.unlockMethodsDisplay()
            if (unlockSummary.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.alpha(0.15f),
                    color = contentColor
                )
                Spacer(Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "Sblocco: $unlockSummary",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Sheet sblocco
    if (showUnlockSheet) {
        UnlockToActionSheet(
            block       = block,
            actionLabel = pendingActionLabel,
            onDismiss   = { showUnlockSheet = false; pendingAction = null },
            onUnlocked  = {
                showUnlockSheet = false
                pendingAction?.invoke()
                pendingAction = null
            },
            onScanQr    = ::launchQrScan
        )
    }
}

// BottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlockToActionSheet(
    block: Block,
    actionLabel: String,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
    onScanQr: (onResult: (String) -> Unit) -> Unit
) {
    val context    = LocalContext.current
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text       = "Verifica per: $actionLabel",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "Sblocca tramite uno dei metodi configurati per \"${block.name}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Timer ────────────────────────────────────────────────────────
            if (block.unlockTimer) {
                TimerUnlockSection(
                    timerSecs  = block.unlockTimerSeconds,
                    onUnlocked = onUnlocked
                )
            }

            // ── QR Code ──────────────────────────────────────────────────────
            if (block.unlockQrCode && block.unlockQrSecret.isNotBlank()) {
                QrUnlockSection(
                    qrSecret   = block.unlockQrSecret,
                    context    = context,
                    onScanQr   = onScanQr,
                    onUnlocked = onUnlocked
                )
            }

            // ── PIN ──────────────────────────────────────────────────────────
            if (block.unlockPin && block.unlockPinHash.isNotBlank()) {
                PinUnlockSection(
                    pinHash    = block.unlockPinHash,
                    onUnlocked = onUnlocked
                )
            }

            // ── Biometrico ────────────────────────────────────────────────────
            if (block.unlockBiometric) {
                var errorMsg by remember { mutableStateOf<String?>(null) }

                UnlockMethodCard(icon = Icons.Default.Fingerprint, title = "Biometrico") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Usa la biometria configurata sul dispositivo per procedere.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (errorMsg != null) {
                            Text(
                                text  = errorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick  = {
                                errorMsg = null
                                val activity = context as? AppCompatActivity
                                if (activity != null) {
                                    BiometricHelper.showPrompt(
                                        activity  = activity,
                                        onSuccess = onUnlocked,
                                        onError   = { errorMsg = it }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Fingerprint, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Usa biometria")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// Sezione Timer

@Composable
private fun TimerUnlockSection(timerSecs: Int, onUnlocked: () -> Unit) {
    var timerStarted by remember { mutableStateOf(false) }
    var secondsLeft  by remember { mutableIntStateOf(timerSecs) }
    var timerDone    by remember { mutableStateOf(false) }

    LaunchedEffect(timerStarted) {
        if (!timerStarted) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        timerDone = true
    }

    UnlockMethodCard(icon = Icons.Default.Timer, title = "Timer") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                !timerStarted -> {
                    Text(
                        "Devi attendere $timerSecs secondi prima di poter procedere.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick  = { timerStarted = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Timer, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Avvia il timer ($timerSecs s)")
                    }
                }
                timerStarted && !timerDone -> {
                    Text(
                        "Attendi ancora $secondsLeft secondi…",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    Text(
                        "Timer completato.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick  = onUnlocked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Conferma")
                    }
                }
            }
        }
    }
}

// Sezione QR

@Composable
private fun QrUnlockSection(
    qrSecret: String,
    context: Context,
    onScanQr: (onResult: (String) -> Unit) -> Unit,
    onUnlocked: () -> Unit
) {
    var qrError      by remember { mutableStateOf(false) }
    var qrVerified   by remember { mutableStateOf(false) }

    // Genera il bitmap per la condivisione
    val qrBitmap = remember(qrSecret) { generateQrBitmap(qrSecret) }

    UnlockMethodCard(icon = Icons.Default.QrCode, title = "QR Code") {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (qrVerified) {
                Text(
                    "✅ QR Code verificato!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick  = onUnlocked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Conferma")
                }
            } else {
                Text(
                    "Inquadra il QR code di sblocco con la fotocamera.",
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                if (qrError) {
                    Text(
                        "❌ QR Code non valido. Riprova.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        qrError = false
                        onScanQr { scanned ->
                            if (scanned == qrSecret) {
                                qrVerified = true
                                qrError = false
                            } else {
                                qrError = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apri fotocamera")
                }

                // Bottone secondario: condividi il QR (per recuperarlo se perduto)
                if (qrBitmap != null) {
                    Button(
                        onClick  = { shareQrCode(context, qrBitmap) },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Condividi QR (recupero)")
                    }
                }
            }
        }
    }
}

// Sezione PIN

@Composable
private fun PinUnlockSection(pinHash: String, onUnlocked: () -> Unit) {
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    UnlockMethodCard(icon = Icons.Default.Password, title = "PIN") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value               = pinInput,
                onValueChange       = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                        pinInput = it
                        pinError = false
                    }
                },
                label               = { Text("Inserisci PIN") },
                keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                isError             = pinError,
                supportingText      = if (pinError) { { Text("PIN errato") } } else null,
                modifier            = Modifier.fillMaxWidth(),
                singleLine          = true
            )
            Button(
                onClick  = {
                    if (sha256(pinInput) == pinHash) onUnlocked()
                    else pinError = true
                },
                enabled  = pinInput.length == 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Conferma PIN")
            }
        }
    }
}

// Card Wrapper

@Composable
private fun UnlockMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

// Utils

private fun Block.hasAnyUnlockMethod(): Boolean =
    unlockTimer || unlockQrCode || unlockPin || unlockBiometric

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap? = try {
    val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) for (y in 0 until size)
        bmp.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
    bmp
} catch (e: Exception) { null }

private fun shareQrCode(context: Context, bitmap: Bitmap) {
    try {
        val file = File(context.cacheDir, "qr_unlock.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Condividi QR Code"))
    } catch (e: Exception) {
        // Fallback silenzioso
    }
}

private fun sha256(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
}
