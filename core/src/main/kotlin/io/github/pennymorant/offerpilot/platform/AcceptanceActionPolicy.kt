package io.github.pennymorant.offerpilot.platform

import io.github.pennymorant.offerpilot.model.OfferSource
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.model.PlatformPage
import io.github.pennymorant.offerpilot.model.PlatformPageType
import java.util.Locale

/** First-step actions only. Confirmation and add-on pages require separate workflows. */
object AcceptanceActionPolicy {
    private val whitespace = Regex("\\s+")
    private val inlineAmount = Regex("^(?:us\\$|\\$)\\s*\\d+(?:\\.\\d{2})?$")

    fun allowsInitialClick(page: PlatformPage): Boolean =
        page.source == OfferSource.UI_NODES && page.type == PlatformPageType.OFFER

    fun matchesInitialButton(platform: Platform, label: String?): Boolean {
        val normalized = label?.trim()?.replace(whitespace, " ")?.lowercase(Locale.ROOT) ?: return false
        val actions = when (platform) {
            Platform.UBER_EATS -> UberEatsPages.rules.quoteActions
            Platform.DOORDASH -> DoorDashPages.rules.quoteActions
        }
        return actions.any { action ->
            normalized == action || (normalized.startsWith("$action ") &&
                inlineAmount.matches(normalized.removePrefix("$action ")))
        }
    }
}
