package com.example

import com.example.data.local.GameSeedData
import com.example.data.local.PlayerStats
import com.example.model.BiomeType
import com.example.model.DifficultyManager
import com.example.model.Enemy
import com.example.model.EnemyAiState
import com.example.model.Player
import com.example.model.WeatherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun difficultyManager_scalesWithDecliningVitals_elapsedTime_andPlayerLevel() {
    val healthy = PlayerStats(health = 100, maxHealth = 100, stamina = 100, hunger = 100, thirst = 100)
    val starving = PlayerStats(health = 25, maxHealth = 100, stamina = 30, hunger = 10, thirst = 15)

    val baseDiff = DifficultyManager.evaluate(healthy, elapsedSeconds = 0L, playerLevel = 1, WeatherType.CLEAR, BiomeType.DEAD_CITY)
    val starvingDiff = DifficultyManager.evaluate(starving, elapsedSeconds = 0L, playerLevel = 1, WeatherType.CLEAR, BiomeType.DEAD_CITY)
    val lateGameDiff = DifficultyManager.evaluate(healthy, elapsedSeconds = 300L, playerLevel = 5, WeatherType.BIO_STORM, BiomeType.DEAD_CITY)

    assertTrue(starvingDiff.speedMultiplier > baseDiff.speedMultiplier)
    assertTrue(starvingDiff.damageMultiplier > baseDiff.damageMultiplier)
    assertTrue(starvingDiff.detectionRangeMultiplier > baseDiff.detectionRangeMultiplier)
    assertTrue(lateGameDiff.levelScaleFactor > baseDiff.levelScaleFactor)
    assertTrue(lateGameDiff.damageMultiplier > baseDiff.damageMultiplier)
  }

  @Test
  fun campaignGenerator_creates20StoriesBy50Chapters_withBossEvery5Chapters() {
    val missions = GameSeedData.build1000StoryMissions()
    assertEquals(1000, missions.size)
    assertEquals(20, missions.map { it.storyNumber }.distinct().size)

    for (story in 1..20) {
      val chapters = missions.filter { it.storyNumber == story }
      assertEquals(50, chapters.size)
      chapters.forEach { m ->
        assertEquals(m.chapterInStory % 5 == 0, m.isBoss)
      }
    }

    assertTrue(missions.first().unlocked)
    assertFalse(missions[1].unlocked)
    assertTrue(missions.last().enemyLevel > missions.first().enemyLevel)
  }

  @Test
  fun enemyAi_activelyPatrols_chases_andAttacksInCombat() {
    val diff = DifficultyManager.evaluate(
      PlayerStats(health = 60, maxHealth = 100, stamina = 80, hunger = 50, thirst = 50),
      elapsedSeconds = 120L,
      playerLevel = 3,
      weather = WeatherType.CLEAR,
      biome = BiomeType.DEAD_CITY,
    )
    val player = Player(x = 0.50f, y = 0.50f, level = 3)
    val farEnemy = Enemy.createForBiome("e1", "Virus Stalker", BiomeType.DEAD_CITY, isBoss = false, x = 0.15f, y = 0.20f, playerLevel = 3, difficulty = diff)
    val (patrolled, _, _) = farEnemy.stepWorldAi(player, WeatherType.CLEAR, diff)
    assertTrue(patrolled.x != farEnemy.x || patrolled.y != farEnemy.y)

    val nearEnemy = Enemy.createForBiome("e2", "Virus Stalker", BiomeType.DEAD_CITY, isBoss = false, x = 0.35f, y = 0.50f, playerLevel = 3, difficulty = diff)
    val (chased, _, _) = nearEnemy.stepWorldAi(player, WeatherType.CLEAR, diff)
    assertTrue(chased.aiState == EnemyAiState.ALERT || chased.aiState == EnemyAiState.CHASE)
    assertTrue(chased.x > nearEnemy.x)
  }

  @Test
  fun inventoryGrid_initializesWith20Slots_andDifficultyCapsStayBalanced() {
    val slots = GameSeedData.initialInventorySlots()
    assertEquals(20, slots.size)
    assertTrue(slots.take(9).all { it.itemId.isNotEmpty() && it.quantity > 0 && it.rarity.isNotBlank() })
    assertTrue(slots.drop(9).all { it.itemId.isEmpty() && it.quantity == 0 })

    val extreme = DifficultyManager.evaluate(
      stats = PlayerStats(health = 10, maxHealth = 100, stamina = 10, hunger = 5, thirst = 5),
      elapsedSeconds = 1200L,
      playerLevel = 10,
      weather = WeatherType.BIO_STORM,
      biome = BiomeType.SECTOR_9_LAB,
    )
    assertTrue(extreme.speedMultiplier <= 1.66f)
    assertTrue(extreme.damageMultiplier <= 1.86f)
    assertTrue(extreme.detectionRangeMultiplier <= 1.56f)
  }

  @Test
  fun combatModels_directionalHitAndPlayerDamage_behavePredictably() {
    val player = Player(defenseRating = 50)
    assertEquals(0, player.takeDamage(30, dodging = true))
    assertEquals(28, player.takeDamage(30, dodging = false))

    val hit = com.example.model.DirectionalHitIndicator(
      id = 1L,
      angleRad = 0.5f,
      damage = 28,
      intensity = 1.2f,
      ttlTicks = 14,
    )
    assertTrue(hit.ttlTicks > 0 && hit.intensity > 1f)
  }

  @Test
  fun tacticalCombatMoves_overheadPiercesArmorAndWeatherModifiesStaminaRegen() {
    assertEquals(14, com.example.model.CombatMoveType.DODGE.staminaCost)
    assertEquals(20, com.example.model.CombatMoveType.SPRINT_STRIKE.staminaCost)
    assertEquals(28, com.example.model.CombatMoveType.HEAVY_OVERHEAD.staminaCost)

    assertEquals(1, WeatherType.CLEAR.staminaRegenDelta)
    assertEquals(-1, WeatherType.BIO_STORM.staminaRegenDelta)

    val player = Player(level = 1, stamina = 100)
    val weapon = com.example.data.local.WeaponEntity(
      id = "w_test",
      name = "Arc Blade",
      category = "SWORD",
      level = 1,
      baseDamage = 40,
      critChance = 0,
      energyCost = 0,
      unlocked = true,
      woodCost = 0,
      metalCost = 0,
      crystalCost = 0,
      description = "Test heavy tactical blade",
    )
    val (lightDmg, lightCrit) = player.computeAttackDamage(weapon, WeatherType.CLEAR, isHeavy = false, enemyArmor = 20)
    val (overheadDmg, overheadCrit) = player.computeAttackDamage(
      weapon = weapon,
      weather = WeatherType.CLEAR,
      isHeavy = true,
      enemyArmor = 20,
      moveMultiplier = 2.15f,
      armorPierceRatio = 0.55f,
    )
    assertFalse(lightCrit)
    assertFalse(overheadCrit)
    assertTrue(overheadDmg > lightDmg * 2)
    assertTrue(player.staminaRegenForWeather(WeatherType.CLEAR) > player.staminaRegenForWeather(WeatherType.BIO_STORM))
  }

  @Test
  fun strictTouchHitDetection_appliesToPlayerEnemiesAndBosses() {
    // Player melee touch check: misses at 0.50 distance, touches at 0.18 distance
    assertFalse(
      com.example.model.CombatHitbox.doesPlayerMeleeTouchTarget(
        playerX = 0.20f,
        playerY = 0.55f,
        enemyX = 0.70f,
        enemyY = 0.55f,
        isBoss = false,
        moveType = com.example.model.CombatMoveType.PRIMARY,
      )
    )
    assertTrue(
      com.example.model.CombatHitbox.doesPlayerMeleeTouchTarget(
        playerX = 0.52f,
        playerY = 0.55f,
        enemyX = 0.70f,
        enemyY = 0.55f,
        isBoss = false,
        moveType = com.example.model.CombatMoveType.PRIMARY,
      )
    )

    // Projectile touch check against Boss & Player
    assertFalse(com.example.model.CombatHitbox.doesProjectileTouchTarget(0.20f, 0.20f, 0.75f, 0.60f, isBoss = true))
    assertTrue(com.example.model.CombatHitbox.doesProjectileTouchTarget(0.70f, 0.58f, 0.75f, 0.60f, isBoss = true))
    assertFalse(com.example.model.CombatHitbox.doesProjectileTouchPlayer(0.80f, 0.50f, 0.30f, 0.50f))
    assertTrue(com.example.model.CombatHitbox.doesProjectileTouchPlayer(0.33f, 0.50f, 0.30f, 0.50f))

    // Enemy & Boss direct strikes trigger missedDirectStrike when telegraph finishes out of reach
    val diff = DifficultyManager.evaluate(PlayerStats(), elapsedSeconds = 0L, playerLevel = 1, WeatherType.CLEAR, BiomeType.DEAD_CITY)
    val bossNearTrigger = Enemy.createForBiome("b1", "Boss", BiomeType.DEAD_CITY, isBoss = true, x = 0.80f, y = 0.55f)
      .copy(telegraphProgress = 0.99f)
    val missStep = bossNearTrigger.stepCombatAi(0.80f, 0.55f, playerX = 0.20f, playerY = 0.55f, difficulty = diff)
    assertFalse(missStep.strikePlayerDirect)
    assertTrue(missStep.missedDirectStrike)

    val hitStep = bossNearTrigger.stepCombatAi(0.55f, 0.55f, playerX = 0.42f, playerY = 0.55f, difficulty = diff)
    assertTrue(hitStep.strikePlayerDirect)
    assertFalse(hitStep.missedDirectStrike)

    // World map enemy harass does NOT damage when not touching (0.11f > touch limit 0.082f)
    val mob = Enemy.createForBiome("m1", "Stalker", BiomeType.DEAD_CITY, isBoss = false, x = 0.39f, y = 0.56f)
      .copy(actionCooldownTicks = 0)
    val (_, _, farHarass) = mob.stepWorldAi(Player(x = 0.50f, y = 0.56f), WeatherType.CLEAR, diff)
    assertEquals(0, farHarass)
  }

  @Test
  fun baseZoneWaves_triggerInsideBaseAndScaleWithGlobalDifficulty() {
    assertTrue(com.example.model.BaseZoneRules.isInsideBase(0.50f, 0.56f))
    assertFalse(com.example.model.BaseZoneRules.isInsideBase(0.12f, 0.20f))

    val lowDiff = DifficultyManager.evaluate(
      PlayerStats(health = 100, maxHealth = 100, stamina = 100, hunger = 100, thirst = 100),
      elapsedSeconds = 0L,
      playerLevel = 1,
      weather = WeatherType.CLEAR,
      biome = BiomeType.DEAD_CITY,
    )
    val highDiff = DifficultyManager.evaluate(
      PlayerStats(health = 20, maxHealth = 100, stamina = 25, hunger = 10, thirst = 10),
      elapsedSeconds = 450L,
      playerLevel = 5,
      weather = WeatherType.BIO_STORM,
      biome = BiomeType.SECTOR_9_LAB,
    )

    assertTrue(
      com.example.model.BaseZoneRules.computeWaveEnemyCount(3, highDiff) >
        com.example.model.BaseZoneRules.computeWaveEnemyCount(1, lowDiff)
    )
    assertTrue(
      com.example.model.BaseZoneRules.computeWaveEnemyLevel(5, 3, highDiff) >
        com.example.model.BaseZoneRules.computeWaveEnemyLevel(1, 1, lowDiff)
    )
    assertTrue(com.example.model.BaseZoneRules.isBossWave(3, lowDiff))
    assertTrue(com.example.model.BaseZoneRules.turretBarrageDamage(3) > com.example.model.BaseZoneRules.turretBarrageDamage(0))
    assertTrue(com.example.model.BaseZoneRules.maxBaseIntegrity(3) > com.example.model.BaseZoneRules.maxBaseIntegrity(1))
  }

  @Test
  fun ambientWeatherAudio_synthesizesDistinctSoundscapesAndSmoothlyCrossfades() {
    assertTrue(WeatherType.entries.all { it.ambientAudioLabel.isNotBlank() })

    fun chunkRms(pcm: ShortArray): Double {
      val sumSq = pcm.sumOf { sample -> sample.toDouble() * sample.toDouble() }
      return kotlin.math.sqrt(sumSq / pcm.size)
    }

    val clearPcm = com.example.audio.AmbientWeatherSynth.synthesizeAmbientChunk(
      fromWeather = WeatherType.CLEAR,
      toWeather = WeatherType.CLEAR,
      blendStart = 1f,
      blendEnd = 1f,
      startSampleIdx = 0L,
      frameCount = 4096,
    )
    val rainPcm = com.example.audio.AmbientWeatherSynth.synthesizeAmbientChunk(
      fromWeather = WeatherType.RAIN,
      toWeather = WeatherType.RAIN,
      blendStart = 1f,
      blendEnd = 1f,
      startSampleIdx = 0L,
      frameCount = 4096,
    )
    val stormPcm = com.example.audio.AmbientWeatherSynth.synthesizeAmbientChunk(
      fromWeather = WeatherType.BIO_STORM,
      toWeather = WeatherType.BIO_STORM,
      blendStart = 1f,
      blendEnd = 1f,
      startSampleIdx = 0L,
      frameCount = 4096,
    )
    val fogPcm = com.example.audio.AmbientWeatherSynth.synthesizeAmbientChunk(
      fromWeather = WeatherType.ASH_FOG,
      toWeather = WeatherType.ASH_FOG,
      blendStart = 1f,
      blendEnd = 1f,
      startSampleIdx = 0L,
      frameCount = 4096,
    )
    val crossfadeMidPcm = com.example.audio.AmbientWeatherSynth.synthesizeAmbientChunk(
      fromWeather = WeatherType.CLEAR,
      toWeather = WeatherType.BIO_STORM,
      blendStart = 0.45f,
      blendEnd = 0.55f,
      startSampleIdx = 0L,
      frameCount = 4096,
    )

    val clearRms = chunkRms(clearPcm)
    val rainRms = chunkRms(rainPcm)
    val stormRms = chunkRms(stormPcm)
    val fogRms = chunkRms(fogPcm)
    val midRms = chunkRms(crossfadeMidPcm)

    assertTrue(clearRms > 0.0)
    assertTrue(rainRms > clearRms)
    assertTrue(stormRms > rainRms)
    assertTrue(fogRms > clearRms)
    assertTrue(midRms > clearRms && midRms < stormRms)
  }

  @Test
  fun abilityTree_enforcesPrerequisites_spendsPoints_andScalesPlayerCombatStats() {
    val tier1 = com.example.model.AbilityTreeCatalog.findById("vanguard_edge")!!
    val tier2 = com.example.model.AbilityTreeCatalog.findById("vanguard_momentum")!!
    val tier3 = com.example.model.AbilityTreeCatalog.findById("vanguard_executioner")!!

    val earnedLv3 = com.example.model.AbilityTreeCatalog.totalEarnedPoints(level = 3, missionsCompleted = 5)
    assertEquals(5, earnedLv3)
    assertTrue(com.example.model.AbilityTreeCatalog.canUnlock(tier1, emptySet(), earnedLv3))
    assertFalse(com.example.model.AbilityTreeCatalog.canUnlock(tier2, emptySet(), earnedLv3))

    val unlockedVanguard = setOf(tier1.id, tier2.id, tier3.id)
    val spent = com.example.model.AbilityTreeCatalog.spentPoints(unlockedVanguard)
    assertEquals(4, spent)
    assertEquals(1, com.example.model.AbilityTreeCatalog.availablePoints(level = 3, missionsCompleted = 5, unlockedVanguard))

    val roundTripped = com.example.model.AbilityTreeCatalog.parseUnlockedIds(
      com.example.model.AbilityTreeCatalog.encodeUnlockedIds(unlockedVanguard)
    )
    assertEquals(unlockedVanguard, roundTripped)

    val bioGuardSet = setOf("bioguard_metabolism", "bioguard_resilience", "bioguard_bastion")
    val fullSummary = com.example.model.AbilityTreeCatalog.summarize(unlockedVanguard + bioGuardSet)
    assertTrue(fullSummary.damageMultiplier > 1.1f)
    assertTrue(fullSummary.specialStaminaDiscount >= 0.2f)
    assertTrue(fullSummary.armorPierceBonus >= 0.2f)
    assertTrue(fullSummary.turretDamageMultiplier >= 1.3f)
    assertTrue(fullSummary.drainResistance)

    val weapon = com.example.data.local.WeaponEntity(
      id = "w_skill_test",
      name = "Arc Blade",
      category = "SWORD",
      level = 1,
      baseDamage = 50,
      critChance = 0,
      energyCost = 0,
      unlocked = true,
      woodCost = 0,
      metalCost = 0,
      crystalCost = 0,
      description = "Skill tree test sword",
    )
    val basePlayer = Player(level = 3, defenseRating = 40)
    val skilledPlayer = Player(level = 3, defenseRating = 40, abilityBonuses = fullSummary)

    val (baseDmg, _) = basePlayer.computeAttackDamage(weapon, WeatherType.CLEAR, isHeavy = false, enemyArmor = 30)
    val (skilledDmg, _) = skilledPlayer.computeAttackDamage(weapon, WeatherType.CLEAR, isHeavy = false, enemyArmor = 30)
    assertTrue(skilledDmg > baseDmg)
    assertTrue(skilledPlayer.takeDamage(40, dodging = false) < basePlayer.takeDamage(40, dodging = false))
    assertTrue(skilledPlayer.staminaRegenForWeather(WeatherType.BIO_STORM) > basePlayer.staminaRegenForWeather(WeatherType.BIO_STORM))
  }

  @Test
  fun lootRarityAndCraftingStation_dropsScaleAndRequireCorePlusGridResources() {
    val deadCityClear = com.example.model.LootDropTable.rollWorldNodeRarity(
      biome = BiomeType.DEAD_CITY,
      weather = WeatherType.CLEAR,
      rollPercent = 40,
    )
    val sectorLabStorm = com.example.model.LootDropTable.rollWorldNodeRarity(
      biome = BiomeType.SECTOR_9_LAB,
      weather = WeatherType.BIO_STORM,
      rollPercent = 82,
    )
    assertEquals(com.example.model.LootRarity.COMMON, deadCityClear)
    assertEquals(com.example.model.LootRarity.LEGENDARY, sectorLabStorm)
    assertTrue(com.example.model.LootRarity.LEGENDARY.salvageMultiplier > com.example.model.LootRarity.COMMON.salvageMultiplier)

    val starterSlots = GameSeedData.initialInventorySlots()
    val poorSurvivor = com.example.data.local.SurvivorEntity(wood = 2, metal = 2, crystals = 0)
    val stockedSurvivor = com.example.data.local.SurvivorEntity(wood = 40, metal = 40, crystals = 20)

    val bandageRecipe = com.example.model.CraftingStationCatalog.findById("craft_field_bandage")!!
    val nanoSerumRecipe = com.example.model.CraftingStationCatalog.findById("craft_nano_serum")!!
    val turretRecipe = com.example.model.CraftingStationCatalog.findById("craft_turret_module")!!
    val quantumBatteryRecipe = com.example.model.CraftingStationCatalog.findById("craft_quantum_battery")!!

    assertFalse(com.example.model.CraftingStationCatalog.canCraft(bandageRecipe, poorSurvivor, starterSlots))
    assertTrue(com.example.model.CraftingStationCatalog.canCraft(bandageRecipe, stockedSurvivor, starterSlots))
    assertTrue(com.example.model.CraftingStationCatalog.canCraft(nanoSerumRecipe, stockedSurvivor, starterSlots))
    assertTrue(com.example.model.CraftingStationCatalog.canCraft(turretRecipe, stockedSurvivor, starterSlots))
    // Quantum battery requires a Boss Apex Core not in starterSlots
    assertFalse(com.example.model.CraftingStationCatalog.canCraft(quantumBatteryRecipe, stockedSurvivor, starterSlots))
  }

  @Test
  fun threatMeterDifficultyManager_updatesRealTimeFromProximityAndNoise() {
    val stats = PlayerStats(health = 90, maxHealth = 100, stamina = 90, hunger = 85, thirst = 85)
    val quietFar = DifficultyManager.evaluate(
      stats = stats,
      elapsedSeconds = 40L,
      playerLevel = 2,
      weather = WeatherType.CLEAR,
      biome = BiomeType.DEAD_CITY,
      noiseLevel = 12f,
      nearestEnemyDistance = 0.85f,
      nearbyHostileCount = 0,
    )
    val loudSwarmClose = DifficultyManager.evaluate(
      stats = stats,
      elapsedSeconds = 40L,
      playerLevel = 2,
      weather = WeatherType.CLEAR,
      biome = BiomeType.DEAD_CITY,
      noiseLevel = 92f,
      nearestEnemyDistance = 0.08f,
      nearbyHostileCount = 3,
    )

    assertEquals(0, quietFar.proximityPercent)
    assertEquals(12, quietFar.noiseLevel)
    assertTrue(loudSwarmClose.proximityPercent > 80)
    assertEquals(92, loudSwarmClose.noiseLevel)
    assertEquals(3, loudSwarmClose.nearbyHostileCount)
    assertTrue(loudSwarmClose.nearestEnemyDistanceMeters < quietFar.nearestEnemyDistanceMeters)
    assertTrue(loudSwarmClose.liveAlertFactor > quietFar.liveAlertFactor)
    assertTrue(loudSwarmClose.threatProgress > quietFar.threatProgress)
    assertTrue(loudSwarmClose.detectionRangeMultiplier > quietFar.detectionRangeMultiplier)
    assertTrue(loudSwarmClose.speedMultiplier > quietFar.speedMultiplier)
  }
}
