package com.example.model

enum class BiomeType(
  val id: String,
  val displayName: String,
  val subtitle: String,
  val dangerTier: Int,
) {
  DEAD_CITY("DEAD_CITY", "Shattered Metro", "Ruined 2045 Skyscrapers & Stalker Packs", 1),
  ASHWOOD_FOREST("ASHWOOD_FOREST", "Ashwood Forest", "Mutated Pines, Timber & Spore Beasts", 2),
  FROSTPEAK_RIDGE("FROSTPEAK_RIDGE", "Frostpeak Ridge", "Frozen Peaks, Titanium Ore & Rock Golems", 3),
  SECTOR_9_LAB("SECTOR_9_LAB", "Sector-9 Lab", "Underground Bio-Bunker & Apex Titans", 4);

  companion object {
    fun fromId(id: String): BiomeType = entries.firstOrNull { it.id == id } ?: DEAD_CITY
  }
}

enum class WeatherType(
  val index: Int,
  val displayName: String,
  val gameplayEffect: String,
  val combatImpactLabel: String,
  val survivalImpactLabel: String,
  val lootImpactLabel: String,
  val staminaRegenDelta: Int,
  val ambientAudioLabel: String,
) {
  CLEAR(
    index = 0,
    displayName = "Clear Sky",
    gameplayEffect = "Normal visibility & +1 Stamina regen",
    combatImpactLabel = "Steady Aim • Normal DMG",
    survivalImpactLabel = "+1 Stamina Regen / tick",
    lootImpactLabel = "Standard Scavenge",
    staminaRegenDelta = 1,
    ambientAudioLabel = "Mild Breeze",
  ),
  RAIN(
    index = 1,
    displayName = "Acid Rain",
    gameplayEffect = "+40% Purifier yield, slick terrain",
    combatImpactLabel = "+4% Enemy Aggro • Slick Footing",
    survivalImpactLabel = "Normal Stamina • +40% Purifier",
    lootImpactLabel = "+40% Camp Water Yield",
    staminaRegenDelta = 0,
    ambientAudioLabel = "Steady Storm Rain",
  ),
  BIO_STORM(
    index = 2,
    displayName = "Bio-Storm",
    gameplayEffect = "Lightning hazard, +25% Crystal drops",
    combatImpactLabel = "+8% Enemy DMG • +12% Aggro",
    survivalImpactLabel = "-1 Stamina Regen • +2x Drain",
    lootImpactLabel = "+25% Bio-Crystal Drops",
    staminaRegenDelta = -1,
    ambientAudioLabel = "Heavy Rain & Thunder",
  ),
  ASH_FOG(
    index = 3,
    displayName = "Ash Fog",
    gameplayEffect = "Stealth bow crit +15%, low visibility",
    combatImpactLabel = "+15% Bow Crit • -10% Enemy Sight",
    survivalImpactLabel = "Stealth Cover Active",
    lootImpactLabel = "Ambush Advantage",
    staminaRegenDelta = 0,
    ambientAudioLabel = "Blizzard Wind Howl",
  );

  companion object {
    fun fromIndex(index: Int): WeatherType = entries.firstOrNull { it.index == index } ?: CLEAR
  }
}

enum class TimeOfDay(
  val index: Int,
  val displayName: String,
) {
  DAWN(0, "Dawn"),
  DAY(1, "Daylight"),
  DUSK(2, "Dusk"),
  MIDNIGHT(3, "Midnight");

  companion object {
    fun fromIndex(index: Int): TimeOfDay = entries.firstOrNull { it.index == index } ?: DUSK
  }
}

enum class WeaponCategory(val displayName: String) {
  SWORD("Swords"),
  BOW("Bows"),
  FUTURISTIC("Futuristic");

  companion object {
    fun fromName(name: String): WeaponCategory =
      entries.firstOrNull { it.name == name } ?: SWORD
  }
}

enum class LootRarity(
  val label: String,
  val shortTag: String,
  val salvageMultiplier: Float,
  val dropWeight: Int,
) {
  COMMON("Common", "CMN", 1.0f, 52),
  UNCOMMON("Uncommon", "UNC", 1.35f, 26),
  RARE("Rare", "RARE", 1.75f, 14),
  EPIC("Epic", "EPIC", 2.35f, 6),
  LEGENDARY("Legendary", "LGND", 3.20f, 2);

  companion object {
    fun fromName(name: String): LootRarity =
      entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: COMMON
  }
}

enum class NodeKind {
  FOOD,
  WATER,
  SCRAP,
  TIMBER,
  CRYSTAL,
  MONSTER,
  BOSS
}

data class WorldNode(
  val id: String,
  val kind: NodeKind,
  val label: String,
  val x: Float, // 0f..1f normalized map coordinates
  val y: Float, // 0f..1f normalized map coordinates
  val power: Int = 1,
  val rarity: LootRarity = LootRarity.COMMON,
)

data class FloatingHit(
  val id: Long,
  val text: String,
  val isCrit: Boolean = false,
  val isHeal: Boolean = false,
  val isEnemyHit: Boolean = false,
  val ttlTicks: Int = 11,
)

data class DirectionalHitIndicator(
  val id: Long,
  val angleRad: Float,
  val damage: Int,
  val intensity: Float = 1f,
  val ttlTicks: Int = 14,
)

data class ArenaProjectile(
  val id: Long,
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val damage: Int,
  val fromPlayer: Boolean,
  val category: WeaponCategory = WeaponCategory.FUTURISTIC,
  val isHeavy: Boolean = false,
)

data class DangerZone(
  val id: Long,
  val x: Float,
  val y: Float,
  val radius: Float,
  val progress: Float, // 0f..1f -> detonates at 1f
  val damage: Int,
  val label: String,
)

enum class CombatMoveType(val displayLabel: String, val staminaCost: Int) {
  PRIMARY("Strike", 4),
  DODGE("Tactical Dodge", 14),
  SPRINT_STRIKE("Sprint Attack", 20),
  HEAVY_OVERHEAD("Heavy Overhead", 28),
}

data class CombatState(
  val enemyName: String,
  val isBoss: Boolean,
  val biome: BiomeType,
  val enemyHp: Int,
  val enemyMaxHp: Int,
  val enemyAttackPower: Int,
  val enemyLevel: Int = 1,
  val telegraphProgress: Float, // 0f..1f when approaching 1f, enemy strikes!
  val dodgeWindowActive: Boolean,
  val shieldTicks: Int,
  val counterReadyTicks: Int = 0,
  val comboCount: Int,
  val squadUltimateCharge: Int, // 0..100
  val isEnraged: Boolean = false,
  val arenaPlayerX: Float = 0.24f,
  val arenaPlayerY: Float = 0.62f,
  val arenaPlayerFacingRad: Float = 0f,
  val arenaEnemyX: Float = 0.74f,
  val arenaEnemyY: Float = 0.54f,
  val sprintTrailFromX: Float = 0.24f,
  val sprintTrailFromY: Float = 0.62f,
  val enemyHitFlashTicks: Int = 0,
  val playerHurtFlashTicks: Int = 0,
  val playerDamageSeq: Long = 0L,
  val lastHitIntensity: Float = 1f,
  val directionalHits: List<DirectionalHitIndicator> = emptyList(),
  val projectiles: List<ArenaProjectile> = emptyList(),
  val dangerZones: List<DangerZone> = emptyList(),
  val playerStrikeSeq: Long = 0L,
  val playerSprintSeq: Long = 0L,
  val playerOverheadSeq: Long = 0L,
  val lastMoveUsed: CombatMoveType = CombatMoveType.PRIMARY,
  val enemyStrikeSeq: Long = 0L,
  val missionNumber: Int? = null,
  val storyNumber: Int? = null,
  val chapterInStory: Int? = null,
  val isCoopRaid: Boolean = false,
  val combatLog: List<String> = emptyList(),
  val recentHits: List<FloatingHit> = emptyList(),
  val victory: Boolean = false,
  val defeat: Boolean = false,
  val rewardSummary: String = "",
)

data class BaseWaveState(
  val waveNumber: Int = 1,
  val inBaseArea: Boolean = true,
  val waveActive: Boolean = false,
  val countdownSeconds: Int = BaseZoneRules.WAVE_COUNTDOWN_INITIAL_SEC,
  val remainingEnemiesInWave: Int = 0,
  val waveEnemyCount: Int = 0,
  val isBossWave: Boolean = false,
  val baseIntegrity: Int = 100,
  val maxBaseIntegrity: Int = 100,
  val wavesSurvived: Int = 0,
  val turretLasersFired: Int = 0,
)

enum class AppTab(val route: String) {
  WORLD("world"),
  STORY("story"),
  CAMP("camp"),
  ARMORY("armory"),
  SQUAD("squad"),
}
