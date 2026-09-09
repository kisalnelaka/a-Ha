package org.audhd.aha.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

enum class NoiseType(val displayName: String) {
    RAIN("Rainfall (Acoustic Patter)"),
    BROWN("Brown Noise (Deep Ground)"),
    PINK("Pink Noise (Focus Mask)"),
    WHITE("White Noise (High Contrast)")
}

/**
 * Pure mathematical DSP real-time noise & ambient generator streaming directly into AudioTrack.
 * Zero asset dependencies: no MP3/WAV files, zero APK bloat, infinite duration, zero looping seams.
 */
class SoundScapeEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_CHUNK_SIZE = 4096
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val random = Random()

    @Volatile
    var isPlaying: Boolean = false
        private set

    @Volatile
    var currentNoiseType: NoiseType = NoiseType.RAIN
        private set

    @Volatile
    var volume: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            audioTrack?.setVolume(field)
        }

    fun start(scope: CoroutineScope, noiseType: NoiseType = NoiseType.RAIN, initialVolume: Float = 0.5f) {
        if (isPlaying && currentNoiseType == noiseType) return
        stop()

        currentNoiseType = noiseType
        volume = initialVolume

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, BUFFER_CHUNK_SIZE * 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.setVolume(volume)
        audioTrack?.play()
        isPlaying = true

        playbackJob = scope.launch(Dispatchers.Default) {
            streamAudioLoop()
        }
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
            // Safe cleanup
        } finally {
            audioTrack = null
        }
    }

    private suspend fun streamAudioLoop() {
        val pcmBuffer = ShortArray(BUFFER_CHUNK_SIZE)

        // DSP state variables
        var brownLastOutput = 0.0f
        var b0 = 0.0f
        var b1 = 0.0f
        var b2 = 0.0f
        var b3 = 0.0f
        var b4 = 0.0f
        var b5 = 0.0f
        var b6 = 0.0f

        // Rain DSP state variables
        var rainWash = 0.0f
        var gustPhase = 0.0f
        var drop1Amp = 0.0f
        var drop1Freq = 0.0f
        var drop1Phase = 0.0f
        var drop2Amp = 0.0f
        var drop2Freq = 0.0f
        var drop2Phase = 0.0f

        while (isPlaying && kotlinx.coroutines.currentCoroutineContext().isActive) {
            val type = currentNoiseType

            for (i in 0 until BUFFER_CHUNK_SIZE) {
                val white = (random.nextFloat() * 2.0f) - 1.0f

                val sample = when (type) {
                    NoiseType.WHITE -> white * 0.4f
                    NoiseType.BROWN -> {
                        // Leaky integrator 1/f^2 Brownian noise
                        brownLastOutput = (brownLastOutput + (0.02f * white)) / 1.02f
                        (brownLastOutput * 3.5f).coerceIn(-1.0f, 1.0f)
                    }
                    NoiseType.PINK -> {
                        // Paul Kellet's refined 1/f filter approximation
                        b0 = 0.99886f * b0 + white * 0.0555179f
                        b1 = 0.99332f * b1 + white * 0.0750759f
                        b2 = 0.96900f * b2 + white * 0.1538520f
                        b3 = 0.86650f * b3 + white * 0.3104856f
                        b4 = 0.55000f * b4 + white * 0.5329522f
                        b5 = -0.7616f * b5 - white * 0.0168980f
                        val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f
                        b6 = white * 0.115926f
                        (pink * 0.11f).coerceIn(-1.0f, 1.0f)
                    }
                    NoiseType.RAIN -> {
                        // Background rain wash: 2-pole low-pass filtered mix of pink & brown noise
                        brownLastOutput = (brownLastOutput + (0.02f * white)) / 1.02f
                        val brownSample = (brownLastOutput * 3.5f).coerceIn(-1.0f, 1.0f)

                        b0 = 0.99886f * b0 + white * 0.0555179f
                        b1 = 0.99332f * b1 + white * 0.0750759f
                        b2 = 0.96900f * b2 + white * 0.1538520f
                        b3 = 0.86650f * b3 + white * 0.3104856f
                        val pinkSample = ((b0 + b1 + b2 + b3) * 0.18f).coerceIn(-1.0f, 1.0f)

                        rainWash = rainWash * 0.92f + (pinkSample * 0.5f + brownSample * 0.5f) * 0.08f

                        // Naturalistic slow gust undulation (~0.25 Hz)
                        gustPhase += 0.00004f
                        if (gustPhase > 6.2831855f) gustPhase -= 6.2831855f
                        val gust = 0.8f + 0.2f * sin(gustPhase)

                        // Droplet voice 1 (acoustic raindrop on hard surface)
                        if (drop1Amp < 0.01f && random.nextFloat() < 0.0018f) {
                            drop1Amp = 0.35f + random.nextFloat() * 0.35f
                            drop1Freq = (1200f + random.nextFloat() * 1600f) * (6.2831855f / SAMPLE_RATE)
                            drop1Phase = 0f
                        }
                        var drop1Sample = 0f
                        if (drop1Amp >= 0.01f) {
                            drop1Phase += drop1Freq
                            drop1Sample = sin(drop1Phase) * drop1Amp
                            drop1Amp *= 0.994f // Exponential decay
                        }

                        // Droplet voice 2 (lighter patter)
                        if (drop2Amp < 0.01f && random.nextFloat() < 0.0012f) {
                            drop2Amp = 0.25f + random.nextFloat() * 0.3f
                            drop2Freq = (1600f + random.nextFloat() * 1800f) * (6.2831855f / SAMPLE_RATE)
                            drop2Phase = 0f
                        }
                        var drop2Sample = 0f
                        if (drop2Amp >= 0.01f) {
                            drop2Phase += drop2Freq
                            drop2Sample = sin(drop2Phase) * drop2Amp
                            drop2Amp *= 0.992f // Exponential decay
                        }

                        val rain = (rainWash * gust * 1.8f) + (drop1Sample * 0.4f) + (drop2Sample * 0.3f)
                        rain.coerceIn(-1.0f, 1.0f)
                    }
                }

                pcmBuffer[i] = (sample * 32767.0f).toInt().toShort()
            }

            audioTrack?.write(pcmBuffer, 0, BUFFER_CHUNK_SIZE)
        }
    }
}
