package com.overud.model

/** Dispatch channel distinguishes AR-affecting Exclusive offers from non-AR Trip Radar offers. */
enum class OfferChannel(val label: String) {
    UNKNOWN("通道未确认"),
    EXCLUSIVE("专属派单"),
    TRIP_RADAR("雷达单"),
}
