package com.overud.capture

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.overud.di.AppContainer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/** Compact native window; the main app uses the Compose expressive theme. */
class OfferOverlay(private val context: Context, private val runtime: AppContainer) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: LinearLayout? = null
    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(10), dp(16), dp(8))
            background = GradientDrawable().apply { setColor(0xFFF0F5E8.toInt()); cornerRadius = dp(24).toFloat() }
            elevation = dp(8).toFloat()
        }
        val label = TextView(context).apply { setTextColor(0xFF20351A.toInt()); textSize = 14f }
        val pause = Button(context).apply { text = "暂停自动接单"; setOnClickListener { runtime.setAutomatic(false) } }
        panel.addView(label); panel.addView(pause)
        val params = WindowManager.LayoutParams(dp(255), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; y = dp(90); x = dp(12) }
        runCatching { manager.addView(panel, params); view = panel }.onFailure { scope.cancel() }
        scope.launch {
            runtime.state.collectLatest { state ->
                val offer = state.latest
                label.text = "Overud · ${if (state.automatic) "自动接单中" else "手动模式"}\n" +
                    (offer?.let { "${it.platform.label}  $${"%.2f".format((it.amountCents ?: 0) / 100.0)}\n${state.decision?.reasons?.firstOrNull() ?: "符合接单要求"}" } ?: "等待新报价")
                pause.isEnabled = state.automatic
            }
        }
    }
    fun close() { scope.cancel(); view?.let { runCatching { manager.removeView(it) } }; view = null }
}
