package com.overud.ui.components

import com.overud.model.Valuation
data class VehicleCostUiState(val cost: String, val contribution: String, val hourly: String, val perMile: String, val hasCost: Boolean) {
    val compact: String get() = if (hasCost) {
        "估算车辆成本 $cost · 贡献 $contribution\n扣成本后 $hourly/h · $perMile/mi"
    } else {
        "预计时薪 $hourly/h · $perMile/mi"
    }
}

fun vehicleCostPresentation(value: Valuation?) = VehicleCostUiState(
    estimatedMoney(value?.vehicleCostCents),
    estimatedMoney(value?.contributionCents),
    estimatedMoney(value?.contributionCentsPerHour ?: value?.grossCentsPerHour),
    estimatedMoney(value?.contributionCentsPerMile ?: value?.centsPerMile),
    hasCost = (value?.vehicleCostCents ?: 0.0) > 0.0,
)
