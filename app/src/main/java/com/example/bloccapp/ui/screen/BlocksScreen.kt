package com.example.bloccapp.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloccapp.data.db.BlockWithApps
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.ui.viewmodel.BlocksViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocksScreen(
    onAddBlock: () -> Unit,
    vm: BlocksViewModel = viewModel()
) {
    val allBlocks by vm.allBlocks.collectAsStateWithLifecycle()
    val blocked    = allBlocks.filter { it.block.isEnabled }
    val notBlocked = allBlocks.filter { !it.block.isEnabled }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Blocks", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBlock) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi blocco")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Not blocked ─────────────────────────────────────────────────
            if (notBlocked.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "Not blocked",
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

            // ── Blocked ─────────────────────────────────────────────────────
            if (blocked.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text  = "Blocked",
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

            item { Spacer(Modifier.height(88.dp)) } // spazio per FAB
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BlockItem card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BlockItem(
    bwa: BlockWithApps,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val block: Block = bwa.block
    val color = if (block.isEnabled)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    var showPauseSheet by remember { mutableStateOf(false) }

    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // ── Info ────────────────────────────────────────────────────
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = block.name,
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    val appCount = bwa.apps.size
                    Text(
                        text  = if (appCount == 0) "Nessuna app" else "$appCount app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val schedSummary = block.scheduleDisplay()
                    if (schedSummary.isNotBlank()) {
                        Text(
                            text  = schedSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val unlockSummary = block.unlockMethodsDisplay()
                    if (unlockSummary.isNotBlank()) {
                        Text(
                            text  = unlockSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Actions ──────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Bottone "Pause/Unlock" (visibile solo se abilitato e con metodi configurati)
                    if (block.isEnabled && block.hasAnyUnlockMethod()) {
                        IconButton(onClick = { showPauseSheet = true }) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Sblocca",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked         = block.isEnabled,
                        onCheckedChange = { onToggle() }
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // ── PauseBlockSheet ──────────────────────────────────────────────────────
    if (showPauseSheet) {
        PauseBlockSheet(
            block      = block,
            onDismiss  = { showPauseSheet = false },
            onUnlocked = { showPauseSheet = false; onToggle() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pause/Unlock BottomSheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PauseBlockSheet(
    block: Block,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
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
                "Pausa il blocco \"${block.name}\"",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // ── Timer ────────────────────────────────────────────────────────
            if (block.unlockTimer) {
                UnlockMethodCard(icon = Icons.Default.Timer, title = "Timer") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Devi attendere ${block.unlockTimerSeconds} minuti prima di poter sbloccare questa app.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick  = onUnlocked,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Avvia il timer e disabilita blocco")
                        }
                    }
                }
            }

            // ── QR Code ──────────────────────────────────────────────────────
            if (block.unlockQrCode && block.unlockQrSecret.isNotBlank()) {
                val qrBitmap = remember(block.unlockQrSecret) {
                    generateQrBitmap(block.unlockQrSecret)
                }
                UnlockMethodCard(icon = Icons.Default.QrCode, title = "QR Code") {
                    Column(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap            = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code di sblocco",
                                modifier           = Modifier.size(200.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick  = { shareQrCode(context, qrBitmap) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Condividi")
                                }
                                Button(
                                    onClick  = onUnlocked,
                                    modifier = Modifier.weight(1f),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Ho scansionato")
                                }
                            }
                        } else {
                            Text(
                                "Errore nella generazione del QR code.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ── PIN ──────────────────────────────────────────────────────────
            if (block.unlockPin && block.unlockPinHash.isNotBlank()) {
                var pinInput    by remember { mutableStateOf("") }
                var pinError    by remember { mutableStateOf(false) }

                UnlockMethodCard(icon = Icons.Default.Password, title = "PIN") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value               = pinInput,
                            onValueChange       = {
                                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                    pinInput  = it
                                    pinError  = false
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
                            onClick = {
                                if (sha256(pinInput) == block.unlockPinHash) {
                                    onUnlocked()
                                } else {
                                    pinError = true
                                }
                            },
                            enabled  = pinInput.length == 4,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Conferma PIN")
                        }
                    }
                }
            }

            // ── Biometric ────────────────────────────────────────────────────
            if (block.unlockBiometric) {
                UnlockMethodCard(icon = Icons.Default.Fingerprint, title = "Biometrico") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Autenticazione biometrica disponibile nella Fase 5.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick  = onUnlocked,
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

// ─────────────────────────────────────────────────────────────────────────────
// Componenti interni
// ─────────────────────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────────────────────
// Utility functions
// ─────────────────────────────────────────────────────────────────────────────

private fun Block.hasAnyUnlockMethod(): Boolean =
    unlockTimer || unlockQrCode || unlockPin || unlockBiometric

/** Genera un [Bitmap] quadrato con il QR code del contenuto fornito usando ZXing. */
private fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}

/** Condivide il QR code come immagine PNG tramite un Intent di sistema. */
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

/** Hash SHA-256 usato per la verifica del PIN. */
private fun sha256(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
}
