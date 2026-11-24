package com.example.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

enum class EnemyType(
  val displayTag: String,
  val baseSpeed: Float,
  val aggroRadius: Float,
  val baseArmor: Int,
  val specialMoveName: String,
) {
  STALKER("Virus Stalker", 0.0095f, 0.28f, 1, "Pounce Lunge"),
  RAVAGER("Spore Ravager", 0.0090f, 0.27f, 2, "Toxic Spore Burst"),
  BRUTE("Armored Brute", 0.0075f, 0.25f, 5, "Concrete Slam"),
  GOLEM("Frost Golem", 0.0070f, 0.25f, 7, "Glacial Crush"),
  CRYO_MUTANT("Cryo Mutant", 0.0098f, 0.30f, 4, "Bio-Plasma Slash"),
  GIANT_BOSS("Apex Giant Boss", 0.0080f, 0.33f, 8, "Cataclysm Shockwave"),
}

enum class EnemyAiState(val badge: String) {
  PATROL("PATROL"),
  ALERT("ALERT!"),
  CHASE("CHASING!"),
  TELEGRAPH("STRIKING"),
  ENRAGED("ENRAGED"),
}

data class Enemy(
  val id: String,
  val name: String,
  val type: EnemyType,
  val biome: BiomeType,
  val level: Int = 1,
  val x: Float,
  val y: Float,
  val patrolAnchorX: Float = x,
  val patrolAnchorY: Float = y,
  val patrolAngleRad: Float = Random.nextFloat() * 6.28f,
  val hp: Int,
  val maxHp: Int,
  val attackPower: Int,
  val armor: Int = type.baseArmor,
  val aiState: EnemyAiState = EnemyAiState.PATROL,
  val telegraphProgress: Float = 0.08f,
  val actionCooldownTicks: Int = 10,
  val isBoss: Boolean = type == EnemyType.GIANT_BOSS,
  val isBaseRaider: Boolean = false,
) {
  val isEnraged: Boolean
    get() = isBoss && hp <= (maxHp * 0.35f)

  val activeSpecialMove: String
    get() = if (isEnraged) "ENRAGED ${type.specialMoveName}" else type.specialMoveName

  /**
   * High-frequency (~80ms) world AI tick:
   * - Smoothly patrols around anchor
   * - Detects player inside difficulty-scaled aggro ring
   * - Pursues actively with fair speed and occasional close-range harass
   */
  fun stepWorldAi(
    player: Player,
    weather: WeatherType,
    difficulty: DifficultyState,
  ): Triple<Enemy, Boolean, Int> {
    val dx = player.x - x
    val dy = player.y - y
    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

    val sightMod = when (weather) {
      WeatherType.ASH_FOG -> 0.85f
      WeatherType.BIO_STORM -> 1.12f
      else -> 1.0f
    }
    val weatherNoiseDamp = if (weather == WeatherType.ASH_FOG) 0.75f else 1.0f
    val acousticExtension = ((difficulty.noiseLevel / 100f) * 0.14f * weatherNoiseDamp)
    val effectiveAggro = if (isBaseRaider) {
      0.95f
    } else {
      ((type.aggroRadius + acousticExtension) * sightMod * difficulty.detectionRangeMultiplier).coerceIn(0.18f, 0.58f)
    }

    if (CombatHitbox.doesWorldEnemyTouchPlayer(x, y, player.x, player.y, isBoss) && dist < 0.064f) {
      return Triple(copy(aiState = EnemyAiState.CHASE), true, 0)
    }

    val nextCooldown = (actionCooldownTicks - 1).coerceAtLeast(0)

    if (dist <= effectiveAggro) {
      val chaseSpeed = type.baseSpeed * difficulty.speedMultiplier *
        when {
          isBaseRaider -> 1.10f
          aiState == EnemyAiState.PATROL -> 0.72f
          else -> 1.05f
        }
      val nx = (x + (dx / dist) * chaseSpeed).coerceIn(0.06f, 0.94f)
      val ny = (y + (dy / dist) * chaseSpeed).coerceIn(0.14f, 0.88f)
      val nextDist = hypot((player.x - nx).toDouble(), (player.y - ny).toDouble()).toFloat()
      val nextState = if (!isBaseRaider && dist > effectiveAggro * 0.78f && aiState == EnemyAiState.PATROL) {
        EnemyAiState.ALERT
      } else {
        EnemyAiState.CHASE
      }

      var harassDamage = 0
      var resetCooldown = nextCooldown
      val touchesPlayer = CombatHitbox.doesWorldEnemyTouchPlayer(nx, ny, player.x, player.y, isBoss)
      if (touchesPlayer && nextCooldown == 0) {
        harassDamage = ((attackPower * 0.22f) * difficulty.damageMultiplier).toInt().coerceAtLeast(3)
        resetCooldown = 34
      }

      return Triple(
        copy(
          x = nx,
          y = ny,
          aiState = nextState,
          actionCooldownTicks = resetCooldown,
        ),
        nextDist < 0.064f,
        harassDamage,
      )
    }

    val nextAngle = (patrolAngleRad + 0.07f * difficulty.speedMultiplier) % 6.283185f
    val orbitRadius = 0.085f
    val targetX = (patrolAnchorX + cos(nextAngle) * orbitRadius).coerceIn(0.08f, 0.92f)
    val targetY = (patrolAnchorY + sin(nextAngle * 1.3f) * (orbitRadius * 0.75f)).coerceIn(0.18f, 0.86f)
    val pdx = targetX - x
    val pdy = targetY - y
    val pDist = hypot(pdx.toDouble(), pdy.toDouble()).toFloat().coerceAtLeast(0.001f)
    val patrolStep = (type.baseSpeed * 0.62f * difficulty.speedMultiplier).coerceAtMost(pDist)
    val nx = (x + (pdx / pDist) * patrolStep).coerceIn(0.08f, 0.92f)
    val ny = (y + (pdy / pDist) * patrolStep).coerceIn(0.18f, 0.86f)

    return Triple(
      copy(
        x = nx,
        y = ny,
        patrolAngleRad = nextAngle,
        aiState = EnemyAiState.PATROL,
        actionCooldownTicks = nextCooldown,
      ),
      false,
      0,
    )
  }

  data class CombatAiStepResult(
    val enemy: Enemy,
    val nextX: Float,
    val nextY: Float,
    val spawnDangerZone: Boolean,
    val fireProjectile: Boolean,
    val strikePlayerDirect: Boolean,
    val missedDirectStrike: Boolean = false,
  )

  fun stepCombatAi(
    enemyX: Float,
    enemyY: Float,
    playerX: Float,
    playerY: Float,
    difficulty: DifficultyState,
  ): CombatAiStepResult {
    val dx = playerX - enemyX
    val dy = playerY - enemyY
    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.001f)

    val preferredDist = if (type == EnemyType.RAVAGER || type == EnemyType.CRYO_MUTANT) 0.34f else 0.19f
    val moveSpeed = type.baseSpeed * 0.85f * difficulty.speedMultiplier * (if (isEnraged) 1.22f else 1.0f)

    val (moveDx, moveDy) = when {
      dist > preferredDist -> (dx / dist) * moveSpeed to (dy / dist) * moveSpeed
      dist < preferredDist * 0.70f -> -(dx / dist) * moveSpeed * 0.5f to -(dy / dist) * moveSpeed * 0.5f
      else -> {
        val perpAngle = atan2(dy.toDouble(), dx.toDouble()) + 1.57
        (cos(perpAngle).toFloat() * moveSpeed * 0.42f) to (sin(perpAngle).toFloat() * moveSpeed * 0.42f)
      }
    }

    val nx = (enemyX + moveDx).coerceIn(0.16f, 0.86f)
    val ny = (enemyY + moveDy).coerceIn(0.26f, 0.80f)

    // Balanced telegraph charge rate so player has clear time to strike & dodge
    val baseChargeRate = when {
      isEnraged -> 0.036f
      isBoss -> 0.028f
      type == EnemyType.STALKER || type == EnemyType.CRYO_MUTANT -> 0.029f
      else -> 0.024f
    }
    val nextProgress = telegraphProgress + baseChargeRate * difficulty.speedMultiplier
    val strikeTriggered = nextProgress >= 1.0f

    val nextCooldown = (actionCooldownTicks - 1).coerceAtLeast(0)
    val shouldFireProjectile = !strikeTriggered && nextCooldown == 0 && (isBoss || dist > 0.25f)
    val shouldSpawnZone = !strikeTriggered && nextProgress in 0.62f..0.67f && nextCooldown <= 6

    val nextState = when {
      isEnraged -> EnemyAiState.ENRAGED
      nextProgress >= 0.60f -> EnemyAiState.TELEGRAPH
      else -> EnemyAiState.CHASE
    }

    val updated = copy(
      telegraphProgress = if (strikeTriggered) 0f else nextProgress,
      aiState = nextState,
      actionCooldownTicks = if (shouldFireProjectile) (if (isEnraged) 28 else 38) else nextCooldown,
    )

    val touchesPlayer = CombatHitbox.doesEnemyMeleeTouchPlayer(nx, ny, playerX, playerY, isBoss)

    return CombatAiStepResult(
      enemy = updated,
      nextX = nx,
      nextY = ny,
      spawnDangerZone = shouldSpawnZone,
      fireProjectile = shouldFireProjectile,
      strikePlayerDirect = strikeTriggered && touchesPlayer,
      missedDirectStrike = strikeTriggered && !touchesPlayer,
    )
  }

  fun computeStrikeDamage(difficulty: DifficultyState): Int {
    val rageMult = if (isEnraged) 1.30f else 1.0f
    val variance = Random.nextInt(-2, 3)
    return ((attackPower * rageMult * difficulty.damageMultiplier).toInt() + variance).coerceAtLeast(6)
  }

  companion object {
    fun createForBiome(
      id: String,
      name: String,
      biome: BiomeType,
      isBoss: Boolean,
      x: Float,
      y: Float,
      playerLevel: Int = 1,
      difficulty: DifficultyState = DifficultyState(),
      customHp: Int? = null,
    ): Enemy {
      val type = if (isBoss) {
        EnemyType.GIANT_BOSS
      } else {
        when (biome) {
          BiomeType.DEAD_CITY -> if (name.contains("Brute", ignoreCase = true)) EnemyType.BRUTE else EnemyType.STALKER
          BiomeType.ASHWOOD_FOREST -> EnemyType.RAVAGER
          BiomeType.FROSTPEAK_RIDGE -> EnemyType.GOLEM
          BiomeType.SECTOR_9_LAB -> EnemyType.CRYO_MUTANT
        }
      }
      val levelScale = 1f + (playerLevel - 1).coerceAtLeast(0) * 0.09f
      val baseHp = customHp ?: if (isBoss) 380 + biome.dangerTier * 140 else 110 + biome.dangerTier * 42
      val scaledHp = (baseHp * levelScale).toInt()
      val baseAtk = (if (isBoss) 22 else 13) + biome.dangerTier * 4
      val scaledAtk = (baseAtk * levelScale * difficulty.damageMultiplier.coerceAtMost(1.35f)).toInt()
      val scaledArmor = (type.baseArmor + biome.dangerTier + (playerLevel - 1))

      return Enemy(
        id = id,
        name = name,
        type = type,
        biome = biome,
        level = playerLevel,
        x = x,
        y = y,
        hp = scaledHp,
        maxHp = scaledHp,
        attackPower = scaledAtk,
        armor = scaledArmor,
        isBoss = isBoss,
      )
    }
  }
}
