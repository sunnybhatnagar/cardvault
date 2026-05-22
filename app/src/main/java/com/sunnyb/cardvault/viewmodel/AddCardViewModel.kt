package com.sunnyb.cardvault.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.CardVaultApp
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.data.db.entity.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddCardUiState(
    val step: Int = 1,
    val frontImageUri: Uri? = null,
    val backImageUri: Uri? = null,
    val nickname: String = "",
    val issuer: String = "",
    val cardNumber: String = "",
    val expiry: String = "",
    val cvv: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false
)

class AddCardViewModel : ViewModel() {

    private val cardRepository = CardVaultApp.instance.cardRepository
    private val categoryRepository = CardVaultApp.instance.categoryRepository

    private val _state = MutableStateFlow(AddCardUiState())
    val state: StateFlow<AddCardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.allCategories.collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    fun setFrontImage(uri: Uri) {
        _state.update { it.copy(frontImageUri = uri) }
    }

    fun setBackImage(uri: Uri) {
        _state.update { it.copy(backImageUri = uri) }
    }

    fun updateNickname(value: String) {
        _state.update { it.copy(nickname = value) }
    }

    fun updateIssuer(value: String) {
        _state.update { it.copy(issuer = value) }
    }

    fun updateCardNumber(value: String) {
        _state.update { it.copy(cardNumber = value) }
    }

    fun updateExpiry(value: String) {
        _state.update { it.copy(expiry = value) }
    }

    fun updateCvv(value: String) {
        _state.update { it.copy(cvv = value) }
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

    fun saveCard() {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(isSaving = true) }

            cardRepository.insertCard(
                Card(
                    nickname = s.nickname,
                    issuer = s.issuer,
                    cardNumber = s.cardNumber,
                    expiry = s.expiry,
                    cvv = s.cvv,
                    categoryId = s.categoryId
                )
            )

            _state.update { it.copy(isSaving = false) }
        }
    }
}
