package com.tomatotimer.app.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.tomatotimer.app.data.NoiseType
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * 백색소음을 앱 안에서 직접 만들어 재생해요. (음원 파일이 필요 없어요)
 * 켜고 끌 때는 부드럽게 커지고 작아져요.
 */
object NoisePlayer {
    private const val RATE = 44_100
    private const val CHUNK = 2_048

    @Volatile private var want = false
    @Volatile private var type = NoiseType.RAIN
    @Volatile private var volume = 0.5f
    private var thread: Thread? = null

    @Synchronized
    fun update(play: Boolean, newType: NoiseType, newVolume: Float) {
        type = newType
        volume = newVolume.coerceIn(0f, 1f)
        want = play
        if (play && thread?.isAlive != true) {
            thread = Thread(::loop, "noise").apply { start() }
        }
    }

    private fun loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuf, CHUNK * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val out = ShortArray(CHUNK)
        var gain = 0f
        // 소음 색깔별 필터 상태
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0
        var brown = 0.0
        var lp = 0.0
        var t = 0L

        try {
            track.play()
            while (true) {
                val target = if (want) volume * 0.8f else 0f
                if (!want && gain < 0.002f) break
                val kind = type
                for (i in 0 until CHUNK) {
                    gain += (target - gain) * 0.00008f // 약 0.3초에 걸쳐 부드럽게
                    val w = Random.nextDouble(-1.0, 1.0)
                    val s = when (kind) {
                        NoiseType.WHITE -> {
                            lp += (w - lp) * 0.75 // 아주 살짝만 부드럽게
                            lp * 0.32
                        }
                        NoiseType.RAIN -> {
                            b0 = 0.99886 * b0 + w * 0.0555179
                            b1 = 0.99332 * b1 + w * 0.0750759
                            b2 = 0.96900 * b2 + w * 0.1538520
                            b3 = 0.86650 * b3 + w * 0.3104856
                            b4 = 0.55000 * b4 + w * 0.5329522
                            b5 = -0.7616 * b5 - w * 0.0168980
                            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362
                            b6 = w * 0.115926
                            pink * 0.09
                        }
                        NoiseType.BROWN -> {
                            brown = (brown + 0.02 * w) / 1.02
                            brown * 3.0
                        }
                        NoiseType.WAVE -> {
                            brown = (brown + 0.02 * w) / 1.02
                            val swell = 0.6 + 0.4 * sin(2 * PI * 0.09 * t / RATE)
                            brown * 3.0 * swell
                        }
                    }
                    t++
                    val v = (s * gain).coerceIn(-1.0, 1.0)
                    out[i] = (v * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(out, 0, CHUNK)
            }
        } catch (_: Exception) {
            // 오디오 장치 문제 등: 조용히 멈춰요
        } finally {
            try { track.stop() } catch (_: Exception) {}
            track.release()
        }
    }
}
