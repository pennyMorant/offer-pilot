package com.overud.capture

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.overud.bridge.SheveryBridge
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OcrRecognizer(private val bridge: SheveryBridge) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    suspend fun recognize(pkg: String): String? {
        val descriptor = bridge.service?.screenshot(pkg) ?: return null
        val bitmap = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { BitmapFactory.decodeStream(it) } ?: return null
        return try {
            suspendCoroutine { continuation ->
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { continuation.resume(it.text) }
                    .addOnFailureListener { continuation.resume(null) }
            }
        } finally { bitmap.recycle() }
    }
}
