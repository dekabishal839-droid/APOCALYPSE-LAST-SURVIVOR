package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
  @PrimaryKey val id: Int = 1,
  @ColumnInfo(name = "health") val health: Int = 100,
  @ColumnInfo(name = "max_health") val maxHealth: Int = 100,
  @ColumnInfo(name = "stamina") val stamina: Int = 100,
  @ColumnInfo(name = "max_stamina") val maxStamina: Int = 100,
  @ColumnInfo(name = "hunger") val hunger: Int = 65,
  @ColumnInfo(name = "thirst") val thirst: Int = 60,
  @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
