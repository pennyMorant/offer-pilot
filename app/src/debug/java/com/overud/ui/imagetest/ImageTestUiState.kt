package com.overud.ui.imagetest

import com.overud.model.*

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
)
