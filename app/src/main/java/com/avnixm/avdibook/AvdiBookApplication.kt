package com.avnixm.avdibook

import android.app.Application
import com.avnixm.avdibook.debug.DebugCrashReporter

class AvdiBookApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        DebugCrashReporter.install(this)
    }
}
