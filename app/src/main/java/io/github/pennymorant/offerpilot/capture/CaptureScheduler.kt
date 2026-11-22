package io.github.pennymorant.offerpilot.capture

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import android.os.SystemClock

class CaptureScheduler(private val pipeline: CapturePipeline, private val events: Flow<Unit>) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val wake = Channel<Unit>(Channel.CONFLATED)

    fun start() {
        if (job != null) return
        scope.launch { events.collect { wake.trySend(Unit) } }
        job = scope.launch {
            while (isActive) {
                val started = SystemClock.elapsedRealtime()
                pipeline.poll()
                delay((250 - (SystemClock.elapsedRealtime() - started)).coerceAtLeast(0))
                withTimeoutOrNull(1500) { wake.receive() }
            }
        }
    }

    fun close() { scope.cancel(); wake.close() }
}
