package io.github.pennymorant.offerpilot.platform

import io.github.pennymorant.offerpilot.model.*

enum class DeliveryScreenState { UNKNOWN, ACCEPTED, ACTIVE, COMPLETED }

object DeliveryScreenParser {
    fun parse(page: PlatformPage): DeliveryScreenState {
        if (page.source != OfferSource.UI_NODES) return DeliveryScreenState.UNKNOWN
        return when (page.type) {
            PlatformPageType.ACCEPTED -> DeliveryScreenState.ACCEPTED
            PlatformPageType.PICKUP, PlatformPageType.DELIVERY -> DeliveryScreenState.ACTIVE
            PlatformPageType.COMPLETED -> DeliveryScreenState.COMPLETED
            else -> DeliveryScreenState.UNKNOWN
        }
    }
}
