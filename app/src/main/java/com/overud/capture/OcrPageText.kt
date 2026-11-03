package com.overud.capture

import com.google.mlkit.vision.text.Text
import com.overud.model.TextRegion
import com.overud.platform.SpatialPageText

/** Live and imported images use identical line geometry; no image or text is persisted. */
internal fun Text.orderedPageText(): String {
    val lines = textBlocks.flatMap { it.lines }
    if (lines.size > 500) return ""
    val regions = lines.mapNotNull { line ->
        line.boundingBox?.let { TextRegion(line.text, it.left, it.top, it.right, it.bottom) }
    }
    return SpatialPageText.assemble(regions).take(30_000)
}
