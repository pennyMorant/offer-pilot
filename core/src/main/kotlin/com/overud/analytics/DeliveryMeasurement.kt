package com.overud.analytics

/** Observed dispatch-to-completion duration; never a payout or GPS travel measurement. */
object DeliveryMeasurement {
    const val VERSION = 1
    const val MAX_MILLIS = 12L * 3600 * 1000
}
