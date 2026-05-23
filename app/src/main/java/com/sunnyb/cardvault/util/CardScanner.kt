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

data class OcrTextBlock(
    val text: String,
    val left: Int, val top: Int, val right: Int, val bottom: Int
)

data class ScannedCardInfo(
    val cardNumber: String? = null,
    val expiry: String? = null,
    val variant: String? = null,
    val detectedTexts: List<String> = emptyList(),
    val textBlocks: List<OcrTextBlock> = emptyList()
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

            val rawTextBlocks = result.textBlocks
            val rawTexts = rawTextBlocks.map { it.text }
            val detectedTexts = rawTextBlocks.map { it.text.trim() }.filter { it.isNotBlank() }
            val textBlocks = rawTextBlocks.mapNotNull { block ->
                val box = block.boundingBox
                if (box != null) OcrTextBlock(block.text.trim(), box.left, box.top, box.right, box.bottom)
                else null
            }

            val texts = rawTexts.map { normalizeOcr(it) }

            val number = parseCardNumber(texts)
            val expiry = parseExpiry(texts)

            ScannedCardInfo(
                cardNumber = number,
                expiry = expiry,
                variant = if (number != null) detectVariant(number) else null,
                detectedTexts = detectedTexts,
                textBlocks = textBlocks
            )
        } catch (e: Exception) {
            ScannedCardInfo()
        }
    }

    private fun normalizeOcr(text: String): String {
        return text
            .replace('l', '1')
            .replace('I', '1')
            .replace('O', '0')
            .replace('o', '0')
            .replace('S', '5')
            .replace('s', '5')
            .replace('A', '4')
            .replace('B', '8')
            .replace('b', '8')
            .replace('G', '6')
            .replace('g', '6')
    }

    private fun parseCardNumber(texts: List<String>): String? {
        for (text in texts) {
            val cleaned = text.filter { it.isDigit() || it == ' ' }
            Regex("""\d{13,19}""").findAll(cleaned.replace(" ", ""))
                .maxByOrNull { it.value.length }?.value?.let {
                    val corrected = correctOcrErrors(it)
                    if (corrected != null) return corrected
                }
        }

        val combined = texts.joinToString(" ")
        val cleaned = combined.filter { it.isDigit() || it == ' ' }
        val normalized = cleaned.replace(Regex("\\s+"), " ").trim()
        val groups = normalized.split(" ").filter { it.length >= 4 }

        val commonLength = groups.groupBy { it.length }.maxByOrNull { it.value.size }?.key ?: 4
        val sameLenGroups = groups.filter { it.length == commonLength }

        for (i in sameLenGroups.indices) {
            var concat = sameLenGroups[i]
            val corrected = correctOcrErrors(concat)
            if (corrected != null) return corrected
            for (j in i + 1 until sameLenGroups.size) {
                concat += sameLenGroups[j]
                val c = correctOcrErrors(concat)
                if (c != null) return c
                if (concat.length > 19) break
            }
        }

        for (i in groups.indices) {
            var concat = groups[i]
            val corrected = correctOcrErrors(concat)
            if (corrected != null) return corrected
            for (j in i + 1 until groups.size) {
                concat += groups[j]
                val c = correctOcrErrors(concat)
                if (c != null) return c
                if (concat.length > 19) break
            }
        }
        return null
    }

    private fun isValidCardNumber(digits: String): Boolean {
        if (digits.any { !it.isDigit() }) return false
        val first = digits.first()
        if (first !in listOf('3', '4', '5', '6')) return false
        if (digits.length < 13 || digits.length > 19) return false
        return passesLuhn(digits)
    }

    private fun passesLuhn(digits: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in digits.indices.reversed()) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun correctOcrErrors(digits: String): String? {
        if (digits.length < 13 || digits.length > 19) return null
        if (isValidCardNumber(digits)) return digits

        val confusionMap = mapOf(
            '0' to "86", '8' to "096", '6' to "05",
            '1' to "7", '7' to "1",
            '3' to "89", '9' to "538",
            '5' to "69", '4' to "", '2' to ""
        )

        val best = mutableListOf<Triple<Int, Char, Char>>()
        for (i in digits.indices) {
            val original = digits[i]
            val alternatives = confusionMap[original] ?: ""
            for (alt in alternatives) {
                val candidate = digits.substring(0, i) + alt + digits.substring(i + 1)
                if (candidate.length in 13..19 && passesLuhn(candidate) && candidate.first() in listOf('3', '4', '5', '6')) {
                    best.add(Triple(i, original, alt))
                }
            }
        }

        if (best.size == 1) {
            val (idx, _, alt) = best.first()
            return digits.substring(0, idx) + alt + digits.substring(idx + 1)
        }

        if (best.size > 1) {
            for (attempt in best) {
                val (idx, _, alt) = attempt
                val corrected = digits.substring(0, idx) + alt + digits.substring(idx + 1)
                val existing = corrected.substring(0, 6)
                if (existing.startsWith("4") || existing.matches("^5[1-5]".toRegex()) || existing.matches("^3[47]".toRegex())) {
                    return corrected
                }
            }
        }

        return null
    }

    private fun parseExpiry(texts: List<String>): String? {
        val expiryPattern = Regex("""(0[1-9]|1[0-2])/(\d{2}|\d{4})""")
        val shortPattern = Regex("""(0[1-9]|1[0-2])\s*/\s*(\d{2}|\d{4})""")

        data class ExpiryDate(val mm: Int, val yy: Int)
        val found = mutableListOf<ExpiryDate>()

        for (text in texts) {
            expiryPattern.findAll(text).forEach {
                val mm = it.groupValues[1].toIntOrNull() ?: 0
                val yy = it.groupValues[2].take(2).toIntOrNull() ?: 0
                if (mm in 1..12) found.add(ExpiryDate(mm, if (yy < 100) yy + 2000 else yy))
            }
            shortPattern.findAll(text).forEach {
                val mm = it.groupValues[1].toIntOrNull() ?: 0
                val yy = it.groupValues[2].take(2).toIntOrNull() ?: 0
                if (mm in 1..12) found.add(ExpiryDate(mm, if (yy < 100) yy + 2000 else yy))
            }
        }

        if (found.isNotEmpty()) {
            val latest = found.maxByOrNull { it.yy * 100 + it.mm }!!
            val mmStr = latest.mm.toString().padStart(2, '0')
            val yyStr = (latest.yy % 100).toString().padStart(2, '0')
            return "$mmStr/$yyStr"
        }

        val digitPairs = texts.flatMap { text ->
            Regex("""\b(\d{2})\s*/\s*(\d{2})\b""").findAll(text).map {
                val mm = it.groupValues[1].toIntOrNull() ?: 0
                val yy = it.groupValues[2]
                if (mm in 1..12) "$mm/$yy" else null
            }
        }
        digitPairs.firstOrNull()?.let { return it }

        for (text in texts) {
            Regex("""(\d{1})\s*/\s*(\d{2})""").findAll(text).forEach {
                val mm = it.groupValues[1].toIntOrNull() ?: 0
                val yy = it.groupValues[2]
                if (mm in 1..9) return "0$mm/$yy"
            }
        }

        return null
    }

    private fun detectVariant(cardNumber: String): String? {
        return when {
            cardNumber.matches("^35(2[89]|[3-8][0-9]).*".toRegex()) -> "JCB"
            cardNumber.matches("^3[47].*".toRegex()) -> "American Express"
            cardNumber.matches("^3(0[0-5]|[68][0-9]).*".toRegex()) -> "Diners Club"
            cardNumber.matches("^(60|65|81|82|50|6363|6271|6354)[0-9].*".toRegex()) -> "RuPay"
            cardNumber.matches("^5[1-5].*".toRegex()) -> "Mastercard"
            cardNumber.matches("^4[0-9].*".toRegex()) -> "Visa"
            cardNumber.matches("^(5018|5020|5038|5[6-9]|6[0-9])[0-9].*".toRegex()) -> "Maestro"
            else -> null
        }
    }

    fun cropToCard(bitmap: Bitmap, textBlocks: List<OcrTextBlock>): Bitmap {
        if (textBlocks.isEmpty()) {
            val marginX = (bitmap.width * 0.08f).toInt()
            val marginY = (bitmap.height * 0.15f).toInt()
            val cx = bitmap.width / 2
            val cy = bitmap.height / 2
            val cropW = bitmap.width - marginX * 2
            val cropH = bitmap.height - marginY * 2
            if (cropW <= 0 || cropH <= 0) return bitmap
            return Bitmap.createBitmap(bitmap,
                maxOf(0, cx - cropW / 2), maxOf(0, cy - cropH / 2),
                minOf(cropW, bitmap.width), minOf(cropH, bitmap.height))
        }

        var minLeft = Int.MAX_VALUE
        var minTop = Int.MAX_VALUE
        var maxRight = 0
        var maxBottom = 0
        for (block in textBlocks) {
            if (block.left < minLeft) minLeft = block.left
            if (block.top < minTop) minTop = block.top
            if (block.right > maxRight) maxRight = block.right
            if (block.bottom > maxBottom) maxBottom = block.bottom
        }

        val pad = 30
        val x = maxOf(0, minLeft - pad)
        val y = maxOf(0, minTop - pad)
        val w = minOf(maxRight - minLeft + pad * 2, bitmap.width - x)
        val h = minOf(maxBottom - minTop + pad * 2, bitmap.height - y)
        if (w <= 0 || h <= 0) return bitmap
        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }
}