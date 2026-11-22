package io.github.pennymorant.offerpilot.acceptance

import io.github.pennymorant.offerpilot.model.*

/** Caller serializes access; an unresolved dispatch survives process restart in the journal. */
class AcceptanceGate(initial: AcceptanceState = AcceptanceState.IDLE) {
    var state = initial
        private set
    private val attempted = linkedSetOf<String>()
    fun reserve(offer: Offer, decision: Decision, enabled: Boolean, working: Boolean): Boolean {
        if (!enabled || !working || !decision.eligible || state != AcceptanceState.IDLE || !attempted.add(offer.id)) return false
        if (attempted.size > 256) attempted.remove(attempted.first())
        state = AcceptanceState.DISPATCHING
        return true
    }
    fun dispatched() { check(state == AcceptanceState.DISPATCHING); state = AcceptanceState.OUTCOME_UNKNOWN }
    fun confirmed() { check(state != AcceptanceState.IDLE); state = AcceptanceState.ACCEPTED }
    fun releaseBeforeDispatch() { check(state == AcceptanceState.DISPATCHING); state = AcceptanceState.IDLE }
    fun notDispatched() { state = AcceptanceState.IDLE }
    fun userResolved() { state = AcceptanceState.IDLE }
    fun occupyConfirmed() { state = AcceptanceState.ACCEPTED }
}
