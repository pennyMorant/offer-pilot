package com.overud.ui.orders

data class OrderSummary(
    val id: String,
    val platformLabel: String,
    val observedAt: Long,
    val amountCents: Long?,
    val destination: String?,
    val eligible: Boolean,
    val explanation: String,
    val policyVersion: Long,
)
