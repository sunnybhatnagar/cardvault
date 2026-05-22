package com.sunnyb.cardvault.security

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager(private val application: Application) {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val handler = Handler(Looper.getMainLooper())
    private val lockRunnable = Runnable { lock() }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(application)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            application,
            "cardvault_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val timeoutMs: Long
        get() = prefs.getLong("lock_timeout", 30_000L)

    fun setTimeoutMs(ms: Long) {
        prefs.edit().putLong("lock_timeout", ms).apply()
    }

    fun onAuthenticated() {
        _isAuthenticated.value = true
        _isLocked.value = false
        resetTimer()
    }

    fun onBackground() {
        val t = timeoutMs
        when {
            t < 0 -> return
            t == 0L -> handler.post(lockRunnable)
            else -> handler.postDelayed(lockRunnable, t)
        }
    }

    fun onForeground() {
        if (_isLocked.value) return
        resetTimer()
    }

    private fun resetTimer() {
        handler.removeCallbacks(lockRunnable)
    }

    fun lock() {
        _isLocked.value = true
        _isAuthenticated.value = false
        handler.removeCallbacks(lockRunnable)
    }
}