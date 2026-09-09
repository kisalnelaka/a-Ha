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
    BINAURAL_GAMMA("40Hz Gamma (Deep Hyperfocus)"),
    BINAURAL_BETA("14Hz Beta (Alert State)"),
    BROWN("Brown Noise (Deep Ground)"),
    PINK("Pink Noise (Focus Mask)"),
    WHITE("White Noise (High Contrast)")
}

/**
 * Pure mathematical DSP real-time noise & ambient generator streaming directly into AudioTrack.
 * Stereo output: enables true binaural phase entrainment (40Hz Gamma & 14Hz Beta) alongside acoustic noise.
 * Zero asset dependencies: no MP3/WAV files, zero APK bloat, infinite duration, zero looping seams.
 */
class SoundScapeEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_CHUNK_SIZE = 4096 // 2048 stereo frames (2 shorts/frame)
        private const val TWO_PI = 6.283185307179586f
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
            AudioFormat.CHANNEL_OUT_STEREO,
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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
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

        // DSP state variables for filters
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

        // Binaural Phase Oscillators
        var binauralLeftPhase = 0.0f
        var binauralRightPhase = 0.0f

        while (isPlaying && kotlinx.coroutines.currentCoroutineContext().isActive) {
            val type = currentNoiseType

            var i = 0
            while (i < BUFFER_CHUNK_SIZE) {
                val white = (random.nextFloat() * 2.0f) - 1.0f

                var leftSample: Float
                var rightSample: Float

                when (type) {
                    NoiseType.WHITE -> {
                        val mono = white * 0.35f
                        leftSample = mono
                        rightSample = mono
                    }
                    NoiseType.BROWN -> {
                        brownLastOutput = (brownLastOutput + (0.02f * white)) / 1.02f
                        val mono = (brownLastOutput * 3.5f).coerceIn(-1.0f, 1.0f)
                        leftSample = mono
                        rightSample = mono
                    }
                    NoiseType.PINK -> {
                        b0 = 0.99886f * b0 + white * 0.0555179f
                        b1 = 0.99332f * b1 + white * 0.0750759f
                        b2 = 0.96900f * b2 + white * 0.1538520f
                        b3 = 0.86650f * b3 + white * 0.3104856f
                        b4 = 0.55000f * b4 + white * 0.5329522f
                        b5 = -0.7616f * b5 - white * 0.0168980f
                        val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f
                        b6 = white * 0.115926f
                        val mono = (pink * 0.11f).coerceIn(-1.0f, 1.0f)
                        leftSample = mono
                        rightSample = mono
                    }
                    NoiseType.RAIN -> {
                        brownLastOutput = (brownLastOutput + (0.02f * white)) / 1.02f
                        val brownSample = (brownLastOutput * 3.5f).coerceIn(-1.0f, 1.0f)

                        b0 = 0.99886f * b0 + white * 0.0555179f
                        b1 = 0.99332f * b1 + white * 0.0750759f
                        b2 = 0.96900f * b2 + white * 0.1538520f
                        b3 = 0.86650f * b3 + white * 0.3104856f
                        val pinkSample = ((b0 + b1 + b2 + b3) * 0.18f).coerceIn(-1.0f, 1.0f)

                        rainWash = rainWash * 0.92f + (pinkSample * 0.5f + brownSample * 0.5f) * 0.08f

                        gustPhase += 0.00004f
                        if (gustPhase > TWO_PI) gustPhase -= TWO_PI
                        val gust = 0.8f + 0.2f * sin(gustPhase)

                        if (drop1Amp < 0.01f && random.nextFloat() < 0.0018f) {
                            drop1Amp = 0.35f + random.nextFloat() * 0.35f
                            drop1Freq = (1200f + random.nextFloat() * 1600f) * (TWO_PI / SAMPLE_RATE)
                            drop1Phase = 0f
                        }
                        var drop1Sample = 0f
                        if (drop1Amp >= 0.01f) {
                            drop1Phase += drop1Freq
                            drop1Sample = sin(drop1Phase) * drop1Amp
                            drop1Amp *= 0.994f
                        }

                        if (drop2Amp < 0.01f && random.nextFloat() < 0.0012f) {
                            drop2Amp = 0.25f + random.nextFloat() * 0.3f
                            drop2Freq = (1600f + random.nextFloat() * 1800f) * (TWO_PI / SAMPLE_RATE)
                            drop2Phase = 0f
                        }
                        var drop2Sample = 0f
                        if (drop2Amp >= 0.01f) {
                            drop2Phase += drop2Freq
                            drop2Sample = sin(drop2Phase) * drop2Amp
                            drop2Amp *= 0.992f
                        }

                        val rain = (rainWash * gust * 1.8f) + (drop1Sample * 0.4f) + (drop2Sample * 0.3f)
                        val mono = rain.coerceIn(-1.0f, 1.0f)
                        leftSample = mono
                        rightSample = mono
                    }
                    NoiseType.BINAURAL_GAMMA -> {
                        // 40Hz Gamma Focus Beat: Left = 200Hz, Right = 240Hz
                        val leftInc = 200.0f * (TWO_PI / SAMPLE_RATE)
                        val rightInc = 240.0f * (TWO_PI / SAMPLE_RATE)

                        binauralLeftPhase += leftInc
                        if (binauralLeftPhase > TWO_PI) binauralLeftPhase -= TWO_PI

                        binauralRightPhase += rightInc
                        if (binauralRightPhase > TWO_PI) binauralRightPhase -= TWO_PI

                        // Subtle pink bed floor for acoustic warmth
                        b0 = 0.99886f * b0 + white * 0.0555179f
                        val pinkBed = (b0 * 0.04f).coerceIn(-0.08f, 0.08f)

                        leftSample = (sin(binauralLeftPhase) * 0.28f + pinkBed).coerceIn(-1.0f, 1.0f)
                        rightSample = (sin(binauralRightPhase) * 0.28f + pinkBed).coerceIn(-1.0f, 1.0f)
                    }
                    NoiseType.BINAURAL_BETA -> {
                        // 14Hz Beta Alert Beat: Left = 200Hz, Right = 214Hz
                        val leftInc = 200.0f * (TWO_PI / SAMPLE_RATE)
                        val rightInc = 214.0f * (TWO_PI / SAMPLE_RATE)

                        binauralLeftPhase += leftInc
                        if (binauralLeftPhase > TWO_PI) binauralLeftPhase -= TWO_PI

                        binauralRightPhase += rightInc
                        if (binauralRightPhase > TWO_PI) binauralRightPhase -= TWO_PI

                        // Subtle pink bed floor for acoustic warmth
                        b0 = 0.99886f * b0 + white * 0.0555179f
                        val pinkBed = (b0 * 0.04f).coerceIn(-0.08f, 0.08f)

                        leftSample = (sin(binauralLeftPhase) * 0.28f + pinkBed).coerceIn(-1.0f, 1.0f)
                        rightSample = (sin(binauralRightPhase) * 0.28f + pinkBed).coerceIn(-1.0f, 1.0f)
                    }
                }

                pcmBuffer[i] = (leftSample * 32767.0f).toInt().toShort()
                pcmBuffer[i + 1] = (rightSample * 32767.0f).toInt().toShort()
                i += 2
            }

            audioTrack?.write(pcmBuffer, 0, BUFFER_CHUNK_SIZE)
        }
    }
}
