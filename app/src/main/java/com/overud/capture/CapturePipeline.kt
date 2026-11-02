package com.overud.capture

import com.overud.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.overud.analytics.RegionRecorder
import com.overud.acceptance.AutoAcceptController
import com.overud.bridge.SheveryBridge
import com.overud.data.repository.StatisticsRepository
import com.overud.platform.OfferParser
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
    private val mutex: Mutex,
) {
    private var lastOcrAt = 0L
    suspend fun poll() = mutex.withLock {
        if (!session.state.value.working) return@withLock
        try {
            if (statistics.prune()) session.update { it.copy(statistics = statistics.snapshot()) }
            val service = bridge.service ?: return@withLock
            val snapshot = service.snapshot()
            snapshot.getString("error")?.let { error ->
                session.update { it.copy(automatic = false, message = "采集不可用：$error") }
                return@withLock
            }
            val platform = Platform.fromPackage(snapshot.getString("package").orEmpty()) ?: return@withLock
            val text = snapshot.getString("text").orEmpty()
            val now = System.currentTimeMillis()
            regions.trackExposure(platform, now)
            session.update { it.copy(lastCaptureAt = now) }
            acceptance.observe(platform, text)
            var offer = OfferParser.parse(platform, text, now)
            val nodeDecision = offer?.let { EligibilityEvaluator.assess(it, session.state.value.policy, now) }
            if ((offer == null || (!offer.reliable && nodeDecision?.eligible != true)) && now - lastOcrAt > 4000) {
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
                if (!session.state.value.working) return@withLock
                offer = tracker.observe(offer, now)
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
            if (e is CancellationException) throw e
            session.update { it.copy(automatic = false, message = "采集已暂停：${e.message ?: "服务连接异常"}") }
        }
    }
}
