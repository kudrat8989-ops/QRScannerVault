package com.example.qrscannervault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode // Если будет ошибка, заменим на CropFree
import androidx.compose.material.icons.filled.ViewWeek // Иконка штрих-кода
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannervault.data.ScanEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScanHistoryList(
    scans: List<ScanEntity>,
    isEditMode: Boolean,
    onDelete: (ScanEntity) -> Unit,
    onMove: (ScanEntity) -> Unit,
    onClick: (ScanEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp) // Чтобы список не перекрывался кнопкой +
    ) {
        items(scans) { scan ->
            ScanCard(scan, isEditMode, onDelete, onMove, onClick)
        }
    }
}

@Composable
fun ScanCard(
    scan: ScanEntity,
    isEditMode: Boolean,
    onDelete: (ScanEntity) -> Unit,
    onMove: (ScanEntity) -> Unit,
    onClick: (ScanEntity) -> Unit
) {
    // Форматирование даты
    val dateString = remember(scan.timestamp) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(scan.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick(scan) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка: QR (256) или Barcode (остальные)
            Icon(
                imageVector = if (scan.format == 256) Icons.Default.QrCode else Icons.Default.ViewWeek,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = scan.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (isEditMode) {
                Row {
                    IconButton(onClick = { onMove(scan) }) {
                        Icon(Icons.Default.ArrowForward, "Move", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDelete(scan) }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}