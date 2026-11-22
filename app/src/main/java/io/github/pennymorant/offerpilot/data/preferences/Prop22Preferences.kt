package io.github.pennymorant.offerpilot.data.preferences

import android.content.SharedPreferences
import io.github.pennymorant.offerpilot.model.Prop22Policy

object Prop22Preferences {
    fun read(prefs: SharedPreferences): Prop22Policy = runCatching {
        Prop22Policy(
            enabled = prefs.getBoolean("prop22.enabled", false),
            minimumWageCents = prefs.getLong("prop22.wage", 1690),
            motorVehicle = prefs.getBoolean("prop22.motorVehicle", true),
            assumedTipPercent = prefs.getInt("prop22.tipPercent", 0),
        ).takeIf(Prop22Policy::valid) ?: Prop22Policy()
    }.getOrDefault(Prop22Policy())

    fun write(edit: SharedPreferences.Editor, value: Prop22Policy) {
        edit.putBoolean("prop22.enabled", value.enabled)
            .putLong("prop22.wage", value.minimumWageCents)
            .putBoolean("prop22.motorVehicle", value.motorVehicle)
            .putInt("prop22.tipPercent", value.assumedTipPercent)
    }
}
