package com.sunnyb.cardvault

import android.app.Application
import com.sunnyb.cardvault.data.db.AppDatabase
import com.sunnyb.cardvault.data.db.DatabaseFactory
import com.sunnyb.cardvault.data.repository.CardRepository
import com.sunnyb.cardvault.data.repository.CategoryRepository
import com.sunnyb.cardvault.security.EncryptionManager
import com.sunnyb.cardvault.security.SessionManager
import com.sunnyb.cardvault.util.NotificationHelper
import com.sunnyb.cardvault.util.RootDetector
import com.sunnyb.cardvault.util.SafeLoggingTree
import timber.log.Timber
import java.io.File

class CardVaultApp : Application() {

    lateinit var encryptionManager: EncryptionManager
    lateinit var database: AppDatabase
    lateinit var cardRepository: CardRepository
    lateinit var categoryRepository: CategoryRepository
    lateinit var sessionManager: SessionManager
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

        isDeviceRooted = RootDetector.isRooted(this)
        if (isDeviceRooted) Timber.w("Device is rooted — running with reduced security guarantees")
    }

    fun initializeDatabase() {
        if (_initialized) return
        _initialized = true
        try {
            val passphrase = encryptionManager.getDatabasePassphrase()
            database = DatabaseFactory.create(this, passphrase)
            cardRepository = CardRepository(database.cardDao())
            categoryRepository = CategoryRepository(database.categoryDao())
        } catch (e: Exception) {
            Timber.e(e, "Database initialization failed — attempting recovery")
            deleteDatabase("cardvault.db")
            File(applicationContext.filesDir, "cardvault.db").delete()
            File(applicationContext.filesDir, "cardvault.db-shm").delete()
            File(applicationContext.filesDir, "cardvault.db-wal").delete()

            val passphrase = encryptionManager.getDatabasePassphrase()
            database = DatabaseFactory.create(this, passphrase)
            cardRepository = CardRepository(database.cardDao())
            categoryRepository = CategoryRepository(database.categoryDao())
        }
    }

    companion object {
        lateinit var instance: CardVaultApp
            private set
    }
}
