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
    val issuer: String? = null,
    val cardholderName: String? = null,
    val variant: String? = null,
    val product: String? = null
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

            val rawTexts = result.textBlocks.map { it.text }

            val texts = rawTexts.map { normalizeOcr(it) }

            parseCardNumber(texts)?.let { number ->
                ScannedCardInfo(
                    cardNumber = number,
                    expiry = parseExpiry(texts),
                    issuer = parseIssuer(number),
                    cardholderName = parseCardholderName(texts),
                    variant = detectVariant(number),
                    product = parseProduct(texts)
                )
            } ?: ScannedCardInfo()
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
    }

    private fun parseCardNumber(texts: List<String>): String? {
        for (text in texts) {
            val cleaned = text.filter { it.isDigit() || it == ' ' }
            Regex("""\d{13,19}""").findAll(cleaned.replace(" ", ""))
                .maxByOrNull { it.value.length }?.value?.let { return it }
        }

        val combined = texts.joinToString(" ")
        val cleaned = combined.filter { it.isDigit() || it == ' ' }
        val normalized = cleaned.replace(Regex("\\s+"), " ").trim()
        val groups = normalized.split(" ").filter { it.length >= 4 }

        for (i in groups.indices) {
            var concat = groups[i]
            if (concat.length in 13..19) return concat
            for (j in i + 1 until groups.size) {
                concat += groups[j]
                if (concat.length in 13..19) return concat
                if (concat.length > 19) break
            }
        }
        return null
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

    private fun parseCardholderName(texts: List<String>): String? {
        val combined = texts.joinToString(" ")
        val cleaned = combined
            .replace(Regex("""\d{13,19}"""), "")
            .replace(Regex("""\d{2}/\d{2,4}"""), "")
            .replace(Regex("""https?://\S+"""), "")
            .replace(Regex("""www\.\S+"""), "")
            .replace(Regex("""\d{3,4}"""), "")

        val knownWords = setOf(
            "VALID", "THRU", "CARDMEMBER", "SIGNATURE", "AUTHORIZED", "USE",
            "OF", "THIS", "CARD", "IS", "SUBJECT", "TO", "THE", "AGREEMENT",
            "FOR", "ASSISTANCE", "IN", "TOLL", "FREE", "HELPLINE",
            "INTERNATIONAL", "CUSTOMERS", "NOT", "PAYMENT", "FOREIGN",
            "EXCHANGE", "NEPAL", "BHUTAN", "WORLD", "EMERALDE", "PRIVATE",
            "BANK", "VISA", "MASTERCARD", "AMERICAN", "EXPRESS", "RUPAY",
            "DINERS", "CLUB", "JCB", "PLATINUM", "GOLD", "SIGNATURE",
            "INFINITE", "ELITE"
        )

        val tokens = cleaned.split(Regex("\\s+"))
        val isNameToken: (String) -> Boolean = { token ->
            token.length in 2..30 &&
            token.all { c -> c.isUpperCase() || c == '.' } &&
            token.uppercase() !in knownWords &&
            !token.any { it.isDigit() }
        }

        val sequences = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (token in tokens) {
            if (isNameToken(token)) {
                current.add(token)
            } else {
                if (current.size >= 2) sequences.add(current.toList())
                current = mutableListOf()
            }
        }
        if (current.size >= 2) sequences.add(current.toList())

        if (sequences.isEmpty()) {
            for (token in tokens) {
                if (isNameToken(token) && token.length >= 3) {
                    return token
                }
            }
            return null
        }

        return sequences.maxByOrNull { it.sumOf { w -> w.length } }?.joinToString(" ")
    }

    private fun parseProduct(texts: List<String>): String? {
        val combined = texts.joinToString(" ")
        val knownIssuers = setOf(
            "ICICI", "HDFC", "AXIS", "SBI", "YES", "KOTAK", "INDUSIND",
            "RBL", "FEDERAL", "IDBI", "CANARA", "PNB", "BOB", "UNION",
            "CITI", "HSBC", "STANDARD", "CHARTERED", "AMEX", "AMERICAN",
            "CHASE", "WELLS", "FARGO", "BARCLAYS", "CAPITAL", "ONE",
            "DISCOVER", "BANK", "OF", "AMERICA"
        )

        val words = combined.split(Regex("\\s+"))
        for (i in words.indices) {
            if (words[i].uppercase() in knownIssuers) {
                for (j in i + 1 until minOf(i + 4, words.size)) {
                    val candidate = words[j].replace(Regex("[^A-Za-z]"), "")
                    if (candidate.length >= 3 && candidate[0].isUpperCase()) {
                        val next = if (j + 1 < words.size && words[j + 1].length in 3..20
                            && words[j + 1][0].isUpperCase()
                            && words[j + 1].all { it.isLetter() }) {
                            "$candidate ${words[j + 1]}"
                        } else candidate
                        return next
                    }
                }
            }
        }
        return null
    }
}