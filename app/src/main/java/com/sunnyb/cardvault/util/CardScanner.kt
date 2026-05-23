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

            val number = parseCardNumber(texts)
            val expiry = parseExpiry(texts)

            ScannedCardInfo(
                cardNumber = number,
                expiry = expiry,
                issuer = detectIssuerFromText(rawTexts),
                cardholderName = parseCardholderName(rawTexts),
                variant = if (number != null) detectVariant(number) else null,
                product = parseProduct(rawTexts)
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
    }

    private fun parseCardNumber(texts: List<String>): String? {
        for (text in texts) {
            val cleaned = text.filter { it.isDigit() || it == ' ' }
            Regex("""\d{13,19}""").findAll(cleaned.replace(" ", ""))
                .maxByOrNull { it.value.length }?.value?.let {
                    if (isValidCardNumber(it)) return it
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
            if (concat.length in 13..19 && isValidCardNumber(concat)) return concat
            for (j in i + 1 until sameLenGroups.size) {
                concat += sameLenGroups[j]
                if (concat.length in 13..19) {
                    if (isValidCardNumber(concat)) return concat
                    break
                }
                if (concat.length > 19) break
            }
        }

        for (i in groups.indices) {
            var concat = groups[i]
            if (concat.length in 13..19 && isValidCardNumber(concat)) return concat
            for (j in i + 1 until groups.size) {
                concat += groups[j]
                if (concat.length in 13..19) {
                    if (isValidCardNumber(concat)) return concat
                    break
                }
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

    private fun detectIssuerFromText(texts: List<String>): String? {
        val combined = texts.joinToString(" ")
        val upper = combined.uppercase()

        val bankPatterns = listOf(
            "ICICI BANK" to "ICICI Bank",
            "HDFC BANK" to "HDFC Bank",
            "AXIS BANK" to "Axis Bank",
            "SBI" to "SBI",
            "KOTAK MAHINDRA" to "Kotak Mahindra",
            "YES BANK" to "Yes Bank",
            "INDUSIND BANK" to "IndusInd Bank",
            "RBL BANK" to "RBL Bank",
            "FEDERAL BANK" to "Federal Bank",
            "IDBI BANK" to "IDBI Bank",
            "CANARA BANK" to "Canara Bank",
            "PNB" to "PNB",
            "BANK OF BARODA" to "Bank of Baroda",
            "BOB" to "Bank of Baroda",
            "UNION BANK" to "Union Bank",
            "CITI" to "Citi",
            "CITIBANK" to "Citi",
            "HSBC" to "HSBC",
            "CHASE" to "Chase",
            "WELLS FARGO" to "Wells Fargo",
            "AMERICAN EXPRESS" to "American Express",
            "AMEX" to "American Express",
            "DISCOVER" to "Discover",
            "BARCLAYS" to "Barclays",
            "CAPITAL ONE" to "Capital One",
            "STANDARD CHARTERED" to "Standard Chartered",
            "BANK OF AMERICA" to "Bank of America"
        )

        for ((pattern, name) in bankPatterns) {
            if (upper.contains(pattern)) return name
        }

        val singleBanks = listOf(
            "ICICI", "HDFC", "AXIS", "KOTAK", "YES", "INDUSIND",
            "RBL", "FEDERAL", "IDBI", "CANARA", "UNION", "CITI",
            "HSBC", "CHASE", "BARCLAYS", "DISCOVER"
        )
        for (keyword in singleBanks) {
            val regex = Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(combined)) return keyword
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

    private fun parseCardholderName(texts: List<String>): String? {
        val combined = texts.joinToString(" ")
        val cleaned = combined
            .replace(Regex("""\d{13,19}"""), "")
            .replace(Regex("""\d{2}/\d{2,4}"""), "")
            .replace(Regex("""https?://\S+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""www\.\S+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[\d+.\-()]+"""), "")
            .replace(Regex("""\b\d{3,4}\b"""), "")

        val bankWords = setOf(
            "ICICI", "HDFC", "AXIS", "SBI", "KOTAK", "MAHINDRA",
            "YES", "INDUSIND", "RBL", "FEDERAL", "IDBI", "CANARA",
            "PNB", "BARODA", "BOB", "UNION", "CITI", "CITIBANK",
            "HSBC", "CHASE", "WELLS", "FARGO", "AMEX", "DISCOVER",
            "BARCLAYS", "CAPITAL", "ONE", "STANDARD", "CHARTERED"
        )

        val knownWords = setOf(
            "VALID", "THRU", "CARDMEMBER", "CARDHOLDER", "SIGNATURE", "AUTHORIZED", "USE",
            "OF", "THIS", "CARD", "IS", "SUBJECT", "TO", "THE", "AGREEMENT",
            "FOR", "ASSISTANCE", "IN", "TOLL", "FREE", "HELPLINE",
            "INTERNATIONAL", "CUSTOMERS", "NOT", "PAYMENT", "FOREIGN",
            "EXCHANGE", "NEPAL", "BHUTAN", "INDIA",
            "WORLD", "EMERALDE", "PRIVATE",
            "BANK", "VISA", "MASTERCARD", "AMERICAN", "EXPRESS", "RUPAY",
            "DINERS", "CLUB", "JCB", "PLATINUM", "GOLD", "SIGNATURE",
            "INFINITE", "ELITE", "WWW", "COM"
        ) + bankWords + setOf(
            "DEBIT", "CREDIT", "CLASSIC", "STANDARD", "PREMIER",
            "CORPORATE", "BUSINESS", "EXECUTIVE", "REWARDS", "MILEAGE",
            "TITANIUM", "SILVER", "BRONZE"
        )

        val tokens = cleaned.split(Regex("\\s+"))
        val sequences = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (token in tokens) {
            val clean = token.replace(Regex("[^A-Za-z]"), "").uppercase()
            if (clean.length in 2..30 && clean !in knownWords && clean.all { it.isLetter() }) {
                current.add(clean)
            } else {
                if (current.size >= 2) sequences.add(current.toList())
                current = mutableListOf()
            }
        }
        if (current.size >= 2) sequences.add(current.toList())

        if (sequences.isEmpty()) {
            for (token in tokens) {
                val clean = token.replace(Regex("[^A-Za-z]"), "").uppercase()
                if (clean.length >= 3 && clean !in knownWords && clean.all { it.isLetter() }) {
                    return clean
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
            "DISCOVER"
        )
        val skipWords = setOf("BANK", "OF", "THE", "AND", "FOR", "IN", "AT", "TO")

        val words = combined.split(Regex("\\s+"))
        for (i in words.indices) {
            if (words[i].uppercase() in knownIssuers) {
                val issuerIdx = i
                for (j in issuerIdx + 1 until minOf(issuerIdx + 5, words.size)) {
                    val candidate = words[j].replace(Regex("[^A-Za-z]"), "")
                    val upper = candidate.uppercase()
                    if (candidate.length >= 3 && candidate[0].isUpperCase() && upper !in skipWords) {
                        val next = if (j + 1 < words.size) {
                            val nextWord = words[j + 1].replace(Regex("[^A-Za-z]"), "")
                            if (nextWord.length in 3..20 && nextWord[0].isUpperCase()
                                && nextWord.uppercase() !in skipWords
                                && nextWord.all { it.isLetter() }) {
                                "$candidate $nextWord"
                            } else candidate
                        } else candidate
                        return next
                    }
                }
            }
        }
        return null
    }
}