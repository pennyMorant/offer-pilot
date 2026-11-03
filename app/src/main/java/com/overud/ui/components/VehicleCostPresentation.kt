package com.overud.ui.components

import com.overud.model.Valuation
data class VehicleCostUiState(val cost: String, val contribution: String, val hourly: String, val perMile: String) {
    val compact: String get() = "估算车辆成本 $cost · 贡献 $contribution\n扣成本后 $hourly/h · $perMile/mi"
}

fun vehicleCostPresentation(value: Valuation?) = VehicleCostUiState(
    estimatedMoney(value?.vehicleCostCents),
    estimatedMoney(value?.contributionCents),
    estimatedMoney(value?.contributionCentsPerHour),
    estimatedMoney(value?.contributionCentsPerMile),
)
