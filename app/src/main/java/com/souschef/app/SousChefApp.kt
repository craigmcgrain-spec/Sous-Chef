package com.souschef.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class SousChefApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Python.start(AndroidPlatform(this))
        container = AppContainer(this)
    }
}
