package io.github.pennymorant.offerpilot.di

import io.github.pennymorant.offerpilot.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.content.Context
import androidx.room.Room
import io.github.pennymorant.offerpilot.analytics.RegionRecorder
import io.github.pennymorant.offerpilot.analytics.DeliveryRecorder
import io.github.pennymorant.offerpilot.analytics.WaitingRecorder
import io.github.pennymorant.offerpilot.analytics.DestinationController
import io.github.pennymorant.offerpilot.geo.AndroidGeocodingProvider
import io.github.pennymorant.offerpilot.geo.DestinationResolver
import io.github.pennymorant.offerpilot.acceptance.AutoAcceptController
import io.github.pennymorant.offerpilot.acceptance.OfferOutcomeTracker
import io.github.pennymorant.offerpilot.bridge.SheveryBridge
import io.github.pennymorant.offerpilot.capture.*
import io.github.pennymorant.offerpilot.data.database.OverudDatabase
import io.github.pennymorant.offerpilot.data.preferences.PolicyStore
import io.github.pennymorant.offerpilot.data.preferences.TrackerCheckpointStore
import io.github.pennymorant.offerpilot.data.repository.*
import io.github.pennymorant.offerpilot.work.WorkSessionController

class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val database = Room.databaseBuilder(context, OverudDatabase::class.java, "overud.db")
        .fallbackToDestructiveMigration(true)
        .build()
    val appContext = context.applicationContext
    val appearance = io.github.pennymorant.offerpilot.data.preferences.AppearancePreferences(context)
    val arGuard = io.github.pennymorant.offerpilot.data.preferences.ArGuardPreferences(context)
    val bridge = SheveryBridge(context)
    val deviceStatus = io.github.pennymorant.offerpilot.device.DeviceStatusReader(context)
    val session = WorkSessionController(bridge, PolicyStore(context), scope, mutex, DeviceInteractionGuard(context))
    val voice = io.github.pennymorant.offerpilot.speech.VoiceController(context, session, arGuard)
    val state = session.state
    val appInForeground = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val statistics = StatisticsRepository(database)
    val offerHistory = OfferHistoryRepository(database)
    val outcomes = OfferOutcomeTracker(offerHistory, scope)
    val regions = RegionRecorder(session, RegionRepository(database))
    private val waiting = WaitingRecorder(session, regions, WaitingRepository(database), scope, mutex)
    private val deliveries = DeliveryRecorder(session, regions, DeliveryRepository(database), scope, mutex)
    val acceptance = AutoAcceptController(session, AcceptanceRepository(database.acceptance()), bridge, scope, mutex,
        deliveries, waiting::start) { platform ->
        outcomes.onDeliveryConfirmed(platform, System.currentTimeMillis())?.channel
    }
    private val destinations = DestinationController(session,
        DestinationResolver(AndroidGeocodingProvider(context)), scope)
    private val tracker = OfferTracker(TrackerCheckpointStore(context))
    val pipeline = CapturePipeline(session, bridge, OcrRecognizer(bridge), tracker, statistics, offerHistory, regions, acceptance, outcomes, waiting, deliveries, mutex)
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
                    offerHistory.prune()
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
    private var debugOverlayCleanup: (() -> Unit)? = null
    fun registerDebugOverlayCleanup(cleanup: () -> Unit) { debugOverlayCleanup = cleanup }
    fun startWork() { debugOverlayCleanup?.invoke(); debugOverlayCleanup = null; tracker.reset(); regions.clear(); session.startWork() }
    fun stopWork() { outcomes.onIdle(System.currentTimeMillis(), 0L); deliveries.invalidate(); waiting.invalidate(); session.stopWork(); regions.clear(); tracker.reset() }
    fun setAutomatic(enabled: Boolean) = session.setAutomatic(enabled)
    fun notification(platform: Platform, text: String) = session.notification(platform, text)
    fun launchPlatform(target: Platform) {
        scope.launch {
            val launched = runCatching { bridge.service?.launchPlatform(target.packageName) == true }.getOrDefault(false)
            if (!launched) {
                val intent = appContext.packageManager.getLaunchIntentForPackage(target.packageName)
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { appContext.startActivity(intent) }
                }
            }
        }
    }
    fun launchOtherPlatform(active: Platform) {
        Platform.entries.firstOrNull { it != active }?.let(::launchPlatform)
    }
    fun declineCurrentOffer() {
        val offer = session.state.value.latest ?: return
        outcomes.onExplicitDecline(offer)
        val token = io.github.pennymorant.offerpilot.platform.fingerprint(offer.platform, offer.rawText)
        scope.launch {
            runCatching {
                bridge.service?.decline(token, offer.platform.packageName, android.os.SystemClock.elapsedRealtime() + 2000)
            }
        }
    }
}
