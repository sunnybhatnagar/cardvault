package com.sunnyb.cardvault.data.db

import androidx.room.*
import com.sunnyb.cardvault.data.db.entity.Card
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM card ORDER BY updatedAt DESC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM card WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getCardsByCategory(categoryId: Long): Flow<List<Card>>

    @Query(
        "SELECT * FROM card WHERE " +
        "nickname LIKE '%' || :query || '%' OR " +
        "issuer LIKE '%' || :query || '%' OR " +
        "cardNumber LIKE '%' || :query || '%' " +
        "ORDER BY updatedAt DESC"
    )
    fun searchCards(query: String): Flow<List<Card>>

    @Query("SELECT * FROM card WHERE id = :id")
    suspend fun getCardById(id: Long): Card?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)
}
