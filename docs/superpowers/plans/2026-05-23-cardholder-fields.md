# Cardholder Name, Variant & Product Fields Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add cardholder name (full), card network variant (auto-detect + dropdown), and product name (OCR + text) to Card Vault.

**Architecture:** Three new non-null String columns on the `Card` Room entity. Scanner detects all three from OCR (name from front text, variant from BIN, product from front text). Dropdown for variant allows manual override. All card UI components show the new fields.

**Tech Stack:** Room (SQLCipher) migration 1→2, ML Kit OCR, Jetpack Compose ExposedDropdownMenuBox

---

### Task 1: Card Entity + DB Migration

**Files:**
- Modify: `app/src/main/java/com/sunnyb/cardvault/data/db/entity/Card.kt`
- Modify: `app/src/main/java/com/sunnyb/cardvault/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/sunnyb/cardvault/data/db/DatabaseFactory.kt`

- [ ] **Step 1: Add cardholderName, variant, product to Card entity**

Replace the `Card` data class fields to add the three new fields (all `""` default):

```kotlin
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nickname: String,
    val issuer: String? = null,
    val cardholderName: String = "",
    val variant: String = "",
    val product: String = "",
    val cardNumber: String,
    val expiry: String,
    val cvv: String,
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val categoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Bump database version and add migration**

In `AppDatabase.kt`, change version to 2 and add the migration object:

```kotlin
@Database(
    entities = [Card::class, Category::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE card ADD COLUMN cardholderName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE card ADD COLUMN variant TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE card ADD COLUMN product TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
```

Import `androidx.room.migration.Migration` and `androidx.sqlite.db.SupportSQLiteDatabase`.

- [ ] **Step 3: Add migration to DatabaseFactory**

In `DatabaseFactory.kt`, add `.addMigrations(AppDatabase.MIGRATION_1_2)` before `.addCallback`:

```kotlin
return Room.databaseBuilder(
    context.applicationContext,
    AppDatabase::class.java,
    DB_NAME
)
    .openHelperFactory(factory)
    .addMigrations(AppDatabase.MIGRATION_1_2)
    .addCallback(seedCategories())
    .build()
```

- [ ] **Step 4: Build to generate version 2 schema**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd /Users/sunnyb/Development/CardVault && ./gradlew assembleDebug
```

Verify `app/schemas/com.sunnyb.cardvault.data.db.AppDatabase/2.json` is created.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/sunnyb/cardvault/data/db/entity/Card.kt app/src/main/java/com/sunnyb/cardvault/data/db/AppDatabase.kt app/src/main/java/com/sunnyb/cardvault/data/db/DatabaseFactory.kt app/schemas/
git commit -m "feat: add cardholderName, variant, product fields to Card entity"
```

---

### Task 2: CardScanner — Name, Variant & Product Detection

**Files:**
- Modify: `app/src/main/java/com/sunnyb/cardvault/util/CardScanner.kt`

- [ ] **Step 1: Update ScannedCardInfo with new fields**

```kotlin
data class ScannedCardInfo(
    val cardNumber: String? = null,
    val expiry: String? = null,
    val issuer: String? = null,
    val cardholderName: String? = null,
    val variant: String? = null,
    val product: String? = null
)
```

- [ ] **Step 2: Add detectVariant method**

Add after `parseIssuer`. BIN detection from card number prefix (check more specific patterns first):

```kotlin
private fun detectVariant(cardNumber: String): String? {
    return when {
        cardNumber.matches("^35(2[89]|[3-8][0-9]).*".toRegex()) -> "JCB"
        cardNumber.matches("^3[47].*".toRegex()) -> "American Express"
        cardNumber.matches("^3(0[0-5]|[68][0-9]).*".toRegex()) -> "Diners Club"
        cardNumber.matches("^(60|65|81|82|50|6363|6271|6354)[0-9].*".toRegex()) -> "RuPay"
        cardNumber.matches("^5[1-5].*".toRegex()) -> "Mastercard"
        cardNumber.matches("^4[0-9].*".toRegex()) -> "Visa"
        cardNumber.matches("^(5018|5020|5038|5[6-9]|6[0-9])[0-9].*".toRegex()) -> "Maestro"
        else -> null
    }
}
```

- [ ] **Step 3: Add parseCardholderName method**

After extracting card number and expiry, filter remaining text blocks for name-like content:

```kotlin
private fun parseCardholderName(texts: List<String>): String? {
    val combined = texts.joinToString(" ")
    val cleaned = combined
        .replace(Regex("""\d{13,19}"""), "")
        .replace(Regex("""\d{2}/\d{2,4}"""), "")
        .replace(Regex("""https?://\S+"""), "")
        .replace(Regex("""www\.\S+"""), "")
        .replace(Regex("""\d{3,4}"""), "")

    val knownWords = setOf(
        "VALID", "THRU", "CARDMEMBER", "SIGNATURE", "AUTHORIZED", "USE",
        "OF", "THIS", "CARD", "IS", "SUBJECT", "TO", "THE", "AGREEMENT",
        "FOR", "ASSISTANCE", "IN", "TOLL", "FREE", "HELPLINE",
        "INTERNATIONAL", "CUSTOMERS", "NOT", "PAYMENT", "FOREIGN",
        "EXCHANGE", "NEPAL", "BHUTAN", "WORLD", "EMERALDE", "PRIVATE",
        "BANK", "VISA", "MASTERCARD", "AMERICAN", "EXPRESS", "RUPAY",
        "DINERS", "CLUB", "JCB", "PLATINUM", "GOLD", "SIGNATURE",
        "INFINITE", "WORLD", "ELITE", "CARDMEMBER", "SIGNATURE"
    )

    val tokens = cleaned.split(Regex("\\s+"))
    val isNameToken: (String) -> Boolean = { token ->
        token.length in 2..30 &&
        token.all { c -> c.isUpperCase() || c == '.' } &&
        token.uppercase() !in knownWords &&
        !token.any { it.isDigit() }
    }

    val sequences = mutableListOf<List<String>>()
    var current = mutableListOf<String>()
    for (token in tokens) {
        if (isNameToken(token)) {
            current.add(token)
        } else {
            if (current.size >= 2) sequences.add(current.toList())
            current = mutableListOf()
        }
    }
    if (current.size >= 2) sequences.add(current.toList())

    if (sequences.isEmpty()) {
        for (token in tokens) {
            if (isNameToken(token) && token.length >= 3) {
                return token
            }
        }
        return null
    }

    return sequences.maxByOrNull { it.sumOf { w -> w.length } }?.joinToString(" ")
}
```

- [ ] **Step 4: Add parseProduct method**

Extract product name from front-of-card text (capitalized word adjacent to bank name):

```kotlin
private fun parseProduct(texts: List<String>): String? {
    val combined = texts.joinToString(" ")
    val knownIssuers = setOf(
        "ICICI", "HDFC", "AXIS", "SBI", "YES", "KOTAK", "INDUSIND",
        "RBL", "FEDERAL", "IDBI", "CANARA", "PNB", "BOB", "UNION",
        "CITI", "HSBC", "STANDARD", "CHARTERED", "AMEX", "AMERICAN",
        "CHASE", "WELLS", "FARGO", "BARCLAYS", "CAPITAL", "ONE",
        "DISCOVER", "BANK", "OF", "AMERICA"
    )

    val words = combined.split(Regex("\\s+"))
    for (i in words.indices) {
        if (words[i].uppercase() in knownIssuers) {
            for (j in i + 1 until minOf(i + 4, words.size)) {
                val candidate = words[j].replace(Regex("[^A-Za-z]"), "")
                if (candidate.length >= 3 && candidate[0].isUpperCase()) {
                    val next = if (j + 1 < words.size && words[j + 1].length in 3..20
                        && words[j + 1][0].isUpperCase()
                        && words[j + 1].all { it.isLetter() }) {
                        " $candidate ${words[j + 1]}"
                    } else " $candidate"
                    return next.trim()
                }
            }
        }
    }
    return null
}
```

- [ ] **Step 5: Update scan() to use new detection**

Add variant (always from card number BIN), name and product (from texts) to the scan result:

```kotlin
suspend fun scan(context: Context, imageUri: Uri): ScannedCardInfo = withContext(Dispatchers.IO) {
    try {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        val result = suspendCancellableCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text -> cont.resume(text) }
                .addOnFailureListener { cont.resume(null) }
        }

        if (result == null) return@withContext ScannedCardInfo()

        val rawTexts = result.textBlocks.map { it.text }
        val texts = rawTexts.map { normalizeOcr(it) }

        parseCardNumber(texts)?.let { number ->
            ScannedCardInfo(
                cardNumber = number,
                expiry = parseExpiry(texts),
                issuer = parseIssuer(number),
                cardholderName = parseCardholderName(texts),
                variant = detectVariant(number),
                product = parseProduct(texts)
            )
        } ?: ScannedCardInfo()
    } catch (e: Exception) {
        ScannedCardInfo()
    }
}
```

- [ ] **Step 6: Build and commit**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd /Users/sunnyb/Development/CardVault && ./gradlew assembleDebug
```

```bash
git add app/src/main/java/com/sunnyb/cardvault/util/CardScanner.kt
git commit -m "feat: detect cardholder name, variant, product from OCR"
```

---

### Task 3: AddCardViewModel — New State & Scan Integration

**Files:**
- Modify: `app/src/main/java/com/sunnyb/cardvault/viewmodel/AddCardViewModel.kt`

- [ ] **Step 1: Add cardholderName, variant, product to AddCardUiState**

```kotlin
data class AddCardUiState(
    val step: Int = 1,
    val frontImageUri: Uri? = null,
    val backImageUri: Uri? = null,
    val hasExistingFrontImage: Boolean = false,
    val hasExistingBackImage: Boolean = false,
    val nickname: String = "",
    val issuer: String = "",
    val cardholderName: String = "",
    val variant: String = "",
    val product: String = "",
    val cardNumber: String = "",
    val cardNumberError: String? = null,
    val expiry: String = "",
    val cvv: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val ocrFailed: Boolean = false
)
```

- [ ] **Step 2: Add update functions**

```kotlin
fun updateCardholderName(value: String) {
    _state.update { it.copy(cardholderName = value.take(50)) }
}

fun updateVariant(value: String) {
    _state.update { it.copy(variant = value) }
}

fun updateProduct(value: String) {
    _state.update { it.copy(product = value.take(50)) }
}
```

- [ ] **Step 3: Update scanCardImage to populate new fields**

Add after the `info.issuer` block:

```kotlin
info.cardholderName?.let { name ->
    if (_state.value.cardholderName.isBlank()) {
        _state.update { it.copy(cardholderName = name) }
    }
}
info.variant?.let { variant ->
    if (_state.value.variant.isBlank()) {
        _state.update { it.copy(variant = variant) }
    }
}
info.product?.let { product ->
    if (_state.value.product.isBlank()) {
        _state.update { it.copy(product = product) }
    }
}
```

- [ ] **Step 4: Update saveCard to pass new fields**

In the `Card` constructor call within `saveCard()`:

```kotlin
val card = Card(
    nickname = s.nickname,
    issuer = s.issuer,
    cardholderName = s.cardholderName,
    variant = s.variant,
    product = s.product,
    cardNumber = s.cardNumber,
    expiry = formattedExpiry,
    cvv = s.cvv,
    categoryId = s.categoryId
)
```

Also update `loadForEdit` to restore all fields:

```kotlin
fun loadForEdit(cardId: Long) {
    editCardId = cardId
    viewModelScope.launch {
        try {
            val card = cardRepository.getCardById(cardId) ?: return@launch
            _state.update {
                it.copy(
                    step = 3,
                    nickname = card.nickname,
                    issuer = card.issuer ?: "",
                    cardholderName = card.cardholderName,
                    variant = card.variant,
                    product = card.product,
                    cardNumber = card.cardNumber,
                    expiry = card.expiry.replace("/", ""),
                    cvv = card.cvv,
                    categoryId = card.categoryId,
                    hasExistingFrontImage = card.frontImagePath != null,
                    hasExistingBackImage = card.backImagePath != null
                )
            }
        } catch (e: Exception) {
            _error.value = "Failed to load card for editing"
        }
    }
}
```

- [ ] **Step 5: Build and commit**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd /Users/sunnyb/Development/CardVault && ./gradlew assembleDebug
```

```bash
git add app/src/main/java/com/sunnyb/cardvault/viewmodel/AddCardViewModel.kt
git commit -m "feat: add cardholder name, variant, product state to AddCardViewModel"
```

---

### Task 4: AddCardScreen — New Fields UI

**Files:**
- Modify: `app/src/main/java/com/sunnyb/cardvault/ui/screens/AddCardScreen.kt`

- [ ] **Step 1: Update StepCardDetails composable signature**

Add new params between issuer and cardNumber:

```kotlin
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
)
```

- [ ] **Step 2: Add cardholder name and product fields**

After the issuer field and before card number:

```kotlin
CardDetailField("Cardholder Name", cardholderName, onCardholderNameChange)
CardDetailField("Product (optional)", product, onProductChange)
```

- [ ] **Step 3: Add variant dropdown**

After the product field, before card number. Use ExposedDropdownMenuBox:

```kotlin
val variantOptions = listOf("", "Visa", "Mastercard", "American Express", "RuPay", "Diners Club", "JCB", "Maestro")
var variantExpanded by remember { mutableStateOf(false) }

Text("Variant", color = MaterialTheme.colorScheme.onSurfaceVariant,
    style = MaterialTheme.typography.bodyMedium)
Spacer(Modifier.height(8.dp))
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
```

- [ ] **Step 4: Wire new fields from state**

Update the call site in the Scaffold content (around line 146-161) to pass the new state values and callbacks:

```kotlin
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
```

- [ ] **Step 5: Build and commit**

```bash
git add app/src/main/java/com/sunnyb/cardvault/ui/screens/AddCardScreen.kt
git commit -m "feat: add cardholder name, variant dropdown, product fields to AddCardScreen"
```

---

### Task 5: CardFrontView + CardDetailScreen

**Files:**
- Modify: `app/src/main/java/com/sunnyb/cardvault/ui/components/CardFrontView.kt`
- Modify: `app/src/main/java/com/sunnyb/cardvault/ui/screens/CardDetailScreen.kt`

- [ ] **Step 1: Update CardFrontView — show name + variant badge**

Replace the emoji card icon with variant badge. Add cardholder name below card number:

```kotlin
@Composable
fun CardFrontView(
    card: Card,
    modifier: Modifier = Modifier
) {
    val gradient = when (card.issuer?.lowercase()) {
        "chase" -> GradientChase
        "amex", "american express" -> GradientAmex
        else -> GradientDefault
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(gradient),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = card.issuer ?: "CARD",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (card.variant.isNotBlank()) {
                    Text(
                        text = card.variant,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = card.cardNumber.chunked(4).joinToString(" "),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (card.cardholderName.isNotBlank()) {
                        Text(
                            text = card.cardholderName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
                Column {
                    Text(
                        text = "VALID THRU",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                    Text(
                        text = card.expiry,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "CVV",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "•••",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update CardDetailScreen — add InfoRows for new fields**

Add after the existing InfoRow("Category", ...) block:

```kotlin
if (card!!.cardholderName.isNotBlank()) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    InfoRow("Cardholder", card!!.cardholderName)
}
if (card!!.variant.isNotBlank()) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    InfoRow("Variant", card!!.variant)
}
if (card!!.product.isNotBlank()) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    InfoRow("Product", card!!.product)
}
```

- [ ] **Step 3: Build and commit**

```bash
git add app/src/main/java/com/sunnyb/cardvault/ui/components/CardFrontView.kt app/src/main/java/com/sunnyb/cardvault/ui/screens/CardDetailScreen.kt
git commit -m "feat: show cardholder name and variant on CardFrontView and detail screen"
```

---

### Task 6: CardTile + CardListItem — Variant Badge

**Files:**
- Modify: `app/src/main/java/com/sunnyb/cardvault/ui/components/CardTile.kt`
- Modify: `app/src/main/java/com/sunnyb/cardvault/ui/components/CardListItem.kt`

- [ ] **Step 1: Update CardTile — replace emoji with variant text**

In the top Row of CardTile, replace the emoji Text with variant text:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
) {
    Text(
        text = card.issuer ?: "",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary
    )
    if (card.variant.isNotBlank()) {
        Text(
            text = card.variant,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
```

- [ ] **Step 2: Update CardListItem — replace emoji with variant text**

Same change in CardListItem's top Row. The existing emoji is shown via `Text(text = "💳", ...)`. Replace with conditional variant text.

In CardListItem.kt around line 63, replace:
```kotlin
Text(
    text = "•••• ${card.cardNumber.takeLast(4)}",
    style = MaterialTheme.typography.labelSmall,
    color = TextSecondary
)
```
with:
```kotlin
Row {
    if (card.variant.isNotBlank()) {
        Text(
            text = card.variant,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(Modifier.width(8.dp))
    }
    Text(
        text = "•••• ${card.cardNumber.takeLast(4)}",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary
    )
}
```

- [ ] **Step 3: Build and commit**

```bash
git add app/src/main/java/com/sunnyb/cardvault/ui/components/CardTile.kt app/src/main/java/com/sunnyb/cardvault/ui/components/CardListItem.kt
git commit -m "feat: show variant badge on CardTile and CardListItem"
```

---

### Task 7: Verify Build and Test

- [ ] **Step 1: Full build**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd /Users/sunnyb/Development/CardVault && ./gradlew assembleDebug
```

- [ ] **Step 2: Verify migration works**

Build has no errors. Clean install on device will create fresh DB at version 2. Upgrade from v1.3 will apply MIGRATION_1_2.

- [ ] **Step 3: Push**

```bash
git push origin main
```
