package io.github.pennymorant.offerpilot.model

data class RegionalRequirement(
    val enabled: Boolean = false,
    val minimumGoodPercent: Int = 50,
    val minimumClassified: Long = 20,
) {
    fun valid() = minimumGoodPercent in 1..100 && minimumClassified in 10..100_000
}
