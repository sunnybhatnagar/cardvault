package com.sunnyb.cardvault.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.view.WindowManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunnyb.cardvault.ui.components.CardFrontView
import com.sunnyb.cardvault.viewmodel.CardDetailViewModel
import com.sunnyb.cardvault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: CardDetailViewModel = viewModel()
) {
    val card by viewModel.card.collectAsState()
    val categoryName by viewModel.categoryName.collectAsState()
    val backImageBitmap by viewModel.backImageBitmap.collectAsState()
    var showCardNumber by remember { mutableStateOf(false) }
    var showCvv by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        val window = (view.context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400)
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Delete Card") },
            text = { Text("This cannot be undone. Delete ${card?.nickname ?: "this card"}?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteCard()
                    onBack()
                }) { Text("Delete", color = NeonMagenta) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = card?.nickname ?: "Card Detail",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (card == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonCyan)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .graphicsLayer {
                        rotationY = flipRotation
                        cameraDistance = 12f * density.density
                    }
                    .clickable {
                        if (card!!.backImagePath != null) {
                            if (!isFlipped) viewModel.loadBackImage()
                            isFlipped = !isFlipped
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (flipRotation <= 90f) {
                    CardFrontView(
                        card = card!!,
                        modifier = Modifier.graphicsLayer { rotationY = flipRotation }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .graphicsLayer { rotationY = 180f - flipRotation },
                        contentAlignment = Alignment.Center
                    ) {
                        if (backImageBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = backImageBitmap!!.asImageBitmap(),
                                contentDescription = "Back of card",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = if (card!!.backImagePath != null) "Loading..." else "No back image",
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = DarkSurface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                DetailRowWithToggle(
                    label = "Card Number",
                    masked = "•••• •••• •••• ${card!!.cardNumber.takeLast(4)}",
                    revealed = card!!.cardNumber.chunked(4).joinToString(" "),
                    showValue = showCardNumber,
                    onToggle = { showCardNumber = !showCardNumber }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
                    color = TextMuted.copy(alpha = 0.2f))
                InfoRow("Expiry", card!!.expiry)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
                    color = TextMuted.copy(alpha = 0.2f))
                DetailRowWithToggle(
                    label = "CVV",
                    masked = "•••",
                    revealed = card!!.cvv,
                    showValue = showCvv,
                    onToggle = { showCvv = !showCvv }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
                    color = TextMuted.copy(alpha = 0.2f))
                InfoRow("Category", categoryName ?: "None")
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(card!!.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }

                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonMagenta.copy(alpha = 0.15f),
                        contentColor = NeonMagenta
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(0.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun DetailRowWithToggle(
    label: String,
    masked: String,
    revealed: String,
    showValue: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = if (showValue) revealed else masked,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (showValue) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showValue) "Hide" else "Show",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}