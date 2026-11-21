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
import com.overud.ui.permissions.SetupPermission
import com.overud.capture.WorkService
import com.overud.ui.navigation.OverudRoot
import com.overud.di.OverudViewModelFactory
import com.overud.ui.theme.OverudTheme

class MainActivity : ComponentActivity() {
    private val permissionSetup: PermissionSetupViewModel by viewModels()
    private val setupRuntimePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionSetup.advance()
    }
    private val setupSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val bridge = (application as OverudApp).container.bridge
        bridge.refresh()
        permissionSetup.refresh(bridge.status.value.authorized)
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val runtime = (application as OverudApp).container
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
                val setupStep by permissionSetup.step.collectAsStateWithLifecycle()
                val bridge by runtime.bridge.status.collectAsStateWithLifecycle()
                LaunchedEffect(bridge.authorized) { permissionSetup.refresh(bridge.authorized) }
                setupStep?.let { step ->
                    PermissionSetupDialog(step, { requestSetupPermission(step) }, permissionSetup::advance)
                }
            }
        }
    }
    private fun requestSetupPermission(step: SetupPermission) {
        val packageUri = android.net.Uri.parse("package:$packageName")
        val intent = when (step) {
            SetupPermission.NOTIFICATIONS -> {
                setupRuntimePermission.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)); return
            }
            SetupPermission.LOCATION -> {
                setupRuntimePermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)); return
            }
            SetupPermission.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
            SetupPermission.LISTENER -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                android.content.ComponentName(this, com.overud.capture.OfferNotificationListener::class.java).flattenToString(),
            )
            SetupPermission.SHEVERY -> {
                val bridge = (application as OverudApp).container.bridge
                bridge.refresh()
                if (bridge.status.value.connected) { bridge.authorize(); return }
                packageManager.getLaunchIntentForPackage("com.hamondev.shevery") ?: run {
                    android.widget.Toast.makeText(this, "请先安装并启动 Shevery，然后返回继续授权", android.widget.Toast.LENGTH_LONG).show()
                    return
                }
            }
        }
        runCatching { setupSettings.launch(intent) }.onFailure {
            android.widget.Toast.makeText(this, "无法打开授权页面，请从系统设置中授权 OfferPilot", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    override fun onResume() {
        super.onResume()
        (application as OverudApp).container.bridge.refresh()
    }
}
