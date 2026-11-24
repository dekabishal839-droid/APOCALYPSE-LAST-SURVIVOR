package com.example.model

import kotlin.math.hypot

/**
 * Single source of truth for physical contact/touch hitboxes across
 * Player attacks, Normal Enemies, Giant Bosses, and the Base Sanctuary perimeter.
 *
 * Attacks that do not physically touch/reach the target hitbox deal ZERO damage.
 */
object CombatHitbox {
  const val PLAYER_BODY_RADIUS = 0.048f
  const val ENEMY_BODY_RADIUS = 0.068f
  const val BOSS_BODY_RADIUS = 0.098f
  const val PROJECTILE_RADIUS = 0.024f

  fun targetBodyRadius(isBoss: Boolean): Float =
    if (isBoss) BOSS_BODY_RADIUS else ENEMY_BODY_RADIUS

  fun playerMeleeTouchRadius(moveType: CombatMoveType, isBoss: Boolean): Float {
    val weaponReach = when (moveType) {
      CombatMoveType.PRIMARY -> 0.165f
      CombatMoveType.SPRINT_STRIKE -> 0.195f
      CombatMoveType.HEAVY_OVERHEAD -> 0.215f
      CombatMoveType.DODGE -> 0f
    }
    return weaponReach + targetBodyRadius(isBoss)
  }

  fun doesPlayerMeleeTouchTarget(
    playerX: Float,
    playerY: Float,
    enemyX: Float,
    enemyY: Float,
    isBoss: Boolean,
    moveType: CombatMoveType,
  ): Boolean {
    if (moveType == CombatMoveType.DODGE) return false
    val dist = hypot((enemyX - playerX).toDouble(), (enemyY - playerY).toDouble()).toFloat()
    return dist <= playerMeleeTouchRadius(moveType, isBoss)
  }

  fun doesProjectileTouchTarget(
    projX: Float,
    projY: Float,
    targetX: Float,
    targetY: Float,
    isBoss: Boolean,
    extraRadius: Float = 0f,
  ): Boolean {
    val dist = hypot((projX - targetX).toDouble(), (projY - targetY).toDouble()).toFloat()
    return dist <= (targetBodyRadius(isBoss) + PROJECTILE_RADIUS + extraRadius)
  }

  fun doesProjectileTouchPlayer(
    projX: Float,
    projY: Float,
    playerX: Float,
    playerY: Float,
  ): Boolean {
    val dist = hypot((projX - playerX).toDouble(), (projY - playerY).toDouble()).toFloat()
    return dist <= (PLAYER_BODY_RADIUS + PROJECTILE_RADIUS)
  }

  fun enemyMeleeTouchRadius(isBoss: Boolean): Float {
    val enemyReach = if (isBoss) 0.195f else 0.155f
    return enemyReach + PLAYER_BODY_RADIUS
  }

  fun doesEnemyMeleeTouchPlayer(
    enemyX: Float,
    enemyY: Float,
    playerX: Float,
    playerY: Float,
    isBoss: Boolean,
  ): Boolean {
    val dist = hypot((playerX - enemyX).toDouble(), (playerY - enemyY).toDouble()).toFloat()
    return dist <= enemyMeleeTouchRadius(isBoss)
  }

  fun doesWorldEnemyTouchPlayer(
    enemyX: Float,
    enemyY: Float,
    playerX: Float,
    playerY: Float,
    isBoss: Boolean,
  ): Boolean {
    val dist = hypot((playerX - enemyX).toDouble(), (playerY - enemyY).toDouble()).toFloat()
    val touchLimit = if (isBoss) 0.098f else 0.082f
    return dist <= touchLimit
  }
}

/**
 * Base Sanctuary boundary and global-difficulty-scaled wave siege formulas.
 */
object BaseZoneRules {
  const val BASE_CENTER_X = 0.50f
  const val BASE_CENTER_Y = 0.56f
  const val BASE_RADIUS = 0.17f
  const val WAVE_COUNTDOWN_INITIAL_SEC = 8
  const val WAVE_COUNTDOWN_NEXT_SEC = 7

  fun isInsideBase(x: Float, y: Float): Boolean {
    val dist = hypot((x - BASE_CENTER_X).toDouble(), (y - BASE_CENTER_Y).toDouble()).toFloat()
    return dist <= BASE_RADIUS
  }

  fun maxBaseIntegrity(bunkerLevel: Int): Int =
    100 + (bunkerLevel - 1).coerceAtLeast(0) * 40

  fun turretBarrageDamage(turretLevel: Int): Int =
    if (turretLevel <= 0) 0 else 16 + turretLevel * 12

  fun computeWaveEnemyCount(
    waveNumber: Int,
    difficulty: DifficultyState,
  ): Int {
    val threatBonus = ((difficulty.overallThreatMultiplier - 1.0f) * 3.0f).toInt().coerceAtLeast(0)
    return (2 + (waveNumber - 1) / 2 + threatBonus).coerceIn(2, 6)
  }

  fun computeWaveEnemyLevel(
    playerLevel: Int,
    waveNumber: Int,
    difficulty: DifficultyState,
  ): Int {
    val threatLevels = ((difficulty.overallThreatMultiplier - 1.0f) * 2.5f).toInt().coerceAtLeast(0)
    return (playerLevel + waveNumber / 2 + threatLevels).coerceAtLeast(1)
  }

  fun isBossWave(waveNumber: Int, difficulty: DifficultyState): Boolean =
    waveNumber % 3 == 0 || difficulty.threatTier == ThreatTier.NIGHTMARE
}
