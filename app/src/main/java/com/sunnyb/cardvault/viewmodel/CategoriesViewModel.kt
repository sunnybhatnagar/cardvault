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

    init {
        viewModelScope.launch {
            categoryRepository.allCategories.collect { cats ->
                val withCounts = cats.map { cat ->
                    val count = cardRepository.getCardCountForCategory(cat.id)
                    CategoryWithCount(cat, count)
                }
                _categories.value = withCounts
            }
        }
    }

    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            categoryRepository.insertCategory(
                Category(name = name.take(30), icon = icon)
            )
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}