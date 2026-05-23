package com.sunnyb.cardvault.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunnyb.cardvault.ui.theme.*
import com.sunnyb.cardvault.util.DriveBackupService
import com.sunnyb.cardvault.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onHelpClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    val currentTimeoutMs by viewModel.currentTimeoutMs.collectAsState()
    var timeoutExpanded by remember { mutableStateOf(false) }

    val currentLabel = SettingsViewModel.TIMEOUT_OPTIONS
        .find { it.ms == currentTimeoutMs }?.label ?: "30 seconds"

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.exportBackupToUri(context, it) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.parseBackupFile(context, it) }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onDriveSignedIn()
        } else {
            viewModel.resetDriveState()
        }
    }

    val driveState by viewModel.driveState.collectAsState()
    val driveBackups by viewModel.driveBackups.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    var showChangelog by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadTheme(context) }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is SettingsViewModel.ExportState.Success -> {
                Toast.makeText(context, "Backup exported", Toast.LENGTH_SHORT).show()
                viewModel.resetExportState()
            }
            is SettingsViewModel.ExportState.Error -> {
                Toast.makeText(context, "Export failed: ${state.message}", Toast.LENGTH_SHORT).show()
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    LaunchedEffect(restoreState) {
        when (val state = restoreState) {
            is SettingsViewModel.RestoreState.Success -> {
                Toast.makeText(context, "Restore complete", Toast.LENGTH_SHORT).show()
                viewModel.resetRestoreState()
            }
            is SettingsViewModel.RestoreState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetRestoreState()
            }
            else -> {}
        }
    }

    LaunchedEffect(driveState) {
        when (val state = driveState) {
            is SettingsViewModel.DriveState.Success -> {
                Toast.makeText(context, "Drive operation complete", Toast.LENGTH_SHORT).show()
                viewModel.resetDriveState()
            }
            is SettingsViewModel.DriveState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetDriveState()
            }
            else -> {}
        }
    }

    if (restoreState is SettingsViewModel.RestoreState.PendingConfirmation) {
        val count = (restoreState as SettingsViewModel.RestoreState.PendingConfirmation).cardCount
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestore() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Restore Backup") },
            text = { Text("This will replace all $count existing cards. Continue?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }) { Text("Restore", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRestore() }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(24.dp))

        SettingsCard {
            Column {
                Text("Auto-lock", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Lock app after inactivity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedButton(
                        onClick = { timeoutExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(currentLabel) }
                    DropdownMenu(
                        expanded = timeoutExpanded,
                        onDismissRequest = { timeoutExpanded = false }
                    ) {
                        SettingsViewModel.TIMEOUT_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        color = if (option.ms == currentTimeoutMs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.setLockTimeout(option.ms)
                                    timeoutExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = onAboutClick) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("About Card Vault", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Version 1.0 · Developer · Legal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Info, contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = { showPrivacy = true }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Privacy Policy", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("How your data is handled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Security, contentDescription = "Privacy", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = { viewModel.toggleTheme(context) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("App Theme", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(if (themeMode == com.sunnyb.cardvault.ui.theme.ThemeMode.DARK) "Dark mode" else "Light mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = themeMode == com.sunnyb.cardvault.ui.theme.ThemeMode.DARK,
                    onCheckedChange = { viewModel.toggleTheme(context) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = { exportLauncher.launch("cardvault_backup.dat") }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Export Backup", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (exportState is SettingsViewModel.ExportState.Exporting) {
                        Text("Exporting...", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Save encrypted data to your device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (exportState is SettingsViewModel.ExportState.Exporting) {
                    Text("⏳", style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(Icons.Default.Upload, contentDescription = "Export", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = {
            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
        }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Restore from Backup", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Import cards from a backup file",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (restoreState is SettingsViewModel.RestoreState.Restoring) {
                    Text("⏳", style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(Icons.Default.Download, contentDescription = "Restore", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = {
            if (DriveBackupService.isSignedIn(context)) {
                viewModel.backupToDrive(context)
            } else {
                signInLauncher.launch(DriveBackupService.getSignInIntent(context))
            }
        }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Backup to Google Drive", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    val sub = when (driveState) {
                        is SettingsViewModel.DriveState.Idle -> "Sync your cards to the cloud"
                        is SettingsViewModel.DriveState.SigningIn -> "Signing in..."
                        is SettingsViewModel.DriveState.BackingUp -> "Uploading..."
                        else -> "Sync your cards to the cloud"
                    }
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (driveState is SettingsViewModel.DriveState.BackingUp) {
                    Text("⏳", style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(
                        if (DriveBackupService.isSignedIn(context)) Icons.Default.Cloud else Icons.Default.Link,
                        contentDescription = if (DriveBackupService.isSignedIn(context)) "Drive" else "Sign in",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = {
            if (DriveBackupService.isSignedIn(context)) {
                viewModel.listDriveBackups(context)
            } else {
                signInLauncher.launch(DriveBackupService.getSignInIntent(context))
            }
        }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Restore from Google Drive", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    val sub = when (driveState) {
                        is SettingsViewModel.DriveState.SigningIn -> "Signing in..."
                        is SettingsViewModel.DriveState.ListingBackups -> "Loading backups..."
                        is SettingsViewModel.DriveState.Restoring -> "Restoring..."
                        else -> "Import cards from cloud backup"
                    }
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (driveState is SettingsViewModel.DriveState.Restoring) {
                    Text("⏳", style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(
                        if (DriveBackupService.isSignedIn(context)) Icons.Default.Cloud else Icons.Default.Link,
                        contentDescription = if (DriveBackupService.isSignedIn(context)) "Drive" else "Sign in",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (driveBackups.isNotEmpty() && driveState is SettingsViewModel.DriveState.ListingBackups) {
            AlertDialog(
                onDismissRequest = { viewModel.resetDriveState() },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = { Text("Select Backup") },
                text = {
                    Column {
                        driveBackups.forEach { file ->
                            TextButton(onClick = {
                                viewModel.restoreFromDrive(context, file.id)
                            }) {
                                Text(file.name ?: "Backup", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetDriveState() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = onHelpClick) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Help Guide", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Learn how to use the app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = { showChangelog = true }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("What's New", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("See what shipped in this version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Info, contentDescription = "What's New", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (showPrivacy) {
            AlertDialog(
                onDismissRequest = { showPrivacy = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = { Text("Privacy Policy") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        listOf(
                            "Card Vault does not collect, store, or transmit any personal data. All information stays on your device.",
                            "",
                            "DATA STORAGE",
                            "• Card information is stored only on your device",
                            "• Database encrypted with AES-256 via SQLCipher",
                            "• Images encrypted with AES-256-GCM",
                            "• Keys stored in Android Keystore",
                            "• No data sent to any server",
                            "",
                            "PERMISSIONS",
                            "• Camera: taking photos of credit/debit cards",
                            "• Notifications: alerting when a card nears expiry",
                            "• Biometric: unlocking the app",
                            "Photos captured only when you tap capture.",
                            "",
                            "THIRD-PARTY",
                            "• ML Kit for on-device OCR (offline)",
                            "• Drive API only on explicit sign-in",
                            "",
                            "Last updated: May 22, 2026"
                        ).forEach { line ->
                            Text(
                                text = line,
                                color = if (line.startsWith("•") || line.startsWith("Last") || line.startsWith("Photos"))
                                    MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacy = false }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
                }
            )
        }

        if (showChangelog) {
            AlertDialog(
                onDismissRequest = { showChangelog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = { Text("What's New") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("v1.3 — May 2026", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("•  OCR accuracy improvements — embossed card numbers read correctly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Expiry date auto-fill fix for OCR-read cards",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Two-pass OCR parsing for reliable card number detection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))

                        Text("v1.2 — May 2026", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("•  Grid/List view toggle on Home screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Compact card row layout for list mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Card count indicator in toggle area",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))

                        Text("v1.1 — May 2026", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("•  Error handling and crash prevention improvements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Card image compression (smaller storage, faster loading)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Shimmer loading animation while cards load",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Screenshot blocking on all screens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Camera temp files cleaned up automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  OCR failure messages with manual entry guidance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Decrypted image cache for smoother card flipping",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Drive backup reliability improvements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Room migration testing framework",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))

                        Text("v1.0 — Initial Release", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("•  Card flip animation", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Expiry notifications", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Configurable auto-lock timeout", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  OCR card number scanning", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Category management", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Google Drive backup (optional)", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Encrypted backup & restore", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Luhn validation", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("•  Permission rationale dialogs", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChangelog = false }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
                }
            )
        }

    }
}

@Composable
private fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}