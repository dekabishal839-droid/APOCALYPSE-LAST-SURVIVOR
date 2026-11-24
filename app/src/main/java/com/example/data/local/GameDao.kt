package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
  @Query("SELECT * FROM survivor_profile WHERE id = 1")
  fun observeSurvivor(): Flow<SurvivorEntity?>

  @Query("SELECT * FROM survivor_profile WHERE id = 1")
  suspend fun getSurvivorOnce(): SurvivorEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSurvivor(survivor: SurvivorEntity)

  @Query("SELECT * FROM weapons ORDER BY category, baseDamage")
  fun observeWeapons(): Flow<List<WeaponEntity>>

  @Query("SELECT * FROM weapons WHERE id = :id")
  suspend fun getWeaponById(id: String): WeaponEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWeapons(weapons: List<WeaponEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWeapon(weapon: WeaponEntity)

  @Query("SELECT * FROM base_buildings")
  fun observeBuildings(): Flow<List<BaseBuildingEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBuildings(buildings: List<BaseBuildingEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBuilding(building: BaseBuildingEntity)

  @Query("SELECT * FROM story_missions ORDER BY number ASC")
  fun observeMissions(): Flow<List<MissionEntity>>

  @Query("SELECT * FROM story_missions WHERE storyNumber = :storyNumber ORDER BY chapterInStory ASC")
  fun observeMissionsByStory(storyNumber: Int): Flow<List<MissionEntity>>

  @Query("SELECT COUNT(*) FROM story_missions")
  suspend fun getMissionCount(): Int

  @Query("DELETE FROM story_missions")
  suspend fun clearMissions()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMissions(missions: List<MissionEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMission(mission: MissionEntity)

  @Query("SELECT * FROM story_missions WHERE number = :number")
  suspend fun getMissionByNumber(number: Int): MissionEntity?

  @Query("SELECT * FROM squad_mates ORDER BY power DESC")
  fun observeSquadMates(): Flow<List<SquadMateEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSquadMates(mates: List<SquadMateEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSquadMate(mate: SquadMateEntity)

  @Query("SELECT * FROM inventory_slots ORDER BY slotIndex ASC")
  fun observeInventorySlots(): Flow<List<InventorySlotEntity>>

  @Query("SELECT * FROM inventory_slots ORDER BY slotIndex ASC")
  suspend fun getInventorySlotsOnce(): List<InventorySlotEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertInventorySlots(slots: List<InventorySlotEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertInventorySlot(slot: InventorySlotEntity)
}
