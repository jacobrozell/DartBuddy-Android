package com.jacobrozell.dartbuddy.domain.model

import androidx.compose.ui.graphics.Color
import com.jacobrozell.dartbuddy.ui.theme.Brand

enum class PlayerColorToken(val raw: String) {
    Green("green"),
    Amber("amber"),
    Red("red"),
    Blue("blue"),
    Purple("purple"),
    Teal("teal"),
    Orange("orange"),
    Pink("pink"),
    Indigo("indigo"),
    Cyan("cyan"),
    Lime("lime"),
    Coral("coral"),
    Mint("mint"),
    Magenta("magenta"),
    Slate("slate"),
    Gold("gold"),
    Brown("brown"),
    Maroon("maroon"),
    ;

    val color: Color
        get() = when (this) {
            Green -> Brand.Green
            Amber -> Brand.Amber
            Red -> Brand.Red
            Blue -> Color(0xFF598CEB)
            Purple -> Brand.ProBot
            Teal -> Color(0xFF40C7C7)
            Orange -> Brand.Orange
            Pink -> Color(0xFFF2598C)
            Indigo -> Color(0xFF5959D9)
            Cyan -> Color(0xFF33BFF2)
            Lime -> Color(0xFF8CC738)
            Coral -> Color(0xFFFA8072)
            Mint -> Color(0xFF4DD99E)
            Magenta -> Color(0xFFD938B8)
            Slate -> Color(0xFF7A8CA8)
            Gold -> Color(0xFFEBB82E)
            Brown -> Color(0xFFA3734F)
            Maroon -> Color(0xFFB33852)
        }

    companion object {
        val all = entries

        fun resolve(raw: String?): PlayerColorToken =
            entries.find { it.raw.equals(raw, ignoreCase = true) } ?: Green

        fun defaultForPlayer(id: String): PlayerColorToken {
            val index = kotlin.math.abs(id.hashCode()) % all.size
            return all[index]
        }
    }
}
