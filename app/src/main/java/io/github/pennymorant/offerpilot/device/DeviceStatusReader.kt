package io.github.pennymorant.offerpilot.device

import android.Manifest
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.PowerManager
import android.provider.Settings
import io.github.pennymorant.offerpilot.capture.OfferNotificationListener

class DeviceStatusReader(context: Context) {
    private val app = context.applicationContext
    fun read(): DeviceStatus {
        fun probe(block: () -> Boolean) = runCatching(block).getOrNull()
        val notifications = app.getSystemService(NotificationManager::class.java)
        val power = app.getSystemService(PowerManager::class.java)
        return DeviceStatus(
            notifications = probe { notifications.areNotificationsEnabled() },
            workChannel = probe { notifications.getNotificationChannel("work")?.importance != NotificationManager.IMPORTANCE_NONE },
            notificationListener = probe { notifications.isNotificationListenerAccessGranted(
                ComponentName(app, OfferNotificationListener::class.java)) },
            overlay = probe { Settings.canDrawOverlays(app) },
            fineLocation = probe { app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED },
            coarseLocation = probe { app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED },
            locationEnabled = probe { app.getSystemService(LocationManager::class.java).isLocationEnabled },
            batteryExempt = probe { power.isIgnoringBatteryOptimizations(app.packageName) },
            backgroundRestricted = probe { app.getSystemService(ActivityManager::class.java).isBackgroundRestricted },
            interactive = probe { power.isInteractive },
            locked = probe { app.getSystemService(KeyguardManager::class.java).isKeyguardLocked },
        )
    }
}
