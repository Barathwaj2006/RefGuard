package com.refguard.app

import android.app.Application
import android.util.Log

/**
 * Application class — no background surveillance, no credential collection.
 */
class RefGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("RefGuard", "RefGuardApplication started")
    }
}
