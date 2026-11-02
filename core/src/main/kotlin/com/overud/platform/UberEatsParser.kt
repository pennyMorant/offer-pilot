package com.overud.platform

import com.overud.model.Platform

object UberEatsParser : TextOfferParser(Platform.UBER_EATS, UberEatsPages.rules)
