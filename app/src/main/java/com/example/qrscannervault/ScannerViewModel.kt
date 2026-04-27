package com.example.qrscannervault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannervault.data.CategoryEntity
import com.example.qrscannervault.data.ScanDao
import com.example.qrscannervault.data.ScanEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScannerViewModel(private val scanDao: ScanDao) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = scanDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // --- НОВОЕ: Состояние поиска ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    // ------------------------------

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
    }

    // Изменяем логику получения сканов, чтобы учитывать поиск
    fun getScansForCategory(categoryId: Long): Flow<List<ScanEntity>> {
        return scanDao.getScansByCategory(categoryId).combine(searchQuery) { scans, query ->
            if (query.isBlank()) scans
            else scans.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val currentCategories = categories.value
            if (currentCategories.none { it.name.equals(name, ignoreCase = true) }) {
                scanDao.insertCategory(CategoryEntity(name = name))
            }
        }
    }

    fun renameCategory(category: CategoryEntity, newName: String) {
        viewModelScope.launch {
            scanDao.updateCategory(category.copy(name = newName))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (_selectedCategoryId.value == category.id) {
                _selectedCategoryId.value = null
            }
            scanDao.deleteCategory(category)
        }
    }

    fun moveScan(scan: ScanEntity, newCategoryId: Long) {
        viewModelScope.launch {
            scanDao.updateScan(scan.copy(categoryId = newCategoryId))
        }
    }

    fun saveScan(content: String, name: String, format: Int, categoryId: Long) {
        viewModelScope.launch {
            val scan = ScanEntity(
                content = content,
                name = name,
                format = format,
                timestamp = System.currentTimeMillis(),
                categoryId = categoryId
            )
            scanDao.insertScan(scan)
        }
    }

    fun deleteScan(scan: ScanEntity) {
        viewModelScope.launch {
            scanDao.deleteScan(scan)
        }
    }
}