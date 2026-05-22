package com.sunnyb.cardvault.util

import timber.log.Timber

class SafeLoggingTree : Timber.DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val redacted = message.replace(Regex("""\d{13,19}"""), "•••••••••••••••")
        super.log(priority, tag, redacted, t)
    }
}