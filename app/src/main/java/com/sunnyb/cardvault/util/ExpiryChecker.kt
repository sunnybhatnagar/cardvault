package com.sunnyb.cardvault.util

import com.sunnyb.cardvault.data.db.entity.Card
import java.util.Calendar

data class ExpiringCard(
    val nickname: String,
    val expiry: String,
    val daysUntilExpiry: Int
)

object ExpiryChecker {

    private const val WINDOW_DAYS = 30

    suspend fun check(cards: List<Card>): List<ExpiringCard> {
        val now = Calendar.getInstance()
        val deadline = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, WINDOW_DAYS) }

        return cards.mapNotNull { card ->
            val expiryDate = parseExpiry(card.expiry) ?: return@mapNotNull null

            if (expiryDate.before(deadline) && expiryDate.after(now)) {
                val daysUntil = ((expiryDate.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                ExpiringCard(card.nickname, card.expiry, daysUntil)
            } else {
                null
            }
        }
    }

    private fun parseExpiry(expiry: String): Calendar? {
        val parts = expiry.split("/")
        if (parts.size != 2) return null
        val month = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val year = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (month !in 1..12) return null
        val fullYear = if (year < 100) 2000 + year else year
        return Calendar.getInstance().apply {
            set(fullYear, month - 1, 1)
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_YEAR, -1)
        }
    }
}