package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {
  @Query("SELECT * FROM player_stats WHERE id = 1")
  fun observePlayerStats(): Flow<PlayerStats?>

  @Query("SELECT * FROM player_stats WHERE id = 1")
  suspend fun getPlayerStatsOnce(): PlayerStats?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPlayerStats(stats: PlayerStats)

  @Query(
    """
    UPDATE player_stats
    SET health = :health,
        max_health = :maxHealth,
        stamina = :stamina,
        max_stamina = :maxStamina,
        hunger = :hunger,
        thirst = :thirst,
        updated_at = :updatedAt
    WHERE id = 1
    """
  )
  suspend fun updateVitals(
    health: Int,
    maxHealth: Int,
    stamina: Int,
    maxStamina: Int,
    hunger: Int,
    thirst: Int,
    updatedAt: Long = System.currentTimeMillis(),
  )
}
