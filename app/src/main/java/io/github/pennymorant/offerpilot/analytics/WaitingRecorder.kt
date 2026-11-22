package io.github.pennymorant.offerpilot.analytics

import android.os.SystemClock
import io.github.pennymorant.offerpilot.data.repository.WaitingRepository
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.work.WorkSessionController
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** Only continuous foreground observations; online availability is not inferred. */
class WaitingRecorder(
    private val session: WorkSessionController,
    private val regions: RegionRecorder,
    private val repository: WaitingRepository,
    private val scope: CoroutineScope,
    private val mutex: Mutex,
) {
    private data class Interval(
        val bucket: AnalysisBucket, val started: Long, val startedEpoch: Long, var lastSeen: Long,
        val excludedToken: String?, val generation: Long,
    )
    private val generation = AtomicLong()
    private var interval: Interval? = null

    fun start(platform: Platform) {
        interrupt()
        if (!session.state.value.working) return
        val now = SystemClock.elapsedRealtime()
        val epoch = System.currentTimeMillis()
        interval = Interval(AnalysisBucket.at(platform, regions.usableRegion(), epoch),
            now, epoch, now, session.state.value.latest?.id, generation.get())
    }

    fun frame(platform: Platform) {
        val current = interval ?: return
        val now = SystemClock.elapsedRealtime()
        val region = regions.usableRegion() ?: AnalysisBucket.UNKNOWN_REGION
        val state = session.state.value
        if (!state.working || state.pending || platform != current.bucket.platform ||
            current.generation != generation.get() || region != current.bucket.region ||
            now - current.lastSeen !in 0..5000 || now - current.started >= 1_800_000) {
            interrupt()
        } else current.lastSeen = now
    }

    fun quote(offer: Offer) {
        val current = interval ?: return
        val evidence = offer.evidence
        val amount = offer.amountCents
        if (offer.platform != current.bucket.platform || offer.id == current.excludedToken ||
            offer.firstObservedAt < current.startedEpoch ||
            amount == null || amount <= 0 || evidence == null || !evidence.amountVerified || evidence.source != OfferSource.UI_NODES) return
        // OCR and IPC can take time after frame(); never bridge a gap using a late result.
        val now = SystemClock.elapsedRealtime()
        if (current.generation != generation.get() || !session.state.value.working || session.state.value.pending ||
            (regions.usableRegion() ?: AnalysisBucket.UNKNOWN_REGION) != current.bucket.region ||
            now - current.lastSeen !in 0..5000 || now - current.started >= 1_800_000) interrupt()
        else finish(current, current.lastSeen, true)
    }

    fun interrupt() {
        val current = interval ?: return
        finish(current, current.lastSeen, false)
    }

    /** Called immediately by lifecycle signals; database writes stay under the shared mutex. */
    fun invalidate() {
        generation.incrementAndGet()
        scope.launch {
            mutex.withLock {
                runCatching {
                    if (interval?.generation != generation.get()) interrupt()
                }.onFailure { storageFailed() }
            }
        }
    }

    fun refresh() {
        val history = repository.snapshot()
        session.update { it.copy(waitingHistory = history) }
    }

    private fun finish(current: Interval, end: Long, completed: Boolean) {
        // Clear before persistence: failure must never retry the same sample as a second contribution.
        interval = null
        repository.record(current.bucket, (end - current.started).coerceIn(0, 1_800_000), completed)
        refresh()
    }

    private fun storageFailed() {
        session.update { it.copy(automatic = false, autoAcceptEnabled = false,
            message = "等待统计暂时无法保存，自动接单已暂停") }
    }
}
