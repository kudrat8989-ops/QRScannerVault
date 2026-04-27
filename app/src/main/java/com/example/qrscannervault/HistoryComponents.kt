package com.example.qrscannervault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrscannervault.data.ScanEntity

@Composable
fun ScanHistoryList(scans: List<ScanEntity>, onDelete: (ScanEntity) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(scans) { scan ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = scan.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = scan.content, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDelete(scan) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}