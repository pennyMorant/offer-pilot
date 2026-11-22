package io.github.pennymorant.offerpilot.model

/** One-tap LA driving strategy presets built on top of existing AcceptanceRules and Prop 22 settings. */
enum class DrivingScene(val label: String, val summary: String) {
    PEAK_CHERRY_PICK("高峰挑大单", "≥$2/mi · ≥$25/hr · ≤6mi，专挑高小费短途单"),
    OFF_PEAK_GOLD("平峰稳金卡", "≥$1.5/mi · ≥$20/hr · ≤5mi，稳守 30% AR 甜点区"),
    LA_RAIN_PROP22("雨天短途保底", "≤3mi 降堵车与事故风险 · 开启 Prop 22 保底参考");

    fun applyTo(current: Policy): Policy {
        val base = current.sharedRules()
        val updatedRules = when (this) {
            PEAK_CHERRY_PICK -> base.copy(
                minimumCents = 800,
                minimumCentsPerMile = 200,
                minimumCentsPerHour = 2500,
                minimumContributionCents = 500,
                minimumValueScore = 70,
                maximumMeters = 9656, // 6 mi
                maximumSeconds = 2100, // 35 min
                allowPackage = false,
                allowShopAndPay = true,
                maximumShopItems = 10,
                allowTripRadar = true,
                radarExtraScore = 8,
            )
            OFF_PEAK_GOLD -> base.copy(
                minimumCents = 650,
                minimumCentsPerMile = 150,
                minimumCentsPerHour = 2000,
                minimumContributionCents = 400,
                minimumValueScore = 60,
                maximumMeters = 8047, // 5 mi
                maximumSeconds = 2100, // 35 min
                allowPackage = false,
                allowShopAndPay = true,
                maximumShopItems = 15,
                allowTripRadar = true,
                radarExtraScore = 5,
            )
            LA_RAIN_PROP22 -> base.copy(
                minimumCents = 600,
                minimumCentsPerMile = 175,
                minimumCentsPerHour = 2000,
                minimumContributionCents = 400,
                minimumValueScore = 60,
                maximumMeters = 4828, // 3 mi
                maximumSeconds = 1800, // 30 min
                allowPackage = false,
                allowShopAndPay = true,
                maximumShopItems = 25,
                allowTripRadar = true,
                radarExtraScore = 5,
            )
        }
        val prop22 = if (this == LA_RAIN_PROP22) current.prop22.copy(enabled = true) else current.prop22
        return current.withRules(updatedRules).copy(prop22 = prop22)
    }

    companion object {
        fun match(policy: Policy): DrivingScene? = entries.firstOrNull { scene ->
            val expected = scene.applyTo(policy)
            policy.minimumCents == expected.minimumCents &&
                policy.minimumCentsPerMile == expected.minimumCentsPerMile &&
                policy.minimumCentsPerHour == expected.minimumCentsPerHour &&
                policy.maximumMeters == expected.maximumMeters &&
                policy.maximumSeconds == expected.maximumSeconds &&
                (scene != LA_RAIN_PROP22 || policy.prop22.enabled)
        }
    }
}
