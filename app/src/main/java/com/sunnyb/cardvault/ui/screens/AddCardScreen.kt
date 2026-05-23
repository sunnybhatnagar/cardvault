package com.sunnyb.cardvault.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sunnyb.cardvault.ui.theme.*
import com.sunnyb.cardvault.util.CardNumberTransformation
import com.sunnyb.cardvault.util.ExpiryTransformation
import com.sunnyb.cardvault.util.PermissionAction
import com.sunnyb.cardvault.util.getPermissionAction
import com.sunnyb.cardvault.viewmodel.AddCardViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    editCardId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddCardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(editCardId) {
        if (editCardId != null) {
            viewModel.loadForEdit(editCardId)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Card saved", Toast.LENGTH_SHORT).show()
            viewModel.onSaveComplete()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editCardId != null) "Edit Card" else "Add Card",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = state.step / 3f)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                )
            }

            Spacer(Modifier.height(24.dp))

                when (state.step) {
                    1 -> StepPhotoCapture(
                        title = "Front of Card",
                        imageUri = state.frontImageUri,
                        hasExistingImage = state.hasExistingFrontImage,
                        onImageSelected = { viewModel.setFrontImage(it) },
                        onSkip = { viewModel.nextStep() }
                    )
                    2 -> StepPhotoCapture(
                        title = "Back of Card",
                        imageUri = state.backImageUri,
                        hasExistingImage = state.hasExistingBackImage,
                        onImageSelected = { viewModel.setBackImage(it) },
                        onSkip = { viewModel.nextStep() }
                    )
                    3 -> StepCardDetails(
                        nickname = state.nickname,
                        issuer = state.issuer,
                        cardholderName = state.cardholderName,
                        variant = state.variant,
                        product = state.product,
                        cardNumber = state.cardNumber,
                        cardNumberError = state.cardNumberError,
                        expiry = state.expiry,
                        cvv = state.cvv,
                        cvvError = state.cvvError,
                        categories = state.categories,
                        selectedCategoryId = state.categoryId,
                        onNicknameChange = { viewModel.updateNickname(it) },
                        onIssuerChange = { viewModel.updateIssuer(it) },
                        onCardholderNameChange = { viewModel.updateCardholderName(it) },
                        onVariantChange = { viewModel.updateVariant(it) },
                        onProductChange = { viewModel.updateProduct(it) },
                        onCardNumberChange = { viewModel.updateCardNumber(it) },
                        onExpiryChange = { viewModel.updateExpiry(it) },
                        onCvvChange = { viewModel.updateCvv(it) },
                        onCategoryChange = { viewModel.updateCategory(it) }
                    )
                }

            if (state.ocrFailed && state.step == 2 && state.backImageUri != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Could not read card details from image. Try better lighting or enter details manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
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
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next →")
                    }
                } else {
                    Button(
                        onClick = { viewModel.saveCard() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = state.nickname.isNotBlank() && state.cardNumber.isNotBlank() && state.cardNumberError == null && state.issuer.isNotBlank() && state.cardholderName.isNotBlank() && state.cvv.isNotBlank() && state.cvvError == null
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (editCardId != null) "💾 Update Card" else "💾 Save Card")
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
                                if (i <= state.step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
    hasExistingImage: Boolean = false,
    onImageSelected: (Uri) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) tempImageUri?.let { onImageSelected(it) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onImageSelected(it) } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File.createTempFile("card_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            tempImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    var showRationale by remember { mutableStateOf(false) }
    var showSettingsPrompt by remember { mutableStateOf(false) }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Camera Permission") },
            text = { Text("Card Vault needs camera access to scan your credit card details. No photos are uploaded anywhere.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text("Allow", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    if (showSettingsPrompt) {
        AlertDialog(
            onDismissRequest = { showSettingsPrompt = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Camera Permission") },
            text = { Text("Camera permission was permanently denied. Enable it in Settings → Apps → Card Vault → Permissions.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsPrompt = false
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    })
                }) { Text("Open Settings", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsPrompt = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.02f))
                .clickable {
                    val isGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    val shouldRationale = (context as? androidx.activity.ComponentActivity)
                        ?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ?: false

                    when (getPermissionAction(isGranted, shouldRationale)) {
                        PermissionAction.LAUNCH_CAMERA -> {
                            val file = File.createTempFile("card_", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            tempImageUri = uri
                            cameraLauncher.launch(uri)
                        }
                        PermissionAction.SHOW_RATIONALE -> showRationale = true
                        PermissionAction.SHOW_SETTINGS_PROMPT -> showSettingsPrompt = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Card image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (hasExistingImage) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null,
                        modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Photo saved (tap to retake)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null,
                        modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap to capture",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "— or import from gallery —",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { galleryLauncher.launch("image/*") }
        )

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Skip this step", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepCardDetails(
    nickname: String,
    issuer: String,
    cardholderName: String,
    variant: String,
    product: String,
    cardNumber: String,
    cardNumberError: String?,
    expiry: String,
    cvv: String,
    cvvError: String?,
    categories: List<com.sunnyb.cardvault.data.db.entity.Category>,
    selectedCategoryId: Long?,
    onNicknameChange: (String) -> Unit,
    onIssuerChange: (String) -> Unit,
    onCardholderNameChange: (String) -> Unit,
    onVariantChange: (String) -> Unit,
    onProductChange: (String) -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit
) {
    val variantOptions = listOf("", "Visa", "Mastercard", "American Express", "RuPay", "Diners Club", "JCB", "Maestro")
    var variantExpanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Card Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))

        CardDetailField("Nickname", nickname, onNicknameChange)
        CardDetailField("Issuer", issuer, onIssuerChange)
        CardDetailField("Cardholder Name", cardholderName, onCardholderNameChange)
        CardDetailField("Product (optional)", product, onProductChange)

        Spacer(Modifier.height(8.dp))
        Text("Variant", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = variantExpanded,
            onExpandedChange = { variantExpanded = it }
        ) {
            OutlinedTextField(
                value = variant,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = variantExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = variantExpanded,
                onDismissRequest = { variantExpanded = false }
            ) {
                variantOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(if (option.isEmpty()) "None" else option) },
                        onClick = {
                            onVariantChange(option)
                            variantExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        CardDetailField("Card Number", cardNumber, onCardNumberChange,
            keyboardType = KeyboardType.Number, error = cardNumberError,
            visualTransformation = CardNumberTransformation)
        CardDetailField("Expiry (MM/YY)", expiry, onExpiryChange,
            keyboardType = KeyboardType.Number,
            visualTransformation = ExpiryTransformation)
        val cvvDigits = if (variant == "American Express") "4 digits" else "3 digits"
        CardDetailField("CVV ($cvvDigits)", cvv, onCvvChange,
            keyboardType = KeyboardType.Number, error = cvvError)

        Spacer(Modifier.height(16.dp))

        Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
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
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.secondary) } },
        visualTransformation = visualTransformation ?: androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            errorBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
            errorLabelColor = MaterialTheme.colorScheme.secondary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}