kotlin
package com.zipstructure.mapper.ui.mindmap

import androidx.compose.ui.geometry.Offset
import com.zipstructure.mapper.data.model.SortMode
import com.zipstructure.mapper.data.model.ZipNode

data class PlacedNode(val node: ZipNode, val position: Offset, val depth: Int, val expanded: Boolean)

data class TreeLayoutResult(
    val placements: List<PlacedNode>,
    val edges: List<Pair<Offset, Offset>>, // parent anchor -> child anchor (px)
    val width: Float,
    val height: Float
)

const val NODE_WIDTH = 190f
const val NODE_HEIGHT = 56f
const val H_SPACING = 260f
const val V_SPACING = 74f

/** Filters, sorts and lays out visible tree nodes into a tidy horizontal mind map. */
fun layoutTree(
    root: ZipNode,
    expandedPaths: Set<String>,
    query: String,
    sortMode: SortMode
): TreeLayoutResult {
    val placements = mutableListOf<PlacedNode>()
    val edges = mutableListOf<Pair<Offset, Offset>>()
    var leafCursor = 0f

    fun matches(node: ZipNode): Boolean =
        query.isBlank() ||
            node.name.contains(query, ignoreCase = true) ||
            node.children.any { matches(it) }

    fun sorted(children: List<ZipNode>): List<ZipNode> {
        val cmp: Comparator<ZipNode> = when (sortMode) {
            SortMode.NAME -> compareBy { it.name.lowercase() }
            SortMode.DATE -> compareByDescending { it.lastModified }
            SortMode.SIZE -> compareByDescending { it.totalSize }
        }
        return children.sortedWith(compareByDescending<ZipNode> { it.isDirectory }.then(cmp))
    }

    fun place(node: ZipNode, depth: Int): Float {
        val expanded = node.path in expandedPaths
        val visibleChildren =
            if (node.isDirectory && expanded) sorted(node.children.filter { matches(it) }) else emptyList()

        val y: Float
        if (visibleChildren.isEmpty()) {
            y = leafCursor * V_SPACING
            leafCursor += 1f
        } else {
            val childYs = visibleChildren.map { place(it, depth + 1) }
            y = (childYs.first() + childYs.last()) / 2f
        }
        val x = depth * H_SPACING
        placements += PlacedNode(node, Offset(x, y), depth, expanded)
        visibleChildren.forEach { child ->
            val childPlaced = placements.first { it.node.path == child.path }
            edges += Offset(x + NODE_WIDTH, y + NODE_HEIGHT / 2f) to
                Offset(childPlaced.position.x, childPlaced.position.y + NODE_HEIGHT / 2f)
        }
        return y
    }

    place(root, 0)
    val width = (placements.maxOf { it.position.x }) + NODE_WIDTH + H_SPACING
    val height = (placements.maxOf { it.position.y }) + NODE_HEIGHT + V_SPACING
    return TreeLayoutResult(placements, edges, width, height)
}