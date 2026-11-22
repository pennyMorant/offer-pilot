package io.github.pennymorant.offerpilot.device

data class DeviceStatus(
    val notifications: Boolean? = null,
    val workChannel: Boolean? = null,
    val notificationListener: Boolean? = null,
    val overlay: Boolean? = null,
    val fineLocation: Boolean? = null,
    val coarseLocation: Boolean? = null,
    val locationEnabled: Boolean? = null,
    val batteryExempt: Boolean? = null,
    val backgroundRestricted: Boolean? = null,
    val interactive: Boolean? = null,
    val locked: Boolean? = null,
)
