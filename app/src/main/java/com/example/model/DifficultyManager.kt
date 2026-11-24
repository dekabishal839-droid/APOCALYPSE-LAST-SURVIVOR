package com.example.model

import com.example.data.local.PlayerStats

enum class ThreatTier(val label: String) {
  LOW("LOW"),
  ELEVATED("ELEVATED"),
  HIGH("HIGH"),
  EXTREME("EXTREME"),
  NIGHTMARE("NIGHTMARE"),
}

data class DifficultyState(
  val speedMultiplier: Float = 1.0f,
  val damageMultiplier: Float = 1.0f,
  val detectionRangeMultiplier: Float = 1.0f,
  val levelScaleFactor: Float = 1.0f,
  val overallThreatMultiplier: Float = 1.0f,
  val threatProgress: Float = 0.12f,
  val threatTier: ThreatTier = ThreatTier.LOW,
  val elapsedSeconds: Long = 0L,
  val vitalsPenaltyPercent: Int = 0,
  val timeBonusPercent: Int = 0,
  val noiseLevel: Int = 12,
  val proximityPercent: Int = 0,
  val nearestEnemyDistanceMeters: Int = 99,
  val nearbyHostileCount: Int = 0,
  val liveAlertFactor: Float = 0.08f,
) {
  val speedBonusPct: Int get() = ((speedMultiplier - 1f) * 100f).toInt().coerceAtLeast(0)
  val damageBonusPct: Int get() = ((damageMultiplier - 1f) * 100f).toInt().coerceAtLeast(0)
  val rangeBonusPct: Int get() = ((detectionRangeMultiplier - 1f) * 100f).toInt().coerceAtLeast(0)
}

/**
 * Balanced Global Difficulty Manager:
 * Escalates enemy speed, damage, HP/armor, and detection range as player survival
 * stats decline, time progresses, player level increases, and live enemy proximity
 * or player acoustic noise spikes occur.
 */
object DifficultyManager {
  fun evaluate(
    stats: PlayerStats,
    elapsedSeconds: Long,
    playerLevel: Int,
    weather: WeatherType,
    biome: BiomeType,
    noiseLevel: Float = 12f,
    nearestEnemyDistance: Float = 1.0f,
    nearbyHostileCount: Int = 0,
  ): DifficultyState {
    val hpRatio = (stats.health.toFloat() / stats.maxHealth.coerceAtLeast(1)).coerceIn(0f, 1f)
    val hungerRatio = (stats.hunger / 100f).coerceIn(0f, 1f)
    val thirstRatio = (stats.thirst / 100f).coerceIn(0f, 1f)
    val staminaRatio = (stats.stamina / 100f).coerceIn(0f, 1f)

    // Gentle survival deficit curve so low vitals feel tense without overwhelming the player
    val hpDeficit = (1f - hpRatio) * 0.20f
    val hungerDeficit = (1f - hungerRatio) * 0.16f
    val thirstDeficit = (1f - thirstRatio) * 0.14f
    val staminaDeficit = (1f - staminaRatio) * 0.05f
    val criticalShock = (if (stats.hunger < 20) 0.08f else 0f) +
      (if (stats.thirst < 20) 0.08f else 0f) +
      (if (hpRatio < 0.30f) 0.08f else 0f)

    val vitalsStress = (hpDeficit + hungerDeficit + thirstDeficit + staminaDeficit + criticalShock).coerceIn(0f, 0.72f)

    val minutesSurvived = elapsedSeconds / 60f
    val timePressure = (minutesSurvived * 0.06f).coerceIn(0f, 0.50f)

    val lvOffset = (playerLevel - 1).coerceAtLeast(0)
    val levelScaleFactor = 1f + lvOffset * 0.09f
    val levelSpeedBoost = (lvOffset * 0.025f).coerceAtMost(0.30f)
    val levelDetectionBoost = (lvOffset * 0.02f).coerceAtMost(0.25f)

    val biomeBoost = (biome.dangerTier - 1) * 0.05f
    val weatherAggroBoost = when (weather) {
      WeatherType.BIO_STORM -> 0.12f
      WeatherType.RAIN -> 0.04f
      WeatherType.ASH_FOG -> -0.10f
      WeatherType.CLEAR -> 0f
    }
    val weatherDamageBoost = if (weather == WeatherType.BIO_STORM) 0.08f else 0f

    // Real-time enemy proximity & acoustic noise signature
    val clampedNoise = noiseLevel.coerceIn(0f, 100f)
    val noiseRatio = clampedNoise / 100f
    val proximityRatio = ((0.55f - nearestEnemyDistance.coerceIn(0f, 0.55f)) / 0.55f).coerceIn(0f, 1f)
    val swarmBonus = (nearbyHostileCount.coerceAtLeast(0) * 0.08f).coerceAtMost(0.24f)
    val liveAlertFactor = (proximityRatio * 0.52f + noiseRatio * 0.36f + swarmBonus).coerceIn(0f, 1f)

    val noiseRangeBoost = noiseRatio * 0.14f
    val proximitySpeedBoost = proximityRatio * 0.06f

    val speedMult = (1f + vitalsStress * 0.32f + timePressure * 0.28f + levelSpeedBoost + biomeBoost + proximitySpeedBoost)
      .coerceIn(1.0f, 1.65f)
    val damageMult = (1f + vitalsStress * 0.38f + timePressure * 0.34f + (levelScaleFactor - 1f) * 0.45f + biomeBoost + weatherDamageBoost)
      .coerceIn(1.0f, 1.85f)
    val rangeMult = (1f + vitalsStress * 0.28f + timePressure * 0.22f + levelDetectionBoost + weatherAggroBoost + noiseRangeBoost)
      .coerceIn(0.88f, 1.55f)

    val overall = ((speedMult + damageMult + rangeMult) / 3f)
    val baseProgress = ((overall - 1f) / 0.75f).coerceIn(0.05f, 1f)
    val threatProgress = (baseProgress * 0.62f + liveAlertFactor * 0.38f).coerceIn(0.05f, 1f)
    val effectiveThreatScore = overall + liveAlertFactor * 0.22f
    val tier = when {
      effectiveThreatScore >= 1.55f -> ThreatTier.NIGHTMARE
      effectiveThreatScore >= 1.40f -> ThreatTier.EXTREME
      effectiveThreatScore >= 1.26f -> ThreatTier.HIGH
      effectiveThreatScore >= 1.12f -> ThreatTier.ELEVATED
      else -> ThreatTier.LOW
    }

    val meters = (nearestEnemyDistance.coerceIn(0f, 1f) * 90f).toInt().coerceIn(1, 99)

    return DifficultyState(
      speedMultiplier = speedMult,
      damageMultiplier = damageMult,
      detectionRangeMultiplier = rangeMult,
      levelScaleFactor = levelScaleFactor,
      overallThreatMultiplier = overall,
      threatProgress = threatProgress,
      threatTier = tier,
      elapsedSeconds = elapsedSeconds,
      vitalsPenaltyPercent = (vitalsStress * 100f).toInt(),
      timeBonusPercent = (timePressure * 100f).toInt(),
      noiseLevel = clampedNoise.toInt(),
      proximityPercent = (proximityRatio * 100f).toInt(),
      nearestEnemyDistanceMeters = meters,
      nearbyHostileCount = nearbyHostileCount,
      liveAlertFactor = liveAlertFactor,
    )
  }
}
