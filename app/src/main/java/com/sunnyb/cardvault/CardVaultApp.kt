package com.sunnyb.cardvault

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunnyb.cardvault.data.db.AppDatabase
import com.sunnyb.cardvault.security.EncryptionManager
import com.sunnyb.cardvault.security.SessionManager
import com.sunnyb.cardvault.ui.theme.ThemeMode
import com.sunnyb.cardvault.util.NotificationHelper
import com.sunnyb.cardvault.util.RootDetector
import com.sunnyb.cardvault.util.SafeLoggingTree
import net.sqlcipher.database.SupportFactory
import timber.log.Timber
import java.io.File

class CardVaultApp : Application() {

    lateinit var encryptionManager: EncryptionManager
    lateinit var database: AppDatabase
    lateinit var sessionManager: SessionManager
    var themeMode by mutableStateOf(ThemeMode.DARK)
    var isDeviceRooted: Boolean = false
        private set
    private var _initialized = false

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(SafeLoggingTree())
        instance = this
        encryptionManager = EncryptionManager(this)
        sessionManager = SessionManager(this)
        NotificationHelper.createChannel(this)

        val saved = getSharedPreferences("cardvault_theme", MODE_PRIVATE)
            .getString("theme_mode", "DARK") ?: "DARK"
        themeMode = ThemeMode.valueOf(saved)

        isDeviceRooted = RootDetector.isRooted(this)
        if (isDeviceRooted) Timber.w("Device is rooted — running with reduced security guarantees")
    }

    fun initializeDatabase() {
        if (_initialized) return
        _initialized = true
        try {
            val passphrase = encryptionManager.getDatabasePassphrase()
            database = createDatabase(this, passphrase)
        } catch (e: Exception) {
            Timber.e(e, "Database initialization failed — attempting recovery")
            deleteDatabase("cardvault.db")
            File(applicationContext.filesDir, "cardvault.db").delete()
            File(applicationContext.filesDir, "cardvault.db-shm").delete()
            File(applicationContext.filesDir, "cardvault.db-wal").delete()

            val passphrase = encryptionManager.getDatabasePassphrase()
            database = createDatabase(this, passphrase)
        }
    }

    private fun createDatabase(context: Context, passphrase: ByteArray): AppDatabase {
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "cardvault.db"
        )
            .openHelperFactory(factory)
            .addMigrations(AppDatabase.MIGRATION_1_2)
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

    companion object {
        lateinit var instance: CardVaultApp
            private set
    }
}
