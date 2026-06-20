package com.sunnyb.cardvault.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunnyb.cardvault.data.db.AppDatabase
import com.sunnyb.cardvault.data.db.CardDao
import com.sunnyb.cardvault.data.db.CategoryDao
import com.sunnyb.cardvault.security.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import timber.log.Timber
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        encryptionManager: EncryptionManager
    ): AppDatabase {
        return try {
            val passphrase = encryptionManager.getDatabasePassphrase()
            createDatabase(context, passphrase)
        } catch (e: Exception) {
            Timber.e(e, "Database initialization failed — attempting recovery")
            context.deleteDatabase("cardvault.db")
            File(context.filesDir, "cardvault.db").delete()
            File(context.filesDir, "cardvault.db-shm").delete()
            File(context.filesDir, "cardvault.db-wal").delete()

            val passphrase = encryptionManager.getDatabasePassphrase()
            createDatabase(context, passphrase)
        }
    }

    private fun createDatabase(context: Context, passphrase: ByteArray): AppDatabase {
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cardvault.db"
        )
            .openHelperFactory(factory)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "INSERT INTO category (name, icon, sortOrder) VALUES " +
                        "('Personal', '👤', 1), " +
                        "('Work', '💼', 2), " +
                        "('Other', '📁', 3)"
                    )
                }
            })
            .build()
    }

    @Provides
    fun provideCardDao(database: AppDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }
}
