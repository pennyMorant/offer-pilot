package com.overud.data.preferences

import android.content.Context
import com.overud.model.Platform
import com.overud.model.Policy

class PolicyStore(context: Context) {
    private val prefs = context.getSharedPreferences("policy", Context.MODE_PRIVATE)
    fun read() = Policy(
        version = prefs.getLong("version", 1),
        enabledPlatforms = Platform.entries.filter { prefs.getBoolean(it.name, true) }.toSet(),
        minimumCents = prefs.getLong("min", 800),
        minimumCentsPerMile = prefs.getLong("mile", 200),
        minimumCentsPerHour = prefs.getLong("hour", 2500),
        maximumMeters = prefs.getLong("distance", 16093),
        maximumSeconds = prefs.getLong("time", 2700),
        maximumPickupMeters = prefs.getLong("pickup", 0).takeIf { it > 0 },
        costCentsPerMile = prefs.getLong("cost", 35),
        allowStacked = prefs.getBoolean("stack", false),
        allowAddon = prefs.getBoolean("addon", false),
        excludedDestinations = prefs.getStringSet("excluded", emptySet())!!.toSet(),
    )
    fun save(policy: Policy): Boolean {
        if (!policy.valid()) return false
        val edit = prefs.edit().putLong("version", policy.version)
            .putLong("min", policy.minimumCents).putLong("mile", policy.minimumCentsPerMile)
            .putLong("hour", policy.minimumCentsPerHour).putLong("distance", policy.maximumMeters)
            .putLong("time", policy.maximumSeconds).putLong("cost", policy.costCentsPerMile)
            .putLong("pickup", policy.maximumPickupMeters ?: 0).putBoolean("stack", policy.allowStacked)
            .putBoolean("addon", policy.allowAddon).putStringSet("excluded", policy.excludedDestinations)
        Platform.entries.forEach { edit.putBoolean(it.name, it in policy.enabledPlatforms) }
        return edit.commit()
    }
}
