package com.jacobrozell.dartbuddy.support.motion

import android.content.Context
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.jacobrozell.dartbuddy.support.MotionPreferences

object MotionPolicy {
    fun shouldAnimate(context: Context): Boolean = !MotionPreferences.prefersReducedMotion(context)

    fun fastAnimationSpec(reduceMotion: Boolean): AnimationSpec<Float> =
        if (reduceMotion) {
            tween(durationMillis = 0)
        } else {
            spring(stiffness = Spring.StiffnessMedium)
        }

    fun trophyAnimationSpec(reduceMotion: Boolean): AnimationSpec<Float> =
        if (reduceMotion) {
            tween(durationMillis = 0)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            )
        }

    fun staggerDelayMillis(index: Int): Long = 40L * index.coerceAtMost(8)
}
