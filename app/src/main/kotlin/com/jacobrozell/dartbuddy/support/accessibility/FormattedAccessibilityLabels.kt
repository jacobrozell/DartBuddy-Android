package com.jacobrozell.dartbuddy.support.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.support.HistoryStanding
import com.jacobrozell.dartbuddy.ui.activity.StatColumn

@Composable
fun historyStandingAccessibilityLabel(standing: HistoryStanding): String {
    val role = if (standing.isWinner) {
        stringResource(R.string.history_standing_winner_role)
    } else {
        ""
    }
    return stringResource(
        R.string.history_standing_accessibility_format,
        standing.name,
        role,
        standing.score,
    )
}

@Composable
fun historyRowAccessibilityLabel(
    dateText: String,
    configText: String,
    standings: List<HistoryStanding>,
    isForfeited: Boolean,
): String {
    val playerLabels = standings.map { historyStandingAccessibilityLabel(it) }
    val players = playerLabels.joinToString(". ")
    var summary = stringResource(
        R.string.history_row_accessibility_format,
        dateText,
        configText,
        players,
    )
    if (isForfeited) {
        summary += " " + stringResource(R.string.history_row_forfeit_accessibility_suffix)
    }
    return summary
}

@Composable
fun statTableRowAccessibilityLabel(
    index: Int,
    playerName: String,
    columns: List<StatColumn>,
    cells: List<String>,
): String {
    val stats = columns.zip(cells).joinToString(", ") { (label, value) -> "$label $value" }
    return stringResource(R.string.stats_table_row_accessibility_format, index + 1, playerName, stats)
}

@Composable
fun setupChipAccessibilityLabel(title: String, value: String): String =
    stringResource(R.string.play_setup_chip_accessibility_format, title, value)

@Composable
fun selectedModeAccessibilityLabel(modeName: String, configSummary: String): String =
    stringResource(R.string.play_setup_selected_mode_accessibility_format, modeName, configSummary)

@Composable
fun resumeMatchAccessibilityLabel(buttonLabel: String, modeName: String): String =
    stringResource(R.string.play_home_resume_accessibility_format, buttonLabel, modeName)

@Composable
fun activityFilterModeAccessibilityLabel(label: String): String =
    stringResource(R.string.activity_filter_mode_accessibility_format, label)

@Composable
fun activityFilterPlayerAccessibilityLabel(label: String): String =
    stringResource(R.string.activity_filter_player_accessibility_format, label)

@Composable
fun activityFilterPeriodAccessibilityLabel(label: String): String =
    stringResource(R.string.activity_filter_period_accessibility_format, label)
