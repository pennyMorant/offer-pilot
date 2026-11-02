package com.overud.capture

import com.overud.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.overud.analytics.RegionRecorder
import com.overud.analytics.WaitingRecorder
import com.overud.acceptance.AutoAcceptController
import com.overud.bridge.SheveryBridge
import com.overud.data.repository.StatisticsRepository
import com.overud.platform.OfferParser
import com.overud.platform.DeliveryScreenParser
import com.overud.platform.DeliveryScreenState
import com.overud.valuation.EligibilityEvaluator
import com.overud.work.WorkSessionController

class CapturePipeline(
    private val session: WorkSessionController,
    private val bridge: SheveryBridge,
    private val recognizer: OcrRecognizer,
    private val tracker: OfferTracker,
    private val statistics: StatisticsRepository,
    private val regions: RegionRecorder,
    private val acceptance: AutoAcceptController,
    private val waiting: WaitingRecorder,
    private val mutex: Mutex,
) {
    private var lastOcrAt = 0L
    suspend fun poll() = mutex.withLock {
        try {
            if (!session.state.value.working) { waiting.interrupt(); return@withLock }
            if (statistics.prune()) {
                session.update { it.copy(statistics = statistics.snapshot()) }
                waiting.refresh()
            }
            val service = bridge.service ?: run { waiting.interrupt(); return@withLock }
            val snapshot = service.snapshot()
            snapshot.getString("error")?.let { error ->
                waiting.interrupt()
                session.update { it.copy(automatic = false, autoAcceptEnabled = false, message = "采集不可用：$error") }
                return@withLock
            }
            val platform = Platform.fromPackage(snapshot.getString("package").orEmpty())
                ?: run { waiting.interrupt(); return@withLock }
            val text = snapshot.getString("text").orEmpty()
            val now = System.currentTimeMillis()
            regions.trackExposure(platform, now)
            session.update { it.copy(lastCaptureAt = now) }
            acceptance.observe(platform, text)
            if (text.isBlank()) waiting.interrupt() else waiting.frame(platform)
            val deliveryVisible = DeliveryScreenParser.parse(text) != DeliveryScreenState.UNKNOWN
            var offer = OfferParser.parse(platform, text, now)
            val nodeDecision = offer?.let { EligibilityEvaluator.assess(it, session.state.value.policy, now) }
            if (!deliveryVisible && (offer == null || (!offer.reliable && nodeDecision?.eligible != true)) && now - lastOcrAt > 4000) {
                lastOcrAt = now
                val ocr = recognizer.recognize(platform.packageName)
                currentCoroutineContext().ensureActive()
                if (ocr != null) {
                    val parsed = OfferParser.parse(platform, ocr, now)
                    // OCR-only observations inform the driver but do not authorize an action.
                    if (offer == null || (offer.amountCents == null && parsed?.amountCents != null)) {
                        offer = parsed?.let {
                            it.copy(reliable = false, evidence = it.evidence?.copy(source = OfferSource.OCR))
                        }
                    }
                }
            }
            if (offer != null) {
                currentCoroutineContext().ensureActive()
                if (!session.state.value.working) { waiting.interrupt(); return@withLock }
                offer = tracker.observe(offer, now)
                waiting.quote(offer)
                val decision = EligibilityEvaluator.assess(offer, session.state.value.policy, now)
                val changed = statistics.record(offer, regions.usableRegion())
                val aggregate = if (changed) statistics.snapshot() else session.state.value.statistics
                session.update {
                    if (it.working) {
                        val sameDestination = it.latest?.id == offer.id && it.latest?.destination == offer.destination
                        val initial = DestinationAssessment(resolution = if (offer.destination.isNullOrBlank())
                            DestinationResolution.MISSING else DestinationResolution.RESOLVING)
                        it.copy(latest = offer, decision = decision, statistics = aggregate,
                            destinationAssessment = if (sameDestination) it.destinationAssessment else initial)
                    } else it.copy(statistics = aggregate)
                }
                acceptance.maybeAccept(offer, decision, snapshot.getString("token").orEmpty())
            } else {
                tracker.absent(platform, now)
            }
        } catch (e: Exception) {
            if (e is CancellationException) { waiting.invalidate(); throw e }
            runCatching { waiting.interrupt() }
            session.update { it.copy(automatic = false, autoAcceptEnabled = false, message = "采集已暂停：${e.message ?: "服务连接异常"}") }
        }
    }
}
