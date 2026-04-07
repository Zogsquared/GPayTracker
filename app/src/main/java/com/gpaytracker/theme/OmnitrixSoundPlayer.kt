package com.gpaytracker.theme

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Synthesises Omnitrix-style sound effects using Android's built-in ToneGenerator.
 * No copyrighted audio — all sounds are generated on-device.
 */
object OmnitrixSoundPlayer {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)

    /** Played when the dial rotates between aliens */
    fun playDialClick() {
        scope.launch {
            playTone(ToneGenerator.TONE_PROP_BEEP, 60)
        }
    }

    /** The full transformation sequence:
     *  power-up whine → flash beeps → deep activation thud */
    fun playTransformation(onComplete: () -> Unit) {
        scope.launch {
            // Rising power-up sequence
            playTone(ToneGenerator.TONE_CDMA_HIGH_PBX_L, 80)
            delay(80)
            playTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SS, 80)
            delay(80)
            playTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SLS, 100)
            delay(100)
            // Flash beeps
            repeat(3) {
                playTone(ToneGenerator.TONE_PROP_ACK, 60)
                delay(70)
            }
            // Deep activation thud
            playTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 200)
            delay(250)

            handler.post { onComplete() }
        }
    }

    private fun playTone(tone: Int, durationMs: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tg.startTone(tone, durationMs)
            Thread.sleep(durationMs.toLong())
            tg.stopTone()
            tg.release()
        } catch (_: Exception) {}
    }
}
