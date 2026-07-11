package com.zipstructure.mapper.data.model

data class ZipNode(
    val name: String,
    val path: String,          // full path within the zip, unique id
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val children: MutableList<ZipNode> = mutableListOf()
) {
    val totalSize: Long
        get() = if (isDirectory) children.sumOf { it.totalSize } else size

    fun descendantCount(): Int = children.size + children.sumOf { it.descendantCount() }
}

enum class SortMode(val label: String) {
    NAME("Name"), DATE("Modified"), SIZE("Size")
}