package com.overud

import android.app.Application
import com.overud.di.AppContainer

class OverudApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
