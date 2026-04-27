package com.example.qrscannervault

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun MainScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()

    var isCameraVisible by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var lastScannedContent by remember { mutableStateOf("") }
    var scanName by remember { mutableStateOf("") }

    // State for adding new categories
    var showCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

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

    // Dialog for saving scan
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save QR") },
            text = {
                TextField(
                    value = scanName,
                    onValueChange = { scanName = it },
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (scanName.isNotBlank() && selectedCatId != null) {
                        viewModel.saveScan(lastScannedContent, scanName, selectedCatId!!)
                        showSaveDialog = false
                        scanName = ""
                        isCameraVisible = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog for adding new category
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                TextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        viewModel.addCategory(newCategoryName)
                        showCategoryDialog = false
                        newCategoryName = ""
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") }
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
                    if (status == PackageManager.PERMISSION_GRANTED) {
                        isCameraVisible = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
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
                if (categories.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ScrollableTabRow(
                            modifier = Modifier.weight(1f),
                            selectedTabIndex = categories.indexOfFirst { it.id == selectedCatId }.coerceAtLeast(0)
                        ) {
                            categories.forEach { category ->
                                Tab(
                                    selected = selectedCatId == category.id,
                                    onClick = { viewModel.selectCategory(category.id) },
                                    text = { Text(category.name) }
                                )
                            }
                        }
                        IconButton(onClick = { showCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                } else {
                    // Handle case where categories list is empty and initial setup failed (though LaunchedEffect should prevent this)
                    Text("No categories found.")
                }

                selectedCatId?.let { catId ->
                    val scans by viewModel.getScansForCategory(catId).collectAsState(initial = emptyList())
                    ScanHistoryList(scans = scans, onDelete = { viewModel.deleteScan(it) })
                }
            }
        }
    }
}
