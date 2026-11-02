package com.overud.work

import com.overud.model.*
import com.overud.data.repository.StatisticsSnapshot

data class WorkState(
    val working: Boolean = false,
    val automatic: Boolean = false,
    val policy: Policy = Policy(),
    val latest: Offer? = null,
    val decision: Decision? = null,
    val statistics: StatisticsSnapshot = StatisticsSnapshot(),
    val message: String = "开始工作后，将自动分析新报价",
    val pending: Boolean = false,
    val lastCaptureAt: Long? = null,
    val currentRegion: String? = null,
    val policySaving: Boolean = false,
    val policySaveError: String? = null,
    val destinationAssessment: DestinationAssessment = DestinationAssessment(),
)
