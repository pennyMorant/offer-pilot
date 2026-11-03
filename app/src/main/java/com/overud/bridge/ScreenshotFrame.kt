package com.overud.bridge

import android.os.Bundle
import com.overud.model.Platform

/** Binds screen pixels to the entire node structure, not just the selected quote text. */
object ScreenshotFrame {
    fun matches(expected: Bundle, actual: Bundle): Boolean {
        val pkg = expected.getString("package") ?: return false
        val token = expected.getString("frameToken")?.takeIf(String::isNotBlank) ?: return false
        return Platform.fromPackage(pkg) != null && !actual.containsKey("error") &&
            expected.getInt("windowId", -1) >= 0 && actual.getInt("windowId", -1) == expected.getInt("windowId") &&
            expected.getInt("displayId", -1) == 0 && actual.getInt("displayId", -1) == 0 &&
            actual.getString("package") == pkg && actual.getString("frameToken") == token &&
            sameBounds(expected.getIntArray("ocrBounds"), actual.getIntArray("ocrBounds"))
    }

    private fun sameBounds(first: IntArray?, second: IntArray?) =
        if (first == null) second == null else second != null && first.contentEquals(second)
}
