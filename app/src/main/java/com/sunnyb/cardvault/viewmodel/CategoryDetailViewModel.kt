package com.sunnyb.cardvault.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.data.db.CardDao
import com.sunnyb.cardvault.data.db.CategoryDao
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.data.db.entity.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardDao: CardDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val categoryId: Long = savedStateHandle["categoryId"] ?: -1

    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    init {
        if (categoryId != -1L) {
            loadCategory(categoryId)
        }
    }

    fun loadCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                _category.value = categoryDao.getCategoryById(categoryId)

                cardDao.getCardsByCategory(categoryId).collect { cardList ->
                    _cards.value = cardList
                }
            } catch (e: Exception) {
                _error.value = "Failed to load category"
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryDao.updateCategory(category)
                _category.value = category
            } catch (e: Exception) {
                _error.value = "Failed to update category"
            }
        }
    }

    fun deleteCategory() {
        viewModelScope.launch {
            try {
                _category.value?.let { categoryDao.deleteCategory(it) }
                _isDeleted.value = true
            } catch (e: Exception) {
                _error.value = "Failed to delete category"
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
