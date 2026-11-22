package io.github.pennymorant.offerpilot.capture

import android.app.*
import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.Intent
import android.os.Build
import android.os.IBinder
import io.github.pennymorant.offerpilot.MainActivity
import io.github.pennymorant.offerpilot.OverudApp
import io.github.pennymorant.offerpilot.R
import io.github.pennymorant.offerpilot.geo.WorkLocationTracker
import io.github.pennymorant.offerpilot.model.DeliveryPhase
import io.github.pennymorant.offerpilot.overlay.OfferOverlay
import io.github.pennymorant.offerpilot.work.WorkState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class WorkService : Service() {
    private var scheduler: CaptureScheduler? = null
    private var overlay: OfferOverlay? = null
    private var location: WorkLocationTracker? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastChipText: String? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (intent?.action == "MUTE_VOICE") {
            val voice = (application as OverudApp).container.voice
            voice.configure(voice.settings.value.copy(enabled = false))
            if (scheduler == null) stopSelf()
            return START_NOT_STICKY
        }
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel("work_live", "工作状态（实时更新）", NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
        val runtime = (application as OverudApp).container
        val notification = buildWorkNotification(runtime.state.value)
        var serviceType = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        startForeground(1, notification, serviceType)
        if (scheduler == null) {
            runtime.startWork()
            overlay = OfferOverlay(this, runtime).also { it.show() }
            location = WorkLocationTracker(this, runtime.regions).also { it.start() }
            scheduler = CaptureScheduler(runtime.pipeline, runtime.bridge.windowEvents).also { it.start() }
            serviceScope.launch {
                runtime.state.collectLatest { state ->
                    val chip = statusChipText(state)
                    if (chip != lastChipText) {
                        lastChipText = chip
                        manager.notify(1, buildWorkNotification(state))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun statusChipText(state: WorkState): String = when {
        state.automatic -> "自动接单"
        state.deliveryPhase == DeliveryPhase.ACTIVE -> "配送中"
        else -> "工作中"
    }

    private fun buildWorkNotification(state: WorkState): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val open = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1, Intent(this, WorkService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        val mute = PendingIntent.getService(this, 2, Intent(this, WorkService::class.java).setAction("MUTE_VOICE"), PendingIntent.FLAG_IMMUTABLE)
        val chip = statusChipText(state)
        lastChipText = chip
        val builder = Notification.Builder(this, "work_live")
            .setSmallIcon(R.drawable.ic_overud)
            .setContentTitle("OfferPilot · $chip")
            .setContentText("点击进入工作台，或快捷结束本次工作")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, "关闭语音", mute).build())
            .addAction(Notification.Action.Builder(null, "结束工作", stop).build())
        if (Build.VERSION.SDK_INT >= 36) {
            builder.setRequestPromotedOngoing(true)
            builder.setShortCriticalText(chip)
        }
        return builder.build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        scheduler?.close(); overlay?.close(); location?.stop()
        (application as OverudApp).container.stopWork()
        (application as OverudApp).container.voice.endWork()
        super.onDestroy()
    }
}
