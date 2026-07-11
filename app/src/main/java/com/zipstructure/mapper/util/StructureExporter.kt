package com.zipstructure.mapper.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.zipstructure.mapper.data.model.ZipNode
import java.io.File

object StructureExporter {

    fun toMarkdown(root: ZipNode): String = buildString {
        appendLine("# ${root.name}")
        root.children.forEach { render(it, 0) }
    }

    private fun StringBuilder.render(node: ZipNode, depth: Int) {
        append("  ".repeat(depth)).append("- ")
        append(if (node.isDirectory) "📁 **${node.name}**" else "📄 ${node.name} (${formatSize(node.size)})")
        appendLine()
        node.children.forEach { render(it, depth + 1) }
    }

    fun toIndentedText(root: ZipNode): String = buildString {
        appendLine(root.name)
        root.children.forEachIndexed { i, c -> renderTree(c, "", i == root.children.lastIndex) }
    }

    private fun StringBuilder.renderTree(node: ZipNode, prefix: String, last: Boolean) {
        append(prefix).append(if (last) "└── " else "├── ").appendLine(node.name)
        val childPrefix = prefix + if (last) "    " else "│   "
        node.children.forEachIndexed { i, c -> renderTree(c, childPrefix, i == node.children.lastIndex) }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    /** Exports structure text to public Downloads. Returns the written file name or null. */
    fun exportToDownloads(context: Context, fileName: String, content: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            fileName
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            File(dir, fileName).writeText(content)
            fileName
        }
    } catch (_: Exception) { null }
}