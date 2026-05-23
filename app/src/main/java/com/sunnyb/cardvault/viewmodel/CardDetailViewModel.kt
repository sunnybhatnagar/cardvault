package com.sunnyb.cardvault.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.CardVaultApp
import com.sunnyb.cardvault.data.db.entity.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.LruCache

class CardDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = CardVaultApp.instance.cardRepository
    private val categoryRepository = CardVaultApp.instance.categoryRepository
    private val encryptionManager = CardVaultApp.instance.encryptionManager
    private val cardId: Long = savedStateHandle["cardId"] ?: -1

    private val _card = MutableStateFlow<Card?>(null)
    val card: StateFlow<Card?> = _card.asStateFlow()

    private val _categoryName = MutableStateFlow<String?>(null)
    val categoryName: StateFlow<String?> = _categoryName.asStateFlow()

    private val _backImageBitmap = MutableStateFlow<Bitmap?>(null)
    val backImageBitmap: StateFlow<Bitmap?> = _backImageBitmap.asStateFlow()

    private val _frontImageBitmap = MutableStateFlow<Bitmap?>(null)
    val frontImageBitmap: StateFlow<Bitmap?> = _frontImageBitmap.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    private val imageCache = LruCache<String, Bitmap>(10)

    init {
        loadCard()
    }

    private fun loadCard() {
        viewModelScope.launch {
            try {
                val c = repository.getCardById(cardId)
                _card.value = c
                if (c?.categoryId != null) {
                    val cat = categoryRepository.getCategoryById(c.categoryId)
                    _categoryName.value = cat?.name
                }
            } catch (e: Exception) {
                _error.value = "Failed to load card details"
            }
        }
    }

    fun loadFrontImage() {
        viewModelScope.launch {
            val path = _card.value?.frontImagePath ?: return@launch
            val cached = imageCache.get(path)
            if (cached != null) {
                _frontImageBitmap.value = cached
                return@launch
            }
            val bitmap = withContext(Dispatchers.IO) {
                encryptionManager.readEncryptedBitmap(path)
            }
            if (bitmap != null) {
                imageCache.put(path, bitmap)
            }
            _frontImageBitmap.value = bitmap
        }
    }

    fun loadBackImage() {
        viewModelScope.launch {
            val path = _card.value?.backImagePath ?: return@launch
            val cached = imageCache.get(path)
            if (cached != null) {
                _backImageBitmap.value = cached
                return@launch
            }
            val bitmap = withContext(Dispatchers.IO) {
                encryptionManager.readEncryptedBitmap(path)
            }
            if (bitmap != null) {
                imageCache.put(path, bitmap)
            }
            _backImageBitmap.value = bitmap
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            try {
                _card.value?.let { repository.deleteCard(it) }
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