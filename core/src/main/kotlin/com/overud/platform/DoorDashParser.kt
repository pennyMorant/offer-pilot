package com.overud.platform

import com.overud.model.Platform

object DoorDashParser : TextOfferParser(Platform.DOORDASH, DoorDashPages.rules)
