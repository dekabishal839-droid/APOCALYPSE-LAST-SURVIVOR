package com.example.model

enum class AbilityBranch(
  val title: String,
  val subtitle: String,
) {
  VANGUARD("VANGUARD", "Combat Damage & Stamina Strikes"),
  PHANTOM("PHANTOM", "Evasion, Counters & Ult Charge"),
  BIO_GUARD("BIO-GUARD", "Vitality, Regen & Base Defense"),
}

data class AbilityNode(
  val id: String,
  val name: String,
  val branch: AbilityBranch,
  val tier: Int, // 1..3
  val pointCost: Int,
  val prereqId: String?,
  val effectLabel: String,
  val description: String,
)

data class AbilityBonusSummary(
  val damageMultiplier: Float = 1.0f,
  val critBonusPct: Int = 0,
  val specialStaminaDiscount: Float = 0f,
  val armorPierceBonus: Float = 0f,
  val dodgeShieldBonusTicks: Int = 0,
  val dodgeCostReduction: Int = 0,
  val counterDamageBonus: Float = 0f,
  val sprintReachBonus: Float = 0f,
  val ultChargeBonus: Int = 0,
  val maxHpBonus: Int = 0,
  val healMultiplier: Float = 1.0f,
  val staminaRegenBonus: Int = 0,
  val drainResistance: Boolean = false,
  val damageMitigationBonus: Float = 0f,
  val turretDamageMultiplier: Float = 1.0f,
)

object AbilityTreeCatalog {
  val nodes: List<AbilityNode> = listOf(
    // Branch 1: VANGUARD
    AbilityNode(
      id = "vanguard_edge",
      name = "Razor Edge",
      branch = AbilityBranch.VANGUARD,
      tier = 1,
      pointCost = 1,
      prereqId = null,
      effectLabel = "+15% Attack Damage",
      description = "Hones weapon technique across swords, bows, and plasma rifles.",
    ),
    AbilityNode(
      id = "vanguard_momentum",
      name = "Relentless Momentum",
      branch = AbilityBranch.VANGUARD,
      tier = 2,
      pointCost = 1,
      prereqId = "vanguard_edge",
      effectLabel = "-20% Special Move ST",
      description = "Reduces Stamina consumed by Sprint Attacks and Heavy Overhead strikes.",
    ),
    AbilityNode(
      id = "vanguard_executioner",
      name = "Apex Executioner",
      branch = AbilityBranch.VANGUARD,
      tier = 3,
      pointCost = 2,
      prereqId = "vanguard_momentum",
      effectLabel = "+12% Crit • +20% Pierce",
      description = "Tears through armored monsters and Giant Boss plating.",
    ),

    // Branch 2: PHANTOM
    AbilityNode(
      id = "phantom_reflex",
      name = "Adrenal Reflex",
      branch = AbilityBranch.PHANTOM,
      tier = 1,
      pointCost = 1,
      prereqId = null,
      effectLabel = "+4 Dodge Frames • -2 ST",
      description = "Extends Tactical Dodge invulnerability and lowers Stamina cost.",
    ),
    AbilityNode(
      id = "phantom_riposte",
      name = "Lethal Riposte",
      branch = AbilityBranch.PHANTOM,
      tier = 2,
      pointCost = 1,
      prereqId = "phantom_reflex",
      effectLabel = "+50% Counter DMG",
      description = "Upgrades Perfect Dodge Counter Stance from +25% to +50% bonus damage.",
    ),
    AbilityNode(
      id = "phantom_ghost",
      name = "Storm Strider",
      branch = AbilityBranch.PHANTOM,
      tier = 3,
      pointCost = 2,
      prereqId = "phantom_riposte",
      effectLabel = "+20% Dash Reach • +8% Ult",
      description = "Extends Sprint/Dodge stride distance and accelerates Squad Ultimate charge.",
    ),

    // Branch 3: BIO-GUARD
    AbilityNode(
      id = "bioguard_metabolism",
      name = "Iron Metabolism",
      branch = AbilityBranch.BIO_GUARD,
      tier = 1,
      pointCost = 1,
      prereqId = null,
      effectLabel = "+25 Max HP • +25% Heal",
      description = "Strengthens survivor vitality and boosts Medkit & ration recovery.",
    ),
    AbilityNode(
      id = "bioguard_resilience",
      name = "Wasteland Immunity",
      branch = AbilityBranch.BIO_GUARD,
      tier = 2,
      pointCost = 1,
      prereqId = "bioguard_metabolism",
      effectLabel = "+1 ST Regen • -50% Drain",
      description = "Halves hunger/thirst drain in storms and speeds stamina recovery.",
    ),
    AbilityNode(
      id = "bioguard_bastion",
      name = "Sanctuary Bastion",
      branch = AbilityBranch.BIO_GUARD,
      tier = 3,
      pointCost = 2,
      prereqId = "bioguard_resilience",
      effectLabel = "+25% Armor • +30% Turrets",
      description = "Hardens survivor armor and supercharges Base Laser Turret wave defense.",
    ),
  )

  fun findById(id: String): AbilityNode? = nodes.firstOrNull { it.id == id }

  fun parseUnlockedIds(raw: String): Set<String> =
    raw.split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() && findById(it) != null }
      .toSet()

  fun encodeUnlockedIds(ids: Set<String>): String =
    ids.filter { findById(it) != null }.sorted().joinToString(",")

  fun totalEarnedPoints(level: Int, missionsCompleted: Int): Int =
    (level + 1 + (missionsCompleted / 5)).coerceAtLeast(2)

  fun spentPoints(unlockedIds: Set<String>): Int =
    unlockedIds.sumOf { id -> findById(id)?.pointCost ?: 0 }

  fun availablePoints(level: Int, missionsCompleted: Int, unlockedIds: Set<String>): Int =
    (totalEarnedPoints(level, missionsCompleted) - spentPoints(unlockedIds)).coerceAtLeast(0)

  fun canUnlock(
    node: AbilityNode,
    unlockedIds: Set<String>,
    availablePoints: Int,
  ): Boolean {
    if (node.id in unlockedIds) return false
    if (availablePoints < node.pointCost) return false
    val prereq = node.prereqId ?: return true
    return prereq in unlockedIds
  }

  fun summarize(unlockedIds: Set<String>): AbilityBonusSummary {
    var damageMult = 1.0f
    var critBonus = 0
    var staminaDiscount = 0f
    var armorPierce = 0f
    var dodgeShield = 0
    var dodgeCostRed = 0
    var counterBonus = 0f
    var sprintReach = 0f
    var ultBonus = 0
    var maxHp = 0
    var healMult = 1.0f
    var staminaRegen = 0
    var drainResist = false
    var mitigation = 0f
    var turretMult = 1.0f

    if ("vanguard_edge" in unlockedIds) damageMult += 0.15f
    if ("vanguard_momentum" in unlockedIds) staminaDiscount = 0.20f
    if ("vanguard_executioner" in unlockedIds) {
      critBonus += 12
      armorPierce += 0.20f
    }

    if ("phantom_reflex" in unlockedIds) {
      dodgeShield += 4
      dodgeCostRed += 2
    }
    if ("phantom_riposte" in unlockedIds) counterBonus += 0.25f
    if ("phantom_ghost" in unlockedIds) {
      sprintReach += 0.08f
      ultBonus += 8
    }

    if ("bioguard_metabolism" in unlockedIds) {
      maxHp += 25
      healMult += 0.25f
    }
    if ("bioguard_resilience" in unlockedIds) {
      staminaRegen += 1
      drainResist = true
    }
    if ("bioguard_bastion" in unlockedIds) {
      mitigation += 0.25f
      turretMult += 0.30f
    }

    return AbilityBonusSummary(
      damageMultiplier = damageMult,
      critBonusPct = critBonus,
      specialStaminaDiscount = staminaDiscount,
      armorPierceBonus = armorPierce,
      dodgeShieldBonusTicks = dodgeShield,
      dodgeCostReduction = dodgeCostRed,
      counterDamageBonus = counterBonus,
      sprintReachBonus = sprintReach,
      ultChargeBonus = ultBonus,
      maxHpBonus = maxHp,
      healMultiplier = healMult,
      staminaRegenBonus = staminaRegen,
      drainResistance = drainResist,
      damageMitigationBonus = mitigation,
      turretDamageMultiplier = turretMult,
    )
  }
}
