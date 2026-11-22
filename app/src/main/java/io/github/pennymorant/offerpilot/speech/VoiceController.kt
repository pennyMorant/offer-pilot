package io.github.pennymorant.offerpilot.speech

import android.content.Context
import io.github.pennymorant.offerpilot.data.preferences.ArGuardPreferences
import io.github.pennymorant.offerpilot.model.DeliveryPhase
import io.github.pennymorant.offerpilot.model.Platform
import io.github.pennymorant.offerpilot.work.WorkSessionController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class VoiceSettings(val enabled: Boolean = false, val includeProp22: Boolean = false)

class VoiceController(
    context: Context,
    private val session: WorkSessionController,
    private val arGuard: ArGuardPreferences,
) {
    private val prefs = context.getSharedPreferences("voice", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(runCatching {
        VoiceSettings(prefs.getBoolean("enabled", false), prefs.getBoolean("prop22", false))
    }.getOrDefault(VoiceSettings()))
    val settings = mutable.asStateFlow()
    private val engine = SpeechEngine(context.applicationContext)
    val status = engine.status
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val spoken = linkedSetOf<String>()
    private var phase = DeliveryPhase.AVAILABLE
    private var wasWorking = false
    private var previewPending = false
    private var announcedOffer: String? = null
    private var announcedAlertAt = 0L

    init {
        scope.launch {
            combine(session.state, settings, engine.ready) { work, voice, ready -> Triple(work, voice, ready) }
                .collect { (work, voice, ready) ->
                    if (previewPending && ready) {
                        previewPending = false
                        engine.speak("语音播报已准备好。新订单出现后，将播报金额、里程、耗时和接单判断。")
                        return@collect
                    }
                    if (!work.working || !voice.enabled) {
                        if (wasWorking) engine.stop()
                        announcedOffer = null
                        if (!work.working) spoken.clear()
                        wasWorking = work.working && voice.enabled
                        phase = work.deliveryPhase
                        return@collect
                    }
                    wasWorking = true
                    engine.prepare()
                    if (!ready) return@collect
                    if (announcedOffer != null && work.latest?.id != announcedOffer) {
                        engine.stop()
                        announcedOffer = null
                    }
                    if (work.deliveryPhase != phase) {
                        val previousPhase = phase
                        phase = work.deliveryPhase
                        when (phase) {
                            DeliveryPhase.ACTIVE -> {
                                announcedOffer = null
                                val active = work.activePlatform
                                val other = active?.let { p -> Platform.entries.firstOrNull { it != p } }
                                val msg = if (active != null && other != null)
                                    "已接${active.label}，自动接单暂停，记得暂停${other.label}。"
                                else "已识别进行中的配送，自动接单暂停。"
                                engine.speak(msg)
                                return@collect
                            }
                            DeliveryPhase.AVAILABLE -> if (previousPhase == DeliveryPhase.ACTIVE) {
                                announcedOffer = null
                                engine.speak("配送完成，可恢复双开。")
                                return@collect
                            }
                            DeliveryPhase.UNCERTAIN -> {
                                announcedOffer = null
                                engine.speak("接单结果待核对，请在方便时查看平台。")
                                return@collect
                            }
                            else -> Unit
                        }
                    }
                    if (work.deliveryPhase == DeliveryPhase.ACTIVE || work.deliveryPhase == DeliveryPhase.UNCERTAIN) return@collect
                    val bgAlert = work.backgroundAlert
                    if (bgAlert != null && bgAlert.receivedAt != announcedAlertAt &&
                        System.currentTimeMillis() - bgAlert.receivedAt in 0..15_000L &&
                        bgAlert.platform != work.latest?.platform) {
                        announcedAlertAt = bgAlert.receivedAt
                        engine.speak("${bgAlert.platform.label}后台来新单${bgAlert.amountPreview?.let { "，约$it" }.orEmpty()}。")
                        return@collect
                    }
                    val offer = work.latest ?: return@collect
                    val now = System.currentTimeMillis()
                    if (now - offer.observedAt !in 0..10_000 || now - (work.lastCaptureAt ?: 0) !in 0..5000) return@collect
                    if (offer.id in spoken) return@collect
                    val text = OrderAnnouncement.text(work, voice.includeProp22, arGuard.state.value.uberArPercent) ?: return@collect
                    spoken += offer.id
                    if (spoken.size > 256) spoken.remove(spoken.first())
                    if (engine.speak(text)) announcedOffer = offer.id
                }
        }
    }

    fun configure(value: VoiceSettings) {
        prefs.edit().putBoolean("enabled", value.enabled).putBoolean("prop22", value.includeProp22).apply()
        mutable.value = value
        if (!value.enabled) { previewPending = false; engine.stop() }
        else engine.prepare()
    }

    fun preview() {
        if (engine.ready.value) engine.speak("语音播报已准备好。报价十二美元，五英里，预计二十五分钟。这是试听，不会接单。")
        else {
            engine.release()
            previewPending = true
            engine.prepare()
        }
    }

    fun stop() { previewPending = false; engine.stop() }
    fun endWork() { previewPending = false; spoken.clear(); engine.release() }
}
