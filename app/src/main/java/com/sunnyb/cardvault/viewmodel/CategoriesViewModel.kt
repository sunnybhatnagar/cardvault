package com.sunnyb.cardvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnyb.cardvault.CardVaultApp
import com.sunnyb.cardvault.data.db.entity.Category
import kotlinx.coroutines.flow.*

class CategoriesViewModel : ViewModel() {

    private val categoryRepository = CardVaultApp.instance.categoryRepository

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
