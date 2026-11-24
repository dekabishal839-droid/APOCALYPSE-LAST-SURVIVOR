package com.example.model

import com.example.data.local.SurvivorEntity
import com.example.data.local.WeaponEntity
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.random.Random

enum class PlayerActionState {
  IDLE,
  MOVING,
  ATTACKING,
  DODGING,
}

data class Player(
  val callsign: String = "SURVIVOR-01",
  val level: Int = 1,
  val health: Int = 100,
  val maxHealth: Int = 100,
  val stamina: Int = 100,
  val maxStamina: Int = 100,
  val hunger: Int = 65,
  val thirst: Int = 60,
  val immunity: Int = 85,
  val x: Float = 0.50f,
  val y: Float = 0.56f,
  val facingRad: Float = -1.57f,
  val actionState: PlayerActionState = PlayerActionState.IDLE,
  val defenseRating: Int = 20,
  val forgeMultiplier: Float = 1f,
  val abilityBonuses: AbilityBonusSummary = AbilityBonusSummary(),
) {
  val isCriticalVitals: Boolean
    get() = hunger < 20 || thirst < 20 || health <= (maxHealth * 0.25f)

  val staminaRegenPerTick: Int
    get() = if (thirst < 20) 1 else if (actionState == PlayerActionState.MOVING) 2 else 5

  fun staminaRegenForWeather(weather: WeatherType): Int =
    (staminaRegenPerTick + weather.staminaRegenDelta + abilityBonuses.staminaRegenBonus).coerceAtLeast(1)

  fun moveBy(dx: Float, dy: Float): Player {
    val nx = (x + dx).coerceIn(0.08f, 0.92f)
    val ny = (y + dy).coerceIn(0.16f, 0.88f)
    val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
    return copy(
      x = nx,
      y = ny,
      facingRad = angle,
      actionState = PlayerActionState.MOVING,
    )
  }

  fun moveToward(targetX: Float, targetY: Float, maxStep: Float = 0.08f): Player {
    val dx = targetX - x
    val dy = targetY - y
    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (dist < 0.01f) return copy(actionState = PlayerActionState.IDLE)
    val step = maxStep.coerceAtMost(dist)
    return moveBy((dx / dist) * step, (dy / dist) * step)
  }

  fun computeAttackDamage(
    weapon: WeaponEntity,
    weather: WeatherType,
    isHeavy: Boolean,
    enemyArmor: Int,
    moveMultiplier: Float = if (isHeavy) 2.15f else 1.0f,
    armorPierceRatio: Float = if (isHeavy) 0.50f else 0f,
  ): Pair<Int, Boolean> {
    val category = WeaponCategory.fromName(weapon.category)
    val weatherCritBonus = if (category == WeaponCategory.BOW && weather == WeatherType.ASH_FOG) 15 else 0
    val levelCrit = (level - 1).coerceAtMost(10)
    val isCrit = Random.nextInt(100) < (weapon.critChance + weatherCritBonus + levelCrit + abilityBonuses.critBonusPct)
    val weaponScale = 1f + (weapon.level - 1) * 0.22f
    val critMult = if (isCrit) 1.60f else 1.0f
    val raw = (weapon.baseDamage * weaponScale * forgeMultiplier * abilityBonuses.damageMultiplier * moveMultiplier * critMult).toInt()
    val totalPierce = (armorPierceRatio + abilityBonuses.armorPierceBonus).coerceIn(0f, 1f)
    val effectiveArmor = (enemyArmor * (1f - totalPierce)).toInt()
    val mitigated = (raw - effectiveArmor).coerceAtLeast((raw * 0.38f).toInt().coerceAtLeast(6))
    return mitigated to isCrit
  }

  fun takeDamage(rawDamage: Int, dodging: Boolean): Int {
    if (dodging) return 0
    val armorReduction = defenseRating / 25
    val afterArmor = (rawDamage - armorReduction).coerceAtLeast(8)
    val mitigated = (afterArmor * (1f - abilityBonuses.damageMitigationBonus.coerceIn(0f, 0.50f))).toInt()
    return mitigated.coerceAtLeast(6)
  }

  companion object {
    fun fromSurvivor(
      survivor: SurvivorEntity,
      x: Float,
      y: Float,
      facingRad: Float,
      defenseRating: Int,
      forgeMultiplier: Float,
      actionState: PlayerActionState = PlayerActionState.IDLE,
      abilityBonuses: AbilityBonusSummary = AbilityTreeCatalog.summarize(
        AbilityTreeCatalog.parseUnlockedIds(survivor.unlockedAbilityIds)
      ),
    ): Player = Player(
      callsign = survivor.callsign,
      level = survivor.level,
      health = survivor.hp,
      maxHealth = survivor.maxHp,
      stamina = survivor.stamina,
      maxStamina = 100,
      hunger = survivor.food,
      thirst = survivor.water,
      immunity = survivor.immunity,
      x = x,
      y = y,
      facingRad = facingRad,
      actionState = actionState,
      defenseRating = defenseRating,
      forgeMultiplier = forgeMultiplier,
      abilityBonuses = abilityBonuses,
    )
  }
}
