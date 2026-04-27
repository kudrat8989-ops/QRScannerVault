package com.example.qrscannervault

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.qrscannervault.data.AppDatabase
import com.example.qrscannervault.data.ScanEntity
import com.example.qrscannervault.ui.theme.QRScannerVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Database initialization
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "qr_vault_db"
        ).build()
        val dao = db.scanDao()

        // ViewModel setup with a factory to pass DAO
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ScannerViewModel(dao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        })[ScannerViewModel::class.java]

        setContent {
            QRScannerVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()

    // UI State Management for Scanning/Saving
    var isCameraVisible by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var lastScannedContent by remember { mutableStateOf("") }
    var scanName by remember { mutableStateOf("") }

    // UI State Management for Category Creation
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isCameraVisible = true
        } else {
            Toast.makeText(context, "Camera permission is required to scan", Toast.LENGTH_SHORT).show()
        }
    }

    // Default category logic
    LaunchedEffect(categories) {
        if (categories.isEmpty()) {
            viewModel.addCategory("General")
        } else if (selectedCatId == null) {
            viewModel.selectCategory(categories[0].id)
        }
    }

    // Dialog for naming the scanned QR
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save QR") },
            text = {
                Column {
                    Text("Scanned Content: ${lastScannedContent}")
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = scanName,
                        onValueChange = { scanName = it },
                        label = { Text("Enter name") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (scanName.isNotBlank() && selectedCatId != null) {
                        viewModel.saveScan(lastScannedContent, scanName, selectedCatId!!)
                        showSaveDialog = false
                        scanName = ""
                        isCameraVisible = false
                    } else {
                        Toast.makeText(context, "Please enter a name.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog for adding a new category
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Column {
                    TextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Enter category name") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        viewModel.addCategory(newCategoryName)
                        showAddCategoryDialog = false
                        newCategoryName = ""
                    } else {
                        Toast.makeText(context, "Please enter a category name.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
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
                Icon(
                    if (isCameraVisible) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isCameraVisible) {
                // Camera view mode
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(onBarcodeDetected = { content ->
                        // Only trigger save dialog if not already showing one
                        if (!showSaveDialog) {
                            lastScannedContent = content
                            showSaveDialog = true
                        }
                    })
                }
            } else {
                // Normal list view mode
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScrollableTabRow(
                            selectedTabIndex = categories.indexOfFirst { it.id == selectedCatId }.coerceAtLeast(0),
                            modifier = Modifier.weight(1f) // Allow tabs to take available space
                        ) {
                            categories.forEach { category ->
                                Tab(
                                    selected = selectedCatId == category.id,
                                    onClick = { viewModel.selectCategory(category.id) },
                                    text = { Text(category.name) }
                                )
                            }
                        }

                        // Button to add new category
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                }

                // Display scan list for the selected category
                selectedCatId?.let { catId ->
                    val scans by viewModel.getScansForCategory(catId).collectAsState(initial = emptyList())
                    ScanList(scans, onDelete = viewModel::deleteScan) // Pass delete function
                } ?: if (categories.isEmpty()) {
                    Text("Categories not found.")
                } else {
                    // Fallback text if no category is selected but categories exist
                    Text("Please select a category.")
                }
            }
        }
    }
}

@Composable
fun ScanList(scans: List<ScanEntity>, onDelete: (ScanEntity) -> Unit) { // Updated signature
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(scans, key = { it.id }) { scan -> // Added key for better performance
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically // Align items vertically
                ) {
                    // Scan details (Name and Content)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = scan.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = scan.content, style = MaterialTheme.typography.bodySmall)
                    }
                    // Delete button
                    IconButton(onClick = { onDelete(scan) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Scan")
                    }
                }
            }
        }
    }
}
