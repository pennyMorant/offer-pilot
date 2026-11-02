package com.overud.capture

import com.overud.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.overud.analytics.RegionRecorder
import com.overud.analytics.WaitingRecorder
import com.overud.analytics.DeliveryRecorder
import com.overud.acceptance.AutoAcceptController
import com.overud.bridge.SheveryBridge
import com.overud.data.repository.StatisticsRepository
import com.overud.platform.OfferParser
import com.overud.platform.QuoteObservationResolver
import com.overud.work.WorkSessionController
import com.overud.work.assessOffer

class CapturePipeline(
    private val session: WorkSessionController,
    private val bridge: SheveryBridge,
    private val recognizer: OcrRecognizer,
    private val tracker: OfferTracker,
    private val statistics: StatisticsRepository,
    private val regions: RegionRecorder,
    private val acceptance: AutoAcceptController,
    private val waiting: WaitingRecorder,
    private val deliveries: DeliveryRecorder,
    private val mutex: Mutex,
) {
    private var lastOcrAt = 0L
    private val sourceConflictMessage = "节点与截图识别不一致，本帧不接单、不计入报价统计"
    suspend fun poll() = mutex.withLock {
        try {
            deliveries.checkContinuity()
            if (!session.state.value.working) { waiting.interrupt(); return@withLock }
            if (statistics.prune()) {
                session.update { it.copy(statistics = statistics.snapshot()) }
                waiting.refresh()
                deliveries.refresh()
            }
            val service = bridge.service ?: run {
                clearQuote(); waiting.interrupt(); deliveries.invalidate(); return@withLock
            }
            val snapshot = service.snapshot()
            snapshot.getString("error")?.let { error ->
                clearQuote()
                deliveries.invalidate()
                waiting.interrupt()
                session.update { it.copy(automatic = false, autoAcceptEnabled = false, message = "采集不可用：$error") }
                return@withLock
            }
            val platform = Platform.fromPackage(snapshot.getString("package").orEmpty())
                ?: run { clearQuote(); waiting.interrupt(); return@withLock }
            val text = snapshot.getString("text").orEmpty()
            val now = System.currentTimeMillis()
            regions.trackExposure(platform, now)
            session.update { it.copy(lastCaptureAt = now) }
            val nodePage = OfferParser.classify(platform, text)
            acceptance.observe(platform, nodePage)
            if (text.isBlank() || nodePage.type in setOf(PlatformPageType.OFFLINE, PlatformPageType.HISTORY,
                    PlatformPageType.CANCELLED, PlatformPageType.AMBIGUOUS)) waiting.interrupt() else waiting.frame(platform)
            val nodeOffer = OfferParser.parse(platform, text, now)
            val nodeDecision = nodeOffer?.let { session.state.value.assessOffer(it, now) }
            val needsOcr = nodeOffer == null || (!nodeOffer.reliable && nodeDecision?.eligible != true)
            val ocr = if (nodePage.allowsOcrFallback && needsOcr && now - lastOcrAt > 4000) {
                lastOcrAt = now
                recognizer.recognize(platform.packageName)
            } else null
            currentCoroutineContext().ensureActive()
            val observation = QuoteObservationResolver.resolve(platform, text, ocr, now)
            session.update { if (it.working) it.copy(currentPage = observation.page) else it }
            if (observation.sourceConflict) {
                waiting.interrupt()
                session.update { if (it.working) it.copy(message = sourceConflictMessage) else it }
            }
            var offer = observation.offer
            if (offer != null) {
                currentCoroutineContext().ensureActive()
                if (!session.state.value.working) { waiting.interrupt(); return@withLock }
                offer = tracker.observe(offer, now)
                waiting.quote(offer)
                val decision = session.state.value.assessOffer(offer, now)
                val changed = statistics.record(offer, regions.usableRegion())
                val aggregate = if (changed) statistics.snapshot() else session.state.value.statistics
                session.update {
                    if (it.working) {
                        val sameDestination = it.latest?.id == offer.id && it.latest?.destination == offer.destination
                        val initial = DestinationAssessment(resolution = if (offer.destination.isNullOrBlank())
                            DestinationResolution.MISSING else DestinationResolution.RESOLVING)
                        it.copy(latest = offer, decision = decision, statistics = aggregate,
                            message = if (it.message == sourceConflictMessage) "正在分析当前报价" else it.message,
                            destinationAssessment = if (sameDestination) it.destinationAssessment else initial,
                            destinationEvidence = if (sameDestination) it.destinationEvidence else null)
                    } else it.copy(statistics = aggregate)
                }
                acceptance.maybeAccept(offer, decision, snapshot.getString("token").orEmpty())
            } else {
                tracker.absent(platform, now)
                clearQuote(observation.page)
            }
        } catch (e: Exception) {
            if (e is CancellationException) { deliveries.invalidate(); waiting.invalidate(); throw e }
            clearQuote()
            deliveries.invalidate()
            runCatching { waiting.interrupt() }
            session.update { it.copy(automatic = false, autoAcceptEnabled = false, message = "采集已暂停：${e.message ?: "服务连接异常"}") }
        }
    }

    private fun clearQuote(page: PlatformPage? = null) {
        session.update { it.copy(latest = null, decision = null, destinationEvidence = null,
            destinationAssessment = DestinationAssessment(), currentPage = if (it.working) page else null) }
    }
}
