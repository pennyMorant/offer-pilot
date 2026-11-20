package com.overud.platform

import com.overud.model.*

interface OfferParser {
    fun classify(text: String, source: OfferSource = OfferSource.UI_NODES): PlatformPage
    fun parse(text: String, now: Long, source: OfferSource = OfferSource.UI_NODES): Offer?
    fun parseRegions(regions: List<TextRegion>, now: Long, source: OfferSource = OfferSource.OCR): Offer? =
        parse(SpatialPageText.assemble(regions), now, source)

    companion object {
        private fun adapter(platform: Platform): OfferParser = when (platform) {
            Platform.UBER_EATS -> UberEatsParser
            Platform.DOORDASH -> DoorDashParser
        }
        fun classify(platform: Platform, text: String, source: OfferSource = OfferSource.UI_NODES) =
            adapter(platform).classify(text, source)
        fun parse(platform: Platform, text: String, now: Long, source: OfferSource = OfferSource.UI_NODES): Offer? =
            adapter(platform).parse(text, now, source)
        fun parseRegions(platform: Platform, regions: List<TextRegion>, now: Long, source: OfferSource = OfferSource.OCR): Offer? =
            adapter(platform).parseRegions(regions, now, source)
    }
}
