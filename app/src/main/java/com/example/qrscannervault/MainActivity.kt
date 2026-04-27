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
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "qr_vault_db").build()
        val dao = db.scanDao()
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScannerViewModel(dao) as T
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
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()

    // State management for camera and permissions
    var isScanning by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var lastScannedContent by remember { mutableStateOf("") }
    var scanName by remember { mutableStateOf("") }

    // Permission launcher setup
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            // If permission granted, we can start scanning immediately if the user initiated it
            if (isScanning) {
                Toast.makeText(context, "Camera access granted.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show()
        }
    }

    // Logic for default category and selection (safer initialization)
    LaunchedEffect(categories) {
        if (categories.isEmpty()) {
            viewModel.addCategory("General")
        } else if (selectedCatId == null) {
            // Select the first available category upon initial load
            viewModel.selectCategory(categories[0].id)
        }
    }

    // Dialog for saving scan data
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить QR") },
            text = {
                Column {
                    Text("Сканировано: ${lastScannedContent}")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = scanName,
                        onValueChange = { scanName = it },
                        label = { Text("Введите название") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (scanName.isNotBlank() && selectedCatId != null) {
                        viewModel.saveScan(lastScannedContent, scanName, selectedCatId!!)
                        showSaveDialog = false
                        scanName = ""
                        // Exit scanning mode after saving
                        isScanning = false 
                    } else {
                        Toast.makeText(context, "Пожалуйста, введите название.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isScanning) {
                    // Exit scan mode
                    isScanning = false
                } else {
                    // Attempt to start scanning
                    if (hasCameraPermission) {
                        isScanning = true // Start immediately if permission is already granted
                    } else {
                        // Request permission if not granted
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }) {
                Icon(if (isScanning) Icons.Default.Close else Icons.Default.Add, contentDescription = "Переключить режим")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isScanning && hasCameraPermission) {
                // Camera view is only shown if scanning is active AND permission is granted
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(onBarcodeDetected = { content ->
                        if (!showSaveDialog) {
                            lastScannedContent = content
                            showSaveDialog = true
                        }
                    })
                }
            } else if (isScanning && !hasCameraPermission) {
                // Show a placeholder/message while waiting for permission or if it was denied
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Требуется разрешение камеры для сканирования.")
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

                selectedCatId?.let { catId ->
                    val scans by viewModel.getScansForCategory(catId).collectAsState(initial = emptyList())
                    ScanList(scans)
                } ?: if (categories.isEmpty()) {
                    Text("Категории не найдены.")
                } else {
                    // This state should ideally not be reached due to LaunchedEffect, but kept for safety
                    Text("Выберите категорию.")
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
