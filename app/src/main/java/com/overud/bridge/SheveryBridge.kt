package com.overud.bridge

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import rikka.shizuku.Shizuku

/** Shevery uses the Shizuku provider protocol; authorization is not a ready capture service. */
class SheveryBridge(context: Context) {
    private val mutable = MutableStateFlow(BridgeStatus())
    val status = mutable.asStateFlow()
    private val eventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val windowEvents = eventFlow.asSharedFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var requested = false
    private var generation = 0L
    private var active: ServiceConnection? = null
    private var timeout: Job? = null
    @Volatile var service: IPrivilegedService? = null
        private set
    private val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, PrivilegedService::class.java.name))
        .daemon(false).processNameSuffix("capture").debuggable(false).version(7)
    private val received = Shizuku.OnBinderReceivedListener { scope.launch { refresh(); connectIfRequested() } }
    private val dead = Shizuku.OnBinderDeadListener {
        scope.launch { disconnect(); mutable.value = BridgeStatus(message = "Shevery 已断开，自动接单已关闭") }
    }
    private val permission = Shizuku.OnRequestPermissionResultListener { code, _ ->
        if (code == 100) scope.launch { refresh(); connectIfRequested() }
    }
    init {
        Shizuku.addBinderReceivedListenerSticky(received)
        Shizuku.addBinderDeadListener(dead)
        Shizuku.addRequestPermissionResultListener(permission)
    }

    fun refresh() {
        val alive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = alive && runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        if ((!alive || !granted) && active != null) disconnect()
        val ready = granted && service?.asBinder()?.isBinderAlive == true
        mutable.value = BridgeStatus(alive, granted, ready, when {
            !alive -> "未收到 Shevery Binder，请先启动 Shevery 服务"
            !granted -> "Shevery 服务在线，等待授权"
            ready -> "Shevery 采集服务已连接，页面读取可用"
            active != null -> "Shevery 已授权，正在连接并检查页面读取"
            else -> "Shevery 已授权，可开始工作"
        })
    }

    fun authorize() {
        refresh()
        if (mutable.value.connected && !mutable.value.authorized) {
            runCatching { Shizuku.requestPermission(100) }.onFailure {
                mutable.value = mutable.value.copy(message = "Shevery 授权请求失败：${it.message}")
            }
        } else connectIfRequested()
    }

    fun bind() {
        requested = true
        refresh()
        connectIfRequested()
    }

    private fun connectIfRequested() {
        if (!requested || !mutable.value.authorized || active != null) return
        val epoch = ++generation
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                scope.launch { probe(epoch, binder) }
            }
            override fun onServiceDisconnected(name: ComponentName) { lost(epoch, "采集服务已断开，自动接单已关闭") }
            override fun onBindingDied(name: ComponentName) { lost(epoch, "采集绑定已失效，请重新开始工作") }
            override fun onNullBinding(name: ComponentName) { lost(epoch, "Shevery 返回空采集服务，请重新开始工作") }
        }
        active = connection
        mutable.value = mutable.value.copy(serviceReady = false, message = "正在连接 Shevery 采集服务")
        timeout = scope.launch {
            delay(12_000)
            lost(epoch, "Shevery 采集连接超时，请检查授权后重新开始工作")
        }
        runCatching { Shizuku.bindUserService(args, connection) }
            .onFailure { lost(epoch, "采集连接失败：${it.message}") }
    }

    private suspend fun probe(epoch: Long, binder: IBinder) {
        if (!requested || epoch != generation) return
        val remote = IPrivilegedService.Stub.asInterface(binder)
        val result = withContext(Dispatchers.IO) {
            runCatching {
                check(binder.isBinderAlive) { "采集 Binder 已失效" }
                val frame = remote.snapshot()
                check(!frame.containsKey("error")) { frame.getString("error").orEmpty() }
                remote.registerWindowEvents(object : IWindowEvents.Stub() {
                    override fun onWindowChanged(sequence: Long) {
                        if (service?.asBinder() == binder) eventFlow.tryEmit(Unit)
                    }
                })
            }
        }
        if (!requested || epoch != generation) return
        result.onSuccess {
            timeout?.cancel()
            service = remote
            refresh()
            eventFlow.tryEmit(Unit)
        }.onFailure { lost(epoch, "Shevery 已绑定，但页面读取失败：${it.message}") }
    }

    private fun lost(epoch: Long, message: String) {
        scope.launch {
            if (epoch != generation) return@launch
            disconnect()
            mutable.value = mutable.value.copy(serviceReady = false, message = message)
        }
    }

    private fun disconnect() {
        generation++
        timeout?.cancel()
        timeout = null
        service = null
        val old = active
        active = null
        if (old != null) runCatching { Shizuku.unbindUserService(args, old, true) }
    }

    fun unbind() {
        requested = false
        disconnect()
        refresh()
    }
}
