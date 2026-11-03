package com.overud.overlay

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
import com.overud.ui.components.vehicleCostPresentation
import com.overud.ui.components.prop22Compact
import com.overud.ui.components.platformPagePresentation
import com.overud.ui.components.offerAmountPresentation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import androidx.compose.ui.graphics.toArgb
import com.overud.ui.theme.appearanceColors
import com.overud.ui.theme.isDark
import com.overud.ui.components.destinationPresentation
import java.util.Locale

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
            background = GradientDrawable().apply { cornerRadius = dp(24).toFloat() }
            elevation = dp(8).toFloat()
        }
        val label = TextView(context).apply { textSize = 14f }
        val pause = Button(context).apply { text = "暂停自动接单"; setOnClickListener { runtime.setAutomatic(false) } }
        panel.addView(label); panel.addView(pause)
        val params = WindowManager.LayoutParams(dp(255), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; y = dp(90); x = dp(12) }
        runCatching { manager.addView(panel, params); view = panel }.onFailure { scope.cancel() }
        scope.launch {
            combine(runtime.state, runtime.appearance.state) { state, appearance -> state to appearance }
                .collectLatest { (state, appearance) ->
                val systemDark = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                val colors = appearanceColors(context, appearance, appearance.isDark(systemDark))
                (panel.background as GradientDrawable).setColor(colors.surfaceContainer.toArgb())
                label.setTextColor(colors.onSurface.toArgb())
                pause.backgroundTintList = android.content.res.ColorStateList.valueOf(colors.primaryContainer.toArgb())
                pause.setTextColor(colors.onPrimaryContainer.toArgb())
                val offer = state.latest
                val mode = when {
                    state.automatic -> "自动接单中"
                    state.autoAcceptEnabled -> "自动接单已暂停"
                    else -> "手动模式"
                }
                label.text = "Overud · $mode\n" +
                    (offer?.let {
                        val amount = offerAmountPresentation(it)
                        "${it.platform.label}  $amount\n${state.decision?.reasons?.firstOrNull() ?: "符合接单要求"}\n" +
                            vehicleCostPresentation(state.decision?.valuation).compact + "\n" +
                            prop22Compact(state.decision?.valuation?.prop22) +
                            destinationPresentation(state.destinationAssessment, Locale.getDefault()).title
                    } ?: platformPagePresentation(state.currentPage) ?: "等待新报价")
                pause.isEnabled = state.autoAcceptEnabled
            }
        }
    }
    fun close() { scope.cancel(); view?.let { runCatching { manager.removeView(it) } }; view = null }
}
