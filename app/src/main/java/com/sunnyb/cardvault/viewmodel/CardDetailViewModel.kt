package com.sunnyb.cardvault.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.data.db.CardDao
import com.sunnyb.cardvault.data.db.CategoryDao
import com.sunnyb.cardvault.data.db.entity.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardDao: CardDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val cardId: Long = savedStateHandle["cardId"] ?: -1

    private val _card = MutableStateFlow<Card?>(null)
    val card: StateFlow<Card?> = _card.asStateFlow()

    private val _categoryName = MutableStateFlow<String?>(null)
    val categoryName: StateFlow<String?> = _categoryName.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    init {
        loadCard()
    }

    private fun loadCard() {
        viewModelScope.launch {
            try {
                val c = cardDao.getCardById(cardId)
                _card.value = c
                if (c?.categoryId != null) {
                    val cat = categoryDao.getCategoryById(c.categoryId)
                    _categoryName.value = cat?.name
                }
            } catch (e: Exception) {
                _error.value = "Failed to load card details"
            }
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            try {
                _card.value?.let { cardDao.deleteCard(it) }
                _isDeleted.value = true
            } catch (e: Exception) {
                _error.value = "Failed to delete card"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun onDeleteComplete() {
        _isDeleted.value = false
    }
}
