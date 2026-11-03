package com.overud.speech

import android.content.Context
import android.media.*
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** All entry points and state updates run on the main thread. No speech text is persisted. */
class SpeechEngine(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener({ change ->
            if (change < 0) stop()
        }, handler).build()
    private var tts: TextToSpeech? = null
    private var current: String? = null
    private var sequence = 0L
    private var generation = 0L
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private val mutableStatus = MutableStateFlow("语音尚未初始化")
    val status = mutableStatus.asStateFlow()

    fun prepare() {
        if (tts != null) return
        val request = ++generation
        mutableStatus.value = "正在初始化中文语音"
        tts = TextToSpeech(context.applicationContext) { code -> handler.post {
            if (request != generation) return@post
            val engine = tts ?: return@post
            if (code != TextToSpeech.SUCCESS) {
                mutableStatus.value = "语音引擎不可用，请检查系统文字转语音设置"
                return@post
            }
            val language = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
            val voice = engine.voices?.firstOrNull { it.locale.language == "zh" && !it.isNetworkConnectionRequired &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() }
            if (language < 0 || voice == null || engine.setVoice(voice) != TextToSpeech.SUCCESS) {
                mutableStatus.value = "请在系统文字转语音设置中安装离线中文语音，再点击试听重试"
                return@post
            }
            engine.setAudioAttributes(attributes)
            engine.setSpeechRate(1.05f)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) { finish(id, "中文语音就绪") }
                @Deprecated("Required platform callback")
                override fun onError(id: String?) { finish(id, "播报失败，请检查系统语音引擎") }
            })
            mutableReady.value = true
            mutableStatus.value = "离线中文语音就绪"
        } }
    }

    fun speak(text: String): Boolean {
        if (!ready.value) return false
        stop()
        val granted = runCatching { audio.requestAudioFocus(focus) }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mutableStatus.value = "音频正被占用，本次未播报"
            return false
        }
        val id = "overud-${++sequence}"
        current = id
        val result = runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) }.getOrNull()
        if (result != TextToSpeech.SUCCESS) {
            stop()
            mutableStatus.value = "播报失败，请检查系统语音设置"
            return false
        }
        mutableStatus.value = "正在播报"
        return true
    }

    fun stop() {
        current = null
        runCatching { tts?.stop() }
        runCatching { audio.abandonAudioFocusRequest(focus) }
        if (ready.value) mutableStatus.value = "中文语音就绪"
    }

    fun release() {
        generation++
        stop()
        mutableReady.value = false
        runCatching { tts?.shutdown() }
        tts = null
    }

    private fun finish(id: String?, message: String) { handler.post {
        if (id == null || id != current) return@post
        current = null
        audio.abandonAudioFocusRequest(focus)
        mutableStatus.value = message
    } }
}
