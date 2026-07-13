kotlin
package com.zipstructure.mapper.ui.overlay

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zipstructure.mapper.data.model.SortMode
import com.zipstructure.mapper.service.ExplorerStateHolder
import com.zipstructure.mapper.ui.theme.NeonCyan
import com.zipstructure.mapper.ui.theme.TextSecondary
import com.zipstructure.mapper.util.formatDate
import com.zipstructure.mapper.util.formatSize
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpandedExplorer(
    holder: ExplorerStateHolder,
    onCollapse: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    val nativeView = LocalView.current
    val state by holder.state.collectAsState()
    var sortMenu by remember { mutableStateOf(false) }
    var rootMenu by remember { mutableStateOf(false) }
    val entries = remember(state) { holder.visibleEntries() }

    fun startDrag(anchorFiles: List<File>) {
        val started = DragDropHelper.startGlobalDrag(context, nativeView, anchorFiles)
        holder.showToast(
            if (started) "Dragging ${anchorFiles.size} file(s)! Drop to upload"
            else "Unable to start drag"
        )
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(12.dp)) {
                // Header row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(onClick = { rootMenu = true }) {
                            Icon(Icons.Filled.Storage, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(state.currentRoot?.label ?: "Storage", maxLines = 1)
                        }
                        DropdownMenu(expanded = rootMenu, onDismissRequest = { rootMenu = false }) {
                            state.roots.forEach { root ->
                                DropdownMenuItem(
                                    text = { Text(root.label) },
                                    onClick = { holder.selectRoot(root); rootMenu = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortMode.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.label + if (m == state.sortMode) "  ✓" else "") },
                                    onClick = { holder.setSortMode(m); sortMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onCollapse) { Icon(Icons.Filled.CloseFullscreen, "Collapse") }
                    IconButton(onClick = onStopService) { Icon(Icons.Filled.PowerSettingsNew, "Stop") }
                }

                // Path + up navigation
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = holder::navigateUp) { Icon(Icons.Filled.ArrowUpward, "Up") }
                    Text(
                        state.currentDir?.absolutePath ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Search
                OutlinedTextField(
                    value = state.query,
                    onValueChange = holder::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search files…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = MaterialTheme.shapes.large
                )
                Spacer(Modifier.height(8.dp))

                // Selection actions
                if (state.selectedPaths.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("${state.selectedPaths.size} selected", color = NeonCyan,
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.weight(1f))
                        // Long-press drag handle -> global multi-file drag & drop.
                        AssistChip(
                            onClick = { },
                            label = { Text("Hold to drag") },
                            leadingIcon = { Icon(Icons.Filled.DragIndicator, null, Modifier.size(16.dp)) },
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { startDrag(state.selectedPaths.map(::File)) }
                            )
                        )
                        TextButton(onClick = holder::clearSelection) { Text("Clear") }
                    }
                }

                // File list
                LazyColumn(Modifier.weight(1f)) {
                    items(entries, key = { it.absolutePath }) { file ->
                        val selected = file.absolutePath in state.selectedPaths
                        FileRow(
                            file = file,
                            selected = selected,
                            onClick = {
                                if (file.isDirectory) holder.navigate(file)
                                else holder.toggleSelect(file)
                            },
                            onLongClick = {
                                if (!file.isDirectory) {
                                    if (!selected) holder.toggleSelect(file)
                                    val files = (state.selectedPaths + file.absolutePath).map(::File)
                                    startDrag(files.filter { it.isFile })
                                }
                            }
                        )
                    }
                }
            }
        }

        // In-overlay toast banner
        Box(Modifier.align(Alignment.TopCenter).padding(top = 24.dp)) {
            OverlayToast(message = state.toast, onDismiss = holder::consumeToast)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(file: File, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (file.isFile) formatSize(file.length()) + " · " else "") + formatDate(file.lastModified()),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        if (selected) Icon(Icons.Filled.CheckCircle, "Selected", tint = NeonCyan, modifier = Modifier.size(18.dp))
    }
}


That's the full project: 30 files covering Gradle setup, manifest with all four permission groups (with API-level scoping), the ZIP parsing engine, the zoomable bezier mind map with capture-to-gallery via `GraphicsLayer`, the utility toolbar (search, sort, export, copy as markdown/tree text), the `specialUse` foreground service with `WindowManager` overlay, spring snap-to-edge bubble, focus-flag switching for keyboard input when expanded, storage-root discovery, multi-select with `DRAG_FLAG_GLOBAL` drag-and-drop through `FileProvider`, the in-overlay auto-dismissing toast, and full cleanup in `onDestroy`.

If you'd like, share a project URL under [kimkitty874-group](https://gitlab.com/groups/kimkitty874-group) and I can commit all of these files there in one go.