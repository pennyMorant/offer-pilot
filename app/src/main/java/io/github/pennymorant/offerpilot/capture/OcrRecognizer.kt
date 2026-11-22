package io.github.pennymorant.offerpilot.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.pennymorant.offerpilot.bridge.SheveryBridge
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OcrRecognizer(private val bridge: SheveryBridge) {
    private val latin by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val chinese by lazy { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }

    suspend fun recognize(frame: Bundle): OcrPageResult? {
        val descriptor = bridge.service?.screenshot(frame) ?: return null
        val bitmap = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { BitmapFactory.decodeStream(it) } ?: return null
        return try {
            val primary = runRecognizer(latin, bitmap)
            if (primary != null && needsChineseFallback(primary.text)) {
                runRecognizer(chinese, bitmap) ?: primary
            } else primary
        } finally { bitmap.recycle() }
    }

    private suspend fun runRecognizer(client: TextRecognizer, bitmap: Bitmap): OcrPageResult? =
        suspendCoroutine { continuation ->
            client.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { continuation.resume(it.toOcrPageResult()) }
                .addOnFailureListener { continuation.resume(null) }
        }

    companion object {
        internal fun needsChineseFallback(text: String): Boolean {
            if (text.isBlank()) return true
            val hasLatinOfferCue = text.contains('$') ||
                text.contains("accept", ignoreCase = true) ||
                text.contains("match", ignoreCase = true) ||
                Regex("\\b\\d+(?:\\.\\d+)?\\s*mi\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
            return !hasLatinOfferCue
        }
    }
}
