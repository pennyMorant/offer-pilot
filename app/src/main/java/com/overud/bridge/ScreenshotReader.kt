package com.overud.bridge

import android.graphics.Bitmap
import android.os.Bundle
import android.os.ParcelFileDescriptor
import java.util.concurrent.atomic.AtomicBoolean

class ScreenshotReader(private val connection: UiAutomationConnection, private val windows: WindowReader) {
    private val busy = AtomicBoolean(false)

    fun screenshot(expectedFrame: Bundle): ParcelFileDescriptor? {
        if (!busy.compareAndSet(false, true)) return null
        var bitmap: Bitmap? = null
        var transferred = false
        try {
            val before = windows.snapshot()
            if (!ScreenshotFrame.matches(expectedFrame, before)) return null
            val full = connection.get().takeScreenshot() ?: return null
            bitmap = full
            if (!ScreenshotFrame.matches(before, windows.snapshot())) return null
            val bounds = before.getIntArray("ocrBounds")
            if (bounds != null) {
                if (bounds.size != 4 || bounds[0] < 0 || bounds[1] < 0 || bounds[2] > full.width ||
                    bounds[3] > full.height || bounds[2] <= bounds[0] || bounds[3] <= bounds[1]) return null
                val cropped = Bitmap.createBitmap(full, bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1])
                bitmap = cropped
                if (cropped !== full) full.recycle()
            }
            val image = bitmap ?: return null
            val pipe = ParcelFileDescriptor.createPipe()
            try {
                Thread({
                    try {
                        runCatching { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use {
                            image.compress(Bitmap.CompressFormat.JPEG, 85, it)
                        } }
                    } finally { image.recycle(); busy.set(false) }
                }, "overud-screenshot-pipe").apply { isDaemon = true }.start()
            } catch (e: Exception) {
                runCatching { pipe[0].close() }
                runCatching { pipe[1].close() }
                throw e
            }
            transferred = true
            return pipe[0]
        } finally {
            if (!transferred) { bitmap?.recycle(); busy.set(false) }
        }
    }
}
