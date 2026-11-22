package io.github.pennymorant.offerpilot.ui.imagetest

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.pennymorant.offerpilot.capture.ImportedImageRecognizer
import io.github.pennymorant.offerpilot.model.*
import io.github.pennymorant.offerpilot.platform.OfferParser
import io.github.pennymorant.offerpilot.valuation.EligibilityEvaluator
import io.github.pennymorant.offerpilot.work.WorkSessionController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ImageTestViewModel(
    private val recognizer: ImportedImageRecognizer,
    private val session: WorkSessionController,
) : ViewModel() {
    private val mutable = MutableStateFlow(ImageTestUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0L

    init {
        viewModelScope.launch {
            session.state.map { it.policy }.distinctUntilChanged().collect { analyze() }
        }
    }

    fun selectPlatform(platform: Platform) {
        mutable.update { it.copy(platform = platform) }
        analyze()
    }

    fun selectImage(uri: Uri) {
        job?.cancel()
        val request = ++generation
        mutable.value = ImageTestUiState(platform = state.value.platform, busy = true)
        job = viewModelScope.launch {
            try {
                val result = recognizer.recognize(uri)
                if (request != generation) return@launch
                mutable.update { it.copy(busy = false, hasImage = true, text = result.text, regions = result.regions,
                    error = if (result.text.isBlank()) "未识别到文字，请选择清晰、完整的订单截图" else null) }
                analyze()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (request == generation) mutable.update { it.copy(busy = false,
                    error = "图片无法读取或识别，请重新选择本地 JPG、PNG 等图片") }
            }
        }
    }

    fun clear() {
        generation++
        job?.cancel()
        mutable.value = ImageTestUiState(platform = state.value.platform)
    }

    private fun analyze() {
        val current = state.value
        if (!current.hasImage || current.text.isBlank()) return
        val now = System.currentTimeMillis()
        val offer = if (current.regions.isNotEmpty()) {
            OfferParser.parseRegions(current.platform, current.regions, now, OfferSource.OCR)
        } else {
            OfferParser.parse(current.platform, current.text, now, OfferSource.OCR)
        }
        val page = OfferParser.classify(current.platform, current.text, OfferSource.OCR)
        val policy = session.state.value.policy
        val decision = offer?.let { EligibilityEvaluator.assess(it, policy, now) }
        mutable.value = current.copy(
            offer = offer,
            page = page,
            decision = decision,
            preferredZones = policy.effective(current.platform).preferredZones,
        )
    }
}
