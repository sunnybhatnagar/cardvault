package com.sunnyb.cardvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sunnyb.cardvault.ui.theme.*

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(Modifier.height(24.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Biometric Lock", style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary)
                    Text("Require fingerprint to open app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary)
                }
                var enabled by remember { mutableStateOf(true) }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Export Backup", style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary)
                    Text("Save encrypted data to your device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary)
                }
                Text("📤", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column {
                Text("Card Vault", style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary)
                Text("Version 1.0", style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("encrypted · private · yours",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted)
            }
        }
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
