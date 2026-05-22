package com.sunnyb.cardvault.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.sunnyb.cardvault.ui.theme.*
import com.sunnyb.cardvault.viewmodel.AddCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddCardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Card", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(TextMuted.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = state.step / 3f)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(NeonCyan, NeonMagenta)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                )
            }

            Spacer(Modifier.height(24.dp))

            when (state.step) {
                1 -> StepPhotoCapture(
                    title = "📸 Front of Card",
                    imageUri = state.frontImageUri,
                    onImageSelected = { viewModel.setFrontImage(it) },
                    onSkip = { viewModel.nextStep() }
                )
                2 -> StepPhotoCapture(
                    title = "📸 Back of Card",
                    imageUri = state.backImageUri,
                    onImageSelected = { viewModel.setBackImage(it) },
                    onSkip = { viewModel.nextStep() }
                )
                3 -> StepCardDetails(
                    nickname = state.nickname,
                    issuer = state.issuer,
                    cardNumber = state.cardNumber,
                    expiry = state.expiry,
                    cvv = state.cvv,
                    categories = state.categories,
                    selectedCategoryId = state.categoryId,
                    onNicknameChange = { viewModel.updateNickname(it) },
                    onIssuerChange = { viewModel.updateIssuer(it) },
                    onCardNumberChange = { viewModel.updateCardNumber(it) },
                    onExpiryChange = { viewModel.updateExpiry(it) },
                    onCvvChange = { viewModel.updateCvv(it) },
                    onCategoryChange = { viewModel.updateCategory(it) }
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.step > 1) {
                    OutlinedButton(
                        onClick = { viewModel.previousStep() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(TextMuted.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("← Back")
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                if (state.step < 3) {
                    Button(
                        onClick = { viewModel.nextStep() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan.copy(alpha = 0.1f),
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next →")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.saveCard()
                            onSaved()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan.copy(alpha = 0.1f),
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = state.nickname.isNotBlank() && state.cardNumber.isNotBlank()
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("💾 Save Card")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (i <= state.step) NeonCyan
                                else TextMuted.copy(alpha = 0.2f)
                            )
                    )
                    if (i < 3) Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StepPhotoCapture(
    title: String,
    imageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    onSkip: () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = NeonCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(NeonCyan.copy(alpha = 0.02f))
                .clickable {
                    // TODO: launch camera/gallery picker
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📷", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap to capture",
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "— or import from gallery —",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Skip this step", color = TextSecondary)
        }
    }
}

@Composable
private fun StepCardDetails(
    nickname: String,
    issuer: String,
    cardNumber: String,
    expiry: String,
    cvv: String,
    categories: List<com.sunnyb.cardvault.data.db.entity.Category>,
    selectedCategoryId: Long?,
    onNicknameChange: (String) -> Unit,
    onIssuerChange: (String) -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit
) {
    Column {
        Text(
            text = "✏️ Card Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.height(16.dp))

        CardDetailField("Nickname", nickname, onNicknameChange)
        CardDetailField("Issuer (optional)", issuer, onIssuerChange)
        CardDetailField("Card Number", cardNumber, onCardNumberChange,
            keyboardType = KeyboardType.Number)
        CardDetailField("Expiry (MM/YY)", expiry, onExpiryChange,
            keyboardType = KeyboardType.Number)
        CardDetailField("CVV", cvv, onCvvChange,
            keyboardType = KeyboardType.Number)

        Spacer(Modifier.height(16.dp))

        Text("Category", color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { onCategoryChange(category.id) },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.12f),
                        selectedLabelColor = NeonCyan
                    )
                )
            }
        }
    }
}

@Composable
private fun CardDetailField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan.copy(alpha = 0.3f),
            unfocusedBorderColor = TextMuted.copy(alpha = 0.2f),
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            cursorColor = NeonCyan,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = NeonCyan,
            unfocusedLabelColor = TextSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
