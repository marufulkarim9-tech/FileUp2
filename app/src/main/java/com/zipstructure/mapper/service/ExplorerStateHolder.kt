kotlin
package com.zipstructure.mapper.service

import android.content.Context
import com.zipstructure.mapper.data.model.SortMode
import com.zipstructure.mapper.util.StorageRoot
import com.zipstructure.mapper.util.StorageRoots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ExplorerState(
    val roots: List<StorageRoot> = emptyList(),
    val currentRoot: StorageRoot? = null,
    val currentDir: File? = null,
    val entries: List<File> = emptyList(),
    val query: String = "",
    val sortMode: SortMode = SortMode.NAME,
    val selectedPaths: Set<String> = emptySet(),
    val toast: String? = null
)

class ExplorerStateHolder(context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(ExplorerState())
    val state = _state.asStateFlow()

    init {
        val roots = StorageRoots.discover(context)
        _state.update { it.copy(roots = roots) }
        roots.firstOrNull()?.let { selectRoot(it) }
    }

    fun selectRoot(root: StorageRoot) {
        _state.update { it.copy(currentRoot = root, selectedPaths = emptySet()) }
        navigate(root.dir)
    }

    fun navigate(dir: File) {
        scope.launch(Dispatchers.IO) {
            val files = dir.listFiles()?.toList() ?: emptyList()
            _state.update { it.copy(currentDir = dir, entries = files) }
        }
    }

    fun navigateUp() {
        val s = _state.value
        val root = s.currentRoot?.dir ?: return
        val current = s.currentDir ?: return
        if (current.absolutePath != root.absolutePath) {
            current.parentFile?.let { navigate(it) }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setSortMode(m: SortMode) = _state.update { it.copy(sortMode = m) }

    fun toggleSelect(file: File) = _state.update { s ->
        val p = file.absolutePath
        s.copy(selectedPaths = if (p in s.selectedPaths) s.selectedPaths - p else s.selectedPaths + p)
    }

    fun clearSelection() {
        _state.update { it.copy(selectedPaths = emptySet()) }
        showToast("Selection cleared")
    }

    fun visibleEntries(): List<File> {
        val s = _state.value
        val filtered = s.entries.filter {
            s.query.isBlank() || it.name.contains(s.query, ignoreCase = true)
        }
        val cmp: Comparator<File> = when (s.sortMode) {
            SortMode.NAME -> compareBy { it.name.lowercase() }
            SortMode.DATE -> compareByDescending { it.lastModified() }
            SortMode.SIZE -> compareByDescending { it.length() }
        }
        return filtered.sortedWith(compareByDescending<File> { it.isDirectory }.then(cmp))
    }

    /** Custom in-overlay toast (system Toasts are often suppressed for background overlays). */
    fun showToast(message: String) = _state.update { it.copy(toast = message) }
    fun consumeToast() = _state.update { it.copy(toast = null) }
}