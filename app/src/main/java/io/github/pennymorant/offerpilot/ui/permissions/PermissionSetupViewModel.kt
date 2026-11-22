package io.github.pennymorant.offerpilot.ui.permissions

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import io.github.pennymorant.offerpilot.capture.OfferNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionSetupViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("permission_setup", 0)
    private var sheveryAuthorized = false
    private val mutable = MutableStateFlow(PermissionSetupState(
        visible = preferences.getInt("next_step", 0) < SetupPermission.entries.size,
        stage = if (saved.get<Boolean>("started") == true) SetupStage.PERMISSION else SetupStage.WELCOME,
        busy = saved.get<String>("pending") != null,
    ))
    val state = mutable.asStateFlow()

    init { refresh(false) }

    fun start() {
        saved["started"] = true
        mutable.value = mutable.value.copy(stage = SetupStage.PERMISSION)
        selectNext()
    }

    fun requested(step: SetupPermission) = preferences.getBoolean("requested_${step.name}", false)

    fun beginRequest(step: SetupPermission) {
        saved["pending"] = step.name
        preferences.edit().putBoolean("requested_${step.name}", true).apply()
        mutable.value = mutable.value.copy(busy = true, message = null)
    }

    fun returned() {
        val pending = saved.get<String>("pending") ?: return
        saved.remove<String>("pending")
        refresh(sheveryAuthorized)
        val denied = SetupPermission.valueOf(pending) !in mutable.value.granted
        mutable.value = mutable.value.copy(busy = false,
            message = if (denied) "尚未开启。你可以再次授权，也可以稍后在设置中补齐。" else null)
    }

    fun showMessage(message: String) {
        saved.remove<String>("pending")
        mutable.value = mutable.value.copy(busy = false, message = message)
    }

    fun skip() {
        val step = mutable.value.current ?: return
        preferences.edit().putInt("next_step", step.ordinal + 1).apply()
        mutable.value = mutable.value.copy(message = null)
        selectNext()
    }

    fun finish() {
        preferences.edit().putInt("next_step", SetupPermission.entries.size).apply()
        mutable.value = mutable.value.copy(visible = false)
    }

    fun refresh(authorized: Boolean) {
        sheveryAuthorized = authorized
        val granted = SetupPermission.entries.filter { granted(it) }.toSet()
        mutable.value = mutable.value.copy(granted = granted)
        if (mutable.value.stage == SetupStage.PERMISSION) selectNext()
    }

    private fun selectNext() {
        val current = SetupPermission.entries.drop(preferences.getInt("next_step", 0))
            .firstOrNull { it !in mutable.value.granted }
        mutable.value = mutable.value.copy(current = current,
            message = if (current != mutable.value.current) null else mutable.value.message,
            stage = if (current == null) SetupStage.SUMMARY else SetupStage.PERMISSION)
    }

    private fun granted(step: SetupPermission): Boolean {
        val context = getApplication<Application>()
        fun allowed(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        return when (step) {
            SetupPermission.NOTIFICATIONS -> allowed(Manifest.permission.POST_NOTIFICATIONS)
                && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
            SetupPermission.LOCATION -> allowed(Manifest.permission.ACCESS_COARSE_LOCATION)
                || allowed(Manifest.permission.ACCESS_FINE_LOCATION)
            SetupPermission.OVERLAY -> Settings.canDrawOverlays(context)
            SetupPermission.LISTENER -> context.getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(ComponentName(context, OfferNotificationListener::class.java))
            SetupPermission.SHEVERY -> sheveryAuthorized
        }
    }
}
