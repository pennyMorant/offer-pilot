package com.overud.capture

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** User-selected images only; no connection to live capture, acceptance or statistics. */
class ImportedImageRecognizer(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun recognize(uri: Uri): OcrPageResult = withContext(Dispatchers.IO) {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > 3072) {
                val scale = 3072.0 / longest
                decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1))
            }
        }
        try {
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            try {
                // Await the ML task even if the caller closes the dialog, then release its bitmap.
                suspendCoroutine { continuation ->
                    recognizer.process(InputImage.fromBitmap(bitmap, 0))
                        .addOnSuccessListener { continuation.resume(it.toOcrPageResult()) }
                        .addOnFailureListener { continuation.resumeWithException(it) }
                }
            } finally { recognizer.close() }
        } finally { bitmap.recycle() }
    }
}
