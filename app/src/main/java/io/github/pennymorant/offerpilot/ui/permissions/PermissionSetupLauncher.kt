package io.github.pennymorant.offerpilot.ui.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import io.github.pennymorant.offerpilot.bridge.SheveryBridge
import io.github.pennymorant.offerpilot.capture.OfferNotificationListener

/** Owns system permission launches; the dialog only renders ViewModel state. */
class PermissionSetupLauncher(
    private val activity: ComponentActivity,
    private val model: PermissionSetupViewModel,
    private val bridge: SheveryBridge,
) {
    private val runtime = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshResult()
    }
    private val settings = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshResult()
    }

    fun request(step: SetupPermission) {
        if (model.state.value.busy) return
        val uri = Uri.parse("package:${activity.packageName}")
        try {
            when (step) {
                SetupPermission.NOTIFICATIONS -> requestRuntime(step, arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                SetupPermission.LOCATION -> requestRuntime(step,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                SetupPermission.OVERLAY -> open(step, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
                SetupPermission.LISTENER -> {
                    val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(activity, OfferNotificationListener::class.java).flattenToString(),
                    )
                    open(step, if (detail.resolveActivity(activity.packageManager) != null) detail
                        else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                SetupPermission.SHEVERY -> requestShevery()
            }
        } catch (_: Exception) {
            model.showMessage("无法打开授权页面。请在手机系统设置中查找 OfferPilot，或稍后再试。")
        }
    }

    private fun requestRuntime(step: SetupPermission, permissions: Array<String>) {
        val canShowDialog = permissions.any { activity.shouldShowRequestPermissionRationale(it) }
        if (model.requested(step) && !canShowDialog) {
            val intent = if (step == SetupPermission.NOTIFICATIONS) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}"))
            open(step, intent)
        } else {
            model.beginRequest(step)
            runtime.launch(permissions)
        }
    }

    private fun requestShevery() {
        bridge.refresh()
        if (bridge.status.value.authorized) {
            model.refresh(true)
        } else if (bridge.status.value.connected) {
            bridge.authorize()
            model.showMessage("请在 Shevery 弹出的窗口中允许 OfferPilot。未出现窗口时，请打开 Shevery 检查应用授权。")
        } else {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.hamondev.shevery")
            if (intent == null) model.showMessage("尚未安装 Shevery。请先安装并启动服务，或跳过此项稍后设置。")
            else open(SetupPermission.SHEVERY, intent)
        }
    }

    private fun open(step: SetupPermission, intent: Intent) {
        model.beginRequest(step)
        settings.launch(intent)
    }

    private fun refreshResult() {
        bridge.refresh()
        model.refresh(bridge.status.value.authorized)
        model.returned()
    }
}
