package com.example.bloccapp.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bloccapp.data.db.entity.Block
import com.example.bloccapp.service.BlockingState
import com.example.bloccapp.ui.theme.BloccappTheme
import com.example.bloccapp.util.BiometricHelper
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay
import java.security.MessageDigest

/**
 * Activity a tutto schermo mostrata quando l'utente apre un'app bloccata.
 *
 * Viene lanciata da [com.example.bloccapp.service.BlockingService] tramite
 * un Intent con FLAG_ACTIVITY_NEW_TASK.
 *
 * Non fa parte del grafo di navigazione Compose — è un'Activity standalone.
 */
class BlockedAppActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BlockedAppActivity"

        private const val EXTRA_BLOCKED_PKG       = "blocked_pkg"
        private const val EXTRA_BLOCK_ID          = "block_id"
        private const val EXTRA_BLOCK_NAME        = "block_name"
        private const val EXTRA_UNLOCK_TIMER      = "unlock_timer"
        private const val EXTRA_UNLOCK_TIMER_SECS = "unlock_timer_secs"
        private const val EXTRA_UNLOCK_PIN        = "unlock_pin"
        private const val EXTRA_UNLOCK_PIN_HASH   = "unlock_pin_hash"
        private const val EXTRA_UNLOCK_QR         = "unlock_qr"
        private const val EXTRA_UNLOCK_QR_SECRET  = "unlock_qr_secret"
        private const val EXTRA_UNLOCK_BIOMETRIC   = "unlock_biometric"

        /** Costruisce l'intent con tutti gli extra necessari. */
        fun buildIntent(context: Context, packageName: String, block: Block): Intent =
            Intent(context, BlockedAppActivity::class.java).apply {
                putExtra(EXTRA_BLOCKED_PKG,       packageName)
                putExtra(EXTRA_BLOCK_ID,          block.id)
                putExtra(EXTRA_BLOCK_NAME,        block.name)
                putExtra(EXTRA_UNLOCK_TIMER,      block.unlockTimer)
                putExtra(EXTRA_UNLOCK_TIMER_SECS, block.unlockTimerSeconds)
                putExtra(EXTRA_UNLOCK_PIN,        block.unlockPin)
                putExtra(EXTRA_UNLOCK_PIN_HASH,   block.unlockPinHash)
                putExtra(EXTRA_UNLOCK_QR,         block.unlockQrCode)
                putExtra(EXTRA_UNLOCK_QR_SECRET,  block.unlockQrSecret)
                putExtra(EXTRA_UNLOCK_BIOMETRIC,   block.unlockBiometric)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedPkg      = intent.getStringExtra(EXTRA_BLOCKED_PKG)       ?: ""
        val blockName       = intent.getStringExtra(EXTRA_BLOCK_NAME)         ?: ""
        val unlockTimer     = intent.getBooleanExtra(EXTRA_UNLOCK_TIMER,      false)
        val unlockTimerSecs = intent.getIntExtra(EXTRA_UNLOCK_TIMER_SECS,     30)
        val unlockPin       = intent.getBooleanExtra(EXTRA_UNLOCK_PIN,        false)
        val unlockPinHash   = intent.getStringExtra(EXTRA_UNLOCK_PIN_HASH)    ?: ""
        val unlockQr        = intent.getBooleanExtra(EXTRA_UNLOCK_QR,         false)
        val unlockQrSecret  = intent.getStringExtra(EXTRA_UNLOCK_QR_SECRET)   ?: ""
        val unlockBiometric = intent.getBooleanExtra(EXTRA_UNLOCK_BIOMETRIC,   false)

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(blockedPkg, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            blockedPkg
        }

        Log.d(TAG, "Showing blocked screen for $blockedPkg (block: $blockName)")

        setContent {
            BloccappTheme {
                BlockedAppScreen(
                    appName         = appName,
                    blockedPkg      = blockedPkg,
                    blockName       = blockName,
                    unlockTimer     = unlockTimer,
                    unlockTimerSecs = unlockTimerSecs,
                    unlockPin       = unlockPin,
                    unlockPinHash   = unlockPinHash,
                    unlockQr        = unlockQr,
                    unlockQrSecret  = unlockQrSecret,
                    unlockBiometric = unlockBiometric,
                    onUnlocked      = {
                        BlockingState.grantTemporaryUnlock(blockedPkg)
                        Log.d(TAG, "Temporary unlock granted for $blockedPkg")

                        // Rilancia l'app sbloccata per assicurarci di tornarci sopra
                        try {
                            val launchIntent = packageManager.getLaunchIntentForPackage(blockedPkg)
                            if (launchIntent != null) {
                                startActivity(launchIntent)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to relaunch $blockedPkg", e)
                        }

                        finish()
                    },
                    onGoHome        = {
                        startActivity(
                            Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BlockedAppScreen(
    appName: String,
    @Suppress("UNUSED_PARAMETER") blockedPkg: String,
    blockName: String,
    unlockTimer: Boolean,
    unlockTimerSecs: Int,
    unlockPin: Boolean,
    unlockPinHash: String,
    unlockQr: Boolean,
    unlockQrSecret: String,
    unlockBiometric: Boolean,
    onUnlocked: () -> Unit,
    onGoHome: () -> Unit
) {
    val hasAnyUnlock = unlockTimer || unlockPin || unlockQr || unlockBiometric

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // ── App blocked icon ─────────────────────────────────────────────
            Surface(
                shape  = CircleShape,
                color  = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Block,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                    modifier           = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                )
            }

            // ── Header ───────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = appName,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
                Text(
                    text  = "è bloccata da \"$blockName\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // ── Unlock methods ───────────────────────────────────────────────
            if (hasAnyUnlock) {
                Text(
                    text       = "Per continuare, sblocca tramite:",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (unlockTimer) {
                TimerUnlockCard(
                    timerSecs  = unlockTimerSecs,
                    onUnlocked = onUnlocked
                )
            }

            if (unlockPin && unlockPinHash.isNotBlank()) {
                PinUnlockCard(
                    pinHash    = unlockPinHash,
                    onUnlocked = onUnlocked
                )
            }

            if (unlockQr && unlockQrSecret.isNotBlank()) {
                QrUnlockCard(
                    qrSecret   = unlockQrSecret,
                    onUnlocked = onUnlocked
                )
            }

            if (unlockBiometric) {
                BiometricUnlockCard(onUnlocked = onUnlocked)
            }

            // ── Go home button ───────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onGoHome,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Torna alla home")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Timer unlock
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimerUnlockCard(timerSecs: Int, onUnlocked: () -> Unit) {
    var secondsLeft  by remember { mutableIntStateOf(timerSecs) }
    var timerStarted by remember { mutableStateOf(false) }
    var timerDone    by remember { mutableStateOf(false) }

    // Countdown once started
    LaunchedEffect(timerStarted) {
        if (!timerStarted) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        timerDone = true
    }

    UnlockCard(icon = Icons.Default.Timer, title = "Timer di attesa") {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (timerStarted && !timerDone) {
                Text("Attendi ancora $secondsLeft secondi…", style = MaterialTheme.typography.bodyMedium)
                CircularProgressIndicator(
                    progress        = { secondsLeft.toFloat() / timerSecs.toFloat() },
                    modifier        = Modifier.size(64.dp),
                    strokeWidth     = 6.dp
                )
            } else if (!timerStarted) {
                Text(
                    "Devi attendere $timerSecs secondi prima di poter aprire questa app.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick  = { timerStarted = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Timer, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Avvia il timer")
                }
            } else {
                // Timer done
                Text("Tempo scaduto — puoi aprire l'app.", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick  = onUnlocked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LockOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apri per 5 minuti")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PIN unlock
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinUnlockCard(pinHash: String, onUnlocked: () -> Unit) {
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    UnlockCard(icon = Icons.Default.Password, title = "PIN") {
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
                Text("Conferma PIN (per 5 min)")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR unlock
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrUnlockCard(qrSecret: String, onUnlocked: () -> Unit) {
    var qrError by remember { mutableStateOf(false) }
    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents == qrSecret) {
            onUnlocked()
        } else if (result.contents != null) {
            qrError = true
        }
    }

    UnlockCard(icon = Icons.Default.QrCode, title = "QR Code") {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Inquadra il QR code di sblocco con la fotocamera.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (qrError) {
                Text(
                    "QR code non valido. Riprova.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    qrError = false
                    qrLauncher.launch(ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("Scansiona il codice di sblocco")
                        setBeepEnabled(true)
                        setOrientationLocked(false)
                    })
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Apri fotocamera (per 5 min)")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Biometric unlock
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BiometricUnlockCard(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var errorMsg by remember { mutableStateOf<String?>(null) }

    UnlockCard(icon = Icons.Default.Fingerprint, title = "Biometrico") {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Usa l'impronta digitale o il riconoscimento facciale per sbloccare l'app.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (errorMsg != null) {
                Text(
                    text  = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
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
                Text("Sblocca ora (per 5 min)")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared card wrapper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UnlockCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
// Utilities
// ─────────────────────────────────────────────────────────────────────────────

private fun sha256(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
}
