@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.overud.model.*
import kotlin.math.roundToLong
import com.overud.platform.*
import com.overud.valuation.*
import com.overud.acceptance.*
import com.overud.region.RegionCell
import java.text.SimpleDateFormat
import java.util.Date

@Composable fun OfferCard(offer: Offer, decision: Decision?, destination: DestinationAssessment) {
    val locale = LocalConfiguration.current.locales[0]
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${offer.platform.label} · ${offer.serviceKind.label}", style = MaterialTheme.typography.labelLarge)
                Text(SimpleDateFormat("HH:mm", locale).format(Date(offer.observedAt)), style = MaterialTheme.typography.labelMedium)
            }
            Text(offerAmountPresentation(offer), style = MaterialTheme.typography.displayLarge)
            Text(offerRoutePresentation(offer), style = MaterialTheme.typography.titleMedium)
            offer.merchant?.let { Text("商家：$it") }
            offer.pickupAddress?.let { Text("取件地址：$it") }
            offer.destination?.let { Text("目的地：$it") }
            if (offer.includesTip == true) Text("报价含小费，未提供小费明细", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column { Text("每英里报价", style = MaterialTheme.typography.labelMedium); Text(decision?.valuation?.centsPerMile?.let { money(it.roundToLong()) } ?: "—", style = MaterialTheme.typography.titleLarge) }
                Column { Text("预计毛时薪", style = MaterialTheme.typography.labelMedium); Text(decision?.valuation?.grossCentsPerHour?.let { money(it.roundToLong()) } ?: "—", style = MaterialTheme.typography.titleLarge) }
            }
            Text(decision?.reasons?.joinToString("；")?.ifEmpty { "满足所有接单要求" } ?: "等待分析", color = MaterialTheme.colorScheme.primary)
            VehicleCostSummary(decision?.valuation)
            Prop22Summary(decision?.valuation?.prop22)
            DestinationSummary(destination)
        }
    }
}
