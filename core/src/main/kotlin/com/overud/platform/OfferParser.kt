package com.overud.platform

import com.overud.model.*

interface OfferParser {
    fun parse(text: String, now: Long): Offer?

    companion object {
        fun parse(platform: Platform, text: String, now: Long): Offer? = when (platform) {
            Platform.UBER_EATS -> UberEatsParser.parse(text, now)
            Platform.DOORDASH -> DoorDashParser.parse(text, now)
        }
    }
}
