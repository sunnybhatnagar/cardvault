package com.sunnyb.cardvault.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.CardVaultApp
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.data.db.entity.Category
import com.sunnyb.cardvault.util.CardScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AddCardUiState(
    val step: Int = 1,
    val frontImageUri: Uri? = null,
    val backImageUri: Uri? = null,
    val hasExistingFrontImage: Boolean = false,
    val hasExistingBackImage: Boolean = false,
    val nickname: String = "",
    val issuer: String = "",
    val cardNumber: String = "",
    val cardNumberError: String? = null,
    val expiry: String = "",
    val cvv: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false
)

class AddCardViewModel : ViewModel() {

    private val cardRepository = CardVaultApp.instance.cardRepository
    private val categoryRepository = CardVaultApp.instance.categoryRepository
    private val encryptionManager = CardVaultApp.instance.encryptionManager
    private val appContext = CardVaultApp.instance

    private val _state = MutableStateFlow(AddCardUiState())
    val state: StateFlow<AddCardUiState> = _state.asStateFlow()

    private var editCardId: Long? = null

    init {
        viewModelScope.launch {
            categoryRepository.allCategories.collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    fun loadForEdit(cardId: Long) {
        editCardId = cardId
        viewModelScope.launch {
            val card = cardRepository.getCardById(cardId) ?: return@launch
            _state.update {
                it.copy(
                    step = 3,
                    nickname = card.nickname,
                    issuer = card.issuer ?: "",
                    cardNumber = card.cardNumber,
                    expiry = card.expiry.replace("/", ""),
                    cvv = card.cvv,
                    categoryId = card.categoryId,
                    hasExistingFrontImage = card.frontImagePath != null,
                    hasExistingBackImage = card.backImagePath != null
                )
            }
        }
    }

    fun setFrontImage(uri: Uri) {
        _state.update { it.copy(frontImageUri = uri) }
        scanCardImage(uri)
    }

    fun setBackImage(uri: Uri) {
        _state.update { it.copy(backImageUri = uri) }
    }

    fun updateNickname(value: String) {
        _state.update { it.copy(nickname = value.take(50)) }
    }

    fun updateIssuer(value: String) {
        _state.update { it.copy(issuer = value.take(50)) }
    }

    fun updateCardNumber(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(19)
        val error = if (digitsOnly.length >= 13 && !isValidLuhn(digitsOnly)) {
            "Invalid card number"
        } else null
        _state.update { it.copy(cardNumber = digitsOnly, cardNumberError = error) }
    }

    fun updateExpiry(value: String) {
        _state.update { it.copy(expiry = value.filter { it.isDigit() }.take(4)) }
    }

    fun updateCvv(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(4)
        _state.update { it.copy(cvv = digitsOnly) }
    }

    fun updateCategory(id: Long?) {
        _state.update { it.copy(categoryId = id) }
    }

    fun nextStep() {
        _state.update { it.copy(step = it.step + 1) }
    }

    fun previousStep() {
        _state.update { it.copy(step = it.step - 1) }
    }

    private fun scanCardImage(uri: Uri) {
        viewModelScope.launch {
            val info = CardScanner.scan(appContext, uri)
            info.cardNumber?.let { number ->
                if (_state.value.cardNumber.isBlank()) {
                    _state.update { it.copy(cardNumber = number) }
                    updateCardNumber(number)
                }
            }
            info.expiry?.let { expiry ->
                if (_state.value.expiry.isBlank()) {
                    _state.update { it.copy(expiry = expiry) }
                }
            }
            info.issuer?.let { issuer ->
                if (_state.value.issuer.isBlank()) {
                    _state.update { it.copy(issuer = issuer) }
                }
            }
        }
    }

    fun saveCard() {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(isSaving = true) }

            val formattedExpiry = if (s.expiry.length == 4) "${s.expiry.take(2)}/${s.expiry.drop(2)}" else s.expiry
            val card = Card(
                nickname = s.nickname,
                issuer = s.issuer,
                cardNumber = s.cardNumber,
                expiry = formattedExpiry,
                cvv = s.cvv,
                categoryId = s.categoryId
            )

            if (editCardId != null) {
                cardRepository.updateCard(card.copy(id = editCardId!!))
                copyImagesToEncrypted(editCardId!!, s.frontImageUri, s.backImageUri)
            } else {
                val newId = cardRepository.insertCard(card)
                copyImagesToEncrypted(newId, s.frontImageUri, s.backImageUri)
            }

            _state.update { it.copy(isSaving = false, cardNumberError = null) }
        }
    }

    private fun isValidLuhn(digits: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in digits.indices.reversed()) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private suspend fun copyImagesToEncrypted(cardId: Long, frontUri: Uri?, backUri: Uri?) {
        withContext(Dispatchers.IO) {
            for ((uri, suffix) in listOf(frontUri to "front", backUri to "back")) {
                if (uri == null) continue
                val fileName = "card_${cardId}_$suffix.jpg"
                val file = File(appContext.filesDir, fileName)
                val encryptedFile = encryptionManager.createEncryptedFile(file)

                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    encryptedFile.openFileOutput().use { output ->
                        input.copyTo(output)
                    }
                }

                cardRepository.updateCard(
                    cardRepository.getCardById(cardId)!!.let { existing ->
                        if (suffix == "front") existing.copy(frontImagePath = file.absolutePath)
                        else existing.copy(backImagePath = file.absolutePath)
                    }
                )
            }
        }
    }
}