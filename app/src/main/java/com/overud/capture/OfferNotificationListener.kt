package com.overud.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.overud.OverudApp
import com.overud.model.Platform

class OfferNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val platform = Platform.fromPackage(sbn.packageName) ?: return
        val extras = sbn.notification.extras
        val text = listOfNotNull(extras.getCharSequence(Notification.EXTRA_TITLE), extras.getCharSequence(Notification.EXTRA_TEXT)).joinToString("\n")
        (application as OverudApp).runtime.notification(platform, text)
    }
}
