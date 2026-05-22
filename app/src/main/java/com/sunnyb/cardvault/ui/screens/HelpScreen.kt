package com.sunnyb.cardvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sunnyb.cardvault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Section("What is Card Vault?") {
                Bullet("Securely stores photos of your credit/debit cards")
                Bullet("All data encrypted on-device — nothing leaves your phone")
                Bullet("Access via biometric lock (fingerprint / face / PIN)")
            }

            Section("Adding a Card") {
                Bullet("Tap + on the home screen to add a new card")
                Bullet("Step 1: Take or import a photo of the front of your card")
                Bullet("Step 2: Take or import a photo of the back (CVV side)")
                Bullet("Step 3: Fill in nickname, card number, expiry, CVV, category")
                Bullet("Card number auto-scans from the front photo via OCR")
                Bullet("Card number is validated using the Luhn algorithm")
            }

            Section("Viewing Cards") {
                Bullet("Cards appear as tiles on the home screen")
                Bullet("Tap a card to view full details — number, expiry, CVV")
                Bullet("Use the eye icon to toggle between masked and visible")
                Bullet("Tap the card graphic to flip and see the back photo")
            }

            Section("Editing & Deleting") {
                Bullet("Tap Edit on the detail screen to modify card info")
                Bullet("Tap Delete to remove a card (with confirmation)")
            }

            Section("Categories") {
                Bullet("Organize cards into categories (Personal, Work, etc.)")
                Bullet("Add, edit, or delete categories from the Categories tab")
                Bullet("Tap a category to see all cards in it")
            }

            Section("Search") {
                Bullet("Use the search bar on the home screen to find cards")
                Bullet("Searches by nickname, issuer, and card number")
            }

            Section("Security") {
                Bullet("App is locked with biometric authentication on launch")
                Bullet("Auto-locks after a configurable timeout in Settings")
                Bullet("Card images are encrypted with AES-256-GCM on-device")
                Bullet("Database encrypted with SQLCipher using AES-256 key")
                Bullet("Encryption keys stored in Android Keystore")
            }

            Section("Backup & Restore") {
                Bullet("Export a JSON backup from Settings to share or save")
                Bullet("Restore from a backup file using the Restore option")
                Bullet("Backup includes card numbers, CVV — share securely")
            }

            Section("Privacy") {
                Bullet("No accounts, no sign-up, no cloud by default")
                Bullet("All data stays on your device")
                Bullet("No analytics, no tracking, no internet access needed")
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ColumnScope.Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("•  ", color = MaterialTheme.colorScheme.outline)
        Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}