package com.jacobrozell.dartbuddy.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun TabRootScaffold(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Brand.BackgroundDark else Brand.BackgroundLight
    Column(modifier.fillMaxSize().background(bg)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = DS.contentMaxWidth)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = DS.s4, vertical = DS.s2)
                    .padding(bottom = DS.s6),
                verticalArrangement = Arrangement.spacedBy(DS.s4),
                content = content,
            )
        }
        bottomBar()
    }
}

@Composable
fun BrandAppTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.semantics { heading() },
        fontSize = 34.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onBackground,
        letterSpacing = 0.6.sp,
    )
}

@Composable
fun BrandRootScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun SetupSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) Brand.CardDark else Brand.CardLight
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusMd))
            .background(cardBg)
            .padding(DS.s3),
        verticalArrangement = Arrangement.spacedBy(DS.s2),
        content = content,
    )
}

@Composable
fun SetupEmptyHint(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DS.s2),
        horizontalArrangement = Arrangement.spacedBy(DS.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.People,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryActionButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    accent: Color = Brand.Green,
) {
    val dark = isSystemInDarkTheme()
    val disabledBg = if (dark) Brand.CardElevatedDark else Brand.CardElevatedLight
    val disabledText = if (dark) Color(0xFFB8B8B8) else Color(0xFF474749)

    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = title },
        shape = RoundedCornerShape(DS.radiusSm),
        color = if (enabled) accent else disabledBg,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = DS.s3)) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.White else disabledText,
                )
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(Brand.RedAccent)
            .padding(horizontal = DS.s3, vertical = DS.s2),
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
fun SetupStickyFooter(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Brand.BackgroundDark else Brand.BackgroundLight
    val shadow = if (dark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, spotColor = shadow, ambientColor = shadow)
            .background(bg)
            .padding(horizontal = DS.s4, vertical = DS.s3),
        verticalArrangement = Arrangement.spacedBy(DS.s2),
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> HorizontalOptionChips(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelFor: (T) -> String = { it.toString() },
) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.s2)) {
        Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(DS.s2), verticalArrangement = Arrangement.spacedBy(DS.s2)) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    label = { Text(labelFor(option)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Brand.Green.copy(alpha = 0.25f),
                        selectedLabelColor = Brand.Green,
                    ),
                )
            }
        }
    }
}

@Composable
fun GameModeBadge(
    matchType: String,
    modifier: Modifier = Modifier,
    size: Int = 36,
) {
    val color = when (matchType.uppercase()) {
        "X01" -> Brand.Green
        "CRICKET" -> Brand.Amber
        else -> Brand.ProBot
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DS.radiusSm))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(DS.radiusSm))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (matchType.equals("X01", ignoreCase = true)) "01" else matchType.take(2).uppercase(),
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = (size * 0.35f).sp,
        )
    }
}

@Composable
fun SetupHomeChrome(
    onStart: () -> Unit,
    startTitle: String,
    startEnabled: Boolean,
    startLoading: Boolean,
    validationErrors: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Brand.BackgroundDark else Brand.BackgroundLight
    BoxWithConstraints(modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = DS.contentMaxWidth)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = DS.s4)
                        .padding(bottom = 96.dp),
                ) {
                    content()
                }
            }
            SetupStickyFooter {
                PrimaryActionButton(
                    title = startTitle,
                    onClick = onStart,
                    enabled = startEnabled,
                    loading = startLoading,
                )
                validationErrors.forEach { ErrorBanner(it) }
            }
        }
    }
}

@Composable
fun WideOrCompactSetup(
    wideContent: @Composable () -> Unit,
    compactContent: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= DS.setupWideBreakpoint) wideContent() else compactContent()
    }
}
