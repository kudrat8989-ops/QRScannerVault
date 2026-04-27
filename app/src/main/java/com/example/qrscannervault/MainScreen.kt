package com.example.qrscannervault

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.qrscannervault.data.ScanEntity
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isCameraVisible by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    var lastScannedContent by remember { mutableStateOf("") }
    var lastScannedFormat by remember { mutableStateOf(0) }
    var selectedScanForCode by remember { mutableStateOf<ScanEntity?>(null) }
    var scanToMove by remember { mutableStateOf<ScanEntity?>(null) }
    var inputName by remember { mutableStateOf("") }

    val currentCategory = categories.find { it.id == selectedCatId }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(context, it)
                BarcodeScanning.getClient().process(image)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.firstOrNull()
                        if (barcode != null) {
                            lastScannedContent = barcode.rawValue ?: ""
                            lastScannedFormat = barcode.format
                            inputName = ""
                            showSaveDialog = true
                        } else {
                            Toast.makeText(context, "No code found in image", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) isCameraVisible = true
        else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    // Handle auto-selection only. "General" is created by DB Callback.
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCatId == null) {
            viewModel.selectCategory(categories[0].id)
        }
    }

    // View QR/Barcode Dialog
    if (showCodeDialog && selectedScanForCode != null) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            confirmButton = { TextButton(onClick = { showCodeDialog = false }) { Text("Close") } },
            title = { Text(selectedScanForCode!!.name) },
            text = {
                val bitmap = remember(selectedScanForCode) {
                    generateBarcode(selectedScanForCode!!.content, selectedScanForCode!!.format)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    bitmap?.let {
                        // ИСПОЛЬЗУЕМ ПОЛНЫЙ ПУТЬ К IMAGE ДЛЯ ИСКЛЮЧЕНИЯ ОШИБОК
                        androidx.compose.foundation.Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(250.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(selectedScanForCode!!.content)
                }
            }
        )
    }

    // Move Dialog
    if (showMoveDialog && scanToMove != null) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to category") },
            text = {
                Column {
                    categories.filter { it.id != scanToMove!!.categoryId }.forEach { category ->
                        TextButton(
                            onClick = {
                                viewModel.moveScan(scanToMove!!, category.id)
                                showMoveDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(category.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") } }
        )
    }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save QR") },
            text = { TextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Name") }) },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank() && selectedCatId != null) {
                        viewModel.saveScan(lastScannedContent, inputName, lastScannedFormat, selectedCatId!!)
                        showSaveDialog = false
                    }
                }) { Text("Save") }
            }
        )
    }

    // Add Category Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Category") },
            text = { TextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Name") }) },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank()) {
                        viewModel.addCategory(inputName)
                        showAddDialog = false
                    }
                }) { Text("Add") }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && currentCategory != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = { TextField(value = inputName, onValueChange = { inputName = it }) },
            confirmButton = {
                Button(onClick = {
                    if (inputName.isNotBlank()) {
                        viewModel.renameCategory(currentCategory, inputName)
                        showRenameDialog = false
                    }
                }) { Text("OK") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) { Icon(Icons.Default.Search, "Gallery") }

                FloatingActionButton(onClick = {
                    if (isCameraVisible) isCameraVisible = false
                    else {
                        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (status == PackageManager.PERMISSION_GRANTED) isCameraVisible = true
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Icon(if (isCameraVisible) Icons.Default.Close else Icons.Default.Add, "Camera") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isCameraVisible) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(onBarcodeDetected = { content, format ->
                        if (!showSaveDialog) {
                            lastScannedContent = content
                            lastScannedFormat = format
                            inputName = ""
                            showSaveDialog = true
                        }
                    })
                }
            } else {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search scans...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    IconButton(onClick = { inputName = ""; showAddDialog = true }) { Icon(Icons.Default.Add, null) }
                    if (currentCategory != null) {
                        IconButton(onClick = { inputName = currentCategory.name; showRenameDialog = true }) { Icon(Icons.Default.Edit, null) }
                        IconButton(onClick = { viewModel.deleteCategory(currentCategory) }) { Icon(Icons.Default.Delete, null) }
                    }
                }

                selectedCatId?.let { catId ->
                    val scans by viewModel.getScansForCategory(catId).collectAsState(initial = emptyList())
                    ScanHistoryList(
                        scans = scans,
                        onDelete = { viewModel.deleteScan(it) },
                        onMove = { scanToMove = it; showMoveDialog = true },
                        onClick = { selectedScanForCode = it; showCodeDialog = true }
                    )
                }
            }
        }
    }
}

fun generateBarcode(content: String, format: Int): Bitmap? {
    return try {
        val zxingFormat = when (format) {
            256 -> BarcodeFormat.QR_CODE
            1 -> BarcodeFormat.CODE_128
            2 -> BarcodeFormat.CODE_39
            8 -> BarcodeFormat.CODABAR
            16 -> BarcodeFormat.DATA_MATRIX
            32 -> BarcodeFormat.EAN_13
            64 -> BarcodeFormat.EAN_8
            128 -> BarcodeFormat.ITF
            512 -> BarcodeFormat.UPC_A
            1024 -> BarcodeFormat.UPC_E
            else -> BarcodeFormat.QR_CODE
        }
        val width = if (zxingFormat == BarcodeFormat.QR_CODE) 512 else 800
        val height = 512
        val bitMatrix = MultiFormatWriter().encode(content, zxingFormat, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}