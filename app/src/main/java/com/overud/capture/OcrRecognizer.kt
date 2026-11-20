package com.overud.capture

import android.graphics.BitmapFactory
import android.os.Bundle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.overud.bridge.SheveryBridge
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OcrRecognizer(private val bridge: SheveryBridge) {
    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    suspend fun recognize(frame: Bundle): OcrPageResult? {
        val descriptor = bridge.service?.screenshot(frame) ?: return null
        val bitmap = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { BitmapFactory.decodeStream(it) } ?: return null
        return try {
            suspendCoroutine { continuation ->
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { continuation.resume(it.toOcrPageResult()) }
                    .addOnFailureListener { continuation.resume(null) }
            }
        } finally { bitmap.recycle() }
    }
}
