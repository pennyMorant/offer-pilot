package io.github.pennymorant.offerpilot.bridge

import android.os.Bundle
import java.util.concurrent.atomic.AtomicBoolean

class NodeDiagnosticReader(private val bridge: SheveryBridge) {
    private val busy = AtomicBoolean(false)
    fun read(): Bundle {
        check(busy.compareAndSet(false, true)) { "上次诊断调用仍未返回，请结束工作后重试" }
        try {
            val service = bridge.service ?: error("Shevery 尚未连接")
            val result = service.diagnosticSnapshot()
            check(bridge.service?.asBinder() == service.asBinder()) { "采集连接已变化，请重新抓取" }
            return result
        } finally { busy.set(false) }
    }
}
