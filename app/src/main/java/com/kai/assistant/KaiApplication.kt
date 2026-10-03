package com.kai.assistant

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class KaiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
    }

    private fun initializeLogging() {
        // Use a simple check instead of BuildConfig.DEBUG which isn't available until first build
        val isDebug = try {
            packageManager.getApplicationInfo(packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        } catch (e: Exception) {
            false
        }
        
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority >= Log.WARN) {
                        android.util.Log.println(priority, tag ?: "KAI", message)
                    }
                }
            })
        }
    }
}