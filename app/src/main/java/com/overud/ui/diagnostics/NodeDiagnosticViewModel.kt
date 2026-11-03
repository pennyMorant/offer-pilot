package com.overud.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.bridge.NodeDiagnosticReader
import com.overud.bridge.WindowDiagnosticCodec
import com.overud.model.NodeBounds
import com.overud.model.Platform
import com.overud.platform.OfferParser
import com.overud.ui.components.platformPagePresentation
import com.overud.work.WorkSessionController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NodeDiagnosticViewModel(
    private val session: WorkSessionController,
    private val reader: NodeDiagnosticReader,
) : ViewModel() {
    private val mutable = MutableStateFlow(NodeDiagnosticState())
    val state = mutable.asStateFlow()
    private var generation = 0L
    private var capture: Job? = null
    private var opened = false

    fun open() { opened = true; session.diagnosticMode(true) }
    fun close() {
        opened = false
        generation++
        capture?.cancel()
        mutable.value = NodeDiagnosticState()
        session.diagnosticMode(false)
    }
    fun showText(value: Boolean) { mutable.update { it.copy(showText = value) } }
    fun select(index: Int) { mutable.update { it.copy(selectedNode = index) } }

    fun captureAfterDelay() {
        if (!opened || state.value.busy) return
        if (!session.state.value.working) {
            mutable.update { it.copy(message = "请先关闭诊断、开始工作并连接 Shevery，再打开诊断") }
            return
        }
        val request = ++generation
        val work = session.workGeneration
        mutable.value = NodeDiagnosticState(busy = true)
        capture = viewModelScope.launch {
            var timeout: Job? = null
            try {
                for (remaining in 5 downTo 1) {
                    mutable.update { it.copy(countdown = remaining, message = "请切换到 Uber 或 DoorDash 订单页面") }
                    delay(1000)
                }
                check(session.state.value.working && session.workGeneration == work && session.canInteract()) {
                    "工作已结束或设备已锁定，请重新抓取"
                }
                mutable.update { it.copy(countdown = 0, message = "正在读取节点，可稍后返回查看") }
                timeout = viewModelScope.launch {
                    delay(5000)
                    if (request == generation) {
                        generation++
                        mutable.value = NodeDiagnosticState(message = "诊断读取超时，已忽略迟到结果；可关闭诊断")
                    }
                }
                val snapshot = withContext(Dispatchers.IO) { reader.read() }
                if (request != generation || !opened) return@launch
                check(session.state.value.working && session.workGeneration == work && session.canInteract()) {
                    "设备或工作状态已变化，请重新抓取"
                }
                check(!snapshot.containsKey("error")) { "节点读取失败：${snapshot.getString("error")}" }
                val platform = Platform.fromPackage(snapshot.getString("package").orEmpty())
                    ?: error("未抓到支持的平台，请在倒计时内切换到 Uber 或 DoorDash")
                val nodes = WindowDiagnosticCodec.decode(snapshot)
                check(nodes.isNotEmpty()) { "当前页面没有可用节点" }
                val bounds = snapshot.getIntArray("ocrBounds")?.takeIf { it.size == 4 }
                    ?.let { NodeBounds(it[0], it[1], it[2], it[3]) }
                mutable.value = NodeDiagnosticState(nodes = nodes, selectedRoot = snapshot.getInt("selectedRoot", -1),
                    ocrBounds = bounds, message = "已抓取 ${nodes.size} 个节点；仅保留本次内存快照",
                    pageSummary = platformPagePresentation(OfferParser.classify(platform, snapshot.getString("text").orEmpty())))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == generation) mutable.value = NodeDiagnosticState(message = e.message ?: "诊断失败")
            } finally { timeout?.cancel() }
        }
    }

    override fun onCleared() { if (opened) close() }
}
