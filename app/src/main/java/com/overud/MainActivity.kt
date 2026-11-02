package com.overud

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.overud.capture.WorkService
import com.overud.ui.OverudRoot
import com.overud.ui.OverudTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val runtime = (application as OverudApp).runtime
        setContent {
            OverudTheme {
                OverudRoot(runtime,
                    onWork = {
                        if (runtime.state.value.working) stopService(Intent(this, WorkService::class.java))
                        else {
                            if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            startForegroundService(Intent(this, WorkService::class.java))
                        }
                    },
                    onOverlayPermission = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))) },
                    onNotificationPermission = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onLocationPermission = { locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                )
            }
        }
    }
    override fun onResume() {
        super.onResume()
        (application as OverudApp).runtime.bridge.refresh()
    }
}
