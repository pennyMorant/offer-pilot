package io.github.pennymorant.offerpilot.bridge


data class BridgeStatus(val connected: Boolean = false, val authorized: Boolean = false, val serviceReady: Boolean = false, val message: String = "等待 Shevery 连接")
