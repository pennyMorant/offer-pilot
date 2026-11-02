package com.overud.capture

import kotlinx.coroutines.*

class CaptureScheduler(private val pipeline: CapturePipeline) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                pipeline.poll()
                delay(900)
            }
        }
    }

    fun close() { scope.cancel() }
}
