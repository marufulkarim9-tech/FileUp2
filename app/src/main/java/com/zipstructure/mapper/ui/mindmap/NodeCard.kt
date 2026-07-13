kotlin
package com.zipstructure.mapper.ui.mindmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zipstructure.mapper.data.model.ZipNode
import com.zipstructure.mapper.ui.theme.*
import com.zipstructure.mapper.util.formatSize

fun iconFor(node: ZipNode): ImageVector = when {
    node.isDirectory -> Icons.Filled.Folder
    else -> when (node.name.substringAfterLast('.', "").lowercase()) {
        "png", "jpg", "jpeg", "gif", "webp", "svg" -> Icons.Filled.Image
        "mp4", "mkv", "avi", "webm" -> Icons.Filled.Movie
        "mp3", "wav", "ogg", "flac" -> Icons.Filled.MusicNote
        "kt", "java", "js", "ts", "py", "c", "cpp", "rs", "go" -> Icons.Filled.Code
        "zip", "rar", "7z", "tar", "gz" -> Icons.Filled.FolderZip
        "pdf" -> Icons.Filled.PictureAsPdf
        "txt", "md", "json", "xml", "yaml", "yml" -> Icons.Filled.Description
        else -> Icons.Filled.InsertDriveFile
    }
}

fun accentFor(node: ZipNode): Color =
    if (node.isDirectory) NeonPurple else when (iconFor(node)) {
        Icons.Filled.Image -> NeonPink
        Icons.Filled.Code -> NeonGreen
        else -> NeonCyan
    }

@Composable
fun NodeCard(
    node: ZipNode,
    expanded: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val accent = accentFor(node)
    Row(
        modifier = modifier
            .size(width = 190.dp, height = 56.dp)
            .background(
                Brush.horizontalGradient(listOf(SpaceSurfaceHigh, SpaceSurface)),
                RoundedCornerShape(14.dp)
            )
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) NeonCyan else accent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = node.isDirectory) { onToggle() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(iconFor(node), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                node.name.ifEmpty { "/" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (node.isDirectory) "${node.descendantCount()} items · ${formatSize(node.totalSize)}"
                else formatSize(node.size),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1
            )
        }
        if (node.isDirectory && node.children.isNotEmpty()) {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}