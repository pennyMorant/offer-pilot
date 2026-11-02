package com.overud.di

import com.overud.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.content.Context
import androidx.room.Room
import com.overud.analytics.RegionRecorder
import com.overud.acceptance.AutoAcceptController
import com.overud.bridge.SheveryBridge
import com.overud.capture.*
import com.overud.data.database.OverudDatabase
import com.overud.data.preferences.PolicyStore
import com.overud.data.preferences.TrackerCheckpointStore
import com.overud.data.database.migration.DatabaseMigrations
import com.overud.data.repository.*
import com.overud.work.WorkSessionController

class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val database = Room.databaseBuilder(context, OverudDatabase::class.java, "overud.db")
        .addMigrations(DatabaseMigrations.TO_AGGREGATES).build()
    val bridge = SheveryBridge(context)
    val session = WorkSessionController(bridge, PolicyStore(context), scope, mutex)
    val state = session.state
    private val statistics = StatisticsRepository(database)
    val regions = RegionRecorder(session, RegionRepository(database))
    val acceptance = AutoAcceptController(session, AcceptanceRepository(database.acceptance()), bridge, scope, mutex)
    private val tracker = OfferTracker(TrackerCheckpointStore(context))
    val pipeline = CapturePipeline(session, bridge, OcrRecognizer(bridge), tracker, statistics, regions, acceptance, mutex)
    init {
        scope.launch {
            mutex.withLock {
                statistics.prune()
                session.update { it.copy(statistics = statistics.snapshot()) }
                acceptance.restore()
            }
        }
    }
    fun startWork() { tracker.reset(); regions.clear(); session.startWork() }
    fun stopWork() { session.stopWork(); regions.clear(); tracker.reset() }
    fun setAutomatic(enabled: Boolean) = session.setAutomatic(enabled)
    fun notification(platform: Platform, text: String) = session.notification(platform, text)
}
