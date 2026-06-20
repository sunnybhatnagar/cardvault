package com.sunnyb.cardvault

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.sunnyb.cardvault.security.EncryptionManager
import com.sunnyb.cardvault.ui.theme.ThemeMode
import com.sunnyb.cardvault.util.NotificationHelper
import com.sunnyb.cardvault.util.RootDetector
import com.sunnyb.cardvault.util.SafeLoggingTree
import com.sunnyb.cardvault.util.coil.EncryptedImageFetcher
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CardVaultApp : Application(), ImageLoaderFactory {

    @Inject lateinit var encryptionManager: EncryptionManager

    var themeMode by mutableStateOf(ThemeMode.DARK)
    var isDeviceRooted: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(SafeLoggingTree())
        NotificationHelper.createChannel(this)

        val saved = getSharedPreferences("cardvault_theme", MODE_PRIVATE)
            .getString("theme_mode", "DARK") ?: "DARK"
        themeMode = ThemeMode.valueOf(saved)

        isDeviceRooted = RootDetector.isRooted(this)
        if (isDeviceRooted) Timber.w("Device is rooted — running with reduced security guarantees")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(EncryptedImageFetcher.Factory(encryptionManager))
            }
            .build()
    }
}
