```kotlin
package com.zipstructure.mapper.data

import android.content.Context
import android.net.Uri
import com.zipstructure.mapper.data.model.ZipNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

object ZipParser {

    suspend fun parse(context: Context, uri: Uri): ZipNode = withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(context, uri) ?: "archive.zip"
        val root = ZipNode(name = fileName, path = "", isDirectory = true, size = 0L, lastModified = 0L)
        val dirIndex = HashMap<String, ZipNode>().apply { put("", root) }

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val rawPath = entry.name.trimEnd('/')
                    if (rawPath.isNotEmpty()) {
                        val parent = ensureParents(rawPath, dirIndex)
                        if (entry.isDirectory) {
                            ensureDir(rawPath, parent, dirIndex, entry.time)
                        } else {
                            parent.children.add(
                                ZipNode(
                                    name = rawPath.substringAfterLast('/'),
                                    path = rawPath,
                                    isDirectory = false,
                                    size = if (entry.size >= 0) entry.size else 0L,
                                    lastModified = entry.time
                                )
                            )
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: error("Unable to open stream for $uri")
        root
    }

    private fun ensureParents(path: String, index: HashMap<String, ZipNode>): ZipNode {
        val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isEmpty()) return index.getValue("")
        index[parentPath]?.let { return it }
        val grand = ensureParents(parentPath, index)
        return ensureDir(parentPath, grand, index, 0L)
    }

    private fun ensureDir(path: String, parent: ZipNode, index: HashMap<String, ZipNode>, time: Long): ZipNode {
        index[path]?.let { return it }
        val node = ZipNode(
            name = path.substringAfterLast('/'),
            path = path,
            isDirectory = true,
            size = 0L,
            lastModified = time
        )
        parent.children.add(node)
        index[path] = node
        return node
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
}
```

#### Utilities