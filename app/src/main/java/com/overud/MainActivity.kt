package com.overud

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overud.ui.theme.isDark
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.overud.ui.permissions.PermissionSetupDialog
import com.overud.ui.permissions.PermissionSetupViewModel
import com.overud.ui.permissions.PermissionSetupLauncher
import com.overud.capture.WorkService
import com.overud.ui.navigation.OverudRoot
import com.overud.di.OverudViewModelFactory
import com.overud.ui.theme.OverudTheme

class MainActivity : ComponentActivity() {
    private val permissionSetup: PermissionSetupViewModel by viewModels()
    private lateinit var setupLauncher: PermissionSetupLauncher
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val runtime = (application as OverudApp).container
        setupLauncher = PermissionSetupLauncher(this, permissionSetup, runtime.bridge)
        val factory = OverudViewModelFactory(runtime)
        setContent {
            val appearance by runtime.appearance.state.collectAsStateWithLifecycle()
            val dark = appearance.isDark(isSystemInDarkTheme())
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                )
            }
            OverudTheme(appearance) {
                OverudRoot(factory,
                    onWork = {
                        if (runtime.state.value.working) stopService(Intent(this, WorkService::class.java))
                        else {
                            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            startForegroundService(Intent(this, WorkService::class.java))
                        }
                    },
                    onOverlayPermission = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))) },
                    onNotificationPermission = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onLocationPermission = { locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                )
                val setupState by permissionSetup.state.collectAsStateWithLifecycle()
                val bridge by runtime.bridge.status.collectAsStateWithLifecycle()
                LaunchedEffect(bridge.authorized) { permissionSetup.refresh(bridge.authorized) }
                PermissionSetupDialog(setupState, permissionSetup::start, setupLauncher::request,
                    permissionSetup::skip, permissionSetup::finish)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        (application as OverudApp).container.bridge.refresh()
    }
}
