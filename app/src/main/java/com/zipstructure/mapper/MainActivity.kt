kotlin
package com.zipstructure.mapper

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipstructure.mapper.service.FloatingBubbleService
import com.zipstructure.mapper.ui.mindmap.MindMapCanvas
import com.zipstructure.mapper.ui.theme.ZipMapperTheme
import com.zipstructure.mapper.ui.toolbar.UtilityToolbar
import com.zipstructure.mapper.util.BitmapSaver
import com.zipstructure.mapper.util.PermissionUtils
import com.zipstructure.mapper.util.StructureExporter
import com.zipstructure.mapper.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZipMapperTheme { MainScreen() } }
    }
}

@Composable
private fun MainScreen(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val captureLayer = rememberGraphicsLayer()

    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.openZip(it) }
    }
    val storagePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) startBubble(context)
        else vm.showToast("Storage permission denied")
    }

    LaunchedEffect(state.toast) {
        state.toast?.let { snackbar.showSnackbar(it); vm.consumeToast() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    fun toggleBubble() {
        when {
            !PermissionUtils.hasOverlayPermission(context) ->
                context.startActivity(PermissionUtils.overlaySettingsIntent(context))
            !PermissionUtils.hasStorageAccess(context) ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    context.startActivity(PermissionUtils.manageStorageIntent(context))
                else
                    storagePermissions.launch(PermissionUtils.legacyStoragePermissions())
            else -> startBubble(context)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            UtilityToolbar(
                query = state.query,
                sortMode = state.sortMode,
                hasTree = state.root != null,
                onQueryChange = vm::setQuery,
                onSortChange = vm::setSortMode,
                onOpenZip = {
                    zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                },
                onExport = {
                    val root = state.root ?: return@UtilityToolbar
                    val name = "structure_${System.currentTimeMillis()}.md"
                    val saved = StructureExporter.exportToDownloads(
                        context, name, StructureExporter.toMarkdown(root)
                    )
                    vm.showToast(saved?.let { "Exported to Downloads/$it" } ?: "Export failed")
                },
                onSaveImage = {
                    scope.launch {
                        val bitmap = captureLayer.toImageBitmap().asAndroidBitmap()
                        val ok = withContext(Dispatchers.IO) {
                            BitmapSaver.saveToGallery(context, bitmap, "zip_map_${System.currentTimeMillis()}")
                        }
                        vm.showToast(if (ok) "Visualization saved to Gallery" else "Save failed")
                    }
                },
                onCopyMarkdown = {
                    state.root?.let {
                        StructureExporter.copyToClipboard(context, "zip-structure", StructureExporter.toMarkdown(it))
                        vm.showToast("Markdown copied to clipboard")
                    }
                },
                onCopyText = {
                    state.root?.let {
                        StructureExporter.copyToClipboard(context, "zip-structure", StructureExporter.toIndentedText(it))
                        vm.showToast("Tree text copied to clipboard")
                    }
                },
                onToggleBubble = ::toggleBubble
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.root == null -> EmptyState(
                    onOpen = { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
                )
                else -> MindMapCanvas(
                    root = state.root!!,
                    expandedPaths = state.expandedPaths,
                    query = state.query,
                    sortMode = state.sortMode,
                    captureLayer = captureLayer,
                    onToggle = vm::toggleNode
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ZipStructureMapper", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Visualize any ZIP archive as an interactive mind map.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpen) { Text("Open ZIP Archive") }
    }
}

private fun startBubble(context: android.content.Context) {
    val intent = Intent(context, FloatingBubbleService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
    else context.startService(intent)
}