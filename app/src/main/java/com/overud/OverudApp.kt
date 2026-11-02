package com.overud

import android.app.Application
import androidx.room.Room
import com.overud.data.database.OverudDatabase

class OverudApp : Application() {
    lateinit var runtime: OverudRuntime
        private set
    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, OverudDatabase::class.java, "overud.db").build()
        runtime = OverudRuntime(this, database)
    }
}
