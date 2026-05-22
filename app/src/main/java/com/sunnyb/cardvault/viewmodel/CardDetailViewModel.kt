package com.sunnyb.cardvault.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.CardVaultApp
import com.sunnyb.cardvault.data.db.entity.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CardDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = CardVaultApp.instance.cardRepository
    private val cardId: Long = savedStateHandle["cardId"] ?: -1

    private val _card = MutableStateFlow<Card?>(null)
    val card: StateFlow<Card?> = _card.asStateFlow()

    init {
        loadCard()
    }

    private fun loadCard() {
        viewModelScope.launch {
            _card.value = repository.getCardById(cardId)
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            _card.value?.let { repository.deleteCard(it) }
        }
    }
}
