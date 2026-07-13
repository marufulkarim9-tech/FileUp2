kotlin
package com.zipstructure.mapper.ui.mindmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.zipstructure.mapper.data.model.SortMode
import com.zipstructure.mapper.data.model.ZipNode
import com.zipstructure.mapper.ui.theme.BranchColor
import com.zipstructure.mapper.ui.theme.NeonPurple
import kotlin.math.roundToInt

@Composable
fun MindMapCanvas(
    root: ZipNode,
    expandedPaths: Set<String>,
    query: String,
    sortMode: SortMode,
    captureLayer: GraphicsLayer = rememberGraphicsLayer(),
    onToggle: (String) -> Unit
) {
    var scale by remember { mutableFloatStateOf(0.8f) }
    var pan by remember { mutableStateOf(Offset(60f, 120f)) }

    val layout = remember(root, expandedPaths, query, sortMode) {
        layoutTree(root, expandedPaths, query, sortMode)
    }
    val density = LocalDensity.current

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.2f, 3f)
                    pan += panChange
                }
            }
            // Record everything into a GraphicsLayer so "Save Visualization" can rasterize it.
            .drawWithContent {
                captureLayer.record { this@drawWithContent.drawContent() }
                drawLayer(captureLayer)
            }
    ) {
        Box(
            Modifier.graphicsLayer {
                translationX = pan.x
                translationY = pan.y
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        ) {
            // Bezier branch lines between parents and children.
            Canvas(Modifier.fillMaxSize()) {
                layout.edges.forEach { (from, to) ->
                    val midX = (from.x + to.x) / 2f
                    val path = Path().apply {
                        moveTo(from.x, from.y)
                        cubicTo(midX, from.y, midX, to.y, to.x, to.y)
                    }
                    drawPath(
                        path,
                        brush = Brush.linearGradient(
                            colors = listOf(NeonPurple.copy(alpha = 0.7f), BranchColor),
                            start = from,
                            end = to
                        ),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
            }
            // Node cards.
            layout.placements.forEach { placed ->
                key(placed.node.path) {
                    NodeCard(
                        node = placed.node,
                        expanded = placed.expanded,
                        highlighted = query.isNotBlank() &&
                            placed.node.name.contains(query, ignoreCase = true),
                        modifier = Modifier.offset {
                            IntOffset(placed.position.x.roundToInt(), placed.position.y.roundToInt())
                        },
                        onToggle = { onToggle(placed.node.path) }
                    )
                }
            }
        }
    }
}