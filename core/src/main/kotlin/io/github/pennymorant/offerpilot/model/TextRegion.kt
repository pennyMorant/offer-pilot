package io.github.pennymorant.offerpilot.model

/** Temporary OCR/node line geometry; never stored in the database. */
data class TextRegion(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
    val valid: Boolean get() = text.isNotBlank() && right > left && bottom > top
}
