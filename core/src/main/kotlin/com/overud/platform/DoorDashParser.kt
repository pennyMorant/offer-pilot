package com.overud.platform

import com.overud.model.Platform

object DoorDashParser : TextOfferParser(
    platform = Platform.DOORDASH,
    accept = Regex("\\baccept\\b|接受|接单", RegexOption.IGNORE_CASE),
)
