package com.example.qrscannervault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannervault.data.CategoryEntity
import com.example.qrscannervault.data.ScanDao
import com.example.qrscannervault.data.ScanEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScannerViewModel(private val scanDao: ScanDao) : ViewModel() {

    // All categories for tabs
    val categories: StateFlow<List<CategoryEntity>> = scanDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected category ID
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
    }

    // Get scans for the active tab
    fun getScansForCategory(categoryId: Long): Flow<List<ScanEntity>> = 
        scanDao.getScansByCategory(categoryId)

    fun addCategory(name: String) {
        viewModelScope.launch {
            // Check if a category with this name already exists in the current list of categories
            val existing = categories.value.any { it.name.equals(name, ignoreCase = true) }
            if (!existing && name.isNotBlank()) {
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

    // Updated to include format parameter
    fun saveScan(content: String, name: String, format: Int, categoryId: Long) {
        viewModelScope.launch {
            val scan = ScanEntity(
                content = content,
                name = name,
                format = format, // Added format
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
