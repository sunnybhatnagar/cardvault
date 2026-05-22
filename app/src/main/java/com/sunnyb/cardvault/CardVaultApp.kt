package com.sunnyb.cardvault

import android.app.Application
import com.sunnyb.cardvault.data.db.AppDatabase
import com.sunnyb.cardvault.data.db.DatabaseFactory
import com.sunnyb.cardvault.data.repository.CardRepository
import com.sunnyb.cardvault.data.repository.CategoryRepository
import com.sunnyb.cardvault.security.EncryptionManager
import com.sunnyb.cardvault.security.SessionManager

class CardVaultApp : Application() {

    lateinit var encryptionManager: EncryptionManager
    lateinit var database: AppDatabase
    lateinit var cardRepository: CardRepository
    lateinit var categoryRepository: CategoryRepository
    lateinit var sessionManager: SessionManager
    private var _initialized = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        encryptionManager = EncryptionManager(this)
        sessionManager = SessionManager(this)
    }

    fun initializeDatabase() {
        if (_initialized) return
        _initialized = true
        val passphrase = encryptionManager.getDatabasePassphrase()
        database = DatabaseFactory.create(this, passphrase)
        cardRepository = CardRepository(database.cardDao())
        categoryRepository = CategoryRepository(database.categoryDao())
    }

    companion object {
        lateinit var instance: CardVaultApp
            private set
    }
}
