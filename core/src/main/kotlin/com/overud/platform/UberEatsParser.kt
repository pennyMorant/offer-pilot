package com.overud.platform

import com.overud.model.Platform

object UberEatsParser : TextOfferParser(
    platform = Platform.UBER_EATS,
    accept = Regex("\\b(accept|match)\\b|接受|接单", RegexOption.IGNORE_CASE),
)
