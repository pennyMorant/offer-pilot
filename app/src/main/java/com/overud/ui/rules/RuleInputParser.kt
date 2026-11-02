package com.overud.ui.rules

import com.overud.model.Policy
import com.overud.valuation.ValueEngine
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.roundToLong

object RuleInputParser {
    fun fromPolicy(policy: Policy) = RulesUiState(
        minimum = decimal(policy.minimumCents),
        mile = decimal(policy.minimumCentsPerMile),
        hour = decimal(policy.minimumCentsPerHour),
        distance = miles(policy.maximumMeters),
        time = (policy.maximumSeconds / 60).toString(),
        pickup = policy.maximumPickupMeters?.let(::miles).orEmpty(),
        cost = decimal(policy.costCentsPerMile),
        excluded = policy.excludedDestinations.joinToString(", "),
        platforms = policy.enabledPlatforms,
        stacked = policy.allowStacked,
        addon = policy.allowAddon,
    )

    fun toPolicy(input: RulesUiState, base: Policy): Policy? {
        val amount = cents(input.minimum) ?: return null
        val perMile = cents(input.mile) ?: return null
        val perHour = cents(input.hour) ?: return null
        val cost = cents(input.cost) ?: return null
        val distance = if (input.distance == miles(base.maximumMeters)) base.maximumMeters
            else meters(input.distance) ?: return null
        val duration = input.time.toLongOrNull()?.takeIf { it in 1..1440 }?.times(60) ?: return null
        val savedPickup = base.maximumPickupMeters
        val pickup = when {
            input.pickup.isBlank() -> null
            savedPickup != null && input.pickup == miles(savedPickup) -> savedPickup
            else -> meters(input.pickup) ?: return null
        }
        return base.copy(
            enabledPlatforms = input.platforms, minimumCents = amount,
            minimumCentsPerMile = perMile, minimumCentsPerHour = perHour,
            maximumMeters = distance, maximumSeconds = duration,
            maximumPickupMeters = pickup, costCentsPerMile = cost,
            allowStacked = input.stacked, allowAddon = input.addon,
            excludedDestinations = input.excluded.split(',', '，').map(String::trim).filter(String::isNotBlank).toSet(),
        ).takeIf(Policy::valid)
    }

    private fun cents(value: String): Long? = runCatching {
        val number = value.toBigDecimal()
        require(number >= BigDecimal.ZERO && number <= BigDecimal("100000"))
        number.multiply(BigDecimal(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }.getOrNull()

    private fun meters(value: String): Long? = value.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= 1000 }
        ?.times(ValueEngine.METERS_PER_MILE)?.roundToLong()?.takeIf { it > 0 }

    private fun decimal(cents: Long) = "%.2f".format(Locale.US, cents / 100.0)
    private fun miles(meters: Long) = "%.1f".format(Locale.US, meters / ValueEngine.METERS_PER_MILE)
}
