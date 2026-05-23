package com.sunnyb.cardvault.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class ScannedCardInfo(
    val cardNumber: String? = null,
    val expiry: String? = null,
    val issuer: String? = null
)

object CardScanner {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun scan(context: Context, imageUri: Uri): ScannedCardInfo = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val result = suspendCancellableCoroutine { cont ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { text ->
                        cont.resume(text)
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            }

            if (result == null) return@withContext ScannedCardInfo()

            val allText = result.textBlocks.map { it.text }

            parseCardNumber(allText)?.let { number ->
                ScannedCardInfo(
                    cardNumber = number,
                    expiry = parseExpiry(allText),
                    issuer = parseIssuer(number)
                )
            } ?: ScannedCardInfo()
        } catch (e: Exception) {
            ScannedCardInfo()
        }
    }

    private fun parseCardNumber(texts: List<String>): String? {
        val combined = texts.joinToString(" ")
        val cleaned = combined.filter { it.isDigit() || it == ' ' }
        val digitGroups = Regex("""\d{13,19}""").findAll(cleaned.replace(" ", ""))
        return digitGroups.toList().maxByOrNull { it.value.length }?.value
    }

    private fun parseExpiry(texts: List<String>): String? {
        val expiryPattern = Regex("""(0[1-9]|1[0-2])/(\d{2}|\d{4})""")
        val shortPattern = Regex("""(0[1-9]|1[0-2])\s*/\s*(\d{2}|\d{4})""")

        for (text in texts) {
            expiryPattern.find(text)?.let {
                val mm = it.groupValues[1]
                val yy = it.groupValues[2].take(2)
                return "$mm/$yy"
            }
            shortPattern.find(text)?.let {
                val mm = it.groupValues[1]
                val yy = it.groupValues[2].take(2)
                return "$mm/$yy"
            }
        }

        val digitPairs = texts.flatMap { text ->
            Regex("""\b(\d{2})\s*/\s*(\d{2})\b""").findAll(text).map {
                val mm = it.groupValues[1].toIntOrNull() ?: 0
                val yy = it.groupValues[2]
                if (mm in 1..12) "$mm/$yy" else null
            }
        }
        return digitPairs.firstOrNull()
    }

    private fun parseIssuer(cardNumber: String): String? {
        return when {
            cardNumber.startsWith("4") -> "Visa"
            cardNumber.matches("^5[1-5].*".toRegex()) -> "Mastercard"
            cardNumber.matches("^3[47].*".toRegex()) -> "Amex"
            cardNumber.startsWith("6") -> "Discover"
            cardNumber.matches("^35(2[89]|[3-8][0-9]).*".toRegex()) -> "JCB"
            else -> null
        }
    }
}