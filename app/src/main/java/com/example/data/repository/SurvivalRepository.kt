package com.example.data.repository

import com.example.data.local.BaseBuildingEntity
import com.example.data.local.GameDao
import com.example.data.local.GameSeedData
import com.example.data.local.InventorySlotEntity
import com.example.data.local.MissionEntity
import com.example.data.local.PlayerStats
import com.example.data.local.PlayerStatsDao
import com.example.data.local.SquadMateEntity
import com.example.data.local.SurvivorEntity
import com.example.data.local.WeaponEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SurvivalRepository(
  private val dao: GameDao,
  private val playerStatsDao: PlayerStatsDao,
) {
  val playerStatsFlow: Flow<PlayerStats?> = playerStatsDao.observePlayerStats()
  val survivorFlow: Flow<SurvivorEntity?> = dao.observeSurvivor()
  val weaponsFlow: Flow<List<WeaponEntity>> = dao.observeWeapons()
  val buildingsFlow: Flow<List<BaseBuildingEntity>> = dao.observeBuildings()
  val missionsFlow: Flow<List<MissionEntity>> = dao.observeMissions()
  val squadMatesFlow: Flow<List<SquadMateEntity>> = dao.observeSquadMates()
  val inventoryFlow: Flow<List<InventorySlotEntity>> = dao.observeInventorySlots()

  suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
    if (playerStatsDao.getPlayerStatsOnce() == null) {
      playerStatsDao.upsertPlayerStats(GameSeedData.initialPlayerStats)
    }
    if (dao.getSurvivorOnce() == null) {
      dao.upsertSurvivor(GameSeedData.initialSurvivor)
    }
    if (dao.getMissionCount() < 1000) {
      dao.clearMissions()
      dao.upsertWeapons(GameSeedData.initialWeapons)
      dao.upsertBuildings(GameSeedData.initialBuildings)
      dao.upsertMissions(GameSeedData.build1000StoryMissions())
      dao.upsertSquadMates(GameSeedData.initialSquadMates)
    }
    if (dao.getInventorySlotsOnce().size < 20) {
      dao.upsertInventorySlots(GameSeedData.initialInventorySlots())
    }
  }

  suspend fun updateSurvivor(survivor: SurvivorEntity) = withContext(Dispatchers.IO) {
    dao.upsertSurvivor(survivor)
    playerStatsDao.updateVitals(
      health = survivor.hp,
      maxHealth = survivor.maxHp,
      stamina = survivor.stamina,
      maxStamina = 100,
      hunger = survivor.food,
      thirst = survivor.water,
    )
    syncCoreResourcesToInventory(survivor)
  }

  private suspend fun syncCoreResourcesToInventory(survivor: SurvivorEntity) {
    val slots = dao.getInventorySlotsOnce().toMutableList()
    if (slots.isEmpty()) return
    fun syncItem(itemId: String, name: String, category: String, qty: Int, desc: String, rarity: String) {
      val idx = slots.indexOfFirst { it.itemId == itemId }
      if (idx >= 0) {
        if (qty <= 0) {
          slots[idx] = InventorySlotEntity(slots[idx].slotIndex, "", "Empty Slot", "EMPTY", 0, "Collect exploration loot to fill this slot.", "COMMON")
        } else {
          slots[idx] = slots[idx].copy(quantity = qty, rarity = rarity)
        }
      } else if (qty > 0) {
        val emptyIdx = slots.indexOfFirst { it.itemId.isEmpty() }
        if (emptyIdx >= 0) {
          slots[emptyIdx] = InventorySlotEntity(slots[emptyIdx].slotIndex, itemId, name, category, qty, desc, rarity)
        }
      }
    }
    syncItem("salvaged_wood", "Salvaged Timber", "BUILDING", survivor.wood, "Reinforced lumber used for Camp upgrades & Bow crafting.", "COMMON")
    syncItem("scrap_metal", "Scrap Metal", "BUILDING", survivor.metal, "Alloy plates required for Bunker walls, Turrets & Blades.", "COMMON")
    syncItem("bio_crystal", "Bio-Crystal", "TECH", survivor.crystals, "High-energy 2045 shard powering Plasma weapons & Tech Labs.", "UNCOMMON")
    syncItem("tactical_medkit", "Tactical Medkit", "CONSUMABLE", survivor.medkits, "Emergency trauma patch (Restores HP & Immunity).", "UNCOMMON")
    syncItem("energy_cell", "Plasma Cell", "TECH", survivor.energyCells, "Ammunition core for Futuristic weapons & Turrets.", "UNCOMMON")
    dao.upsertInventorySlots(slots)
  }

  suspend fun addInventoryItem(
    itemId: String,
    name: String,
    category: String,
    deltaQty: Int,
    description: String,
    rarity: String = "COMMON",
  ) = withContext(Dispatchers.IO) {
    val slots = dao.getInventorySlotsOnce().toMutableList()
    val existingIdx = slots.indexOfFirst { it.itemId == itemId }
    if (existingIdx >= 0) {
      val nextQty = (slots[existingIdx].quantity + deltaQty).coerceAtLeast(0)
      slots[existingIdx] = if (nextQty == 0) {
        InventorySlotEntity(slots[existingIdx].slotIndex, "", "Empty Slot", "EMPTY", 0, "Collect exploration loot to fill this slot.", "COMMON")
      } else {
        slots[existingIdx].copy(quantity = nextQty, rarity = rarity)
      }
      dao.upsertInventorySlot(slots[existingIdx])
    } else if (deltaQty > 0) {
      val emptyIdx = slots.indexOfFirst { it.itemId.isEmpty() }
      if (emptyIdx >= 0) {
        val filled = InventorySlotEntity(slots[emptyIdx].slotIndex, itemId, name, category, deltaQty, description, rarity)
        dao.upsertInventorySlot(filled)
      }
    }
  }

  suspend fun consumeItemById(itemId: String, amount: Int) = withContext(Dispatchers.IO) {
    if (amount <= 0) return@withContext
    val slots = dao.getInventorySlotsOnce()
    val slot = slots.firstOrNull { it.itemId == itemId } ?: return@withContext
    val nextQty = (slot.quantity - amount).coerceAtLeast(0)
    val updated = if (nextQty <= 0) {
      InventorySlotEntity(slot.slotIndex, "", "Empty Slot", "EMPTY", 0, "Collect exploration loot to fill this slot.", "COMMON")
    } else {
      slot.copy(quantity = nextQty)
    }
    dao.upsertInventorySlot(updated)
  }

  suspend fun consumeOrRemoveSlot(slotIndex: Int, removeAll: Boolean = false) = withContext(Dispatchers.IO) {
    val slots = dao.getInventorySlotsOnce()
    val slot = slots.firstOrNull { it.slotIndex == slotIndex } ?: return@withContext
    val nextQty = if (removeAll) 0 else (slot.quantity - 1).coerceAtLeast(0)
    val updated = if (nextQty <= 0) {
      InventorySlotEntity(slot.slotIndex, "", "Empty Slot", "EMPTY", 0, "Collect exploration loot to fill this slot.", "COMMON")
    } else {
      slot.copy(quantity = nextQty)
    }
    dao.upsertInventorySlot(updated)
  }

  suspend fun updatePlayerVitals(
    health: Int,
    maxHealth: Int,
    stamina: Int,
    hunger: Int,
    thirst: Int,
  ) = withContext(Dispatchers.IO) {
    playerStatsDao.updateVitals(
      health = health.coerceIn(0, maxHealth),
      maxHealth = maxHealth,
      stamina = stamina.coerceIn(0, 100),
      maxStamina = 100,
      hunger = hunger.coerceIn(0, 100),
      thirst = thirst.coerceIn(0, 100),
    )
    dao.getSurvivorOnce()?.let { current ->
      dao.upsertSurvivor(
        current.copy(
          hp = health.coerceIn(0, maxHealth),
          maxHp = maxHealth,
          stamina = stamina.coerceIn(0, 100),
          food = hunger.coerceIn(0, 100),
          water = thirst.coerceIn(0, 100),
        )
      )
    }
  }

  suspend fun upsertWeapon(weapon: WeaponEntity) = withContext(Dispatchers.IO) {
    dao.upsertWeapon(weapon)
  }

  suspend fun upsertBuilding(building: BaseBuildingEntity) = withContext(Dispatchers.IO) {
    dao.upsertBuilding(building)
  }

  suspend fun completeMission(missionNumber: Int) = withContext(Dispatchers.IO) {
    val current = dao.getMissionByNumber(missionNumber) ?: return@withContext
    if (!current.completed) {
      dao.upsertMission(current.copy(completed = true))
    }
    val next = dao.getMissionByNumber(missionNumber + 1)
    if (next != null && !next.unlocked) {
      dao.upsertMission(next.copy(unlocked = true))
    }
  }

  suspend fun toggleSquadMate(mate: SquadMateEntity) = withContext(Dispatchers.IO) {
    dao.upsertSquadMate(mate.copy(isSelected = !mate.isSelected))
  }
}
