package com.sunnyb.cardvault

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class CardVaultTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, "dagger.hilt.android.testing.HiltTestApplication", context)
    }
}
