package com.sunnyb.cardvault.security

import android.app.Application
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager(private val application: Application) {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val handler = Handler(Looper.getMainLooper())
    private val timeoutMs = 30_000L
    private val lockRunnable = Runnable { lock() }

    fun onAuthenticated() {
        _isAuthenticated.value = true
        _isLocked.value = false
        resetTimer()
    }

    fun onBackground() {
        handler.postDelayed(lockRunnable, timeoutMs)
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
