package io.github.pennymorant.offerpilot.capture

import com.google.mlkit.vision.text.Text
import io.github.pennymorant.offerpilot.model.TextRegion
import io.github.pennymorant.offerpilot.platform.SpatialPageText

/** Temporary OCR text + line geometry; never persisted to disk or database. */
data class OcrPageResult(
    val text: String,
    val regions: List<TextRegion>,
)

/** Live and imported images use identical line geometry; no image or text is persisted. */
internal fun Text.toOcrPageResult(): OcrPageResult {
    val lines = textBlocks.flatMap { it.lines }
    if (lines.size > 500) return OcrPageResult("", emptyList())
    val regions = lines.mapNotNull { line ->
        line.boundingBox?.let { TextRegion(line.text, it.left, it.top, it.right, it.bottom) }
    }
    return OcrPageResult(SpatialPageText.assemble(regions).take(30_000), regions)
}
