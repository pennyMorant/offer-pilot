package com.overud.analytics

import com.overud.data.repository.StatisticsSnapshot
import com.overud.geo.DestinationResolver
import com.overud.model.*
import com.overud.work.WorkSessionController
import com.overud.work.assessOffer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Resolution runs independently; incoming offers and acceptance never wait for geocoding. */
class DestinationController(
    private val session: WorkSessionController,
    private val resolver: DestinationResolver,
    scope: CoroutineScope,
) {
    private data class Request(val working: Boolean, val token: String?, val address: String?)
    private data class Located(val token: String?, val address: String?, val result: ResolvedDestination)
    private data class Input(
        val working: Boolean, val token: String?, val address: String?, val platform: Platform?,
        val duration: Long?, val minute: Long, val history: StatisticsSnapshot,
        val waiting: List<WaitingObservation>, val tick: Long, val serviceKind: OfferService,
    )
    private val located = MutableStateFlow(Located(null, null, ResolvedDestination(DestinationResolution.MISSING)))

    init {
        scope.launch {
            session.state.map { Request(it.working, it.latest?.id, it.latest?.destination) }
                .distinctUntilChanged().collectLatest { request ->
                    val address = request.address
                    if (!request.working || address.isNullOrBlank()) {
                        located.value = Located(request.token, address, ResolvedDestination(DestinationResolution.MISSING))
                        if (!request.working) resolver.clear()
                        return@collectLatest
                    }
                    located.value = Located(request.token, address, ResolvedDestination(DestinationResolution.RESOLVING))
                    val result = try {
                        resolver.resolve(address)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        ResolvedDestination(DestinationResolution.UNAVAILABLE)
                    }
                    currentCoroutineContext().ensureActive()
                    located.value = Located(request.token, address, result)
                }
        }
        scope.launch {
            val inputs = session.state.map { state ->
                val offer = state.latest
                Input(state.working, offer?.id, offer?.destination, offer?.platform,
                    trustedDuration(offer),
                    (offer?.observedAt ?: 0) / 60000 * 60000, state.statistics, state.waitingHistory, (offer?.observedAt ?: 0) / 10000,
                    offer?.serviceKind ?: OfferService.UNKNOWN)
            }.distinctUntilChanged()
            combine(located, inputs) { location, input -> location to input }.collect { (location, input) ->
                val assessment = evaluate(location, input)
                val now = System.currentTimeMillis()
                val evidence = bind(input, assessment, now)
                session.update { state ->
                    val current = state.latest
                    if (state.working == input.working && current?.id == input.token &&
                        current?.destination == input.address && (current?.serviceKind ?: OfferService.UNKNOWN) == input.serviceKind &&
                        trustedDuration(current) == input.duration &&
                        (current?.observedAt ?: 0) / 60000 * 60000 == input.minute &&
                        state.statistics.revision == input.history.revision) {
                        val updated = state.copy(destinationAssessment = assessment, destinationEvidence = evidence)
                        updated.copy(decision = current?.let { updated.assessOffer(it, now) })
                    } else state
                }
            }
        }
    }

    private fun trustedDuration(offer: Offer?): Long? {
        val known = offer?.hasVerifiedDuration() == true
        return offer?.durationSeconds?.takeIf { known && it in 1..86400 }
    }

    private fun bind(input: Input, assessment: DestinationAssessment, now: Long): DestinationEvidence? {
        val token = input.token ?: return null
        val platform = input.platform ?: return null
        val address = input.address?.takeIf(String::isNotBlank) ?: return null
        if (!input.working) return null
        return DestinationEvidence(token, platform, address, input.duration, input.minute, now,
            input.history.revision, QualityEvaluator.VERSION, assessment)
    }

    private fun evaluate(location: Located, input: Input): DestinationAssessment {
        if (!input.working || input.token == null || input.address.isNullOrBlank()) return DestinationAssessment()
        if (location.token != input.token || location.address != input.address) {
            return DestinationAssessment(resolution = DestinationResolution.RESOLVING)
        }
        val platform = input.platform ?: return DestinationAssessment()
        val arrival = input.duration?.let { input.minute + it * 1000 }
        val history = input.history.times.mapNotNull { row ->
            val source = Platform.entries.find { it.name == row.platform } ?: return@mapNotNull null
            RegionalQualityObservation(source, row.regionKey, row.zoneId, row.weekday, row.localHour,
                row.qualityVersion, row.offerCount, row.goodCount, row.normalCount, row.lowCount,
                OfferService.entries.find { it.name == row.serviceKind } ?: OfferService.UNKNOWN)
        }
        return DestinationEvaluator.assess(location.result, platform, arrival, history, waitingHistory = input.waiting,
            serviceKind = input.serviceKind)
    }
}
