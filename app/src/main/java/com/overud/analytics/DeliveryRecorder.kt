package com.overud.analytics

import android.os.SystemClock
import com.overud.data.repository.DeliveryRepository
import com.overud.model.*
import com.overud.work.WorkSessionController
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** One in-memory association; only aggregate outcomes are persisted. */
class DeliveryRecorder(
    private val session: WorkSessionController,
    private val regions: RegionRecorder,
    private val repository: DeliveryRepository,
    private val scope: CoroutineScope,
    private val mutex: Mutex,
) {
    private data class Observation(
        val token: String, val bucket: AnalysisBucket, val started: Long?,
        val quotedMillis: Long?, val generation: Long,
    )
    private val generation = AtomicLong()
    private var candidate: Observation? = null
    private var active: Observation? = null

    fun dispatch(offer: Offer) {
        val duration = offer.durationSeconds
        val knownTotal = offer.reliable || offer.evidence?.totalsVerified == true
        val quote = duration?.takeIf {
            knownTotal && !offer.isAddon && it in 1..DeliveryMeasurement.MAX_MILLIS / 1000
        }?.times(1000)
        candidate = Observation(offer.id,
            AnalysisBucket.at(offer.platform, regions.usableRegion(), System.currentTimeMillis()),
            SystemClock.elapsedRealtime(), quote, generation.get())
    }

    fun discardCandidate() { candidate = null }

    fun confirmed(token: String, platform: Platform) {
        settleInvalidated()
        val current = active
        if (current?.token == token && current.bucket.platform == platform) return
        interrupt()
        val now = SystemClock.elapsedRealtime()
        val match = candidate?.takeIf {
            val started = it.started
            it.token == token && it.bucket.platform == platform && it.generation == generation.get() &&
                started != null && now - started in 0..120_000
        }
        active = match ?: Observation(token,
            AnalysisBucket.at(platform, regions.usableRegion(), System.currentTimeMillis()), null, null, generation.get())
        candidate = null
    }

    fun completed(token: String, platform: Platform) {
        settleInvalidated()
        val current = active ?: return
        if (current.token != token || current.bucket.platform != platform) return
        val elapsed = current.started?.let { SystemClock.elapsedRealtime() - it }
        active = null
        candidate = null
        persist(current, true, elapsed)
    }

    fun interrupt() {
        val current = active
        active = null
        if (current != null) persist(current, false, null)
    }

    fun resolved() { discardCandidate(); interrupt() }

    fun invalidate() {
        generation.incrementAndGet()
        scope.launch { mutex.withLock { settleInvalidated() } }
    }

    fun refresh() {
        val rows = repository.snapshot()
        session.update { it.copy(deliveryHistory = rows) }
    }

    private fun settleInvalidated() {
        if (candidate?.generation != generation.get()) candidate = null
        if (active?.generation != generation.get()) interrupt()
    }

    private fun persist(current: Observation, completed: Boolean, elapsed: Long?) {
        // Clear association before the write; a storage failure must not duplicate a contribution.
        runCatching {
            repository.record(current.bucket, completed, elapsed, current.quotedMillis)
            refresh()
        }.onFailure {
            session.update { it.copy(automatic = false, autoAcceptEnabled = false,
                message = "配送汇总暂时无法保存，自动接单已暂停") }
        }
    }
}
