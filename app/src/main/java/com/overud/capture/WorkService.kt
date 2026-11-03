package com.overud.capture

import android.app.*
import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.Intent
import android.os.IBinder
import com.overud.MainActivity
import com.overud.OverudApp
import com.overud.R
import com.overud.geo.WorkLocationTracker
import com.overud.overlay.OfferOverlay

class WorkService : Service() {
    private var scheduler: CaptureScheduler? = null
    private var overlay: OfferOverlay? = null
    private var location: WorkLocationTracker? = null
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
        manager.createNotificationChannel(NotificationChannel("work", "订单监听", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, WorkService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        val mute = PendingIntent.getService(this, 2, Intent(this, WorkService::class.java).setAction("MUTE_VOICE"), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, "work").setSmallIcon(R.drawable.ic_overud)
            .setContentTitle("Overud 正在分析订单").setContentText("点击查看，或结束本次工作")
            .setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "关闭语音", mute).build())
            .addAction(Notification.Action.Builder(null, "结束工作", stop).build()).build()
        var serviceType = if (android.os.Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        startForeground(1, notification, serviceType)
        if (scheduler == null) {
            val runtime = (application as OverudApp).container
            runtime.startWork()
            overlay = OfferOverlay(this, runtime).also { it.show() }
            location = WorkLocationTracker(this, runtime.regions).also { it.start() }
            scheduler = CaptureScheduler(runtime.pipeline, runtime.bridge.windowEvents).also { it.start() }
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        scheduler?.close(); overlay?.close(); location?.stop()
        (application as OverudApp).container.stopWork()
        (application as OverudApp).container.voice.endWork()
        super.onDestroy()
    }
}
