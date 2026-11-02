package com.overud.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.model.Platform
import com.overud.platform.OfferParser
import com.overud.valuation.EligibilityEvaluator
import com.overud.work.WorkSessionController
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RulesViewModel(private val session: WorkSessionController) : ViewModel() {
    private val mutable = MutableStateFlow(RuleInputParser.fromPolicy(session.state.value.policy))
    val state = mutable.asStateFlow()
    private val mutablePreview = MutableStateFlow(RulePreviewState())
    val preview = mutablePreview.asStateFlow()

    init {
        viewModelScope.launch {
            session.state.map { it.policy }.distinctUntilChanged().collect { policy ->
                mutable.value = RuleInputParser.fromPolicy(policy)
                updatePreview(mutablePreview.value.text, mutablePreview.value.platform)
            }
        }
    }

    fun edit(field: RuleField, value: String) {
        mutable.update {
            when (field) {
                RuleField.MINIMUM -> it.copy(minimum = value)
                RuleField.MILE -> it.copy(mile = value)
                RuleField.HOUR -> it.copy(hour = value)
                RuleField.DISTANCE -> it.copy(distance = value)
                RuleField.TIME -> it.copy(time = value)
                RuleField.PICKUP -> it.copy(pickup = value)
                RuleField.COST -> it.copy(cost = value)
                RuleField.EXCLUDED -> it.copy(excluded = value)
            }.copy(error = null)
        }
    }
    fun platform(platform: Platform, enabled: Boolean) {
        mutable.update { it.copy(platforms = if (enabled) it.platforms + platform else it.platforms - platform, error = null) }
    }
    fun stacked(enabled: Boolean) { mutable.update { it.copy(stacked = enabled) } }
    fun addon(enabled: Boolean) { mutable.update { it.copy(addon = enabled) } }
    fun save() {
        val policy = RuleInputParser.toPolicy(state.value, session.state.value.policy)
        if (policy == null) mutable.update { it.copy(error = "请输入有效的接单要求，并至少开启一个平台") }
        else session.savePolicy(policy)
    }
    fun updatePreview(text: String = preview.value.text, platform: Platform = preview.value.platform) {
        val offer = OfferParser.parse(platform, text, System.currentTimeMillis())
        val decision = offer?.let { EligibilityEvaluator.assess(it, session.state.value.policy, it.observedAt) }
        val explanation = decision?.reasons?.joinToString("；")?.ifEmpty { "符合当前全部接单要求" }
            ?: "无法识别报价，请包含接单按钮文字和金额"
        mutablePreview.value = RulePreviewState(text, platform, explanation)
    }
}
