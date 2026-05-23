package com.sunnyb.cardvault.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object CardCropper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun detectCardBounds(context: Context, imageUri: Uri): Rect? = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val result = suspendCancellableCoroutine { cont ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            if (result == null) return@withContext null

            val blocks = result.textBlocks.filter { it.boundingBox != null }
            if (blocks.isEmpty()) return@withContext null

            val union = blocks.mapNotNull { it.boundingBox }.reduce { acc, rect ->
                Rect(
                    minOf(acc.left, rect.left),
                    minOf(acc.top, rect.top),
                    maxOf(acc.right, rect.right),
                    maxOf(acc.bottom, rect.bottom)
                )
            }
            union
        } catch (e: Exception) {
            null
        }
    }

    fun cropBitmap(bitmap: Bitmap, bounds: Rect, paddingFraction: Float = 0.08f): Bitmap {
        val padX = (bounds.width() * paddingFraction).toInt()
        val padY = (bounds.height() * paddingFraction).toInt()
        val x = maxOf(0, bounds.left - padX)
        val y = maxOf(0, bounds.top - padY)
        val w = minOf(bounds.width() + padX * 2, bitmap.width - x)
        val h = minOf(bounds.height() + padY * 2, bitmap.height - y)
        if (w <= 0 || h <= 0) return bitmap
        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }
}
