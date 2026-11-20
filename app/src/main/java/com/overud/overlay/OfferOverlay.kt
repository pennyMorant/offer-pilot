package com.overud.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.overud.di.AppContainer
import com.overud.model.DeliveryPhase
import com.overud.model.Platform
import com.overud.ui.components.*
import com.overud.ui.theme.appearanceColors
import com.overud.ui.theme.isDark
import com.overud.valuation.StrategyAdvisor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.util.Locale

/** Compact Android 17 utility overlay synchronized with M3 Expressive dynamic colors. */
class OfferOverlay(private val context: Context, private val runtime: AppContainer) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: LinearLayout? = null

    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val cardBg = GradientDrawable().apply { cornerRadius = dp(20).toFloat() }
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = cardBg
            elevation = dp(10).toFloat()
        }
        val header = TextView(context).apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        val headline = TextView(context).apply {
            textSize = 20f; typeface = Typeface.DEFAULT_BOLD; fontFeatureSettings = "tnum"
        }
        val body = TextView(context).apply { textSize = 12.5f; fontFeatureSettings = "tnum" }
        val pause = Button(context).apply { text = "暂停自动接单"; textSize = 12f; setOnClickListener { runtime.setAutomatic(false) } }
        val switchOther = Button(context).apply { textSize = 12f; visibility = View.GONE }
        panel.addView(header); panel.addView(headline); panel.addView(body); panel.addView(pause); panel.addView(switchOther)

        val params = WindowManager.LayoutParams(dp(248), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; y = dp(84); x = dp(10) }
        runCatching { manager.addView(panel, params); view = panel }.onFailure { scope.cancel() }

        scope.launch {
            combine(runtime.state, runtime.appearance.state) { state, appearance -> state to appearance }
                .collectLatest { (state, appearance) ->
                    val systemDark = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    val colors = appearanceColors(context, appearance, appearance.isDark(systemDark))
                    val offer = state.latest
                    val advice = offer?.let { StrategyAdvisor.advise(it, state.decision) }
                    val alert = advice?.fakePreferred == true
                    val bgColor = if (alert) colors.errorContainer.toArgb() else colors.surfaceContainer.toArgb()
                    val fgColor = if (alert) colors.onErrorContainer.toArgb() else colors.onSurface.toArgb()
                    cardBg.setColor(bgColor)
                    cardBg.setStroke(dp(1), colors.outlineVariant.toArgb())
                    header.setTextColor(if (alert) fgColor else colors.onSurfaceVariant.toArgb())
                    headline.setTextColor(fgColor)
                    body.setTextColor(fgColor)
                    pause.backgroundTintList = android.content.res.ColorStateList.valueOf(colors.primaryContainer.toArgb())
                    pause.setTextColor(colors.onPrimaryContainer.toArgb())
                    switchOther.backgroundTintList = android.content.res.ColorStateList.valueOf(colors.tertiaryContainer.toArgb())
                    switchOther.setTextColor(colors.onTertiaryContainer.toArgb())

                    val mode = when {
                        state.automatic -> "自动接单中"
                        state.autoAcceptEnabled -> "自动接单待命"
                        else -> "手动监测"
                    }
                    header.text = "OVERUD · $mode"
                    if (offer != null) {
                        val score = state.decision?.valuation?.score?.let { " · ${it}分" }.orEmpty()
                        headline.visibility = View.VISIBLE
                        headline.text = "${offer.platform.label} ${offerAmountPresentation(offer)}$score"
                        val strategy = advice?.tags?.takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let { "$it\n" }.orEmpty()
                        body.text = "$strategy${state.decision?.reasons?.firstOrNull() ?: "全部条件通过"}\n" +
                            vehicleCostPresentation(state.decision?.valuation).compact + "\n" +
                            prop22Compact(state.decision?.valuation?.prop22) +
                            destinationPresentation(state.destinationAssessment, Locale.getDefault()).title
                    } else {
                        headline.visibility = View.GONE
                        body.text = platformPagePresentation(state.currentPage) ?: "等待新报价…"
                    }
                    pause.visibility = if (state.autoAcceptEnabled) View.VISIBLE else View.GONE
                    val active = state.activePlatform
                    val other = active?.let { p -> Platform.entries.firstOrNull { it != p } }
                    if (state.deliveryPhase == DeliveryPhase.ACTIVE && active != null && other != null) {
                        switchOther.visibility = View.VISIBLE
                        switchOther.text = "去暂停 ${other.label}"
                        switchOther.setOnClickListener { runtime.launchOtherPlatform(active) }
                    } else {
                        switchOther.visibility = View.GONE
                    }
                }
        }
    }

    fun close() { scope.cancel(); view?.let { runCatching { manager.removeView(it) } }; view = null }
}
