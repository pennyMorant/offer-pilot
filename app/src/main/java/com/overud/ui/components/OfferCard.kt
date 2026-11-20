@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.overud.model.*
import com.overud.valuation.StrategyAdvisor
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToLong

@Composable
fun OfferCard(
    offer: Offer,
    decision: Decision?,
    destination: DestinationAssessment,
    uberArPercent: Int? = null,
    customPreferredZones: Set<String> = emptySet(),
) {
    var expanded by rememberSaveable(offer.id) { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val advice = StrategyAdvisor.advise(offer, decision, destination, uberArPercent, customPreferredZones)
    val valuation = decision?.valuation
    val rating = valueRatingPresentation(valuation)
    val eligible = decision?.eligible == true

    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ToolBadge(offer.platform.label, MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer)
                    ToolBadge(offer.serviceKind.label)
                }
                Text(SimpleDateFormat("HH:mm:ss", locale).format(Date(offer.observedAt)),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (advice.tags.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    advice.tags.take(2).forEach { tag ->
                        ToolBadge(
                            text = tag,
                            containerColor = if (advice.fakePreferred) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = if (advice.fakePreferred) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(offerAmountPresentation(offer), style = MaterialTheme.typography.displayMedium)
                val score = valuation?.score
                Surface(
                    color = when {
                        score == null -> MaterialTheme.colorScheme.surfaceContainerHigh
                        score >= 70 -> MaterialTheme.colorScheme.primaryContainer
                        score >= 50 -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.End) {
                        Text(score?.let { "${it}分" } ?: "—", style = MaterialTheme.typography.headlineSmall)
                        Text(rating.compact.substringAfter("·").trim().ifBlank { "评分" },
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolMetricTile("每英里", valuation?.centsPerMile?.let { money(it.roundToLong()) } ?: "—",
                    Modifier.weight(1f), highlight = (valuation?.centsPerMile ?: 0.0) >= 200.0)
                ToolMetricTile("贡献时薪", valuation?.contributionCentsPerHour?.let { money(it.roundToLong()) }
                    ?: valuation?.grossCentsPerHour?.let { money(it.roundToLong()) } ?: "—", Modifier.weight(1f))
                ToolMetricTile("路线", offerRoutePresentation(offer), Modifier.weight(1.2f))
            }
            advice.cycleCentsPerHour?.let { cycle ->
                Text(
                    "含目的地历史等单（~${advice.waitingMinutes ?: 0}分）周期时薪参考：${money(cycle.roundToLong())}/hr",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Surface(
                color = if (eligible) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (eligible) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(decision?.reasons?.firstOrNull() ?: if (eligible) "全部条件通过" else "等待分析",
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text(if (expanded) "收起" else "详情", style = MaterialTheme.typography.labelLarge)
                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    offer.merchant?.let { Text("商家：$it", style = MaterialTheme.typography.bodySmall) }
                    offer.pickupAddress?.let { Text("取件：$it", style = MaterialTheme.typography.bodySmall) }
                    offer.destination?.let { Text("送达：$it", style = MaterialTheme.typography.bodySmall) }
                    if (offer.includesTip == true) Text("报价含小费（明细未知）", style = MaterialTheme.typography.bodySmall)
                    Text("耗时依据：${rating.timeBasis}", style = MaterialTheme.typography.bodySmall)
                    Text("评分分项：${rating.breakdown}", style = MaterialTheme.typography.bodySmall)
                    valuation?.scoreNotes?.takeIf { it.isNotEmpty() }?.let { notes ->
                        Text("评价模型修正：${notes.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                    decision?.reasons?.drop(1)?.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    VehicleCostSummary(valuation)
                    Prop22Summary(valuation?.prop22)
                    DestinationSummary(destination)
                }
            }
        }
    }
}
