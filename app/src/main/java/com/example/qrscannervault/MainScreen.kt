package com.example.qrscannervault

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun MainScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    
    var isCameraVisible by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    var lastScannedContent by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }

    val currentCategory = categories.find { it.id == selectedCatId }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) isCameraVisible = true
        else Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(categories) {
        if (categories.isEmpty()) {
            viewModel.addCategory("General")
        } else if (selectedCatId == null) {
            viewModel.selectCategory(categories[0].id)
        }
    }

    // Dialog for Saving QR
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save QR") },
            text = { TextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Name") }) },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank() && selectedCatId != null) {
                        viewModel.saveScan(lastScannedContent, inputName, selectedCatId!!)
                        showSaveDialog = false
                        inputName = ""
                        isCameraVisible = false
                    }
                }) { Text("Save") }
            }
        )
    }

    // Dialog for Adding Category
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Category") },
            text = { TextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Category Name") }) },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank()) {
                        viewModel.addCategory(inputName)
                        showAddDialog = false
                        inputName = ""
                    }
                }) { Text("Add") }
            }
        )
    }

    // Dialog for Renaming Category
    if (showRenameDialog && currentCategory != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Category") },
            text = { TextField(value = inputName, onValueChange = { inputName = it }, label = { Text("New Name") }) },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank()) {
                        viewModel.renameCategory(currentCategory, inputName)
                        showRenameDialog = false
                        inputName = ""
                    }
                }) { Text("Rename") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isCameraVisible) {
                    isCameraVisible = false
                } else {
                    val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (status == PackageManager.PERMISSION_GRANTED) isCameraVisible = true
                    else permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Icon(if (isCameraVisible) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isCameraVisible) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(onBarcodeDetected = { content ->
                        if (!showSaveDialog) {
                            lastScannedContent = content
                            showSaveDialog = true
                        }
                    })
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (categories.isNotEmpty()) {
                        ScrollableTabRow(
                            modifier = Modifier.weight(1f),
                            selectedTabIndex = categories.indexOfFirst { it.id == selectedCatId }.coerceAtLeast(0),
                            edgePadding = 8.dp
                        ) {
                            categories.forEach { category ->
                                Tab(
                                    selected = selectedCatId == category.id,
                                    onClick = { viewModel.selectCategory(category.id) },
                                    text = { Text(category.name) }
                                )
                            }
                        }
                    }
                    
                    // Category Management Buttons
                    IconButton(onClick = { inputName = ""; showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                    if (currentCategory != null) {
                        IconButton(onClick = { inputName = currentCategory.name; showRenameDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename")
                        }
                        IconButton(onClick = { viewModel.deleteCategory(currentCategory) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }

                selectedCatId?.let { catId ->
                    val scans by viewModel.getScansForCategory(catId).collectAsState(initial = emptyList())
                    ScanHistoryList(scans = scans, onDelete = { viewModel.deleteScan(it) })
                }
            }
        }
    }
}
