package com.sunnyb.cardvault.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object CardCropper {

    private val detector by lazy {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }

    suspend fun detectCardBounds(context: Context, imageUri: Uri): Rect? = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val objects = suspendCancellableCoroutine<List<DetectedObject>?> { cont ->
                detector.process(inputImage)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            if (objects.isNullOrEmpty()) return@withContext null
            objects.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height() }?.boundingBox
        } catch (e: Exception) {
            null
        }
    }

    fun cropBitmap(bitmap: Bitmap, bounds: Rect, paddingFraction: Float = 0.05f): Bitmap {
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
