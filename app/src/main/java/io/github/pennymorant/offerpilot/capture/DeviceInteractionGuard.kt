package io.github.pennymorant.offerpilot.capture

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

class DeviceInteractionGuard(context: Context) {
    private val power = context.getSystemService(PowerManager::class.java)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    fun allowed(): Boolean = runCatching { power.isInteractive && !keyguard.isKeyguardLocked }.getOrDefault(false)
}
