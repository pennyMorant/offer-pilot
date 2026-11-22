package io.github.pennymorant.offerpilot.ui.imagetest

import io.github.pennymorant.offerpilot.model.*

data class ImageTestUiState(
    val platform: Platform = Platform.UBER_EATS,
    val busy: Boolean = false,
    val text: String = "",
    val regions: List<TextRegion> = emptyList(),
    val hasImage: Boolean = false,
    val error: String? = null,
    val page: PlatformPage? = null,
    val offer: Offer? = null,
    val decision: Decision? = null,
    val preferredZones: Set<String> = emptySet(),
)
