package com.overud

import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.overud.bridge.SheveryBridge
import com.overud.data.*
import com.overud.model.*
import com.overud.platform.*
import com.overud.valuation.*
import com.overud.acceptance.*
import com.overud.region.RegionCell
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

data class RuntimeState(
    val working: Boolean = false,
    val automatic: Boolean = false,
    val policy: Policy = Policy(),
    val latest: Offer? = null,
    val decision: Decision? = null,
    val records: List<OfferRecord> = emptyList(),
    val message: String = "开始工作后，将自动分析新报价",
    val pending: Boolean = false,
    val lastCaptureAt: Long? = null,
    val regions: List<RegionSummary> = emptyList(),
    val currentRegion: String? = null,
)

class OverudRuntime(context: Context, private val database: OverudDatabase) {
    val bridge = SheveryBridge(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = PolicyStore(context)
    private val mutable = MutableStateFlow(RuntimeState(policy = store.read()))
    val state = mutable.asStateFlow()
    private val mutex = Mutex()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var lastOcrAt = 0L
    private var pendingId: String? = null
    private var pendingPlatform: Platform? = null
    @Volatile private var lastLocation: Location? = null
    private var exposureKey: String? = null
    private var exposureAt = 0L
    private var lastCanonical: String? = null
    private var lastOfferAt = 0L
    private var currentOfferId: String? = null
    private var regionsRefreshedAt = 0L
    private var gate = AcceptanceGate()

    init {
        scope.launch {
            refreshRecords()
            refreshRegions()
            mutex.withLock {
                val unresolved = database.offers().unresolvedCount() > 0
                if (unresolved) gate = AcceptanceGate(AcceptanceState.OUTCOME_UNKNOWN)
                mutable.update { it.copy(pending = unresolved, message = if (unresolved) "有接单结果待核对，请先确认配送状态" else it.message) }
            }
        }
        scope.launch {
            bridge.status.collect { status ->
                if (!status.serviceReady) mutable.update { it.copy(automatic = false) }
            }
        }
    }
    fun startWork() {
        bridge.bind()
        mutable.update { it.copy(working = true, automatic = false, message = "正在监听订单页面") }
    }
    fun stopWork() {
        mutable.update { it.copy(working = false, automatic = false, message = "工作已结束") }
        bridge.unbind()
        lastLocation = null
    }
    fun location(location: Location) {
        if (!state.value.working) return
        lastLocation = Location(location)
        mutable.update { it.copy(currentRegion = usableRegion()) }
    }
    private fun usableRegion(): String? {
        val location = lastLocation ?: return null
        if (System.currentTimeMillis() - location.time !in 0..120_000 || !location.hasAccuracy() || location.accuracy > 300) return null
        return RegionCell.at(location.latitude, location.longitude).key
    }
    fun setAutomatic(enabled: Boolean) {
        mutable.update {
            val allowed = enabled && it.working && it.policy.valid() && bridge.status.value.serviceReady && !it.pending
            it.copy(automatic = allowed, message = when {
                !enabled -> "自动接单已关闭"
                allowed -> "自动接单运行中，所有已配置要求必须同时满足"
                else -> "请先连接 Shevery、开始工作并核对待处理订单"
            })
        }
    }
    fun savePolicy(policy: Policy) {
        // Immediately invalidate pending decisions while persistence completes.
        mutable.update { it.copy(automatic = false) }
        scope.launch {
            mutex.withLock {
                val updated = policy.copy(version = state.value.policy.version + 1)
                if (store.save(updated)) mutable.update { it.copy(policy = updated, message = "规则已保存；可重新开启自动接单") }
                else mutable.update { it.copy(message = "规则保存失败，请检查输入") }
            }
        }
    }
    fun resolvePending() {
        scope.launch {
            mutex.withLock {
                database.offers().resolve()
                gate.userResolved()
                pendingId = null; pendingPlatform = null
                mutable.update { it.copy(pending = false, automatic = false, message = "已确认当前无进行中的配送；可重新开启自动接单") }
            }
        }
    }
    suspend fun poll() = mutex.withLock {
        if (!state.value.working) return@withLock
        val service = bridge.service ?: return@withLock
        try {
            val snapshot = service.snapshot()
            snapshot.getString("error")?.let { error ->
                mutable.update { it.copy(automatic = false, message = "采集不可用：$error") }
                return@withLock
            }
            val platform = Platform.fromPackage(snapshot.getString("package").orEmpty()) ?: return@withLock
            val text = snapshot.getString("text").orEmpty()
            val now = System.currentTimeMillis()
            trackExposure(platform, now)
            mutable.update { it.copy(lastCaptureAt = now) }
            if (pendingId != null && pendingPlatform == platform &&
                Regex("order accepted|delivery accepted|订单已接受|接单成功", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                database.offers().updateAttempt(pendingId!!, "accepted", "平台界面显示明确接单成功文本")
                if (gate.state != AcceptanceState.ACCEPTED) gate.confirmed()
                mutable.update { it.copy(message = "平台已确认接单；配送完成后再恢复自动接单") }
            }
            var offer = OfferParser.parse(platform, text, now)
            if ((offer == null || !offer.reliable) && now - lastOcrAt > 4000) {
                lastOcrAt = now
                val ocr = recognize(platform.packageName)
                if (ocr != null) {
                    val parsed = OfferParser.parse(platform, ocr, now)
                    // OCR-only observations inform the driver but do not authorize an action.
                    if (offer == null || (offer.amountCents == null && parsed?.amountCents != null)) offer = parsed?.copy(reliable = false)
                }
            }
            if (offer != null) {
                val canonical = offer.id
                if (canonical != lastCanonical || now - lastOfferAt > 10_000 || currentOfferId == null) currentOfferId = "$canonical-$now"
                lastCanonical = canonical; lastOfferAt = now
                offer = offer.copy(id = currentOfferId!!)
                val decision = EligibilityEvaluator.assess(offer, state.value.policy, now)
                record(offer, decision)
                mutable.update { it.copy(latest = offer, decision = decision) }
                maybeAccept(offer, decision, snapshot.getString("token").orEmpty())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            mutable.update { it.copy(automatic = false, message = "采集已暂停：${e.message ?: "服务连接异常"}") }
        }
    }
    private suspend fun recognize(pkg: String): String? {
        val descriptor = bridge.service?.screenshot(pkg) ?: return null
        val bitmap = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { BitmapFactory.decodeStream(it) } ?: return null
        return try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it.text) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
            }
        } finally { bitmap.recycle() }
    }
    private fun maybeAccept(offer: Offer, decision: Decision, token: String) {
        val before = state.value
        if (!before.automatic || !before.working || before.pending || !decision.eligible || token.isBlank() ||
            token != fingerprint(offer.platform, offer.rawText)) return
        if (database.offers().unresolvedCount() > 0) {
            mutable.update { it.copy(pending = true, automatic = false, message = "接单记录待核对") }
            return
        }
        if (!gate.reserve(offer, decision, before.automatic, before.working)) return
        val attempt = AttemptRecord().apply {
            offerId = offer.id; platform = offer.platform.name; startedAt = System.currentTimeMillis(); policyVersion = decision.policyVersion
        }
        if (database.offers().reserve(attempt) == -1L) { gate.releaseBeforeDispatch(); return }
        try {
            val current = state.value
            val rechecked = EligibilityEvaluator.assess(offer, current.policy, System.currentTimeMillis())
            if (!current.automatic || !current.working || current.policy.version != decision.policyVersion || !rechecked.eligible) {
                database.offers().updateAttempt(offer.id, "aborted", "开关或规则已变化")
                gate.releaseBeforeDispatch()
                return
            }
            // Persist before IPC. A process failure can never cause a blind retry.
            mutable.update { it.copy(pending = true) }
            gate.dispatched()
            val result = bridge.service?.accept(token, offer.platform.packageName) ?: "service_disconnected"
            if (result == "dispatched") {
                pendingId = offer.id; pendingPlatform = offer.platform
                database.offers().updateAttempt(offer.id, "outcome_unknown", "点击已发送，等待平台确认")
                mutable.update { it.copy(automatic = false, pending = true, message = "接单动作已发出，正在核对平台结果") }
            } else {
                database.offers().updateAttempt(offer.id, "aborted", result)
                gate.notDispatched()
                mutable.update { it.copy(pending = false, message = "本次未执行接单：$result") }
            }
        } catch (e: Exception) {
            database.offers().updateAttempt(offer.id, "outcome_unknown", "执行中断：${e.javaClass.simpleName}")
            mutable.update { it.copy(automatic = false, pending = true, message = "接单结果待核对，请检查平台") }
        }
    }
    fun notification(platform: Platform, text: String) {
        // Notifications only trigger status information; incomplete background offers never authorize clicks.
        if (state.value.working) mutable.update { it.copy(message = "${platform.label} 有新通知，请查看订单页面") }
    }
    private fun record(offer: Offer, decision: Decision) {
        val previous = database.offers().find(offer.id)
        database.offers().save(OfferRecord().apply {
            id = offer.id; platform = offer.platform.name; observedAt = previous?.observedAt ?: offer.observedAt
            amountCents = offer.amountCents; distanceMeters = offer.distanceMeters; durationSeconds = offer.durationSeconds
            destination = offer.destination; eligible = decision.eligible
            originRegion = previous?.originRegion ?: usableRegion()
            explanation = decision.reasons.joinToString("；").ifEmpty { "满足所有接单要求" }; policyVersion = decision.policyVersion
        })
        refreshRecords()
    }
    private fun refreshRecords() { mutable.update { it.copy(records = database.offers().recent()) } }
    private fun refreshRegions() {
        val since = (System.currentTimeMillis() - 28L * 24 * 3600 * 1000) / 3_600_000 * 3_600_000
        mutable.update { it.copy(regions = database.offers().regions(since)) }
    }
    private fun trackExposure(platform: Platform, now: Long) {
        val region = usableRegion()
        val key = if (region != null && !state.value.pending) "$region:${platform.name}" else null
        val elapsed = now - exposureAt
        if (key != null && key == exposureKey && elapsed in 1..5000) {
            val bucket = now / 3_600_000 * 3_600_000
            val id = "$key:$bucket"
            val local = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault())
            database.offers().ensureExposure(ExposureRecord().apply {
                this.id = id; regionKey = region!!; this.platform = platform.name; bucketStart = bucket
                localHour = local.hour; localWeekday = local.dayOfWeek.value
            })
            database.offers().addExposure(id, elapsed)
        }
        exposureAt = now; exposureKey = key
        if (now - regionsRefreshedAt > 15_000) { refreshRegions(); regionsRefreshedAt = now }
    }
}
