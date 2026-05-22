package com.sunnyb.cardvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.CardVaultApp
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.data.db.entity.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryDetailViewModel : ViewModel() {

    private val categoryRepository = CardVaultApp.instance.categoryRepository
    private val cardRepository = CardVaultApp.instance.cardRepository

    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    fun loadCategory(categoryId: Long) {
        viewModelScope.launch {
            _category.value = categoryRepository.getCategoryById(categoryId)

            cardRepository.getCardsByCategory(categoryId).collect { cardList ->
                _cards.value = cardList
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
            _category.value = category
        }
    }

    fun deleteCategory() {
        viewModelScope.launch {
            _category.value?.let { categoryRepository.deleteCategory(it) }
        }
    }
}