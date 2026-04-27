package com.example.qrscannervault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.qrscannervault.data.AppDatabase
import com.example.qrscannervault.data.ScanEntity
import com.example.qrscannervault.ui.theme.QRScannerVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "qr_vault_db"
        ).build()
        val dao = db.scanDao()

        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ScannerViewModel(dao) as T
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
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    var isScanning by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCatId == null && categories.isNotEmpty()) {
            viewModel.selectCategory(categories[0].id)
        } else if (categories.isEmpty()) {
            viewModel.addCategory("General")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { isScanning = !isScanning }) {
                Icon(Icons.Default.Add, contentDescription = if (isScanning) "Exit Scan Mode" else "Start Scan")
            }
        }
    ) { padding ->
        if (isScanning) {
            CameraPreview(
                onBarcodeDetected = { barcodeValue ->
                    // When a barcode is detected, save it.
                    val categoryIdToUse = selectedCatId ?: categories.firstOrNull()?.id
                    if (categoryIdToUse != null) {
                        viewModel.saveScan(content = barcodeValue, name = "QR Scan", categoryId = categoryIdToUse)
                        // Optionally stop scanning or show a confirmation here
                    }
                }
            )
        } else {
            Column(modifier = Modifier.padding(padding)) {
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
                    Text("No categories found.")
                } else {
                    // Display message if no category is selected yet, though LaunchedEffect should handle this
                    Text("Select a category or wait for initial load.")
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
                    Text(text = scan.content, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
