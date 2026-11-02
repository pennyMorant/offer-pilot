package com.overud.ui.components

import com.overud.model.Valuation
import java.util.Locale

data class VehicleCostUiState(val cost: String, val contribution: String, val hourly: String) {
    val compact: String get() = "车辆成本 $cost · 扣成本贡献 $contribution\n扣成本后时薪 $hourly/h"
}

fun vehicleCostPresentation(value: Valuation?) = VehicleCostUiState(
    estimatedMoney(value?.vehicleCostCents),
    estimatedMoney(value?.contributionCents),
    estimatedMoney(value?.contributionCentsPerHour),
)

private fun estimatedMoney(cents: Double?): String =
    if (cents != null && cents.isFinite()) String.format(Locale.US, "$%.2f", cents / 100) else "—"
