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
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.ui.layout.GameplayLayoutMetrics
import com.jacobrozell.dartbuddy.ui.layout.isLandscapeOrientation
import com.jacobrozell.dartbuddy.ui.layout.usesLandscapeMatchScoringLayout

@Composable
fun MatchScoringBody(
    playerCount: Int,
    modifier: Modifier = Modifier,
    activeBand: @Composable () -> Unit = {},
    scoreboard: @Composable () -> Unit,
    padChrome: @Composable () -> Unit = {},
    pad: @Composable () -> Unit,
) {
    val landscape = isLandscapeOrientation()

    if (usesLandscapeMatchScoringLayout(landscape)) {
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
                Column(
                    modifier = Modifier
                        .width(GameplayLayoutMetrics.landscapeScoringPadWidth)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    padChrome()
                    pad()
                }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                padChrome()
                pad()
            }
        }
    }
}
