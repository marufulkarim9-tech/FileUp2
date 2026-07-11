```kotlin
package com.zipstructure.mapper.util

import android.content.Context
import android.os.Environment
import java.io.File

data class StorageRoot(val label: String, val dir: File)

object StorageRoots {

    /** Discovers app sandbox + all mounted physical storage roots. */
    fun discover(context: Context): List<StorageRoot> {
        val roots = mutableListOf(StorageRoot("App Sandbox", context.filesDir))

        val primary = Environment.getExternalStorageDirectory() // /storage/emulated/0
        if (primary != null && primary.canRead()) roots += StorageRoot("Internal Storage", primary)

        // Derive removable roots from getExternalFilesDirs: /storage/XXXX-XXXX/Android/data/pkg/files
        context.getExternalFilesDirs(null).filterNotNull().forEach { appDir ->
            val root = generateSequence(appDir) { it.parentFile }
                .firstOrNull { it.name.matches(Regex("[0-9A-F]{4}-[0-9A-F]{4}")) }
            if (root != null && root.canRead() && roots.none { it.dir.absolutePath == root.absolutePath }) {
                roots += StorageRoot("SD Card (${root.name})", root)
            }
        }
        return roots
    }
}
```

#### ViewModel