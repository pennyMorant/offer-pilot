package com.overud.ui.diagnostics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.overud.model.NodeBounds

/** Schematic node geometry, not a screenshot. Coordinates remain relative to the captured window. */
@Composable fun NodeRegionDiagram(state: NodeDiagnosticState) {
    val window = state.nodes.firstOrNull()?.bounds?.takeIf { it.valid } ?: return
    val neutral = MaterialTheme.colorScheme.outlineVariant
    val ocr = MaterialTheme.colorScheme.tertiary
    val selected = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(260.dp)) {
        val width = (window.right.toFloat() - window.left).coerceAtLeast(1f)
        val height = (window.bottom.toFloat() - window.top).coerceAtLeast(1f)
        val scale = minOf(size.width / width, size.height / height)
        val origin = Offset((size.width - width * scale) / 2, (size.height - height * scale) / 2)
        fun outline(bounds: NodeBounds, color: androidx.compose.ui.graphics.Color, thickness: Float) {
            if (!window.contains(bounds)) return
            drawRect(color, origin + Offset((bounds.left - window.left) * scale, (bounds.top - window.top) * scale),
                Size((bounds.right - bounds.left) * scale, (bounds.bottom - bounds.top) * scale), style = Stroke(thickness))
        }
        outline(window, neutral, 2.dp.toPx())
        state.nodes.filter { it.visible }.forEach { outline(it.bounds, neutral, 0.5.dp.toPx()) }
        state.ocrBounds?.let { outline(it, ocr, 3.dp.toPx()) }
        state.nodes.firstOrNull { it.index == state.selectedNode }?.let { outline(it.bounds, selected, 4.dp.toPx()) }
    }
}
