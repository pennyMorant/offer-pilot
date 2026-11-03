package com.overud.model

/** Temporary OCR line geometry; never stored in the database. */
data class TextRegion(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)
