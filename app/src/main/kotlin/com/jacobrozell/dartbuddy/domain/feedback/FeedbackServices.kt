package com.jacobrozell.dartbuddy.domain.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackServices @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val hitSounds: IntArray = intArrayOf(
        soundPool.load(context, R.raw.dart_hit_1, 1),
        soundPool.load(context, R.raw.dart_hit_2, 1),
        soundPool.load(context, R.raw.dart_hit_3, 1),
    )
    private val missSound: Int = soundPool.load(context, R.raw.dart_miss, 1)
    private val legFinishedSound: Int = soundPool.load(context, R.raw.leg_finished, 1)
    private val matchFinishedSound: Int = soundPool.load(context, R.raw.game_finished, 1)

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    fun playDartFeedback(dart: DartInput, hapticsEnabled: Boolean, soundEnabled: Boolean) {
        if (soundEnabled) {
            if (dart.isMiss) playMiss() else playHit()
        }
        if (hapticsEnabled) vibrateOneShot(12)
    }

    fun playBotDartFeedback(dart: DartInput, soundEnabled: Boolean, botDartHapticsEnabled: Boolean) {
        if (soundEnabled) {
            if (dart.isMiss) playMiss() else playHit()
        }
        if (botDartHapticsEnabled) vibrateOneShot(12)
    }

    fun bust(hapticsEnabled: Boolean) {
        if (hapticsEnabled) {
            vibrateWaveform(longArrayOf(0, 40, 60, 40))
        }
    }

    fun legFinished(hapticsEnabled: Boolean, soundEnabled: Boolean) {
        if (hapticsEnabled) vibrateOneShot(80, amplitude = 180)
        if (soundEnabled) playSound(legFinishedSound)
    }

    fun matchFinished(hapticsEnabled: Boolean, soundEnabled: Boolean) {
        if (hapticsEnabled) vibrateOneShot(80, amplitude = 200)
        if (soundEnabled) playSound(matchFinishedSound)
    }

    /** Light haptic when match summary trophy appears (sound already played at match end). */
    fun summaryCelebration(hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrateOneShot(45, amplitude = 160)
    }

    fun announceTurnTotal(total: Int, soundEnabled: Boolean, turnTotalCallerEnabled: Boolean) {
        if (!soundEnabled || !turnTotalCallerEnabled) return
        tts.speak(total.toString(), TextToSpeech.QUEUE_FLUSH, null, "turn_total_$total")
    }

    private fun playHit() {
        val soundId = hitSounds.randomOrNull() ?: return
        playSound(soundId)
    }

    private fun playMiss() = playSound(missSound)

    private fun playSound(soundId: Int) {
        if (soundId == 0) return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun vibrateOneShot(ms: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(ms, amplitude))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(ms)
            }
        }
    }

    private fun vibrateWaveform(pattern: LongArray) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, -1)
            }
        }
    }
}
