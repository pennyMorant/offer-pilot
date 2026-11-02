package com.overud.work

import com.overud.model.*
import com.overud.data.database.entity.OfferRecord
import com.overud.data.database.projection.RegionSummary

data class WorkState(
    val working: Boolean = false,
    val automatic: Boolean = false,
    val policy: Policy = Policy(),
    val latest: Offer? = null,
    val decision: Decision? = null,
    val records: List<OfferRecord> = emptyList(),
    val message: String = "开始工作后，将自动分析新报价",
    val pending: Boolean = false,
    val lastCaptureAt: Long? = null,
    val regions: List<RegionSummary> = emptyList(),
    val currentRegion: String? = null,
    val policySaving: Boolean = false,
    val policySaveError: String? = null,
)
