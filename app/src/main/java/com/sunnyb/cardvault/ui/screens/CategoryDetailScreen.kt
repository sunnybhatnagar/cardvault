package com.sunnyb.cardvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.ui.theme.*
import com.sunnyb.cardvault.viewmodel.CategoryDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: Long,
    onBack: () -> Unit,
    onCardClick: (Long) -> Unit,
    viewModel: CategoryDetailViewModel = viewModel()
) {
    val category by viewModel.category.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val error by viewModel.error.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editIcon by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(categoryId) {
        viewModel.loadCategory(categoryId)
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            viewModel.onDeleteComplete()
            onBack()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Delete Category") },
            text = { Text("Cards in this category will be uncategorized. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory()
                    showDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.secondary) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category?.name ?: "Category", fontWeight = FontWeight.Bold) },
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
        if (category == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editIcon,
                    onValueChange = { editIcon = it },
                    label = { Text("Icon (emoji)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.updateCategory(category!!.copy(name = editName, icon = editIcon))
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = editName.isNotBlank()
                ) { Text("💾 Save") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = category!!.icon ?: "📁", fontSize = 40.sp)
                    Column {
                        Text(category!!.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${cards.size} cards", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    editName = category!!.name
                    editIcon = category!!.icon ?: "📁"
                    isEditing = true
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Cards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))

            if (cards.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No cards in this category", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                cards.forEach { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onCardClick(card.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(card.nickname, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                if (card.issuer != null) {
                                    Text(card.issuer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("•••• ${card.cardNumber.takeLast(4)}", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
                Text("Delete Category")
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