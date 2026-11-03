package com.overud.bridge

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku

class SheveryBridge(context: Context) {
    private val mutable = MutableStateFlow(BridgeStatus())
    val status = mutable.asStateFlow()
    private val eventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val windowEvents = eventFlow.asSharedFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile var service: IPrivilegedService? = null
        private set
    private val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, PrivilegedService::class.java.name))
        .daemon(false).processNameSuffix("capture").debuggable(false).version(5)
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IPrivilegedService.Stub.asInterface(binder)
            mutable.value = BridgeStatus(true, true, true, "Shevery 采集服务已连接")
            val connected = service
            scope.launch {
                runCatching { connected?.registerWindowEvents(object : IWindowEvents.Stub() {
                    override fun onWindowChanged(sequence: Long) {
                        if (service?.asBinder() == binder) eventFlow.tryEmit(Unit)
                    }
                }) }
                if (service?.asBinder() == binder) eventFlow.tryEmit(Unit)
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            mutable.value = mutable.value.copy(serviceReady = false, message = "采集服务已断开")
        }
    }
    private val received = Shizuku.OnBinderReceivedListener { refresh() }
    private val dead = Shizuku.OnBinderDeadListener { service = null; mutable.value = BridgeStatus(message = "Shevery 已断开，自动接单暂停") }
    private val permission = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }
    init {
        Shizuku.addBinderReceivedListenerSticky(received)
        Shizuku.addBinderDeadListener(dead)
        Shizuku.addRequestPermissionResultListener(permission)
    }
    fun refresh() {
        val alive = Shizuku.pingBinder()
        val granted = alive && runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        mutable.value = BridgeStatus(alive, granted, service != null, when {
            !alive -> "请先启动 Shevery"
            !granted -> "等待授予 Shevery 权限"
            service != null -> "Shevery 采集服务已连接"
            else -> "Shevery 已授权，可开始工作"
        })
    }
    fun authorize() {
        refresh()
        if (mutable.value.connected && !mutable.value.authorized) Shizuku.requestPermission(100)
    }
    fun bind() {
        refresh()
        if (mutable.value.authorized && service == null) runCatching { Shizuku.bindUserService(args, connection) }
            .onFailure { mutable.value = mutable.value.copy(message = "采集连接失败：${it.message}") }
    }
    fun unbind() {
        runCatching { Shizuku.unbindUserService(args, connection, true) }
        service = null
        refresh()
    }
}
