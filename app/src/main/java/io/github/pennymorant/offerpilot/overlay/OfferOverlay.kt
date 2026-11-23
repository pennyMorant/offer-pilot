package io.github.pennymorant.offerpilot.overlay

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import io.github.pennymorant.offerpilot.di.AppContainer
import io.github.pennymorant.offerpilot.ui.theme.isDark
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

/** Glanceable Material 3 Expressive floating offer card covering the top status bar. */
class OfferOverlay(private val context: Context, private val runtime: AppContainer) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: View? = null
    private var window: OverlayWindowLayout? = null
    private val collapsedOfferId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private var lastOfferId: String? = null
    private var timeoutJob: Job? = null
    private var dismissing = false

    fun show() {
        if (view != null || !Settings.canDrawOverlays(context)) return
        var layoutRef: OverlayWindowLayout? = null
        fun dismissCurrent() {
            val targetId = lastOfferId ?: return
            if (dismissing) return
            dismissing = true
            val l = layoutRef
            if (l != null) {
                l.animateOut {
                    dismissing = false
                    collapsedOfferId.value = targetId
                }
            } else {
                dismissing = false
                collapsedOfferId.value = targetId
            }
        }
        val v = buildOverlayViews(context, onSwipeUp = ::dismissCurrent)
        val layout = OverlayWindowLayout(context, manager, v)
        layoutRef = layout

        v.closeBtn.setOnClickListener { dismissCurrent() }
        v.root.visibility = View.GONE
        runCatching { layout.attach(); window = layout; view = v.root }.onFailure {
            layout.close(); scope.cancel(); return
        }

        scope.launch {
            combine(runtime.state, runtime.appearance.state, collapsedOfferId, runtime.appInForeground) {
                state, appearance, collapsed, inApp -> Quad(state, appearance, collapsed, inApp)
            }.collectLatest { (state, appearance, collapsed, inApp) ->
                val offer = state.latest
                val isDemo = offer?.id?.startsWith("demo-") == true
                if (offer == null || (inApp && !isDemo)) {
                    v.root.visibility = View.GONE
                    return@collectLatest
                }
                if (offer.id != lastOfferId) {
                    lastOfferId = offer.id
                    dismissing = false
                    if (collapsedOfferId.value != null) collapsedOfferId.value = null
                    timeoutJob?.cancel()
                    timeoutJob = scope.launch {
                        delay(30_000L)
                        if (lastOfferId == offer.id) dismissCurrent()
                    }
                }
                if (collapsed == offer.id || dismissing) {
                    if (!dismissing) v.root.visibility = View.GONE
                    return@collectLatest
                }
                val wasHidden = v.root.visibility != View.VISIBLE
                val systemDark = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                val dark = appearance.isDark(systemDark)
                val tier = resolveOfferTier(state.decision)

                applyTierColors(v, tier, dark)
                renderOfferContent(v, state, offer, tier)

                layout.refresh()
                v.root.visibility = View.VISIBLE
                if (wasHidden) layout.animateIn()
            }
        }
    }

    fun close() {
        timeoutJob?.cancel()
        scope.cancel()
        window?.close()
        window = null
        view = null
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
