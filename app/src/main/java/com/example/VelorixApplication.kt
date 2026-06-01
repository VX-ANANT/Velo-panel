package com.example

import android.app.Application

class VelorixApplication : Application() {
    lateinit var appContainer: AppContainer
    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
