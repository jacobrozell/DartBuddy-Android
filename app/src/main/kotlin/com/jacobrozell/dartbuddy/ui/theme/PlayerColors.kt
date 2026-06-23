package com.jacobrozell.dartbuddy.ui.theme

import androidx.compose.ui.graphics.Color
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken

object PlayerColors {
    fun accent(index: Int): Color = PlayerColorToken.all[index % PlayerColorToken.all.size].color

    fun fromToken(colorToken: String?, playerId: String): Color =
        PlayerColorToken.resolve(colorToken ?: PlayerColorToken.defaultForPlayer(playerId).raw).color
}
