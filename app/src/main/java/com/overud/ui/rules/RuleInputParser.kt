package com.overud.ui.rules

import com.overud.model.*
import com.overud.valuation.ValueEngine
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.roundToLong

object RuleInputParser {
    fun fromPolicy(policy: Policy, scope: Platform? = null): RulesUiState {
        val selected = scope?.let(policy::effective) ?: policy
        val disabled = selected.disabledConditions.toMutableSet()
        if (selected.maximumPickupMeters == null) disabled += RuleCondition.MAXIMUM_PICKUP
        return RulesUiState(
            minimum = decimal(selected.minimumCents),
            mile = decimal(selected.minimumCentsPerMile),
            hour = decimal(selected.minimumCentsPerHour),
            distance = miles(selected.maximumMeters),
            time = (selected.maximumSeconds / 60).toString(),
            pickup = selected.maximumPickupMeters?.let(::miles).orEmpty(),
            cost = decimal(selected.costCentsPerMile),
            hourlyBasis = selected.hourlyBasis,
            regionalEnabled = selected.regionalRequirement.enabled,
            regionalGoodPercent = selected.regionalRequirement.minimumGoodPercent.toString(),
            regionalSamples = selected.regionalRequirement.minimumClassified.toString(),
            excluded = selected.excludedDestinations.joinToString(", "),
            platforms = policy.enabledPlatforms,
            stacked = selected.allowStacked,
            addon = selected.allowAddon,
            excludedMerchants = selected.excludedMerchants.joinToString(", "),
            disabledConditions = disabled,
            scope = scope,
            inheritShared = scope != null && scope !in policy.platformOverrides,
        )
    }

    fun toPolicy(input: RulesUiState, base: Policy): Policy? {
        val scope = input.scope
        if (scope != null && input.inheritShared) {
            return base.copy(platformOverrides = base.platformOverrides - scope)
        }
        val selected = scope?.let(base::effective) ?: base
        val rules = parseRules(input, selected) ?: return null
        return base.withRules(rules, scope)
    }

    private fun parseRules(input: RulesUiState, selected: Policy): AcceptanceRules? {
        fun active(condition: RuleCondition) = condition !in input.disabledConditions
        val amount = if (active(RuleCondition.MINIMUM_AMOUNT)) cents(input.minimum) ?: return null else selected.minimumCents
        val mile = if (active(RuleCondition.MINIMUM_PER_MILE)) cents(input.mile) ?: return null else selected.minimumCentsPerMile
        val hour = if (active(RuleCondition.MINIMUM_HOURLY)) cents(input.hour) ?: return null else selected.minimumCentsPerHour
        val distance = if (!active(RuleCondition.MAXIMUM_DISTANCE) || input.distance == miles(selected.maximumMeters)) {
            selected.maximumMeters
        } else meters(input.distance) ?: return null
        val duration = if (active(RuleCondition.MAXIMUM_DURATION)) {
            input.time.toLongOrNull()?.takeIf { it in 1..1440 }?.times(60) ?: return null
        } else selected.maximumSeconds
        val savedPickup = selected.maximumPickupMeters
        val pickup = when {
            !active(RuleCondition.MAXIMUM_PICKUP) -> savedPickup
            savedPickup != null && input.pickup == miles(savedPickup) -> savedPickup
            else -> meters(input.pickup) ?: return null
        }
        val regional = regional(input, selected) ?: return null
        return AcceptanceRules(
            minimumCents = amount,
            minimumCentsPerMile = mile,
            minimumCentsPerHour = hour,
            maximumMeters = distance,
            maximumSeconds = duration,
            maximumPickupMeters = pickup,
            costCentsPerMile = cents(input.cost) ?: return null,
            hourlyBasis = input.hourlyBasis,
            regionalRequirement = regional,
            allowStacked = input.stacked,
            allowAddon = input.addon,
            excludedDestinations = keywords(input.excluded),
            excludedMerchants = keywords(input.excludedMerchants),
            disabledConditions = input.disabledConditions,
        ).takeIf(AcceptanceRules::valid)
    }

    private fun regional(input: RulesUiState, selected: Policy): RegionalRequirement? {
        if (!input.regionalEnabled) return selected.regionalRequirement.copy(enabled = false)
        return RegionalRequirement(true, input.regionalGoodPercent.toIntOrNull() ?: return null,
            input.regionalSamples.toLongOrNull() ?: return null).takeIf(RegionalRequirement::valid)
    }

    private fun cents(value: String): Long? = runCatching {
        val number = value.toBigDecimal()
        require(number >= BigDecimal.ZERO && number <= BigDecimal("100000"))
        number.multiply(BigDecimal(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }.getOrNull()

    private fun meters(value: String): Long? = value.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= 1000 }
        ?.times(ValueEngine.METERS_PER_MILE)?.roundToLong()?.takeIf { it > 0 }

    private fun keywords(value: String) = value.split(',', '，').map(String::trim).filter(String::isNotBlank).toSet()
    private fun decimal(cents: Long) = "%.2f".format(Locale.US, cents / 100.0)
    private fun miles(meters: Long) = "%.1f".format(Locale.US, meters / ValueEngine.METERS_PER_MILE)
}
