package com.overud.di

import com.overud.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.content.Context
import androidx.room.Room
import com.overud.analytics.RegionRecorder
import com.overud.analytics.DeliveryRecorder
import com.overud.analytics.WaitingRecorder
import com.overud.analytics.DestinationController
import com.overud.geo.AndroidGeocodingProvider
import com.overud.geo.DestinationResolver
import com.overud.acceptance.AutoAcceptController
import com.overud.bridge.SheveryBridge
import com.overud.capture.*
import com.overud.data.database.OverudDatabase
import com.overud.data.preferences.PolicyStore
import com.overud.data.preferences.TrackerCheckpointStore
import com.overud.data.database.migration.DatabaseMigrations
import com.overud.data.database.migration.WaitingSchema
import com.overud.data.database.migration.DeliverySchema
import com.overud.data.repository.*
import com.overud.work.WorkSessionController

class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val database = Room.databaseBuilder(context, OverudDatabase::class.java, "overud.db")
        .addMigrations(DatabaseMigrations.TO_AGGREGATES, WaitingSchema.MIGRATION, DeliverySchema.MIGRATION,
            com.overud.data.database.migration.OfferServiceSchema.MIGRATION).build()
    val bridge = SheveryBridge(context)
    val diagnosticReader = com.overud.bridge.NodeDiagnosticReader(bridge)
    val deviceStatus = com.overud.device.DeviceStatusReader(context)
    val session = WorkSessionController(bridge, PolicyStore(context), scope, mutex, DeviceInteractionGuard(context))
    val voice = com.overud.speech.VoiceController(context, session)
    val importedImages = ImportedImageRecognizer(context)
    val state = session.state
    private val statistics = StatisticsRepository(database)
    val regions = RegionRecorder(session, RegionRepository(database))
    private val waiting = WaitingRecorder(session, regions, WaitingRepository(database), scope, mutex)
    private val deliveries = DeliveryRecorder(session, regions, DeliveryRepository(database), scope, mutex)
    val acceptance = AutoAcceptController(session, AcceptanceRepository(database.acceptance()), bridge, scope, mutex,
        deliveries, waiting::start)
    private val destinations = DestinationController(session,
        DestinationResolver(AndroidGeocodingProvider(context)), scope)
    private val tracker = OfferTracker(TrackerCheckpointStore(context))
    val pipeline = CapturePipeline(session, bridge, OcrRecognizer(bridge), tracker, statistics, regions, acceptance, waiting, deliveries, mutex)
    init {
        scope.launch {
            bridge.status.collect {
                if (!it.serviceReady) { waiting.invalidate(); deliveries.invalidate() }
            }
        }
        scope.launch {
            mutex.withLock {
                runCatching {
                    statistics.prune()
                    session.update { it.copy(statistics = statistics.snapshot()) }
                    waiting.refresh()
                    deliveries.refresh()
                    acceptance.restore()
                }.onFailure {
                    session.update { state -> state.copy(automatic = false, autoAcceptEnabled = false, pending = true,
                        message = "统计存储暂不可用，请检查设备存储后重启应用") }
                }
            }
        }
    }
    fun startWork() { tracker.reset(); regions.clear(); session.startWork() }
    fun stopWork() { deliveries.invalidate(); waiting.invalidate(); session.stopWork(); regions.clear(); tracker.reset() }
    fun setAutomatic(enabled: Boolean) = session.setAutomatic(enabled)
    fun notification(platform: Platform, text: String) = session.notification(platform, text)
}
