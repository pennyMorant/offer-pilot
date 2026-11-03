package com.overud.ui.diagnostics

import com.overud.model.NodeBounds
import com.overud.model.UiNodeSnapshot

data class NodeDiagnosticState(
    val busy: Boolean = false,
    val countdown: Int = 0,
    val message: String = "开始工作并连接 Shevery 后，可倒计时抓取平台页面",
    val nodes: List<UiNodeSnapshot> = emptyList(),
    val selectedRoot: Int = -1,
    val ocrBounds: NodeBounds? = null,
    val pageSummary: String? = null,
    val showText: Boolean = false,
    val selectedNode: Int? = null,
    val actionCandidates: List<Int> = emptyList(),
)
