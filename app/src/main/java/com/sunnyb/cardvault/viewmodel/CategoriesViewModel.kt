package com.sunnyb.cardvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.CardVaultApp
import com.sunnyb.cardvault.data.db.entity.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryWithCount(
    val category: Category,
    val cardCount: Int
)

class CategoriesViewModel : ViewModel() {

    private val categoryRepository = CardVaultApp.instance.categoryRepository
    private val cardRepository = CardVaultApp.instance.cardRepository

    private val _categories = MutableStateFlow<List<CategoryWithCount>>(emptyList())
    val categories: StateFlow<List<CategoryWithCount>> = _categories.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                categoryRepository.allCategories.collect { cats ->
                    val withCounts = cats.map { cat ->
                        val count = cardRepository.getCardCountForCategory(cat.id)
                        CategoryWithCount(cat, count)
                    }
                    _categories.value = withCounts
                }
            } catch (e: Exception) {
                _error.value = "Failed to load categories"
            }
        }
    }

    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            try {
                categoryRepository.insertCategory(
                    Category(name = name.take(30), icon = icon)
                )
            } catch (e: Exception) {
                _error.value = "Failed to add category"
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category)
            } catch (e: Exception) {
                _error.value = "Failed to update category"
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(category)
            } catch (e: Exception) {
                _error.value = "Failed to delete category"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun addCategoryWithCallback(name: String, icon: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                categoryRepository.insertCategory(
                    Category(name = name.take(30), icon = icon)
                )
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to add category"
            }
        }
    }

    fun deleteCategoryWithCallback(category: Category, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(category)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to delete category"
            }
        }
    }
}