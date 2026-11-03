package com.overud.ui.rules

import androidx.lifecycle.ViewModel
import com.overud.model.*
import com.overud.platform.OfferParser
import com.overud.valuation.EligibilityEvaluator
import com.overud.ui.components.platformPagePresentation
import com.overud.work.WorkSessionController
import kotlinx.coroutines.flow.*

class RulePreviewViewModel(private val session: WorkSessionController) : ViewModel() {
    private val mutablePreview = MutableStateFlow(RulePreviewState())
    val preview = mutablePreview.asStateFlow()
    init { updatePreview() }
    fun updatePreview(text: String = preview.value.text, platform: Platform = preview.value.platform) {
        val offer = OfferParser.parse(platform, text, System.currentTimeMillis())
        val decision = offer?.let { EligibilityEvaluator.assess(it, session.state.value.policy, it.observedAt) }
        val explanation = decision?.reasons?.joinToString("；")?.ifEmpty { "符合当前全部接单要求" }
            ?: "无法识别报价，请包含接单按钮文字和金额"
        mutablePreview.value = RulePreviewState(text, platform, explanation, decision?.valuation,
            platformPagePresentation(OfferParser.classify(platform, text)))
    }
}

data class RulePreviewState(
    val text: String = "Accept\n$12.00\nTotal 5 mi 25 min\nMerchant: Restaurant\nDestination: Downtown",
    val platform: Platform = Platform.UBER_EATS,
    val explanation: String = "",
    val valuation: Valuation? = null,
    val pageSummary: String? = null,
)
