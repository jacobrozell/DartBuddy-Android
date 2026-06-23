package com.jacobrozell.dartbuddy.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.service.PlayerMatchStats
import com.jacobrozell.dartbuddy.ui.designsystem.DS
import com.jacobrozell.dartbuddy.ui.layout.rememberLayoutContext
import com.jacobrozell.dartbuddy.ui.layout.usesAccessibilityMatchScoringLayout
import com.jacobrozell.dartbuddy.ui.theme.Brand

data class StatColumn(val title: String, val weight: Float = 1f)

@Composable
fun StatTable(
    title: String? = null,
    columns: List<StatColumn>,
    rows: List<PlayerMatchStats>,
    cellsFor: (PlayerMatchStats) -> List<String>,
) {
    val layout = rememberLayoutContext()
    if (usesAccessibilityMatchScoringLayout(layout)) {
        AccessibilityStatTable(title, columns, rows, cellsFor)
        return
    }
    CompactStatTable(title, columns, rows, cellsFor)
}

@Composable
private fun CompactStatTable(
    title: String?,
    columns: List<StatColumn>,
    rows: List<PlayerMatchStats>,
    cellsFor: (PlayerMatchStats) -> List<String>,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(DS.s3),
        verticalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        title?.let {
            Text(it, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.stats_table_players),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1.2f),
                style = MaterialTheme.typography.labelMedium,
            )
            columns.forEach { column ->
                Text(
                    column.title,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(column.weight),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                )
            }
        }
        rows.forEachIndexed { index, row ->
            val stripe = if (index % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else null
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (stripe != null) Modifier.background(stripe) else Modifier)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    row.name,
                    modifier = Modifier.weight(1.2f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                cellsFor(row).forEachIndexed { cellIndex, cell ->
                    Text(
                        cell,
                        modifier = Modifier.weight(columns[cellIndex].weight),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessibilityStatTable(
    title: String?,
    columns: List<StatColumn>,
    rows: List<PlayerMatchStats>,
    cellsFor: (PlayerMatchStats) -> List<String>,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Column(verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        title?.let { Text(it, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
        rows.forEach { row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DS.radiusSm))
                    .background(cardBg)
                    .padding(DS.s3),
                verticalArrangement = Arrangement.spacedBy(DS.s2),
            ) {
                Text(row.name, fontWeight = FontWeight.Bold)
                cellsFor(row).forEachIndexed { index, cell ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(columns[index].title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cell, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SectorHitsChart(
    hitsBySector: Map<String, Int>,
    matchType: MatchType,
    modifier: Modifier = Modifier,
    chartHeight: androidx.compose.ui.unit.Dp = 180.dp,
) {
    val hits = hitsBySector.entries
        .map { SectorHit(it.key, it.value) }
        .sortedBy { StatsSectorOrder.rank(it.sector, matchType) }

    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight

    if (hits.isEmpty()) {
        Text(
            stringResource(R.string.stats_no_dart_data),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = DS.s3),
        )
        return
    }

    val maxCount = hits.maxOf { it.count }.coerceAtLeast(1)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(DS.s3)
            .height(chartHeight),
        horizontalArrangement = Arrangement.spacedBy(DS.s2),
        verticalAlignment = Alignment.Bottom,
    ) {
        hits.forEach { hit ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.width(32.dp),
            ) {
                Text(
                    hit.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight(hit.count.toFloat() / maxCount)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Brand.Green),
                )
                Text(
                    sectorLabel(hit.sector),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
fun PlayerAverageBarChart(
    rows: List<PlayerMatchStats>,
    valueFor: (PlayerMatchStats) -> Double,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    val maxValue = rows.maxOf { valueFor(it) }.coerceAtLeast(1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(DS.s3),
        verticalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        rows.forEach { row ->
            val value = valueFor(row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DS.s2),
            ) {
                Text(
                    row.name,
                    modifier = Modifier.widthIn(max = 120.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((value / maxValue).toFloat().coerceIn(0.05f, 1f))
                            .background(Brand.Green),
                    )
                }
                Text(
                    "%.1f".format(value),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun AverageTrendChart(
    points: List<com.jacobrozell.dartbuddy.domain.service.StatsTrendPoint>,
    modifier: Modifier = Modifier,
    chartHeight: androidx.compose.ui.unit.Dp = 180.dp,
) {
    if (points.size < 2) return
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    val maxAvg = points.maxOf { it.average3Dart }.coerceAtLeast(1.0)
    val minAvg = points.minOf { it.average3Dart }
    val range = (maxAvg - minAvg).coerceAtLeast(1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(cardBg)
            .padding(DS.s3),
        verticalArrangement = Arrangement.spacedBy(DS.s2),
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
            val path = androidx.compose.ui.graphics.Path()
            points.forEachIndexed { index, point ->
                val x = index * stepX
                val normalized = (point.average3Dart - minAvg) / range
                val y = size.height - (normalized * size.height * 0.85f).toFloat() - size.height * 0.05f
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Brand.Green, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            points.forEachIndexed { index, point ->
                val x = index * stepX
                val normalized = (point.average3Dart - minAvg) / range
                val y = size.height - (normalized * size.height * 0.85f).toFloat() - size.height * 0.05f
                drawCircle(Brand.Green, radius = 5f, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "%.1f".format(points.first().average3Dart),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "%.1f".format(points.last().average3Dart),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class SectorHit(val sector: String, val count: Int)

@Composable
private fun sectorLabel(sector: String): String = when (sector) {
    "25" -> stringResource(R.string.stats_sector_bull)
    "0" -> "—"
    else -> sector
}

private object StatsSectorOrder {
    private val sectorOrder = listOf("20", "19", "18", "17", "16", "15", "25", "0")

    fun rank(sector: String, @Suppress("UNUSED_PARAMETER") mode: MatchType): Int {
        val index = sectorOrder.indexOf(sector)
        return if (index >= 0) index else sectorOrder.size + sector.hashCode()
    }
}
