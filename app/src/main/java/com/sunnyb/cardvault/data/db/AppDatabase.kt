package com.sunnyb.cardvault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunnyb.cardvault.data.db.entity.Card
import com.sunnyb.cardvault.data.db.entity.Category

@Database(
    entities = [Card::class, Category::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE card ADD COLUMN cardholderName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE card ADD COLUMN variant TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE card ADD COLUMN product TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
