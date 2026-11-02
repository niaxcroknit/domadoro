package com.tomatotimer.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.tomatotimer.app.timer.Phase
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 끝났을 때의 소리와 진동.
 * 화면을 안 봐도 무엇이 끝났는지 알 수 있게 둘의 리듬을 다르게 했어요.
 *  - 집중 끝(토마토 수확): 올라가는 '딩-동-댕-딩' 두 번 / 짧은 진동 두 번
 *  - 휴식 끝(다시 집중):   '띵띵-띠링' 두 번 / 긴 진동 세 번
 */
object Alerts {
    private const val RATE = 44_100

    fun chime(ended: Phase) {
        val notes = if (ended == Phase.FOCUS) {
            listOf(784.0 to 0.0, 988.0 to 0.17, 1175.0 to 0.34, 1568.0 to 0.55,
                784.0 to 1.4, 988.0 to 1.57, 1175.0 to 1.74, 1568.0 to 1.95)
        } else {
            listOf(1319.0 to 0.0, 1319.0 to 0.18, 1760.0 to 0.4,
                1319.0 to 1.2, 1319.0 to 1.38, 1760.0 to 1.6)
        }
        thread(name = "chime") {
            try {
                val total = ((notes.maxOf { it.second } + 1.6) * RATE).toInt()
                val mix = FloatArray(total)
                val noteLen = (1.5 * RATE).toInt()
                for ((freq, at) in notes) {
                    val start = (at * RATE).toInt()
                    for (i in 0 until noteLen) {
                        val idx = start + i
                        if (idx >= total) break
                        val t = i.toDouble() / RATE
                        val env = (if (t < 0.015) t / 0.015 else 1.0) * exp(-t * 3.2)
                        val s = sin(2 * PI * freq * t) + 0.18 * sin(4 * PI * freq * t)
                        mix[idx] += (0.26 * env * s).toFloat()
                    }
                }
                val pcm = ShortArray(total) { (mix[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((total * 1000L / RATE) + 200)
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    fun vibrate(context: Context, ended: Phase) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        val pattern = if (ended == Phase.FOCUS) {
            longArrayOf(0, 220, 120, 220)
        } else {
            longArrayOf(0, 600, 200, 600, 200, 600)
        }
        val effect = VibrationEffect.createWaveform(pattern, -1)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        } catch (_: Exception) {
        }
    }
}
