package com.overud.platform

import com.overud.model.TextRegion

/** Preserve reading order and join split badges/route metrics without flattening ML blocks first. */
object SpatialPageText {
    fun assemble(regions: List<TextRegion>): String {
        val ordered = regions.filter { it.text.isNotBlank() && it.right > it.left && it.bottom > it.top }
            .sortedWith(compareBy<TextRegion> { it.top }.thenBy { it.left })
        val rows = mutableListOf<MutableList<TextRegion>>()
        for (line in ordered) {
            val row = rows.lastOrNull()
            val anchor = row?.firstOrNull()
            val overlap = anchor?.let { minOf(it.bottom, line.bottom) - maxOf(it.top, line.top) } ?: 0
            val sameRow = anchor != null && overlap * 2 >= minOf(anchor.bottom - anchor.top, line.bottom - line.top) &&
                row?.none { it.left < line.right && line.left < it.right } == true
            if (sameRow) row!!.add(line) else rows += mutableListOf(line)
        }
        return rows.joinToString("\n") { row -> row.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
    }
}
