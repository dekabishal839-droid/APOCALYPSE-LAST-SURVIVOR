package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "survivor_profile")
data class SurvivorEntity(
  @PrimaryKey val id: Int = 1,
  val callsign: String = "SURVIVOR-01",
  val level: Int = 1,
  val xp: Int = 0,
  val hp: Int = 120,
  val maxHp: Int = 120,
  val food: Int = 85,
  val water: Int = 80,
  val stamina: Int = 100,
  val immunity: Int = 92,
  val wood: Int = 45,
  val metal: Int = 35,
  val crystals: Int = 18,
  val medkits: Int = 4,
  val energyCells: Int = 24,
  val equippedWeaponId: String = "sword_machete",
  val activeBiomeId: String = "DEAD_CITY",
  val weatherIndex: Int = 1,
  val timeOfDayIndex: Int = 2,
  val autoCycleWeather: Boolean = true,
  val squadCode: String = "NOVA-2045",
  val monstersSlain: Int = 0,
  val missionsCompleted: Int = 0,
  val unlockedAbilityIds: String = "",
)

@Entity(tableName = "weapons")
data class WeaponEntity(
  @PrimaryKey val id: String,
  val name: String,
  val category: String, // SWORD, BOW, FUTURISTIC
  val level: Int,
  val baseDamage: Int,
  val critChance: Int, // percentage 0..100
  val energyCost: Int,
  val unlocked: Boolean,
  val woodCost: Int,
  val metalCost: Int,
  val crystalCost: Int,
  val description: String,
)

@Entity(tableName = "base_buildings")
data class BaseBuildingEntity(
  @PrimaryKey val id: String,
  val name: String,
  val level: Int,
  val maxLevel: Int,
  val woodCost: Int,
  val metalCost: Int,
  val crystalCost: Int,
  val bonusLabel: String,
  val description: String,
)

@Entity(tableName = "story_missions")
data class MissionEntity(
  @PrimaryKey val number: Int, // 1..1000 global sequential ID
  val storyNumber: Int,        // 1..20
  val storyTitle: String,
  val chapter: Int,            // 1..20 legacy alias matching storyNumber
  val chapterInStory: Int,     // 1..50
  val chapterTitle: String,
  val title: String,
  val biomeId: String,
  val weatherIndex: Int,
  val isBoss: Boolean,
  val enemyName: String,
  val enemyLevel: Int,
  val enemyHp: Int,
  val completed: Boolean,
  val unlocked: Boolean,
  val briefing: String,
  val objective: String,
  val rewardXp: Int,
  val rewardCrystals: Int,
  val rewardMetal: Int,
)

@Entity(tableName = "squad_mates")
data class SquadMateEntity(
  @PrimaryKey val id: String,
  val callsign: String,
  val role: String,
  val weaponName: String,
  val power: Int,
  val isSelected: Boolean,
)

@Entity(tableName = "inventory_slots")
data class InventorySlotEntity(
  @PrimaryKey val slotIndex: Int, // 0..19 (5x4 grid)
  val itemId: String,             // empty string "" means empty slot
  val name: String,
  val category: String,           // BUILDING, CONSUMABLE, TECH, RARE
  val quantity: Int,
  val description: String,
  val rarity: String = "COMMON",  // COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
)
