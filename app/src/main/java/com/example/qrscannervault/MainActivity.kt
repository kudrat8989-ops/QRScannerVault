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

        // Initialize Room database
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "qr_vault_db").build()
        val dao = db.scanDao()

        // Setup ViewModel
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
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    // Collect state from ViewModel
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()

    // UI State Management
    var isCameraVisible by remember { mutableStateOf(false) } // Controls if the camera view is active
    var showSaveDialog by remember { mutableStateOf(false) }
    var lastScannedContent by remember { mutableStateOf("") }
    var scanName by remember { mutableStateOf("") }

    // Permission launcher setup for Camera access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // If granted, allow camera view to be shown/started
            isCameraVisible = true
        } else {
            Toast.makeText(context, "Camera permission is required for scanning.", Toast.LENGTH_LONG).show()
        }
    }

    // Initialization logic: Ensure a default category exists and select the first one on load
    LaunchedEffect(categories) {
        if (categories.isEmpty()) {
            viewModel.addCategory("General") // Add default category if none exist
        } else if (selectedCatId == null) {
            // Select the first available category upon initial load
            viewModel.selectCategory(categories[0].id)
        }
    }

    // Dialog for saving scan data
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
                        label = { Text("Enter Name") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (scanName.isNotBlank() && selectedCatId != null) {
                        // Save the scan data
                        viewModel.saveScan(lastScannedContent, scanName, selectedCatId!!)
                        showSaveDialog = false
                        scanName = ""
                        // Exit scanning mode after successful save
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isCameraVisible) {
                    // Exit scan mode if camera is visible
                    isCameraVisible = false
                } else {
                    // Attempt to start scanning
                    val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (status == PackageManager.PERMISSION_GRANTED) {
                        // Start immediately if permission is already granted
                        isCameraVisible = true
                    } else {
                        // Request camera permission
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }) {
                Icon(if (isCameraVisible) Icons.Default.Close else Icons.Default.Add, contentDescription = if (isCameraVisible) "Exit Scan" else "Start Scan")
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
                    ScrollableTabRow(
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
                }

                // Display scan list for the selected category
                selectedCatId?.let { catId ->
                    val scans by viewModel.getScansForCategory(catId).collectAsState(initial = emptyList())
                    ScanList(scans)
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
fun ScanList(scans: List<ScanEntity>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(scans) { scan ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = scan.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = scan.content, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
