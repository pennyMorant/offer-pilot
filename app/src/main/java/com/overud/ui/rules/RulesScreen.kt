@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import com.overud.domain.*
import com.overud.ui.components.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable fun RulesScreen(policy: Policy, onSave: (Policy) -> Unit, onOverlayPermission: () -> Unit, onNotificationPermission: () -> Unit, onLocationPermission: () -> Unit) {
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

private fun parseCents(value: String): Long? = runCatching {
    val number = value.toBigDecimal()
    require(number >= BigDecimal.ZERO && number <= BigDecimal("100000"))
    number.multiply(BigDecimal(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
}.getOrNull()
