package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import com.example.model.WeatherType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class CombatSfx {
  SWORD_SLASH,
  BOW_SHOT,
  PLASMA_SHOT,
  HEAVY_IMPACT,
  ENEMY_HIT,
  PLAYER_HURT,
  DODGE_WHOOSH,
  BOSS_SIREN,
  HEAL_CHIME,
  VICTORY,
}

/**
 * Pure procedural ambient soundscape synthesizer used by TacticalAudioEngine
 * to smoothly crossfade between Clear breeze, Acid Rain patter, Bio-Storm thunder,
 * and Ash Fog / Blizzard wind howls.
 */
object AmbientWeatherSynth {
  private fun pseudoNoise(idx: Long): Float {
    var x = (idx * 6364136223846793005L + 1442695040888963407L)
    x = x xor (x ushr 30)
    x *= -4658895280553007687L
    x = x xor (x ushr 27)
    return ((x and 0xFFFFL).toFloat() / 32768f) - 1.0f
  }

  private fun smoothNoise(idx: Long): Float =
    (pseudoNoise(idx) + pseudoNoise(idx - 1) + pseudoNoise(idx - 2) + pseudoNoise(idx - 3)) * 0.25f

  fun sampleForWeather(
    weather: WeatherType,
    sampleIdx: Long,
    sampleRate: Int = 22050,
  ): Float {
    val t = sampleIdx.toDouble() / sampleRate
    val baseNoise = smoothNoise(sampleIdx)
    val deepNoise = smoothNoise(sampleIdx / 4L)
    return when (weather) {
      WeatherType.CLEAR -> {
        val breezeMod = (0.45 + 0.35 * sin(2.0 * PI * 0.14 * t)).toFloat()
        val air = deepNoise * 0.08f * breezeMod
        val pad = sin(2.0 * PI * 110.0 * t).toFloat() * 0.02f
        air + pad
      }
      WeatherType.RAIN -> {
        val rainSwell = (0.78 + 0.22 * sin(2.0 * PI * 0.22 * t)).toFloat()
        val patter = baseNoise * 0.22f * rainSwell
        val dropletBurst = if ((sampleIdx % 1373L) < 28L) {
          sin(2.0 * PI * 1150.0 * t).toFloat() * 0.14f
        } else 0f
        val distantRumble = deepNoise * 0.09f
        patter + dropletBurst + distantRumble
      }
      WeatherType.BIO_STORM -> {
        val gustEnv = (0.65 + 0.35 * sin(2.0 * PI * 0.31 * t)).toFloat()
        val heavyRain = baseNoise * 0.34f * gustEnv
        val howlFreq = 290.0 + 145.0 * sin(2.0 * PI * 0.26 * t)
        val howl = sin(2.0 * PI * howlFreq * t).toFloat() * 0.10f * gustEnv
        val thunderCycle = (t % 7.5)
        val thunder = if (thunderCycle < 1.2) {
          (deepNoise * 0.32f + sin(2.0 * PI * 54.0 * t).toFloat() * 0.18f) * exp(-2.1 * thunderCycle).toFloat()
        } else 0f
        heavyRain + howl + thunder
      }
      WeatherType.ASH_FOG -> {
        // Blizzard / wasteland wind howl with dual modulated formants
        val windGust = (0.50 + 0.50 * sin(2.0 * PI * 0.24 * t)).toFloat()
        val primaryFreq = 220.0 + 130.0 * sin(2.0 * PI * 0.19 * t)
        val secondaryFreq = 350.0 + 160.0 * sin(2.0 * PI * 0.13 * t + 1.1)
        val whistle = (sin(2.0 * PI * primaryFreq * t) * 0.13 + sin(2.0 * PI * secondaryFreq * t) * 0.08).toFloat() * windGust
        val icyWindNoise = (baseNoise * 0.14f + deepNoise * 0.18f) * (0.60f + 0.40f * windGust)
        whistle + icyWindNoise
      }
    }
  }

  fun synthesizeAmbientChunk(
    fromWeather: WeatherType,
    toWeather: WeatherType,
    blendStart: Float,
    blendEnd: Float,
    startSampleIdx: Long,
    frameCount: Int,
    sampleRate: Int = 22050,
  ): ShortArray {
    val out = ShortArray(frameCount)
    for (i in 0 until frameCount) {
      val t = if (frameCount > 1) i.toFloat() / (frameCount - 1) else 1f
      val alpha = (blendStart + (blendEnd - blendStart) * t).coerceIn(0f, 1f)
      val idx = startSampleIdx + i
      val s = if (fromWeather == toWeather || alpha >= 0.999f) {
        sampleForWeather(toWeather, idx, sampleRate)
      } else {
        val sPrev = sampleForWeather(fromWeather, idx, sampleRate)
        val sNext = sampleForWeather(toWeather, idx, sampleRate)
        sPrev * (1f - alpha) + sNext * alpha
      }
      out[i] = (s.coerceIn(-1f, 1f) * 9500f).toInt().toShort()
    }
    return out
  }
}

/**
 * Offline AI Tactical Audio Engine:
 * - Synthesizes procedural combat/monster PCM sound effects via AudioTrack
 * - Runs continuous ambient weather soundscapes with smooth crossfades
 * - Uses on-device Android TextToSpeech for AI Tactical Commander voice callouts
 */
class TacticalAudioEngine(context: Context) {
  private val appContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  @Volatile
  var isMuted: Boolean = false
    private set

  @Volatile
  var currentAmbientWeather: WeatherType = WeatherType.RAIN
    private set

  @Volatile
  private var previousAmbientWeather: WeatherType = WeatherType.RAIN

  @Volatile
  private var ambientBlendProgress: Float = 1f

  private var ambientLoopJob: Job? = null

  private var tts: TextToSpeech? = null
  @Volatile
  private var ttsReady = false
  private var lastVoiceTimestamp = 0L
  private val sfxCache = mutableMapOf<CombatSfx, ShortArray>()

  init {
    try {
      tts = TextToSpeech(appContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          tts?.language = Locale.US
          tts?.setPitch(0.88f)
          tts?.setSpeechRate(1.14f)
          ttsReady = true
        }
      }
    } catch (_: Throwable) {
      ttsReady = false
    }

    scope.launch {
      CombatSfx.entries.forEach { sfx ->
        sfxCache[sfx] = synthesizePcm(sfx)
      }
    }
  }

  fun setAmbientWeather(weather: WeatherType) {
    if (weather != currentAmbientWeather) {
      previousAmbientWeather = currentAmbientWeather
      currentAmbientWeather = weather
      ambientBlendProgress = 0f
    }
    if (!isMuted && (ambientLoopJob == null || ambientLoopJob?.isActive != true)) {
      startAmbientLoop()
    }
  }

  private fun startAmbientLoop() {
    ambientLoopJob?.cancel()
    ambientLoopJob = scope.launch {
      val sampleRate = 22050
      val chunkSamples = 2048 // ~93ms smooth streaming frames
      val minBuf = runCatching {
        AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        )
      }.getOrDefault(chunkSamples * 4)
      val bufSize = maxOf(minBuf, chunkSamples * 4)

      val track = runCatching {
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(bufSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      }.getOrNull()

      if (track == null) return@launch
      try {
        track.play()
        var sampleCursor = 0L
        while (isActive && !isMuted) {
          val startAlpha = ambientBlendProgress
          val nextAlpha = (startAlpha + 0.065f).coerceAtMost(1f) // ~1.4s crossfade across ~15 chunks
          ambientBlendProgress = nextAlpha
          val pcm = AmbientWeatherSynth.synthesizeAmbientChunk(
            fromWeather = previousAmbientWeather,
            toWeather = currentAmbientWeather,
            blendStart = startAlpha,
            blendEnd = nextAlpha,
            startSampleIdx = sampleCursor,
            frameCount = chunkSamples,
            sampleRate = sampleRate,
          )
          sampleCursor += chunkSamples
          val written = track.write(pcm, 0, pcm.size)
          if (written < 0) break
        }
      } catch (_: Throwable) {
      } finally {
        runCatching {
          track.pause()
          track.flush()
          track.release()
        }
      }
    }
  }

  fun toggleMute(): Boolean {
    isMuted = !isMuted
    if (isMuted) {
      ambientLoopJob?.cancel()
      ambientLoopJob = null
      try {
        tts?.stop()
      } catch (_: Throwable) {
      }
    } else {
      ambientBlendProgress = 0f
      previousAmbientWeather = WeatherType.CLEAR
      startAmbientLoop()
      speak("Tactical audio online.", force = true)
    }
    return isMuted
  }

  fun playSfx(sfx: CombatSfx) {
    if (isMuted) return
    scope.launch {
      val samples = sfxCache[sfx] ?: synthesizePcm(sfx).also { sfxCache[sfx] = it }
      playSamples(samples)
    }
  }

  fun speak(message: String, force: Boolean = false) {
    if (isMuted || !ttsReady) return
    val now = System.currentTimeMillis()
    if (!force && now - lastVoiceTimestamp < 2600L) return
    lastVoiceTimestamp = now
    try {
      tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "ai_callout_$now")
    } catch (_: Throwable) {
    }
  }

  private fun playSamples(samples: ShortArray) {
    val sampleRate = 22050
    val byteCount = samples.size * 2
    try {
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(byteCount)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(samples, 0, samples.size)
      track.play()
      Thread.sleep((samples.size * 1000L / sampleRate) + 35L)
      track.release()
    } catch (_: Throwable) {
    }
  }

  private fun synthesizePcm(sfx: CombatSfx): ShortArray {
    val sampleRate = 22050
    val durationSec = when (sfx) {
      CombatSfx.BOSS_SIREN -> 0.48f
      CombatSfx.VICTORY -> 0.45f
      CombatSfx.HEAVY_IMPACT -> 0.26f
      else -> 0.16f
    }
    val count = (sampleRate * durationSec).toInt()
    val out = ShortArray(count)
    val rng = Random(sfx.ordinal * 97 + 13)

    for (i in 0 until count) {
      val t = i.toFloat() / sampleRate
      val norm = i.toFloat() / count
      val env = exp(-4.5f * norm)
      val wave = when (sfx) {
        CombatSfx.SWORD_SLASH -> {
          val freq = 900f - 620f * norm
          val sweep = sin(2.0 * PI * freq * t).toFloat()
          val hiss = (rng.nextFloat() * 2f - 1f) * 0.45f
          (sweep * 0.5f + hiss) * env
        }
        CombatSfx.BOW_SHOT -> {
          val twang = sin(2.0 * PI * (340f - 140f * norm) * t).toFloat()
          val air = (rng.nextFloat() * 2f - 1f) * 0.25f * (1f - norm)
          (twang * 0.7f + air) * env
        }
        CombatSfx.PLASMA_SHOT -> {
          val freq = 1450f * exp(-2.2f * norm) + 180f
          sin(2.0 * PI * freq * t).toFloat() * env * 0.75f
        }
        CombatSfx.HEAVY_IMPACT -> {
          val sub = sin(2.0 * PI * (120f - 70f * norm) * t).toFloat()
          val crunch = (rng.nextFloat() * 2f - 1f) * 0.55f
          (sub * 0.8f + crunch) * exp(-5.0f * norm)
        }
        CombatSfx.ENEMY_HIT -> {
          val punch = sin(2.0 * PI * (220f - 120f * norm) * t).toFloat()
          punch * exp(-6.5f * norm) * 0.8f
        }
        CombatSfx.PLAYER_HURT -> {
          val buzz = if (sin(2.0 * PI * 155f * t) > 0) 0.55f else -0.55f
          buzz * exp(-4.0f * norm)
        }
        CombatSfx.DODGE_WHOOSH -> {
          val noise = rng.nextFloat() * 2f - 1f
          val bell = sin(PI * norm).toFloat()
          noise * bell * 0.42f
        }
        CombatSfx.BOSS_SIREN -> {
          val sirenFreq = 440f + 220f * sin(2.0 * PI * 4.5f * t).toFloat()
          sin(2.0 * PI * sirenFreq * t).toFloat() * 0.5f * (1f - norm * 0.3f)
        }
        CombatSfx.HEAL_CHIME -> {
          val f = if (norm < 0.5f) 523.25f else 783.99f
          sin(2.0 * PI * f * t).toFloat() * 0.5f * exp(-2.5f * norm)
        }
        CombatSfx.VICTORY -> {
          val f = when {
            norm < 0.33f -> 440f
            norm < 0.66f -> 554.37f
            else -> 659.25f
          }
          sin(2.0 * PI * f * t).toFloat() * 0.55f * (1f - norm * 0.35f)
        }
      }
      out[i] = (wave.coerceIn(-1f, 1f) * 14000f).toInt().toShort()
    }
    return out
  }

  fun release() {
    ambientLoopJob?.cancel()
    scope.cancel()
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (_: Throwable) {
    }
  }
}
