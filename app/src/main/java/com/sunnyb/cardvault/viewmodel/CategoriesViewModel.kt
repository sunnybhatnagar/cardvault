package com.sunnyb.cardvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.data.db.CardDao
import com.sunnyb.cardvault.data.db.CategoryDao
import com.sunnyb.cardvault.data.db.entity.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryWithCount(
    val category: Category,
    val cardCount: Int
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val cardDao: CardDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryWithCount>>(emptyList())
    val categories: StateFlow<List<CategoryWithCount>> = _categories.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                categoryDao.getAllCategories().collect { cats ->
                    val withCounts = cats.map { cat ->
                        val count = cardDao.getCardCountForCategory(cat.id)
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
                categoryDao.insertCategory(
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
                categoryDao.updateCategory(category)
            } catch (e: Exception) {
                _error.value = "Failed to update category"
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryDao.deleteCategory(category)
            } catch (e: Exception) {
                _error.value = "Failed to delete category"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
