package com.overud.analytics

object MetricHistogram {
    val AMOUNT_BOUNDS = listOf(500L, 800L, 1200L, 2000L, 3000L)
    val MILE_BOUNDS = listOf(100_000L, 150_000L, 200_000L, 300_000L, 500_000L)
    val HOUR_BOUNDS = listOf(1_500_000L, 2_000_000L, 2_500_000L, 3_000_000L, 4_000_000L)

    fun adjust(serialized: String, bounds: List<Long>, value: Long?, delta: Int): String {
        val counts = decode(serialized, bounds.size + 1).toMutableList()
        if (value != null) {
            val index = bounds.indexOfFirst { value < it }.let { if (it < 0) bounds.size else it }
            counts[index] += delta
            require(counts[index] >= 0)
        }
        return counts.joinToString(",")
    }

    fun decode(serialized: String, size: Int): List<Long> {
        if (serialized.isBlank()) return List(size) { 0L }
        val values = serialized.split(',').map(String::toLong)
        require(values.size == size && values.all { it >= 0 })
        return values
    }
}
