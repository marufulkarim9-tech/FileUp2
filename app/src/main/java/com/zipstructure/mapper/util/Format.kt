package com.zipstructure.mapper.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
    return String.format(Locale.US, "%.1f %s", value, units[unit])
}

fun formatDate(millis: Long): String =
    if (millis <= 0) "—"
    else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))