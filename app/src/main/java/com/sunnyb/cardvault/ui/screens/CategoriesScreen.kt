package com.sunnyb.cardvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.sunnyb.cardvault.ui.theme.*
import com.sunnyb.cardvault.viewmodel.CategoriesViewModel

@Composable
fun CategoriesScreen(
    onCategoryClick: (Long) -> Unit = {},
    viewModel: CategoriesViewModel = viewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val error by viewModel.error.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newIcon by remember { mutableStateOf("📁") }
    val context = LocalContext.current

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Add Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newIcon,
                        onValueChange = { newIcon = it },
                        label = { Text("Icon (emoji)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCategory(newName.trim(), newIcon.trim())
                            newName = ""
                            newIcon = "📁"
                            showAddDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Add", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            text = "Categories",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(20.dp))

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📁", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No categories yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap + to create one",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            categories.forEach { cat ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onCategoryClick(cat.category.id) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = cat.category.icon ?: "📁", fontSize = 20.sp)
                            Text(
                                text = cat.category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${cat.cardCount} cards",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { showAddDialog = true }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("+", color = MaterialTheme.colorScheme.outline, fontSize = 20.sp)
                Text("Add Category", color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)