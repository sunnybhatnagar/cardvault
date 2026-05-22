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

    private val imageCache = LruCache<String, Bitmap>(10)

    init {
        loadCard()
    }

    private fun loadCard() {
        viewModelScope.launch {
            val c = repository.getCardById(cardId)
            _card.value = c
            if (c?.categoryId != null) {
                val cat = categoryRepository.getCategoryById(c.categoryId)
                _categoryName.value = cat?.name
            }
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
            _card.value?.let { repository.deleteCard(it) }
        }
    }
}