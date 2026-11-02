package com.overud.work

import com.overud.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.overud.bridge.SheveryBridge
import com.overud.data.preferences.PolicyStore

class WorkSessionController(
    private val bridge: SheveryBridge,
    private val store: PolicyStore,
    private val scope: CoroutineScope,
    private val mutex: Mutex,
) {
    private val mutable = MutableStateFlow(WorkState(policy = store.read()))
    val state = mutable.asStateFlow()
    init {
        scope.launch {
            bridge.status.collect { if (!it.serviceReady) mutable.update { state -> state.copy(automatic = false) } }
        }
    }
    fun update(transform: (WorkState) -> WorkState) = mutable.update(transform)
    fun startWork() {
        bridge.bind()
        mutable.update { it.copy(working = true, automatic = false, message = "正在监听订单页面") }
    }
    fun stopWork() {
        mutable.update { it.copy(working = false, automatic = false, latest = null, decision = null,
            destinationAssessment = DestinationAssessment(), message = "工作已结束") }
        bridge.unbind()
    }
    fun setAutomatic(enabled: Boolean) {
        mutable.update {
            val allowed = enabled && it.working && it.policy.valid() && !it.policySaving &&
                bridge.status.value.serviceReady && !it.pending
            it.copy(automatic = allowed, message = when {
                !enabled -> "自动接单已关闭"
                allowed -> "自动接单运行中，所有已配置要求必须同时满足"
                else -> "请先连接 Shevery、开始工作并核对待处理订单"
            })
        }
    }
    fun savePolicy(policy: Policy) {
        if (state.value.policySaving) return
        // Immediately invalidate pending decisions while persistence completes.
        mutable.update { it.copy(automatic = false, policySaving = true, policySaveError = null) }
        scope.launch {
            mutex.withLock {
                val updated = policy.copy(version = state.value.policy.version + 1)
                val saved = runCatching { store.save(updated) }.getOrDefault(false)
                if (saved) mutable.update {
                    it.copy(policy = updated, policySaving = false, message = "规则已保存；可重新开启自动接单")
                }
                else mutable.update {
                    it.copy(policySaving = false, policySaveError = "规则保存失败，请检查输入", message = "规则保存失败")
                }
            }
        }
    }
    fun notification(platform: Platform, text: String) {
        // Notifications only trigger status information; incomplete background offers never authorize clicks.
        if (state.value.working) mutable.update { it.copy(message = "${platform.label} 有新通知，请查看订单页面") }
    }
}
