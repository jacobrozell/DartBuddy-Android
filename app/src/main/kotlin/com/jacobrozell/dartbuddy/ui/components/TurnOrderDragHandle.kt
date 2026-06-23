package com.jacobrozell.dartbuddy.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.ui.theme.Brand

@Composable
fun TurnOrderDragHandle(
    enabled: Boolean,
    onDragUp: () -> Unit,
    onDragDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return
    val rowHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val accumulated = remember { mutableFloatStateOf(0f) }
    IconButton(
        onClick = {},
        modifier = modifier
            .size(32.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulated.floatValue += dragAmount.y
                        when {
                            accumulated.floatValue > rowHeightPx -> {
                                onDragDown()
                                accumulated.floatValue = 0f
                            }
                            accumulated.floatValue < -rowHeightPx -> {
                                onDragUp()
                                accumulated.floatValue = 0f
                            }
                        }
                    },
                )
            },
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = stringResource(R.string.play_setup_reorder_handle),
            tint = Brand.Green,
        )
    }
}
