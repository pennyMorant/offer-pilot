package io.github.pennymorant.offerpilot

import android.app.Application
import io.github.pennymorant.offerpilot.di.AppContainer

class OverudApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
