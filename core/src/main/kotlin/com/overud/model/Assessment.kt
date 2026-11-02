package com.overud.model

data class Valuation(
    val centsPerMile: Double?,
    val grossCentsPerHour: Double?,
    val contributionCents: Double?,
    val vehicleCostCents: Double? = null,
    val contributionCentsPerHour: Double? = null,
)
data class Decision(val eligible: Boolean, val reasons: List<String>, val valuation: Valuation, val policyVersion: Long)
