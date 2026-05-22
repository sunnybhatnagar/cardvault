package com.sunnyb.cardvault.util

import android.content.Context
import java.io.File

object RootDetector {

    private val rootPaths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    private val rootPackages = listOf(
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.topjohnwu.magisk"
    )

    fun isRooted(context: Context): Boolean {
        for (path in rootPaths) {
            if (File(path).exists()) return true
        }

        try {
            Runtime.getRuntime().exec("su -c id")
            return true
        } catch (_: Exception) {
        }

        for (pkg in rootPackages) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                return true
            } catch (_: Exception) {
            }
        }

        return false
    }
}