package com.overud.ui.permissions

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import com.overud.capture.OfferNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SetupPermission(val title: String, val explanation: String) {
    NOTIFICATIONS("允许工作通知", "用于显示工作状态、订单提醒和停止工作入口。"),
    LOCATION("允许使用位置", "用于分析当前区域及目的地附近的历史订单质量。选择大致位置也可继续。"),
    OVERLAY("允许悬浮窗", "在 Uber 或 DoorDash 上方显示订单分析结果。请在系统设置中允许 OfferPilot 显示在其他应用上层。"),
    LISTENER("允许读取订单通知", "用于监听 Uber 和 DoorDash 的订单通知并触发识别。请在系统设置中开启 OfferPilot 的通知使用权。"),
    SHEVERY("连接 Shevery", "页面读取与自动接单需要 Shevery。请先启动 Shevery 服务，再允许 OfferPilot 使用。"),
}

/** One-time setup; declining a step never enables work or automatic acceptance. */
class PermissionSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("permission_setup", 0)
    private val mutable = MutableStateFlow<SetupPermission?>(null)
    val step = mutable.asStateFlow()

    init { selectNext() }

    fun advance() {
        val current = mutable.value ?: return
        preferences.edit().putInt("next_step", current.ordinal + 1).apply()
        selectNext()
    }

    fun refresh(sheveryAuthorized: Boolean) {
        while (mutable.value?.let { granted(it, sheveryAuthorized) } == true) advance()
    }

    private fun selectNext() {
        mutable.value = SetupPermission.entries.drop(preferences.getInt("next_step", 0))
            .firstOrNull { !granted(it, false) }
    }

    private fun granted(step: SetupPermission, sheveryAuthorized: Boolean): Boolean {
        val context = getApplication<Application>()
        fun allowed(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        return when (step) {
            SetupPermission.NOTIFICATIONS -> allowed(Manifest.permission.POST_NOTIFICATIONS)
            SetupPermission.LOCATION -> allowed(Manifest.permission.ACCESS_COARSE_LOCATION)
                || allowed(Manifest.permission.ACCESS_FINE_LOCATION)
            SetupPermission.OVERLAY -> Settings.canDrawOverlays(context)
            SetupPermission.LISTENER -> context.getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(ComponentName(context, OfferNotificationListener::class.java))
            SetupPermission.SHEVERY -> sheveryAuthorized
        }
    }
}
