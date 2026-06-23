package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.ui.layout.rememberLayoutContext
import com.jacobrozell.dartbuddy.ui.layout.scoringPadWidth
import com.jacobrozell.dartbuddy.ui.layout.usesAccessibilityMatchScoringLayout
import com.jacobrozell.dartbuddy.ui.layout.usesSideBySideMatchScoringLayout

@Composable
fun MatchScoringBody(
    playerCount: Int,
    modifier: Modifier = Modifier,
    activeBand: @Composable () -> Unit = {},
    scoreboard: @Composable () -> Unit,
    padChrome: @Composable () -> Unit = {},
    pad: @Composable () -> Unit,
) {
    val layout = rememberLayoutContext(playerCount = playerCount)
    val sideBySide = usesSideBySideMatchScoringLayout(layout)
    val padWidth = scoringPadWidth(layout)
    val accessibility = usesAccessibilityMatchScoringLayout(layout)

    if (accessibility) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            activeBand()
            scoreboard()
            padChrome()
            pad()
        }
        return
    }

    if (sideBySide) {
        Column(modifier.fillMaxSize()) {
            activeBand()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    scoreboard()
                }
                PadColumn(padWidth, padChrome, pad)
            }
        }
    } else {
        Column(modifier.fillMaxSize()) {
            activeBand()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                scoreboard()
            }
            PadColumn(Dp.Unspecified, padChrome, pad, fullWidth = true)
        }
    }
}

@Composable
private fun PadColumn(
    padWidth: Dp,
    padChrome: @Composable () -> Unit,
    pad: @Composable () -> Unit,
    fullWidth: Boolean = false,
) {
    Column(
        modifier = Modifier
            .then(
                when {
                    fullWidth -> Modifier.fillMaxWidth()
                    padWidth != Dp.Unspecified -> Modifier.width(padWidth)
                    else -> Modifier
                },
            )
            .then(if (fullWidth) Modifier else Modifier.fillMaxHeight().padding(start = 8.dp))
            .padding(horizontal = if (fullWidth) 16.dp else 0.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        padChrome()
        pad()
    }
}
