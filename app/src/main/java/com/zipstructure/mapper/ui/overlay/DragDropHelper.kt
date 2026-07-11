package com.zipstructure.mapper.ui.overlay

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.view.View
import androidx.core.content.FileProvider
import java.io.File

object DragDropHelper {

    /**
     * Starts a system-wide drag with FileProvider-secured URIs so files can be
     * dropped into external apps (Slack, Gmail, file managers) or the main app.
     */
    fun startGlobalDrag(context: Context, view: View, files: List<File>): Boolean {
        if (files.isEmpty()) return false
        val authority = "${context.packageName}.fileprovider"
        val uris = files.mapNotNull { runCatching { FileProvider.getUriForFile(context, authority, it) }.getOrNull() }
        if (uris.isEmpty()) return false

        val clip = ClipData(
            ClipDescription("zip-mapper-files", arrayOf(ClipDescription.MIMETYPE_TEXT_URILIST)),
            ClipData.Item(uris.first())
        )
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }

        return view.startDragAndDrop(
            clip,
            View.DragShadowBuilder(view),
            null,
            View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
        )
    }
}