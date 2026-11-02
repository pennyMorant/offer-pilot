package com.overud.bridge

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import com.overud.model.Platform

class ScreenshotReader(private val connection: UiAutomationConnection) {
    private val supported = Platform.entries.map { it.packageName }.toSet()
    fun screenshot(expectedPackage: String): ParcelFileDescriptor? {
        if (expectedPackage !in supported) return null
        if (connection.get().rootInActiveWindow?.packageName?.toString() != expectedPackage) return null
        val bitmap = connection.get().takeScreenshot() ?: return null
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            try { runCatching { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } } }
            finally { bitmap.recycle() }
        }.start()
        return pipe[0]
    }

}
