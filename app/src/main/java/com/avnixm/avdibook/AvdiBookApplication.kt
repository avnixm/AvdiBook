package com.avnixm.avdibook

import android.app.Application

class AvdiBookApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
