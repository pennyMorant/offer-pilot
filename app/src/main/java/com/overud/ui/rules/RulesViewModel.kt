package com.overud.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.model.*
import com.overud.platform.OfferParser
import com.overud.valuation.EligibilityEvaluator
import com.overud.ui.components.platformPagePresentation
import com.overud.work.WorkSessionController
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RulesViewModel(private val session: WorkSessionController) : ViewModel() {
    private val mutable = MutableStateFlow(RuleInputParser.fromPolicy(session.state.value.policy))
    val state = mutable.asStateFlow()
    private val drafts = mutableMapOf<Platform?, RulesUiState>()
    private var loadedVersion = -1L
    private val mutablePreview = MutableStateFlow(RulePreviewState())
    val preview = mutablePreview.asStateFlow()
    private val mutableProp22 = MutableStateFlow(Prop22Input.from(session.state.value.policy.prop22))
    val prop22 = mutableProp22.asStateFlow()

    init {
        viewModelScope.launch {
            session.state.map { Triple(it.policy, it.policySaving, it.policySaveError) }
                .distinctUntilChanged().collect { (policy, saving, error) ->
                    if (policy.version != loadedVersion) {
                        val scope = state.value.scope
                        loadedVersion = policy.version
                        drafts.clear()
                        mutable.value = RuleInputParser.fromPolicy(policy, scope)
                        mutableProp22.value = Prop22Input.from(policy.prop22)
                        updatePreview()
                    }
                    mutable.update { it.copy(saving = saving, error = error ?: it.error) }
                }
        }
    }

    fun selectScope(scope: Platform?) {
        if (state.value.saving) return
        drafts[state.value.scope] = state.value
        val next = drafts[scope] ?: RuleInputParser.fromPolicy(session.state.value.policy, scope)
        mutable.value = next.copy(platforms = state.value.platforms, error = null)
    }
    fun inheritShared(enabled: Boolean) {
        if (!state.value.saving) mutable.update { it.copy(inheritShared = enabled, error = null) }
    }
    fun condition(condition: RuleCondition, enabled: Boolean) {
        if (state.value.saving) return
        mutable.update {
            it.copy(disabledConditions = if (enabled) it.disabledConditions - condition
                else it.disabledConditions + condition, error = null)
        }
    }
    fun hourlyBasis(basis: HourlyBasis) {
        if (!state.value.saving) mutable.update { it.copy(hourlyBasis = basis, error = null) }
    }
    fun regional(enabled: Boolean) {
        if (!state.value.saving) mutable.update { it.copy(regionalEnabled = enabled, error = null) }
    }
    fun edit(field: RuleField, value: String) {
        if (state.value.saving) return
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
                RuleField.MERCHANTS -> it.copy(excludedMerchants = value)
                RuleField.REGION_PERCENT -> it.copy(regionalGoodPercent = value)
                RuleField.REGION_SAMPLES -> it.copy(regionalSamples = value)
            }.copy(error = null)
        }
    }
    fun platform(platform: Platform, enabled: Boolean) {
        if (state.value.saving) return
        mutable.update { it.copy(platforms = if (enabled) it.platforms + platform else it.platforms - platform, error = null) }
    }
    fun stacked(enabled: Boolean) {
        if (!state.value.saving) mutable.update { it.copy(stacked = enabled) }
    }
    fun addon(enabled: Boolean) {
        if (!state.value.saving) mutable.update { it.copy(addon = enabled) }
    }
    fun save() {
        if (state.value.saving) return
        val prop22 = mutableProp22.value.parse(session.state.value.policy.prop22)
        if (prop22 == null) {
            mutable.update { it.copy(error = "Prop 22：最低时薪需为 16.90–1000.00 美元，小费比例需为 0–100 的整数") }
            return
        }
        drafts[state.value.scope] = state.value
        var policy = session.state.value.policy.copy(enabledPlatforms = state.value.platforms, prop22 = prop22)
        val scopes = listOf<Platform?>(null) + Platform.entries
        for (scope in scopes) {
            val draft = drafts[scope] ?: continue
            val updated = RuleInputParser.toPolicy(draft, policy)
            if (updated == null) {
                mutable.update { it.copy(error = "${scope?.label ?: "公共要求"}输入无效，请检查数值范围并至少启用一项最低收益要求") }
                return
            }
            policy = updated
        }
        if (policy.valid()) session.savePolicy(policy)
        else mutable.update { it.copy(error = "请至少开启一个平台，并检查所有接单要求") }
    }
    fun updatePreview(text: String = preview.value.text, platform: Platform = preview.value.platform) {
        val offer = OfferParser.parse(platform, text, System.currentTimeMillis())
        val decision = offer?.let { EligibilityEvaluator.assess(it, session.state.value.policy, it.observedAt) }
        val explanation = decision?.reasons?.joinToString("；")?.ifEmpty { "符合当前全部接单要求" }
            ?: "无法识别报价，请包含接单按钮文字和金额"
        mutablePreview.value = RulePreviewState(text, platform, explanation, decision?.valuation,
            platformPagePresentation(OfferParser.classify(platform, text)))
    }
    fun updateProp22(input: Prop22Input) {
        if (state.value.saving) return
        mutableProp22.value = input
        mutable.update { it.copy(error = null) }
    }
}
