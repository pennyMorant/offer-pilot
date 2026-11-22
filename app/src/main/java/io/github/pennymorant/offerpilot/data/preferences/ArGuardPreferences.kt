package io.github.pennymorant.offerpilot.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ArGuardState(
    val uberArPercent: Int = 33,
    val goldFloorPercent: Int = 30,
) {
    val hint: String get() = when {
        uberArPercent < goldFloorPercent -> "低于 Gold ${goldFloorPercent}% 门槛，优先接顺路短途单回升"
        uberArPercent < goldFloorPercent + 5 -> "处于 Gold ${goldFloorPercent}%–${goldFloorPercent + 4}% 警戒区，建议保级"
        uberArPercent < 48 -> "处于 Gold 甜点区，放心严格挑大单"
        else -> "接近 50%，无需为冲白金硬接亏本长途单"
    }
}

/** Fixed-capacity AR guard state in SharedPreferences; no per-order log is ever persisted. */
class ArGuardPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("ar_guard", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    val state = mutable.asStateFlow()

    fun adjustUberAr(delta: Int) {
        val next = (mutable.value.uberArPercent + delta).coerceIn(0, 100)
        prefs.edit().putInt(KEY_UBER_AR, next).apply()
        mutable.value = mutable.value.copy(uberArPercent = next)
    }

    private fun read() = ArGuardState(
        uberArPercent = prefs.getInt(KEY_UBER_AR, 33).coerceIn(0, 100),
    )

    companion object {
        private const val KEY_UBER_AR = "uber_ar_percent"
    }
}
