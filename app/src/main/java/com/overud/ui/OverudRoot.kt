@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overud.OverudRuntime
import com.overud.RuntimeState
import com.overud.bridge.BridgeStatus
import com.overud.data.OfferRecord
import com.overud.data.RegionSummary
import com.overud.domain.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun OverudRoot(runtime: OverudRuntime, onWork: () -> Unit, onOverlayPermission: () -> Unit, onNotificationPermission: () -> Unit, onLocationPermission: () -> Unit) {
    val state by runtime.state.collectAsStateWithLifecycle()
    val bridge by runtime.bridge.status.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var replay by rememberSaveable { mutableStateOf(false) }
    var resolve by remember { mutableStateOf(false) }
    val pages = listOf("工作台", "订单", "区域", "规则")
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                pages.forEachIndexed { index, label ->
                    NavigationBarItem(selected = selected == index, onClick = { selected = index },
                        icon = { Text(listOf("◉", "≡", "⌖", "☷")[index], style = MaterialTheme.typography.titleLarge) },
                        label = { Text(label) })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            AnimatedContent(selected, label = "navigation") { page ->
                when (page) {
                    0 -> Dashboard(state, bridge, onWork, runtime::setAutomatic, runtime.bridge::authorize,
                        { selected = 3 }, { replay = true }, { resolve = true })
                    1 -> History(state.records)
                    2 -> Regions(state.records, state.regions)
                    else -> Rules(state.policy, runtime::savePolicy, onOverlayPermission, onNotificationPermission, onLocationPermission)
                }
            }
        }
    }
    if (replay) ReplayDialog(state.policy) { replay = false }
    if (resolve) AlertDialog(onDismissRequest = { resolve = false }, title = { Text("核对当前配送状态") },
        text = { Text("请先查看 Uber 和 DoorDash，确认当前没有正在进行的配送，再清除订单占用状态。此操作不会取消平台订单。") },
        confirmButton = { TextButton(onClick = { runtime.resolvePending(); resolve = false }) { Text("确认当前无配送") } },
        dismissButton = { TextButton(onClick = { resolve = false }) { Text("继续暂停") } })
}

@Composable private fun PageTitle(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun Dashboard(state: RuntimeState, bridge: BridgeStatus, onWork: () -> Unit,
    onAutomatic: (Boolean) -> Unit, onAuthorize: () -> Unit, onRules: () -> Unit, onReplay: () -> Unit, onResolve: () -> Unit) {
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("overud", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            StatusPill(if (bridge.serviceReady) "已连接" else "待连接", bridge.serviceReady)
        } }
        item { PageTitle("DRIVE WITH INTENTION", "每一程，都值得。", "看清订单价值，让接单更有把握。") }
        item {
            val bg by animateColorAsState(if (state.working) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, label = "work")
            Card(colors = CardDefaults.cardColors(containerColor = bg), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (state.working) "工作进行中" else "准备好出发了吗？", style = MaterialTheme.typography.titleLarge)
                        if (state.working) LoadingIndicator(Modifier.size(34.dp))
                    }
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onWork, enabled = state.working || bridge.authorized,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (state.working) "结束工作" else "开始工作") }
                }
            }
        }
        if (!bridge.serviceReady) item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("连接 Shevery", style = MaterialTheme.typography.titleMedium)
                    Text(bridge.message, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onAuthorize) { Text("检查连接并授权") }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("自动接单", style = MaterialTheme.typography.titleLarge)
                            Text(if (state.automatic) "符合全部要求时直接接单" else "开启后按自定义要求执行", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = state.automatic, onCheckedChange = onAutomatic,
                            enabled = state.automatic || (state.working && bridge.serviceReady && !state.pending))
                    }
                    HorizontalDivider()
                    Text("≥ ${money(state.policy.minimumCents)}  ·  ≥ ${money(state.policy.minimumCentsPerMile)}/mi  ·  ≥ ${money(state.policy.minimumCentsPerHour)}/h",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onRules) { Text("调整接单要求 →") }
                    if (state.pending) FilledTonalButton(onClick = onResolve) { Text("核对接单结果 / 配送状态") }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("最新报价", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onReplay) { Text("规则预览") }
        } }
        item {
            state.latest?.let { offer -> OfferCard(offer, state.decision) }
                ?: EmptyCard("等待第一张报价", "开始工作后，在 Uber 或 DoorDash 打开订单报价，分析结果会出现在这里。")
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("已记录报价", state.records.size.toString(), Modifier.weight(1f))
            Metric("符合当前记录规则", state.records.count { it.eligible }.toString(), Modifier.weight(1f))
        } }
    }
}

@Composable private fun StatusPill(text: String, active: Boolean) {
    Surface(color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun OfferCard(offer: Offer, decision: Decision?) {
    val locale = LocalConfiguration.current.locales[0]
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(offer.platform.label, style = MaterialTheme.typography.labelLarge)
                Text(SimpleDateFormat("HH:mm", locale).format(Date(offer.observedAt)), style = MaterialTheme.typography.labelMedium)
            }
            Text(offer.amountCents?.let(::money) ?: "金额待识别", style = MaterialTheme.typography.displayLarge)
            Text("${offer.distanceMeters?.let { "%.1f mi".format(it / ValueEngine.METERS_PER_MILE) } ?: "里程未知"}  ·  ${offer.durationSeconds?.let { "${it / 60} min" } ?: "耗时未知"}",
                style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column { Text("每英里报价", style = MaterialTheme.typography.labelMedium); Text(decision?.valuation?.centsPerMile?.let { money(it.toLong()) } ?: "—", style = MaterialTheme.typography.titleLarge) }
                Column { Text("预计毛时薪", style = MaterialTheme.typography.labelMedium); Text(decision?.valuation?.grossCentsPerHour?.let { money(it.toLong()) } ?: "—", style = MaterialTheme.typography.titleLarge) }
            }
            Text(decision?.reasons?.joinToString("；")?.ifEmpty { "满足所有接单要求" } ?: "等待分析", color = MaterialTheme.colorScheme.primary)
            Text("目的地接单机会：证据不足", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.headlineLarge)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun EmptyCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun History(records: List<OfferRecord>) {
    val locale = LocalConfiguration.current.locales[0]
    var detail by remember { mutableStateOf<OfferRecord?>(null) }
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("YOUR ORDER JOURNAL", "每一单，有迹可循。", "记录收到的报价，区分报价与实际收入。") }
        if (records.isEmpty()) item { EmptyCard("还没有订单记录", "订单识别后会保存在本机，未接的报价也会参与价值统计。") }
        items(records, key = { it.id }) { record ->
            Card(onClick = { detail = record }) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(Platform.valueOf(record.platform).label, style = MaterialTheme.typography.titleMedium)
                        Text(SimpleDateFormat("MM/dd HH:mm", locale).format(Date(record.observedAt)), style = MaterialTheme.typography.labelMedium)
                        Text(if (record.eligible) "符合当时规则" else "未满足接单要求", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(record.amountCents?.let(::money) ?: "—", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
    detail?.let { record -> ModalBottomSheet(onDismissRequest = { detail = null }) {
        Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("报价分析", style = MaterialTheme.typography.headlineLarge)
            Text(record.amountCents?.let(::money) ?: "金额未知", style = MaterialTheme.typography.displayLarge)
            Text(record.explanation)
            Text("目的地：${record.destination ?: "未识别"}")
            Text("规则版本 ${record.policyVersion} · 此金额为报价，尚非结算收入", style = MaterialTheme.typography.bodySmall)
        }
    } }
}

@Composable private fun Regions(records: List<OfferRecord>, regions: List<RegionSummary>) {
    val destinations = records.filter { !it.destination.isNullOrBlank() }.groupBy { it.destination!! }
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("KNOW YOUR NEXT STOP", "好区域，慢慢发现。", "从目的地报价记录开始，积累你的区域经验。") }
        item { EmptyCard("区域经验 · 最近 28 天", "按位置网格记录报价和采集覆盖时长。覆盖时长仅包含目标平台在前台、定位精度足够且无待核对接单的时间，是可接单时长的代理。") }
        items(regions, key = { "${it.regionKey}:${it.platform}" }) { region ->
            Card { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("网格 ${region.regionKey}", style = MaterialTheme.typography.titleLarge)
                Text("${Platform.valueOf(region.platform).label} · ${region.offerCount} 张报价")
                Text("${region.eligibleCount} 张符合当时规则 · 覆盖 ${region.exposureMs / 60000} 分钟")
                Text(if (region.exposureMs >= 3_600_000 && region.offerCount >= 10) "观测报价 ${"%.1f".format(region.offerCount * 3_600_000.0 / region.exposureMs)} 张/覆盖小时" else "样本不足，暂不推断接单机会", style = MaterialTheme.typography.bodySmall)
            } }
        }
        if (destinations.isEmpty()) item { EmptyCard("目的地记录不足", "有明确目的地的报价会汇总在这里，缺失地址不会被推断成精确位置。") }
        destinations.forEach { (destination, offers) -> item {
            Card { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(destination, style = MaterialTheme.typography.titleLarge)
                Text("${offers.size} 张报价 · ${offers.count { it.eligible }} 张符合当时规则")
                Text("个人报价样本 · 尚无等待时间统计", style = MaterialTheme.typography.bodySmall)
            } }
        } }
    }
}

@Composable private fun Rules(policy: Policy, onSave: (Policy) -> Unit, onOverlayPermission: () -> Unit, onNotificationPermission: () -> Unit, onLocationPermission: () -> Unit) {
    var minimum by rememberSaveable(policy.version) { mutableStateOf(decimal(policy.minimumCents)) }
    var mile by rememberSaveable(policy.version) { mutableStateOf(decimal(policy.minimumCentsPerMile)) }
    var hour by rememberSaveable(policy.version) { mutableStateOf(decimal(policy.minimumCentsPerHour)) }
    var distance by rememberSaveable(policy.version) { mutableStateOf("%.1f".format(Locale.US, policy.maximumMeters / ValueEngine.METERS_PER_MILE)) }
    var time by rememberSaveable(policy.version) { mutableStateOf((policy.maximumSeconds / 60).toString()) }
    var pickup by rememberSaveable(policy.version) { mutableStateOf(policy.maximumPickupMeters?.let { "%.1f".format(Locale.US, it / ValueEngine.METERS_PER_MILE) }.orEmpty()) }
    var cost by rememberSaveable(policy.version) { mutableStateOf(decimal(policy.costCentsPerMile)) }
    var excluded by rememberSaveable(policy.version) { mutableStateOf(policy.excludedDestinations.joinToString(", ")) }
    var platforms by remember(policy.version) { mutableStateOf(policy.enabledPlatforms) }
    var stack by rememberSaveable(policy.version) { mutableStateOf(policy.allowStacked) }
    var addon by rememberSaveable(policy.version) { mutableStateOf(policy.allowAddon) }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("YOUR DRIVE, YOUR RULES", "接什么单，你来定。", "所有要求同时满足才自动接单。保存后重新开启自动接单。") }
        item { SectionLabel("接单平台") }
        items(Platform.entries) { platform -> ToggleRow(platform.label, "使用以下接单要求", platform in platforms) {
            platforms = if (it) platforms + platform else platforms - platform
        } }
        item { SectionLabel("最低收益 · USD") }
        item { NumberField("最低报价", minimum, "$", { minimum = it }) }
        item { NumberField("最低每英里报价", mile, "$/mi", { mile = it }) }
        item { NumberField("最低预计毛时薪", hour, "$/h", { hour = it }) }
        item { SectionLabel("距离与时间") }
        item { NumberField("最大总里程", distance, "mi", { distance = it }) }
        item { NumberField("最长订单耗时", time, "min", { time = it }) }
        item { NumberField("最大取餐距离（留空不限制）", pickup, "mi", { pickup = it }) }
        item { NumberField("车辆成本（用于贡献估计）", cost, "$/mi", { cost = it }) }
        item { SectionLabel("额外偏好") }
        item { ToggleRow("允许叠单", "按整组报价与路线评估", stack) { stack = it } }
        item { ToggleRow("允许追加单", "仅在新增收入及增量路线完整时执行", addon) { addon = it } }
        item { OutlinedTextField(value = excluded, onValueChange = { excluded = it }, label = { Text("排除目的地关键词，用逗号分隔") }, modifier = Modifier.fillMaxWidth()) }
        item {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                val cents = listOf(minimum, mile, hour, cost).map(::parseCents)
                val meters = distance.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it <= 1000 }?.times(ValueEngine.METERS_PER_MILE)?.toLong()
                val seconds = time.toLongOrNull()?.takeIf { it in 1..1440 }?.times(60)
                val pickupMeters = pickup.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it <= 1000 }?.times(ValueEngine.METERS_PER_MILE)?.toLong()
                if (cents.any { it == null } || meters == null || seconds == null || (pickup.isNotBlank() && pickupMeters == null)) error = "请输入有效的金额、距离和时间"
                else {
                    val updated = policy.copy(enabledPlatforms = platforms, minimumCents = cents[0]!!, minimumCentsPerMile = cents[1]!!,
                        minimumCentsPerHour = cents[2]!!, maximumMeters = meters, maximumSeconds = seconds,
                        maximumPickupMeters = pickupMeters, costCentsPerMile = cents[3]!!, allowStacked = stack, allowAddon = addon,
                        excludedDestinations = excluded.split(',', '，').map(String::trim).filter(String::isNotBlank).toSet())
                    if (updated.valid()) { error = null; onSave(updated) } else error = "至少启用一个平台，收益要求必须大于零"
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("保存接单要求") }
        }
        item { SectionLabel("设备权限") }
        item { FilledTonalButton(onClick = onOverlayPermission, modifier = Modifier.fillMaxWidth()) { Text("设置悬浮窗权限") } }
        item { OutlinedButton(onClick = onNotificationPermission, modifier = Modifier.fillMaxWidth()) { Text("设置通知读取权限") } }
        item { OutlinedButton(onClick = onLocationPermission, modifier = Modifier.fillMaxWidth()) { Text("授权位置记录（下次开始工作生效）") } }
        item { Text("Overud 0.1.0 · Material 3 Expressive", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun SectionLabel(text: String) { Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
@Composable private fun NumberField(label: String, value: String, unit: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, suffix = { Text(unit) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
}
@Composable private fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodySmall) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable private fun ReplayDialog(policy: Policy, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("Accept\n$12.00\nTotal 5 mi 25 min\nMerchant: Restaurant\nDestination: Downtown") }
    var platform by remember { mutableStateOf(Platform.UBER_EATS) }
    val offer = remember(text, platform) { OfferParser.parse(platform, text, System.currentTimeMillis()) }
    val decision = offer?.let { ValueEngine.assess(it, policy, it.observedAt) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("规则预览") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("仅分析输入文本，不会接单，也不会写入真实订单记录。")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Platform.entries.forEach { item -> FilterChip(selected = platform == item, onClick = { platform = item }, label = { Text(item.label) }) } }
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("报价文本") }, minLines = 4, maxLines = 8)
            Text(decision?.reasons?.joinToString("；")?.ifEmpty { "符合当前全部接单要求" } ?: "无法识别报价，请包含接单按钮文字和金额")
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } })
}

private fun money(cents: Long) = "$${"%.2f".format(Locale.US, cents / 100.0)}"
private fun decimal(cents: Long) = "%.2f".format(Locale.US, cents / 100.0)
private fun parseCents(value: String): Long? = runCatching {
    val number = value.toBigDecimal()
    require(number >= BigDecimal.ZERO && number <= BigDecimal("100000"))
    number.multiply(BigDecimal(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
}.getOrNull()
