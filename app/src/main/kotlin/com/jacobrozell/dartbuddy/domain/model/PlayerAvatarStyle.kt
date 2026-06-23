package com.jacobrozell.dartbuddy.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import com.jacobrozell.dartbuddy.R
import kotlin.math.abs

enum class PlayerAvatarStyle(val raw: String) {
    Dart("dart"),
    Target("target"),
    Trophy("trophy"),
    Flame("flame"),
    Star("star"),
    Crown("crown"),
    Bolt("bolt"),
    Heart("heart"),
    Medal("medal"),
    Shield("shield"),
    GameController("gameController"),
    Sparkles("sparkles"),
    ThumbsUp("thumbsUp"),
    Dice("dice"),
    Leaf("leaf"),
    Moon("moon"),
    ;

    val icon: ImageVector
        get() = when (this) {
            Dart -> Icons.Default.Bolt
            Target -> Icons.Default.Shield
            Trophy -> Icons.Default.EmojiEvents
            Flame -> Icons.Default.Whatshot
            Star -> Icons.Default.Star
            Crown -> Icons.Default.WorkspacePremium
            Bolt -> Icons.Default.Bolt
            Heart -> Icons.Default.Favorite
            Medal -> Icons.Default.MilitaryTech
            Shield -> Icons.Default.Shield
            GameController -> Icons.Default.SportsEsports
            Sparkles -> Icons.Default.Star
            ThumbsUp -> Icons.Default.ThumbUp
            Dice -> Icons.Default.Casino
            Leaf -> Icons.Default.LocalFlorist
            Moon -> Icons.Default.Nightlight
        }

    val labelRes: Int
        get() = when (this) {
            Dart -> R.string.players_avatar_dart
            Target -> R.string.players_avatar_target
            Trophy -> R.string.players_avatar_trophy
            Flame -> R.string.players_avatar_flame
            Star -> R.string.players_avatar_star
            Crown -> R.string.players_avatar_crown
            Bolt -> R.string.players_avatar_bolt
            Heart -> R.string.players_avatar_heart
            Medal -> R.string.players_avatar_medal
            Shield -> R.string.players_avatar_shield
            GameController -> R.string.players_avatar_game_controller
            Sparkles -> R.string.players_avatar_sparkles
            ThumbsUp -> R.string.players_avatar_thumbs_up
            Dice -> R.string.players_avatar_dice
            Leaf -> R.string.players_avatar_leaf
            Moon -> R.string.players_avatar_moon
        }

    companion object {
        val all = entries

        fun resolve(raw: String?): PlayerAvatarStyle =
            entries.find { it.raw == raw } ?: Dart

        fun defaultForPlayer(playerId: String, isBot: Boolean): PlayerAvatarStyle {
            if (isBot) return Target
            val index = abs(playerId.hashCode()) % entries.size
            return entries[index]
        }
    }
}
