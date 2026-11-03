package com.overud.ui.rules

import androidx.lifecycle.ViewModel
import com.overud.speech.VoiceController

class VoiceViewModel(private val voice: VoiceController) : ViewModel() {
    val settings = voice.settings
    val status = voice.status
    fun enabled(value: Boolean) = voice.configure(settings.value.copy(enabled = value))
    fun prop22(value: Boolean) = voice.configure(settings.value.copy(includeProp22 = value))
    fun preview() = voice.preview()
    fun stop() = voice.stop()
}
