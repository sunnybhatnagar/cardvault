package com.sunnyb.cardvault.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory

object DatabaseFactory {

    private const val DB_NAME = "cardvault.db"

    fun create(context: Context, passphrase: ByteArray): AppDatabase {
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
            .openHelperFactory(factory)
            .addCallback(seedCategories())
            .build()
    }

    private fun seedCategories() = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO category (name, icon, sortOrder) VALUES " +
                "('Personal', '👤', 1), " +
                "('Work', '💼', 2), " +
                "('Other', '📁', 3)"
            )
        }
    }
}
