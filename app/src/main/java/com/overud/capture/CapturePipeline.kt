package com.overud.capture

import com.overud.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.overud.analytics.RegionRecorder
import com.overud.acceptance.AutoAcceptController
import com.overud.bridge.SheveryBridge
import com.overud.data.repository.OfferRepository
import com.overud.platform.OfferParser
import com.overud.valuation.EligibilityEvaluator
import com.overud.work.WorkSessionController

class CapturePipeline(
    private val session: WorkSessionController,
    private val bridge: SheveryBridge,
    private val recognizer: OcrRecognizer,
    private val tracker: OfferTracker,
    private val offers: OfferRepository,
    private val regions: RegionRecorder,
    private val acceptance: AutoAcceptController,
    private val mutex: Mutex,
) {
    private var lastOcrAt = 0L
    suspend fun poll() = mutex.withLock {
        if (!session.state.value.working) return@withLock
        val service = bridge.service ?: return@withLock
        try {
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
            if ((offer == null || !offer.reliable) && now - lastOcrAt > 4000) {
                lastOcrAt = now
                val ocr = recognizer.recognize(platform.packageName)
                currentCoroutineContext().ensureActive()
                if (ocr != null) {
                    val parsed = OfferParser.parse(platform, ocr, now)
                    // OCR-only observations inform the driver but do not authorize an action.
                    if (offer == null || (offer.amountCents == null && parsed?.amountCents != null)) {
                        offer = parsed?.copy(
                            reliable = false,
                            evidence = parsed.evidence?.copy(source = OfferSource.OCR),
                        )
                    }
                }
            }
            if (offer != null) {
                currentCoroutineContext().ensureActive()
                if (!session.state.value.working) return@withLock
                offer = tracker.observe(offer, now)
                val decision = EligibilityEvaluator.assess(offer, session.state.value.policy, now)
                offers.record(offer, decision, regions.usableRegion())
                session.update { it.copy(latest = offer, decision = decision, records = offers.recent()) }
                acceptance.maybeAccept(offer, decision, snapshot.getString("token").orEmpty())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            session.update { it.copy(automatic = false, message = "采集已暂停：${e.message ?: "服务连接异常"}") }
        }
    }
}
