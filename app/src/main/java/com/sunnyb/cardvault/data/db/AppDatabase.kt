package com.sunnyb.cardvault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.data.db.entity.Category

@Database(
    entities = [Card::class, Category::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao
}
