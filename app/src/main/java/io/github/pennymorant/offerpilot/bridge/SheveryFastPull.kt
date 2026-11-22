package io.github.pennymorant.offerpilot.bridge

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import rikka.shizuku.Shizuku

/**
 * Best-effort native Shevery fast-pull trigger for Wireless ADB (UID 2000) sessions.
 * Falls back transparently to the Shizuku provider delivery path when unavailable.
 */
object SheveryFastPull {
    private const val SHEVERY_PACKAGE = "com.hamondev.shevery"
    private val PROVIDER_URI: Uri = Uri.parse("content://com.hamondev.shevery.shevery")

    fun isSheveryInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHEVERY_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        true
    }.getOrDefault(false)

    fun tryPull(context: Context): Boolean {
        if (runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return true
        val args = Bundle().apply { putString("package", context.packageName) }
        val methods = listOf("requestBinder", "sendBinder", "getBinder")
        for (method in methods) {
            val reply = runCatching {
                context.contentResolver.call(PROVIDER_URI, method, context.packageName, args)
            }.getOrNull() ?: continue
            val binder = reply.getBinder("binder") ?: reply.getBinder("rikka.shizuku.intent.extra.BINDER")
            if (binder != null && binder.isBinderAlive) {
                runCatching {
                    Shizuku::class.java.getMethod("onBinderReceived", IBinder::class.java, String::class.java)
                        .invoke(null, binder, context.packageName)
                }
                if (runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return true
            }
        }
        return runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    }

    fun uidLabel(): String = runCatching {
        when (Shizuku.getUid()) {
            0 -> "Root (UID 0)"
            2000 -> "无线调试 Shell (UID 2000)"
            else -> "UID ${Shizuku.getUid()}"
        }
    }.getOrDefault("未知身份")
}
