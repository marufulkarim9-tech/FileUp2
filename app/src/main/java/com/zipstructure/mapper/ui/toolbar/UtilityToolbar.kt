kotlin
package com.zipstructure.mapper.ui.toolbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zipstructure.mapper.data.model.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityToolbar(
    query: String,
    sortMode: SortMode,
    hasTree: Boolean,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortMode) -> Unit,
    onOpenZip: () -> Unit,
    onExport: () -> Unit,
    onSaveImage: () -> Unit,
    onCopyMarkdown: () -> Unit,
    onCopyText: () -> Unit,
    onToggleBubble: () -> Unit
) {
    var sortMenu by remember { mutableStateOf(false) }
    var copyMenu by remember { mutableStateOf(false) }

    Surface(tonalElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search nodes…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, "Clear")
                    }
                },
                shape = MaterialTheme.shapes.large
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenZip) { Icon(Icons.Filled.FolderZip, "Open ZIP") }

                Box {
                    IconButton(onClick = { sortMenu = true }, enabled = hasTree) {
                        Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label + if (mode == sortMode) "  ✓" else "") },
                                onClick = { onSortChange(mode); sortMenu = false }
                            )
                        }
                    }
                }
                IconButton(onClick = onExport, enabled = hasTree) {
                    Icon(Icons.Filled.FileDownload, "Export structure file")
                }
                IconButton(onClick = onSaveImage, enabled = hasTree) {
                    Icon(Icons.Filled.PhotoCamera, "Save visualization to gallery")
                }
                Box {
                    IconButton(onClick = { copyMenu = true }, enabled = hasTree) {
                        Icon(Icons.Filled.ContentCopy, "Copy structure")
                    }
                    DropdownMenu(expanded = copyMenu, onDismissRequest = { copyMenu = false }) {
                        DropdownMenuItem(text = { Text("Copy as Markdown") },
                            onClick = { onCopyMarkdown(); copyMenu = false })
                        DropdownMenuItem(text = { Text("Copy as Tree Text") },
                            onClick = { onCopyText(); copyMenu = false })
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleBubble) {
                    Icon(Icons.Filled.BubbleChart, "Floating bubble")
                }
            }
        }
    }
}