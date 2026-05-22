package com.sunnyb.cardvault.data.repository

import com.sunnyb.cardvault.data.db.CardDao
import com.sunnyb.cardvault.data.db.entity.Card
import kotlinx.coroutines.flow.Flow

class CardRepository(private val cardDao: CardDao) {

    val allCards: Flow<List<Card>> = cardDao.getAllCards()

    fun getCardsByCategory(categoryId: Long): Flow<List<Card>> =
        cardDao.getCardsByCategory(categoryId)

    fun searchCards(query: String): Flow<List<Card>> =
        cardDao.searchCards(query)

    suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id)

    suspend fun getCardCountForCategory(categoryId: Long): Int =
        cardDao.getCardCountForCategory(categoryId)

    suspend fun insertCard(card: Card): Long = cardDao.insertCard(card)

    suspend fun updateCard(card: Card) = cardDao.updateCard(card)

    suspend fun deleteCard(card: Card) = cardDao.deleteCard(card)

    suspend fun deleteAllCards() = cardDao.deleteAllCards()
}
