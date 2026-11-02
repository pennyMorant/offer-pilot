package com.overud.ui.rules

import com.overud.model.Prop22Policy

data class Prop22Input(
    val enabled: Boolean = false,
    val wage: String = "16.90",
    val motorVehicle: Boolean = true,
    val tipPercent: String = "0",
) {
    fun parse(previous: Prop22Policy): Prop22Policy? {
        if (!enabled) return previous.copy(enabled = false)
        val cents = runCatching { wage.trim().toBigDecimal().movePointRight(2).longValueExact() }.getOrNull()
            ?: return null
        val percent = tipPercent.trim().toIntOrNull() ?: return null
        return Prop22Policy(true, cents, motorVehicle, percent).takeIf(Prop22Policy::valid)
    }

    companion object {
        fun from(value: Prop22Policy) = Prop22Input(value.enabled,
            value.minimumWageCents.toBigDecimal().movePointLeft(2).toPlainString(),
            value.motorVehicle, value.assumedTipPercent.toString())
    }
}
