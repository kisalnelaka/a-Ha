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

enum class NoiseType(val displayName: String) {
    BROWN("Brown Noise (Deep Ground)"),
    PINK("Pink Noise (Focus Mask)"),
    WHITE("White Noise (High Contrast)")
}

/**
 * Pure mathematical DSP real-time noise generator streaming directly into AudioTrack.
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
    var currentNoiseType: NoiseType = NoiseType.BROWN
        private set

    @Volatile
    var volume: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            audioTrack?.setVolume(field)
        }

    fun start(scope: CoroutineScope, noiseType: NoiseType = NoiseType.BROWN, initialVolume: Float = 0.5f) {
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
                }

                pcmBuffer[i] = (sample * 32767.0f).toInt().toShort()
            }

            audioTrack?.write(pcmBuffer, 0, BUFFER_CHUNK_SIZE)
        }
    }
}
