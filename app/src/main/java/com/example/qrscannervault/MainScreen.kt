package com.example.qrscannervault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import kotlinx.coroutines.flow.flowOf
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var isCameraVisible by remember { mutableStateOf(false) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var lastScannedContent by remember { mutableStateOf("") }
    var lastScannedFormat by remember { mutableStateOf(0) }
    var selectedScanForCode by remember { mutableStateOf<ScanEntity?>(null) }
    var scanToMove by remember { mutableStateOf<ScanEntity?>(null) }
    var inputName by remember { mutableStateOf("") }

    val currentCategory = categories.find { it.id == selectedCatId }
    val currentScans by (if (selectedCatId != null) viewModel.getScansForCategory(selectedCatId!!) else flowOf<List<ScanEntity>>(emptyList())).collectAsState(initial = emptyList())

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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) isCameraVisible = true
        else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCatId == null) {
            viewModel.selectCategory(categories[0].id)
        }
    }

    // --- ДИАЛОГ ПРОСМОТРА С КНОПКОЙ "ПОДЕЛИТЬСЯ КАРТИНКОЙ" ---
    if (showCodeDialog && selectedScanForCode != null) {
        val bitmap = remember(selectedScanForCode) {
            generateBarcode(selectedScanForCode!!.content, selectedScanForCode!!.format)
        }
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        bitmap?.let { shareBarcodeImage(context, it, selectedScanForCode!!.name) }
                    }) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Share QR")
                    }
                    TextButton(onClick = { showCodeDialog = false }) { Text("Close") }
                }
            },
            title = { Text(selectedScanForCode!!.name) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    bitmap?.let {
                        Image(
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

    // [ОСТАЛЬНЫЕ ДИАЛОГИ БЕЗ ИЗМЕНЕНИЙ - Save, Add, Rename, Move, DeleteConfirm]
    if (showDeleteConfirmDialog && currentCategory != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete category?") },
            text = { Text("This category contains ${currentScans.size} scans. Everything inside will be deleted.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteCategory(currentCategory)
                        showDeleteConfirmDialog = false
                    }
                ) { Text("Delete Anyway") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMoveDialog && scanToMove != null) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to...") },
            text = {
                Column {
                    categories.filter { it.id != scanToMove!!.categoryId }.forEach { category ->
                        TextButton(onClick = { viewModel.moveScan(scanToMove!!, category.id); showMoveDialog = false })
                        { Text(category.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") } }
        )
    }

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
            if (!isCameraVisible) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Icon(Icons.Default.Collections, "Gallery") }

                    FloatingActionButton(onClick = {
                        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (status == PackageManager.PERMISSION_GRANTED) isCameraVisible = true
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Icon(if (isCameraVisible) Icons.Default.Close else Icons.Default.Add, "Camera") }
                }
            } else {
                FloatingActionButton(onClick = { isCameraVisible = false }) { Icon(Icons.Default.Close, null) }
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search scans...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

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

                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.CheckCircle else Icons.Default.Settings,
                            contentDescription = "Toggle Edit",
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }

                if (isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { inputName = ""; showAddDialog = true }) {
                            Icon(Icons.Default.Add, null)
                            Text("New Tab")
                        }
                        if (currentCategory != null) {
                            TextButton(onClick = { inputName = currentCategory.name; showRenameDialog = true }) {
                                Icon(Icons.Default.Edit, null)
                                Text("Rename")
                            }
                            TextButton(
                                onClick = {
                                    if (currentScans.isEmpty()) {
                                        viewModel.deleteCategory(currentCategory)
                                    } else {
                                        showDeleteConfirmDialog = true
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, null)
                                Text("Delete Tab")
                            }
                        }
                    }
                }

                selectedCatId?.let {
                    ScanHistoryList(
                        scans = currentScans,
                        isEditMode = isEditMode,
                        onDelete = { viewModel.deleteScan(it) },
                        onMove = { scanToMove = it; showMoveDialog = true },
                        onClick = { if (!isEditMode) { selectedScanForCode = it; showCodeDialog = true } }
                    )
                }
            }
        }
    }
}

// --- ВСПОМОГАТЕЛЬНАЯ ФУНКЦИЯ ДЛЯ ОТПРАВКИ КАРТИНКИ ---
fun shareBarcodeImage(context: android.content.Context, bitmap: Bitmap, name: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/image.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imagePath = File(context.cacheDir, "images/image.png")
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imagePath)

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
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