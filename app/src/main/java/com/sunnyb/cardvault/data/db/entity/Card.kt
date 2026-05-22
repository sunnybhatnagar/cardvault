package com.sunnyb.cardvault.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nickname: String,
    val issuer: String? = null,
    val cardNumber: String,
    val expiry: String,
    val cvv: String,
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val categoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
