kotlin
package com.zipstructure.mapper.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.zipstructure.mapper.ui.theme.NeonCyan
import com.zipstructure.mapper.ui.theme.NeonPurple
import com.zipstructure.mapper.ui.theme.TextPrimary

@Composable
fun BubbleCollapsed() {
    Box(
        Modifier
            .size(58.dp)
            .shadow(10.dp, CircleShape)
            .background(Brush.linearGradient(listOf(NeonPurple, NeonCyan)), CircleShape)
            .border(1.dp, TextPrimary.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.FolderZip, "ZipStructureMapper bubble", tint = TextPrimary)
    }
}