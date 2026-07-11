```kotlin
package com.zipstructure.mapper.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zipstructure.mapper.data.ZipParser
import com.zipstructure.mapper.data.model.SortMode
import com.zipstructure.mapper.data.model.ZipNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val root: ZipNode? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val sortMode: SortMode = SortMode.NAME,
    val expandedPaths: Set<String> = emptySet(),
    val toast: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun openZip(uri: Uri) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { ZipParser.parse(getApplication(), uri) }
                .onSuccess { root ->
                    // Expand root + first level by default.
                    val expanded = buildSet {
                        add(root.path)
                        root.children.filter { it.isDirectory }.forEach { add(it.path) }
                    }
                    _state.update { it.copy(root = root, isLoading = false, expandedPaths = expanded) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to parse ZIP") }
                }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setSortMode(mode: SortMode) = _state.update { it.copy(sortMode = mode) }

    fun toggleNode(path: String) = _state.update { s ->
        s.copy(expandedPaths = if (path in s.expandedPaths) s.expandedPaths - path else s.expandedPaths + path)
    }

    fun showToast(message: String) {
        _state.update { it.copy(toast = message) }
    }

    fun consumeToast() = _state.update { it.copy(toast = null) }
}
```

#### Mind map UI