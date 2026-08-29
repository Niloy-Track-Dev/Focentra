package com.niloy.focentra.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(val title: String, val iconName: String) {
    NONE("Off", "VolumeOff"),
    WHITE_NOISE("White Noise", "GraphicEq"),
    PINK_NOISE("Pink Noise", "Waves"),
    BINAURAL_ALPHA("Alpha Waves (10Hz)", "Psychology"),
    DEEP_BETA("Beta Focus (18Hz)", "Bolt"),
    GAMMA_40HZ("Gamma Focus (40Hz)", "ElectricBolt"),
    RAIN_DRIZZLE("Rain Stream", "WaterDrop"),
    CAMPFIRE("Cozy Campfire", "LocalFireDepartment"),
    METRONOME("Study Metronome", "HourglassTop")
}

class AmbientSoundEngine private constructor() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var generatorJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _currentSound = MutableStateFlow(AmbientSoundType.NONE)
    val currentSound: StateFlow<AmbientSoundType> = _currentSound.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    companion object {
        @Volatile
        private var instance: AmbientSoundEngine? = null

        fun getInstance(): AmbientSoundEngine {
            return instance ?: synchronized(this) {
                instance ?: AmbientSoundEngine().also { instance = it }
            }
        }
    }

    fun setSound(soundType: AmbientSoundType) {
        if (_currentSound.value == soundType) {
            stop()
            return
        }
        stop()
        if (soundType == AmbientSoundType.NONE) return

        _currentSound.value = soundType
        startAudioStream(soundType)
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        audioTrack?.setVolume(clamped)
    }

    fun stop() {
        _currentSound.value = AmbientSoundType.NONE
        generatorJob?.cancel()
        generatorJob = null
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioTrack = null
        }
    }

    private fun startAudioStream(soundType: AmbientSoundType) {
        generatorJob = scope.launch {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

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
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(_volume.value)
            track.play()
            audioTrack = track

            val buffer = ShortArray(bufferSize / 2) // Short is 2 bytes
            var phaseLeft = 0.0
            var phaseRight = 0.0
            var pinkB0 = 0.0
            var pinkB1 = 0.0
            var pinkB2 = 0.0

            try {
                while (isActive) {
                    when (soundType) {
                        AmbientSoundType.WHITE_NOISE -> {
                            for (i in buffer.indices) {
                                val noise = (Random.nextFloat() * 2f - 1f) * 0.18f
                                buffer[i] = (noise * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            }
                        }
                        AmbientSoundType.PINK_NOISE -> {
                            for (i in 0 until buffer.size step 2) {
                                val white = (Random.nextDouble() * 2.0 - 1.0) * 0.2
                                pinkB0 = 0.99886 * pinkB0 + white * 0.0555179
                                pinkB1 = 0.99332 * pinkB1 + white * 0.0750759
                                pinkB2 = 0.96900 * pinkB2 + white * 0.1538520
                                val pink = (pinkB0 + pinkB1 + pinkB2 + white * 0.5362) * 0.35
                                val shortVal = (pink * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                                buffer[i] = shortVal
                                if (i + 1 < buffer.size) buffer[i + 1] = shortVal
                            }
                        }
                        AmbientSoundType.BINAURAL_ALPHA -> {
                            // Base 216Hz on left ear, 226Hz on right ear -> 10Hz Alpha beat
                            val freqL = 216.0
                            val freqR = 226.0
                            val deltaL = 2.0 * Math.PI * freqL / sampleRate
                            val deltaR = 2.0 * Math.PI * freqR / sampleRate

                            for (i in 0 until buffer.size step 2) {
                                val sampleL = sin(phaseLeft) * 0.22
                                val sampleR = sin(phaseRight) * 0.22
                                phaseLeft = (phaseLeft + deltaL) % (2.0 * Math.PI)
                                phaseRight = (phaseRight + deltaR) % (2.0 * Math.PI)

                                buffer[i] = (sampleL * Short.MAX_VALUE).toInt().toShort()
                                if (i + 1 < buffer.size) {
                                    buffer[i + 1] = (sampleR * Short.MAX_VALUE).toInt().toShort()
                                }
                            }
                        }
                        AmbientSoundType.DEEP_BETA -> {
                            // Base 250Hz left, 268Hz right -> 18Hz Beta focus beat
                            val freqL = 250.0
                            val freqR = 268.0
                            val deltaL = 2.0 * Math.PI * freqL / sampleRate
                            val deltaR = 2.0 * Math.PI * freqR / sampleRate

                            for (i in 0 until buffer.size step 2) {
                                val sampleL = sin(phaseLeft) * 0.20
                                val sampleR = sin(phaseRight) * 0.20
                                phaseLeft = (phaseLeft + deltaL) % (2.0 * Math.PI)
                                phaseRight = (phaseRight + deltaR) % (2.0 * Math.PI)

                                buffer[i] = (sampleL * Short.MAX_VALUE).toInt().toShort()
                                if (i + 1 < buffer.size) {
                                    buffer[i + 1] = (sampleR * Short.MAX_VALUE).toInt().toShort()
                                }
                            }
                        }
                        AmbientSoundType.GAMMA_40HZ -> {
                            // 40Hz Gamma wave for maximum cognitive processing & focus
                            val freqL = 200.0
                            val freqR = 240.0
                            val deltaL = 2.0 * Math.PI * freqL / sampleRate
                            val deltaR = 2.0 * Math.PI * freqR / sampleRate

                            for (i in 0 until buffer.size step 2) {
                                val sampleL = sin(phaseLeft) * 0.20
                                val sampleR = sin(phaseRight) * 0.20
                                phaseLeft = (phaseLeft + deltaL) % (2.0 * Math.PI)
                                phaseRight = (phaseRight + deltaR) % (2.0 * Math.PI)

                                buffer[i] = (sampleL * Short.MAX_VALUE).toInt().toShort()
                                if (i + 1 < buffer.size) {
                                    buffer[i + 1] = (sampleR * Short.MAX_VALUE).toInt().toShort()
                                }
                            }
                        }
                        AmbientSoundType.RAIN_DRIZZLE -> {
                            // Filtered modulated noise simulating rainfall
                            for (i in 0 until buffer.size step 2) {
                                val white = (Random.nextDouble() * 2.0 - 1.0) * 0.15
                                pinkB0 = 0.992 * pinkB0 + white * 0.08
                                val mod = 0.7 + 0.3 * sin(phaseLeft)
                                phaseLeft = (phaseLeft + 0.0003) % (2.0 * Math.PI)
                                val rainSample = pinkB0 * mod * 0.6
                                val shortVal = (rainSample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                                buffer[i] = shortVal
                                if (i + 1 < buffer.size) buffer[i + 1] = shortVal
                            }
                        }
                        AmbientSoundType.CAMPFIRE -> {
                            // Low frequency rumble + intermittent crackle sparks
                            for (i in 0 until buffer.size step 2) {
                                val white = (Random.nextDouble() * 2.0 - 1.0) * 0.1
                                pinkB0 = 0.995 * pinkB0 + white * 0.05
                                val crackle = if (Random.nextDouble() > 0.9985) (Random.nextDouble() * 0.7) else 0.0
                                val sample = (pinkB0 * 0.4 + crackle)
                                val shortVal = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                                buffer[i] = shortVal
                                if (i + 1 < buffer.size) buffer[i + 1] = shortVal
                            }
                        }
                        AmbientSoundType.METRONOME -> {
                            // Periodic 1-second pulse tick
                            for (i in 0 until buffer.size step 2) {
                                phaseLeft += 1.0
                                val tickPos = (phaseLeft % sampleRate).toInt()
                                val sample = if (tickPos < 800) {
                                    sin(tickPos * 0.15) * Math.exp(-tickPos / 150.0) * 0.3
                                } else 0.0
                                val shortVal = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                                buffer[i] = shortVal
                                if (i + 1 < buffer.size) buffer[i + 1] = shortVal
                            }
                        }
                        AmbientSoundType.NONE -> break
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                // Cancelled or stopped
            }
        }
    }
}
