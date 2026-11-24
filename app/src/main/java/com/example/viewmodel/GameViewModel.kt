package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.CombatSfx
import com.example.audio.TacticalAudioEngine
import com.example.data.local.BaseBuildingEntity
import com.example.data.local.InventorySlotEntity
import com.example.data.local.MissionEntity
import com.example.data.local.PlayerStats
import com.example.data.local.SquadMateEntity
import com.example.data.local.SurvivorEntity
import com.example.data.local.WeaponEntity
import com.example.data.repository.SurvivalRepository
import com.example.model.AbilityBonusSummary
import com.example.model.AbilityTreeCatalog
import com.example.model.AppTab
import com.example.model.ArenaProjectile
import com.example.model.BaseWaveState
import com.example.model.BaseZoneRules
import com.example.model.BiomeType
import com.example.model.CombatHitbox
import com.example.model.CombatMoveType
import com.example.model.CombatState
import com.example.model.CraftingCategory
import com.example.model.CraftingStationCatalog
import com.example.model.DangerZone
import com.example.model.DifficultyManager
import com.example.model.DifficultyState
import com.example.model.DirectionalHitIndicator
import com.example.model.Enemy
import com.example.model.EnemyAiState
import com.example.model.FloatingHit
import com.example.model.LootDropTable
import com.example.model.LootRarity
import com.example.model.NodeKind
import com.example.model.Player
import com.example.model.PlayerActionState
import com.example.model.ThreatTier
import com.example.model.TimeOfDay
import com.example.model.WeaponCategory
import com.example.model.WeatherType
import com.example.model.WorldNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class GameUiState(
  val isLoading: Boolean = true,
  val activeTab: AppTab = AppTab.WORLD,
  val playerStats: PlayerStats = PlayerStats(),
  val player: Player = Player(),
  val survivor: SurvivorEntity = SurvivorEntity(),
  val difficulty: DifficultyState = DifficultyState(),
  val audioMuted: Boolean = false,
  val weapons: List<WeaponEntity> = emptyList(),
  val buildings: List<BaseBuildingEntity> = emptyList(),
  val missions: List<MissionEntity> = emptyList(),
  val squadMates: List<SquadMateEntity> = emptyList(),
  val inventorySlots: List<InventorySlotEntity> = emptyList(),
  val isInventoryOpen: Boolean = false,
  val isCraftingOpen: Boolean = false,
  val unlockedAbilityIds: Set<String> = emptySet(),
  val abilityBonuses: AbilityBonusSummary = AbilityBonusSummary(),
  val availableAbilityPoints: Int = 0,
  val isAbilityTreeOpen: Boolean = false,
  val resourceNodes: List<WorldNode> = emptyList(),
  val enemies: List<Enemy> = emptyList(),
  val activeCombatEnemy: Enemy? = null,
  val combat: CombatState? = null,
  val selectedMissionForBriefing: MissionEntity? = null,
  val worldDamageSeq: Long = 0L,
  val worldHitIntensity: Float = 1f,
  val worldDirectionalHits: List<DirectionalHitIndicator> = emptyList(),
  val baseWave: BaseWaveState = BaseWaveState(),
  val statusBanner: String? = null,
  val campYieldReady: Boolean = true,
) {
  fun effectiveMoveCost(move: CombatMoveType): Int = when (move) {
    CombatMoveType.DODGE -> (move.staminaCost - abilityBonuses.dodgeCostReduction).coerceAtLeast(6)
    CombatMoveType.SPRINT_STRIKE, CombatMoveType.HEAVY_OVERHEAD ->
      (move.staminaCost * (1f - abilityBonuses.specialStaminaDiscount)).roundToInt().coerceAtLeast(8)
    else -> move.staminaCost
  }
  val playerX: Float get() = player.x
  val playerY: Float get() = player.y
  val playerFacingRad: Float get() = player.facingRad

  val equippedWeapon: WeaponEntity?
    get() = weapons.firstOrNull { it.id == survivor.equippedWeaponId } ?: weapons.firstOrNull { it.unlocked }

  val currentBiome: BiomeType
    get() = BiomeType.fromId(survivor.activeBiomeId)

  val currentWeather: WeatherType
    get() = WeatherType.fromIndex(survivor.weatherIndex)

  val currentTimeOfDay: TimeOfDay
    get() = TimeOfDay.fromIndex(survivor.timeOfDayIndex)

  val forgeBonusMultiplier: Float
    get() {
      val forgeLv = buildings.firstOrNull { it.id == "forge" }?.level ?: 1
      return 1f + (forgeLv - 1) * 0.10f
    }

  val defenseRating: Int
    get() {
      val bunkerLv = buildings.firstOrNull { it.id == "bunker" }?.level ?: 1
      val turretLv = buildings.firstOrNull { it.id == "turrets" }?.level ?: 0
      return bunkerLv * 20 + turretLv * 15
    }

  val activeSquadPower: Int
    get() {
      val radioLv = buildings.firstOrNull { it.id == "radio" }?.level ?: 1
      val base = squadMates.filter { it.isSelected }.sumOf { it.power }
      return (base * (1f + (radioLv - 1) * 0.15f)).toInt()
    }
}

private data class TransientUiState(
  val activeTab: AppTab = AppTab.WORLD,
  val playerX: Float = 0.50f,
  val playerY: Float = 0.56f,
  val playerFacingRad: Float = 0f,
  val playerActionState: PlayerActionState = PlayerActionState.IDLE,
  val elapsedSeconds: Long = 0L,
  val noiseLevel: Float = 12f,
  val audioMuted: Boolean = false,
  val resourceNodes: List<WorldNode> = emptyList(),
  val enemies: List<Enemy> = emptyList(),
  val activeCombatEnemy: Enemy? = null,
  val combat: CombatState? = null,
  val selectedMissionForBriefing: MissionEntity? = null,
  val isInventoryOpen: Boolean = false,
  val isCraftingOpen: Boolean = false,
  val isAbilityTreeOpen: Boolean = false,
  val worldDamageSeq: Long = 0L,
  val worldHitIntensity: Float = 1f,
  val worldDirectionalHits: List<DirectionalHitIndicator> = emptyList(),
  val baseWave: BaseWaveState = BaseWaveState(),
  val statusBanner: String? = null,
  val campYieldReady: Boolean = true,
  val seeded: Boolean = false,
)

class GameViewModel(
  private val repository: SurvivalRepository,
  private val audioEngine: TacticalAudioEngine? = null,
) : ViewModel() {

  private val transientState = MutableStateFlow(TransientUiState())
  private var combatLoopJob: Job? = null
  private var hitIdCounter = 1L
  private var projectileIdCounter = 1L
  private var lastAnnouncedThreatTier: ThreatTier = ThreatTier.LOW

  @Volatile
  private var joystickVx: Float = 0f

  @Volatile
  private var joystickVy: Float = 0f

  val uiState: StateFlow<GameUiState> = combine(
    repository.playerStatsFlow,
    repository.survivorFlow,
    repository.weaponsFlow,
    repository.buildingsFlow,
    repository.missionsFlow,
    repository.squadMatesFlow,
    repository.inventoryFlow,
    transientState,
  ) { args ->
    val stats = args[0] as? PlayerStats
    val survivor = args[1] as? SurvivorEntity
    @Suppress("UNCHECKED_CAST")
    val weapons = args[2] as List<WeaponEntity>
    @Suppress("UNCHECKED_CAST")
    val buildings = args[3] as List<BaseBuildingEntity>
    @Suppress("UNCHECKED_CAST")
    val missions = args[4] as List<MissionEntity>
    @Suppress("UNCHECKED_CAST")
    val squadMates = args[5] as List<SquadMateEntity>
    @Suppress("UNCHECKED_CAST")
    val inventorySlots = args[6] as List<InventorySlotEntity>
    val transient = args[7] as TransientUiState

    if (survivor == null || stats == null || !transient.seeded) {
      GameUiState(isLoading = true)
    } else {
      val syncedSurvivor = survivor.copy(
        hp = stats.health,
        maxHp = stats.maxHealth,
        stamina = stats.stamina,
        food = stats.hunger,
        water = stats.thirst,
      )
      val bunkerLv = buildings.firstOrNull { it.id == "bunker" }?.level ?: 1
      val turretLv = buildings.firstOrNull { it.id == "turrets" }?.level ?: 0
      val forgeLv = buildings.firstOrNull { it.id == "forge" }?.level ?: 1
      val biome = BiomeType.fromId(syncedSurvivor.activeBiomeId)
      val weather = WeatherType.fromIndex(syncedSurvivor.weatherIndex)
      val combatNow = transient.combat
      val (nearestDist, nearbyCount) = if (combatNow != null) {
        val cDist = hypot(
          (combatNow.arenaEnemyX - combatNow.arenaPlayerX).toDouble(),
          (combatNow.arenaEnemyY - combatNow.arenaPlayerY).toDouble(),
        ).toFloat()
        cDist to 1
      } else {
        val distances = transient.enemies.map { e ->
          hypot((e.x - transient.playerX).toDouble(), (e.y - transient.playerY).toDouble()).toFloat()
        }
        val minDist = distances.minOrNull() ?: 1.0f
        val closeCount = distances.count { it <= 0.36f }
        minDist to closeCount
      }
      val difficulty = DifficultyManager.evaluate(
        stats = stats,
        elapsedSeconds = transient.elapsedSeconds,
        playerLevel = syncedSurvivor.level,
        weather = weather,
        biome = biome,
        noiseLevel = transient.noiseLevel,
        nearestEnemyDistance = nearestDist,
        nearbyHostileCount = nearbyCount,
      )

      val unlockedIds = AbilityTreeCatalog.parseUnlockedIds(syncedSurvivor.unlockedAbilityIds)
      val abilityBonuses = AbilityTreeCatalog.summarize(unlockedIds)
      val availableAp = AbilityTreeCatalog.availablePoints(
        level = syncedSurvivor.level,
        missionsCompleted = syncedSurvivor.missionsCompleted,
        unlockedIds = unlockedIds,
      )

      val playerModel = Player.fromSurvivor(
        survivor = syncedSurvivor,
        x = transient.playerX,
        y = transient.playerY,
        facingRad = transient.playerFacingRad,
        defenseRating = bunkerLv * 20 + turretLv * 15,
        forgeMultiplier = 1f + (forgeLv - 1) * 0.10f,
        actionState = transient.playerActionState,
        abilityBonuses = abilityBonuses,
      )
      GameUiState(
        isLoading = false,
        activeTab = transient.activeTab,
        playerStats = stats,
        player = playerModel,
        survivor = syncedSurvivor,
        difficulty = difficulty,
        audioMuted = transient.audioMuted,
        weapons = weapons,
        buildings = buildings,
        missions = missions,
        squadMates = squadMates,
        inventorySlots = inventorySlots,
        isInventoryOpen = transient.isInventoryOpen,
        isCraftingOpen = transient.isCraftingOpen,
        unlockedAbilityIds = unlockedIds,
        abilityBonuses = abilityBonuses,
        availableAbilityPoints = availableAp,
        isAbilityTreeOpen = transient.isAbilityTreeOpen,
        resourceNodes = transient.resourceNodes,
        enemies = transient.enemies,
        activeCombatEnemy = transient.activeCombatEnemy,
        combat = transient.combat,
        selectedMissionForBriefing = transient.selectedMissionForBriefing,
        worldDamageSeq = transient.worldDamageSeq,
        worldHitIntensity = transient.worldHitIntensity,
        worldDirectionalHits = transient.worldDirectionalHits,
        baseWave = transient.baseWave.copy(
          inBaseArea = BaseZoneRules.isInsideBase(playerModel.x, playerModel.y),
          maxBaseIntegrity = BaseZoneRules.maxBaseIntegrity(bunkerLv),
        ),
        statusBanner = transient.statusBanner,
        campYieldReady = transient.campYieldReady,
      )
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = GameUiState(isLoading = true),
  )

  init {
    viewModelScope.launch {
      repository.ensureSeeded()
      val initialBiome = BiomeType.DEAD_CITY
      transientState.update {
        it.copy(
          seeded = true,
          resourceNodes = generateResourceNodes(initialBiome, WeatherType.RAIN),
          enemies = generateBiomeEnemies(initialBiome, playerLevel = 1, difficulty = DifficultyState()),
        )
      }
      startRealtimeActionLoop()
      startSurvivalVitalsLoop()
    }
    viewModelScope.launch {
      repository.survivorFlow
        .filterNotNull()
        .map { WeatherType.fromIndex(it.weatherIndex) }
        .distinctUntilChanged()
        .collect { weather ->
          audioEngine?.setAmbientWeather(weather)
        }
    }
  }

  /**
   * High-frequency (~80ms) real-time engine:
   * - Applies smooth Left-Thumb Virtual Joystick velocity to Player in World OR Combat Arena
   * - Runs active Enemy patrol, aggro detection, pursuit & harass strikes on the World Map
   * - Tracks survival time for Global Difficulty Manager
   */
  private fun startRealtimeActionLoop() {
    viewModelScope.launch {
      var fastTick = 0
      while (true) {
        delay(80L)
        fastTick++
        val state = uiState.value
        if (state.isLoading) continue

        if (fastTick % 12 == 0) {
          transientState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1L) }
          stepBaseWaveTick(uiState.value, fastTick)
        } else if (fastTick % 25 == 0) {
          stepBaseTurretAndSiege(uiState.value)
        }

        // Decay World directional hit indicators smoothly (~80ms per tick)
        if (transientState.value.worldDirectionalHits.isNotEmpty()) {
          transientState.update { cur ->
            cur.copy(
              worldDirectionalHits = cur.worldDirectionalHits
                .map { it.copy(ttlTicks = it.ttlTicks - 1) }
                .filter { it.ttlTicks > 0 }
            )
          }
        }

        // Notify AI Voice when Threat Tier escalates
        if (state.difficulty.threatTier.ordinal > lastAnnouncedThreatTier.ordinal) {
          lastAnnouncedThreatTier = state.difficulty.threatTier
          audioEngine?.speak("Warning: Threat level ${state.difficulty.threatTier.label}. Hostile speed and damage rising.")
        }

        val vx = joystickVx
        val vy = joystickVy
        val hasInput = hypot(vx.toDouble(), vy.toDouble()) > 0.08

        // Live Acoustic Noise Simulation: spikes with movement/combat, settles toward weather ambient floor
        val ambientNoiseFloor = when (state.currentWeather) {
          WeatherType.ASH_FOG -> 8f
          WeatherType.CLEAR -> 12f
          WeatherType.RAIN -> 16f
          WeatherType.BIO_STORM -> 24f
        }
        val moveNoiseTarget = if (hasInput) 42f else ambientNoiseFloor
        transientState.update { cur ->
          val nextNoise = if (cur.noiseLevel > moveNoiseTarget) {
            (cur.noiseLevel - 3.6f).coerceAtLeast(ambientNoiseFloor)
          } else {
            (cur.noiseLevel + 2.8f).coerceAtMost(moveNoiseTarget)
          }
          cur.copy(noiseLevel = nextNoise)
        }

        if (state.combat == null && state.activeTab == AppTab.WORLD) {
          if (hasInput) {
            movePlayerBy(vx * 0.022f, vy * 0.022f)
          } else if (transientState.value.playerActionState == PlayerActionState.MOVING) {
            transientState.update { it.copy(playerActionState = PlayerActionState.IDLE) }
          }

          // Active World Enemy AI every 80ms so monsters smoothly patrol, alert, and chase!
          stepEnemiesWorldAi(uiState.value)
        } else if (state.combat != null && !state.combat.victory && !state.combat.defeat) {
          if (hasInput) {
            moveArenaPlayerBy(vx * 0.024f, vy * 0.024f)
          }
        }
      }
    }
  }

  private fun stepBaseWaveTick(state: GameUiState, fastTick: Int) {
    if (state.activeTab != AppTab.WORLD || state.combat != null) return
    val inBase = BaseZoneRules.isInsideBase(state.player.x, state.player.y)
    val raidersRemaining = transientState.value.enemies.count { it.isBaseRaider }
    val wave = transientState.value.baseWave

    if (wave.waveActive) {
      if (raidersRemaining == 0) {
        completeBaseWave(state)
      } else {
        transientState.update {
          it.copy(baseWave = wave.copy(inBaseArea = inBase, remainingEnemiesInWave = raidersRemaining))
        }
      }
      return
    }

    if (inBase) {
      val nextCountdown = (wave.countdownSeconds - 1).coerceAtLeast(0)
      if (nextCountdown == 0) {
        spawnBaseWave(state)
      } else {
        transientState.update {
          it.copy(baseWave = wave.copy(inBaseArea = true, countdownSeconds = nextCountdown))
        }
      }
    } else if (wave.inBaseArea) {
      transientState.update {
        it.copy(baseWave = wave.copy(inBaseArea = false))
      }
    }
    if (fastTick % 24 == 0) {
      stepBaseTurretAndSiege(uiState.value)
    }
  }

  private fun stepBaseTurretAndSiege(state: GameUiState) {
    if (state.activeTab != AppTab.WORLD || state.combat != null) return
    val wave = transientState.value.baseWave
    if (!wave.waveActive) return

    val turretLv = state.buildings.firstOrNull { it.id == "turrets" }?.level ?: 0
    val bunkerLv = state.buildings.firstOrNull { it.id == "bunker" }?.level ?: 1
    val maxIntegrity = BaseZoneRules.maxBaseIntegrity(bunkerLv)
    var currentEnemies = transientState.value.enemies
    var turretKillReward = 0
    var lasersFired = wave.turretLasersFired

    // Automated Perimeter Laser Turrets engage nearest Base Raider
    if (turretLv > 0) {
      val target = currentEnemies
        .filter { it.isBaseRaider }
        .minByOrNull { hypot((it.x - BaseZoneRules.BASE_CENTER_X).toDouble(), (it.y - BaseZoneRules.BASE_CENTER_Y).toDouble()) }
      if (target != null) {
        val distToBase = hypot((target.x - BaseZoneRules.BASE_CENTER_X).toDouble(), (target.y - BaseZoneRules.BASE_CENTER_Y).toDouble()).toFloat()
        if (distToBase <= BaseZoneRules.BASE_RADIUS * 1.95f) {
          val turretDmg = (BaseZoneRules.turretBarrageDamage(turretLv) * state.abilityBonuses.turretDamageMultiplier).roundToInt()
          lasersFired++
          val remHp = target.hp - turretDmg
          currentEnemies = if (remHp <= 0) {
            turretKillReward++
            currentEnemies.filterNot { it.id == target.id }
          } else {
            currentEnemies.map { if (it.id == target.id) it.copy(hp = remHp) else it }
          }
        }
      }
    }

    // Raiders touching Base Core damage Base Integrity
    val breachingRaiders = currentEnemies.count {
      it.isBaseRaider && hypot((it.x - BaseZoneRules.BASE_CENTER_X).toDouble(), (it.y - BaseZoneRules.BASE_CENTER_Y).toDouble()) <= 0.12
    }
    var nextIntegrity = wave.baseIntegrity
    var banner: String? = null
    if (breachingRaiders > 0) {
      val breachDmg = (breachingRaiders * 3 * state.difficulty.damageMultiplier).toInt().coerceAtLeast(2)
      nextIntegrity = (nextIntegrity - breachDmg).coerceAtLeast(0)
      if (nextIntegrity == 0) {
        nextIntegrity = (maxIntegrity * 0.55f).toInt()
        currentEnemies = currentEnemies.filterNot { it.isBaseRaider }
        banner = "BASE PERIMETER BREACHED! Emergency Lockdown cleared raiders & restored 55% Base HP."
        audioEngine?.speak("Base perimeter breached! Emergency lockdown engaged.", force = true)
      }
    }

    val remaining = currentEnemies.count { it.isBaseRaider }
    transientState.update {
      it.copy(
        enemies = currentEnemies,
        baseWave = it.baseWave.copy(
          baseIntegrity = nextIntegrity.coerceAtMost(maxIntegrity),
          maxBaseIntegrity = maxIntegrity,
          remainingEnemiesInWave = remaining,
          turretLasersFired = lasersFired,
        ),
        statusBanner = banner ?: it.statusBanner,
      )
    }

    if (turretKillReward > 0) {
      viewModelScope.launch {
        val s = uiState.value.survivor
        repository.updateSurvivor(applyLevelUp(s.copy(metal = s.metal + 8 * turretKillReward, xp = s.xp + 25 * turretKillReward)))
      }
    }
    if (remaining == 0 && transientState.value.baseWave.waveActive) {
      completeBaseWave(uiState.value)
    }
  }

  private fun spawnBaseWave(state: GameUiState) {
    val wave = transientState.value.baseWave
    val waveNum = wave.waveNumber
    val count = BaseZoneRules.computeWaveEnemyCount(waveNum, state.difficulty)
    val scaledLevel = BaseZoneRules.computeWaveEnemyLevel(state.survivor.level, waveNum, state.difficulty)
    val hasBoss = BaseZoneRules.isBossWave(waveNum, state.difficulty)
    val bunkerLv = state.buildings.firstOrNull { it.id == "bunker" }?.level ?: 1
    val maxIntegrity = BaseZoneRules.maxBaseIntegrity(bunkerLv)

    val biome = state.currentBiome
    val spawned = (0 until count).map { idx ->
      val angle = (idx.toFloat() / count) * 6.283185f + 0.35f
      val spawnR = 0.38f
      val ex = (BaseZoneRules.BASE_CENTER_X + cos(angle) * spawnR).coerceIn(0.08f, 0.92f)
      val ey = (BaseZoneRules.BASE_CENTER_Y + sin(angle) * spawnR * 0.78f).coerceIn(0.16f, 0.86f)
      val isBossUnit = hasBoss && idx == 0
      val name = if (isBossUnit) "Wave-$waveNum Siege Boss" else "Wave-$waveNum Raider"
      Enemy.createForBiome(
        id = "wave_${waveNum}_${idx}_${Random.nextInt(9999)}",
        name = name,
        biome = biome,
        isBoss = isBossUnit,
        x = ex,
        y = ey,
        playerLevel = scaledLevel,
        difficulty = state.difficulty,
      ).copy(
        aiState = EnemyAiState.CHASE,
        isBaseRaider = true,
      )
    }

    audioEngine?.playSfx(if (hasBoss) CombatSfx.BOSS_SIREN else CombatSfx.HEAVY_IMPACT)
    audioEngine?.speak(
      if (hasBoss) "Base Siege Wave $waveNum incoming! Giant Siege Boss detected!"
      else "Base Wave $waveNum approaching perimeter! Threat ${state.difficulty.threatTier.label}."
    )

    transientState.update {
      it.copy(
        enemies = it.enemies + spawned,
        baseWave = wave.copy(
          waveActive = true,
          countdownSeconds = 0,
          remainingEnemiesInWave = spawned.size,
          waveEnemyCount = spawned.size,
          isBossWave = hasBoss,
          maxBaseIntegrity = maxIntegrity,
          baseIntegrity = wave.baseIntegrity.coerceAtMost(maxIntegrity),
        ),
        statusBanner = "BASE WAVE $waveNum TRIGGERED! $count Threat-Scaled Raiders (Lv.$scaledLevel) converging!",
      )
    }
  }

  private fun completeBaseWave(state: GameUiState) {
    val wave = transientState.value.baseWave
    val clearedWave = wave.waveNumber
    val bunkerLv = state.buildings.firstOrNull { it.id == "bunker" }?.level ?: 1
    val maxIntegrity = BaseZoneRules.maxBaseIntegrity(bunkerLv)
    val xpBonus = 30 * clearedWave
    val metalBonus = 14 + clearedWave * 4
    val woodBonus = 10 + clearedWave * 2
    val crystalBonus = 2 + clearedWave / 2

    audioEngine?.playSfx(CombatSfx.VICTORY)
    audioEngine?.speak("Base Wave $clearedWave cleared! Perimeter holding.")

    viewModelScope.launch {
      val s = uiState.value.survivor
      repository.updateSurvivor(
        applyLevelUp(
          s.copy(
            xp = s.xp + xpBonus,
            metal = s.metal + metalBonus,
            wood = s.wood + woodBonus,
            crystals = s.crystals + crystalBonus,
          )
        )
      )
    }

    transientState.update {
      it.copy(
        baseWave = wave.copy(
          waveNumber = clearedWave + 1,
          waveActive = false,
          countdownSeconds = BaseZoneRules.WAVE_COUNTDOWN_NEXT_SEC,
          remainingEnemiesInWave = 0,
          waveEnemyCount = 0,
          isBossWave = false,
          baseIntegrity = (wave.baseIntegrity + 20).coerceAtMost(maxIntegrity),
          maxBaseIntegrity = maxIntegrity,
          wavesSurvived = wave.wavesSurvived + 1,
        ),
        statusBanner = "WAVE $clearedWave CLEARED! +$metalBonus Metal, +$woodBonus Wood, +$crystalBonus Crystals!",
      )
    }
  }

  fun triggerBaseWaveNow() {
    val state = uiState.value
    if (state.combat != null || transientState.value.baseWave.waveActive) return
    if (!BaseZoneRules.isInsideBase(state.player.x, state.player.y)) {
      transientState.update {
        it.copy(
          playerX = BaseZoneRules.BASE_CENTER_X,
          playerY = BaseZoneRules.BASE_CENTER_Y,
        )
      }
    }
    spawnBaseWave(uiState.value)
  }

  fun deployToBaseDefense() {
    transientState.update {
      it.copy(
        activeTab = AppTab.WORLD,
        playerX = BaseZoneRules.BASE_CENTER_X,
        playerY = BaseZoneRules.BASE_CENTER_Y,
      )
    }
    if (!transientState.value.baseWave.waveActive) {
      spawnBaseWave(uiState.value)
    }
  }

  /**
   * Slower (~1500ms) survival vitals & dynamic weather cycle loop.
   */
  private fun startSurvivalVitalsLoop() {
    viewModelScope.launch {
      var tick = 0
      while (true) {
        delay(1500L)
        tick++
        val state = uiState.value
        if (state.isLoading) continue

        val current = state.survivor
        val playerModel = state.player
        val weather = state.currentWeather
        val isHarsh = weather == WeatherType.BIO_STORM || state.currentBiome == BiomeType.FROSTPEAK_RIDGE

        val newStamina = (current.stamina + playerModel.staminaRegenForWeather(weather)).coerceAtMost(100)
        val foodInterval = if (playerModel.abilityBonuses.drainResistance) 6 else 3
        val waterInterval = if (playerModel.abilityBonuses.drainResistance) 4 else 2
        val foodDrain = if (tick % foodInterval == 0) (if (isHarsh) 2 else 1) else 0
        val waterDrain = if (tick % waterInterval == 0) (if (isHarsh) 2 else 1) else 0
        val newFood = (current.food - foodDrain).coerceAtLeast(0)
        val newWater = (current.water - waterDrain).coerceAtLeast(0)

        var newHp = current.hp
        var starvationAlert: String? = null
        if (newFood < 20 || newWater < 20) {
          newHp = (current.hp - 3).coerceAtLeast(1)
          if (tick % 5 == 0) {
            starvationAlert = "CRITICAL: Low Hunger/Thirst escalating Enemy Threat!"
            audioEngine?.speak("Vitals critical. Enemy aggression escalating.")
          }
        } else if (newFood > 55 && newWater > 55 && state.combat == null && tick % 4 == 0) {
          newHp = (current.hp + 1).coerceAtMost(current.maxHp)
        }

        val nextTimeIdx = if (current.autoCycleWeather && tick % 9 == 0) {
          (current.timeOfDayIndex + 1) % TimeOfDay.entries.size
        } else current.timeOfDayIndex
        val nextWeatherIdx = if (current.autoCycleWeather && tick % 14 == 0) {
          (current.weatherIndex + 1) % WeatherType.entries.size
        } else current.weatherIndex

        if (tick % 14 == 0) {
          transientState.update { it.copy(campYieldReady = true) }
        }

        repository.updateSurvivor(
          current.copy(
            stamina = newStamina,
            food = newFood,
            water = newWater,
            hp = newHp,
            timeOfDayIndex = nextTimeIdx,
            weatherIndex = nextWeatherIdx,
          )
        )
        if (starvationAlert != null && transientState.value.statusBanner == null) {
          transientState.update { it.copy(statusBanner = starvationAlert) }
        }
      }
    }
  }

  private fun stepEnemiesWorldAi(state: GameUiState) {
    var ambushingEnemy: Enemy? = null
    var harassDamageTotal = 0
    var harassSourceEnemy: Enemy? = null
    val updatedEnemies = transientState.value.enemies.map { enemy ->
      val (stepped, caughtPlayer, harassDmg) = enemy.stepWorldAi(
        player = state.player,
        weather = state.currentWeather,
        difficulty = state.difficulty,
      )
      if (caughtPlayer && ambushingEnemy == null) {
        ambushingEnemy = stepped
      }
      if (harassDmg > 0 && harassSourceEnemy == null) {
        harassSourceEnemy = stepped
      }
      harassDamageTotal += harassDmg
      stepped
    }
    transientState.update { it.copy(enemies = updatedEnemies) }

    if (harassDamageTotal > 0 && ambushingEnemy == null) {
      val reduced = state.player.takeDamage(harassDamageTotal, dodging = false)
      val nextHp = (state.survivor.hp - reduced).coerceAtLeast(1)
      val src = harassSourceEnemy
      val angle = if (src != null) {
        atan2((src.y - state.player.y).toDouble(), (src.x - state.player.x).toDouble()).toFloat()
      } else 0f
      val intensity = (reduced / 20f).coerceIn(0.6f, 1.35f)
      audioEngine?.playSfx(CombatSfx.PLAYER_HURT)
      viewModelScope.launch {
        repository.updatePlayerVitals(nextHp, state.survivor.maxHp, state.survivor.stamina, state.survivor.food, state.survivor.water)
      }
      transientState.update {
        it.copy(
          worldDamageSeq = it.worldDamageSeq + 1L,
          worldHitIntensity = intensity,
          worldDirectionalHits = (it.worldDirectionalHits + DirectionalHitIndicator(
            id = hitIdCounter++,
            angleRad = angle,
            damage = reduced,
            intensity = intensity,
            ttlTicks = 15,
          )).takeLast(3),
          statusBanner = "HOSTILE STRIKE! ${src?.name ?: "Monster"} scratched you (-$reduced HP) — Fight or Dash!",
        )
      }
    }

    ambushingEnemy?.let { ambush ->
      transientState.update {
        it.copy(
          enemies = it.enemies.filterNot { e -> e.id == ambush.id },
          statusBanner = "AMBUSH! ${ambush.name} (${ambush.aiState.badge}) engaged you!",
        )
      }
      audioEngine?.speak("Hostile ambush! Engaging ${ambush.name}.")
      startCombat(
        enemy = ambush,
        mission = null,
        isCoopRaid = false,
      )
    }
  }

  fun setJoystickVector(vx: Float, vy: Float) {
    joystickVx = vx.coerceIn(-1f, 1f)
    joystickVy = vy.coerceIn(-1f, 1f)
  }

  fun toggleAudioMute() {
    val muted = audioEngine?.toggleMute() ?: !transientState.value.audioMuted
    transientState.update {
      it.copy(
        audioMuted = muted,
        statusBanner = if (muted) "AI Audio & Combat SFX muted" else "AI Voice & Combat SFX enabled",
      )
    }
  }

  fun selectTab(tab: AppTab) {
    transientState.update { it.copy(activeTab = tab, statusBanner = null) }
  }

  fun dismissBanner() {
    transientState.update { it.copy(statusBanner = null) }
  }

  fun openInventory() {
    transientState.update { it.copy(isInventoryOpen = true) }
  }

  fun closeInventory() {
    transientState.update { it.copy(isInventoryOpen = false) }
  }

  fun openCraftingStation() {
    transientState.update { it.copy(isCraftingOpen = true, isInventoryOpen = false) }
  }

  fun closeCraftingStation() {
    transientState.update { it.copy(isCraftingOpen = false) }
  }

  fun craftRecipe(recipeId: String) {
    val state = uiState.value
    val recipe = CraftingStationCatalog.findById(recipeId) ?: return
    val s = state.survivor
    if (!CraftingStationCatalog.canCraft(recipe, s, state.inventorySlots)) {
      transientState.update {
        it.copy(statusBanner = "Missing required resources/materials for ${recipe.name}.")
      }
      return
    }
    viewModelScope.launch {
      // 1. Deduct core resources + Grid Inventory reagents
      var nextSurvivor = s.copy(
        wood = (s.wood - recipe.woodCost).coerceAtLeast(0),
        metal = (s.metal - recipe.metalCost).coerceAtLeast(0),
        crystals = (s.crystals - recipe.crystalCost).coerceAtLeast(0),
        xp = s.xp + 18,
      )
      recipe.gridIngredients.forEach { ing ->
        repository.consumeItemById(ing.itemId, ing.quantity)
      }

      // 2. Apply Ammunition, Medical Supply, or Base Upgrade output
      var craftMessage = "Crafted ${recipe.name}!"
      when (recipe.category) {
        CraftingCategory.AMMUNITION -> {
          audioEngine?.playSfx(CombatSfx.PLASMA_SHOT)
          if (recipe.outputItemId == "energy_cell") {
            nextSurvivor = nextSurvivor.copy(energyCells = nextSurvivor.energyCells + recipe.outputQty)
          } else {
            repository.addInventoryItem(
              itemId = recipe.outputItemId,
              name = recipe.name.substringBefore(" ("),
              category = recipe.outputCategory,
              deltaQty = recipe.outputQty,
              description = recipe.summary,
              rarity = recipe.rarity.name,
            )
          }
        }
        CraftingCategory.MEDICAL -> {
          audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
          if (recipe.outputItemId == "tactical_medkit") {
            nextSurvivor = nextSurvivor.copy(medkits = nextSurvivor.medkits + recipe.outputQty)
          } else {
            repository.addInventoryItem(
              itemId = recipe.outputItemId,
              name = recipe.name.substringBefore(" ("),
              category = recipe.outputCategory,
              deltaQty = recipe.outputQty,
              description = recipe.summary,
              rarity = recipe.rarity.name,
            )
          }
        }
        CraftingCategory.BASE_UPGRADE -> {
          audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
          if (recipe.targetBuildingId != null) {
            val targetBuilding = state.buildings.firstOrNull { it.id == recipe.targetBuildingId }
            if (targetBuilding != null && targetBuilding.level < targetBuilding.maxLevel) {
              val newLv = targetBuilding.level + 1
              val hpBoost = if (targetBuilding.id == "bunker") 25 else 0
              nextSurvivor = nextSurvivor.copy(
                maxHp = nextSurvivor.maxHp + hpBoost,
                hp = (nextSurvivor.hp + hpBoost).coerceAtMost(nextSurvivor.maxHp + hpBoost),
                xp = nextSurvivor.xp + 30,
              )
              repository.upsertBuilding(
                targetBuilding.copy(
                  level = newLv,
                  woodCost = (targetBuilding.woodCost * 1.45f).toInt(),
                  metalCost = (targetBuilding.metalCost * 1.45f).toInt(),
                  crystalCost = (targetBuilding.crystalCost * 1.45f).toInt() + 2,
                )
              )
              craftMessage = "${targetBuilding.name} upgraded to Lv.$newLv via Crafting Station!"
            } else {
              // Building at max level: convert kit into reinforced Base Integrity boost
              val maxInt = state.baseWave.maxBaseIntegrity
              transientState.update {
                it.copy(baseWave = it.baseWave.copy(baseIntegrity = (it.baseWave.baseIntegrity + 55).coerceAtMost(maxInt)))
              }
              craftMessage = "Module maxed! Base Integrity reinforced (+55 HP)."
            }
          } else {
            repository.addInventoryItem(
              itemId = recipe.outputItemId,
              name = recipe.name.substringBefore(" ("),
              category = recipe.outputCategory,
              deltaQty = recipe.outputQty,
              description = recipe.summary,
              rarity = recipe.rarity.name,
            )
          }
        }
      }

      repository.updateSurvivor(applyLevelUp(nextSurvivor))
      emitNoiseSpike(54f)
      transientState.update {
        it.copy(statusBanner = "[${recipe.rarity.label}] $craftMessage")
      }
    }
  }

  private fun emitNoiseSpike(level: Float) {
    transientState.update {
      it.copy(noiseLevel = maxOf(it.noiseLevel, level.coerceIn(0f, 100f)))
    }
  }

  fun openAbilityTree() {
    transientState.update { it.copy(isAbilityTreeOpen = true) }
  }

  fun closeAbilityTree() {
    transientState.update { it.copy(isAbilityTreeOpen = false) }
  }

  fun unlockAbility(nodeId: String) {
    val state = uiState.value
    val node = AbilityTreeCatalog.findById(nodeId) ?: return
    if (!AbilityTreeCatalog.canUnlock(node, state.unlockedAbilityIds, state.availableAbilityPoints)) {
      transientState.update {
        it.copy(statusBanner = "Requires prerequisite unlock and ${node.pointCost} AP.")
      }
      return
    }
    viewModelScope.launch {
      val s = uiState.value.survivor
      val nextIds = AbilityTreeCatalog.parseUnlockedIds(s.unlockedAbilityIds) + node.id
      val hpGain = if (node.id == "bioguard_metabolism") 25 else 0
      audioEngine?.playSfx(CombatSfx.VICTORY)
      audioEngine?.speak("Ability unlocked: ${node.name}.")
      repository.updateSurvivor(
        s.copy(
          unlockedAbilityIds = AbilityTreeCatalog.encodeUnlockedIds(nextIds),
          maxHp = s.maxHp + hpGain,
          hp = (s.hp + hpGain).coerceAtMost(s.maxHp + hpGain),
        )
      )
      transientState.update {
        it.copy(statusBanner = "UNLOCKED ${node.name}: ${node.effectLabel}")
      }
    }
  }

  fun resetAbilityTree() {
    val state = uiState.value
    if (state.unlockedAbilityIds.isEmpty()) return
    viewModelScope.launch {
      val s = uiState.value.survivor
      val hadMetabolism = "bioguard_metabolism" in state.unlockedAbilityIds
      val nextMaxHp = (s.maxHp - if (hadMetabolism) 25 else 0).coerceAtLeast(100)
      audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
      repository.updateSurvivor(
        s.copy(
          unlockedAbilityIds = "",
          maxHp = nextMaxHp,
          hp = s.hp.coerceAtMost(nextMaxHp),
        )
      )
      transientState.update {
        it.copy(statusBanner = "Ability Tree reset — all Ability Points refunded!")
      }
    }
  }

  fun useInventorySlot(slot: InventorySlotEntity) {
    if (slot.itemId.isEmpty() || slot.quantity <= 0) return
    viewModelScope.launch {
      val state = uiState.value
      val s = state.survivor
      val healMult = state.abilityBonuses.healMultiplier
      when (slot.itemId) {
        "ration_pack" -> {
          val hpHeal = (12 * healMult).roundToInt()
          audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
          repository.consumeOrRemoveSlot(slot.slotIndex)
          repository.updatePlayerVitals(
            health = (s.hp + hpHeal).coerceAtMost(s.maxHp),
            maxHealth = s.maxHp,
            stamina = s.stamina,
            hunger = (s.food + 25).coerceAtMost(100),
            thirst = s.water,
          )
          transientState.update { it.copy(statusBanner = "Used MRE Ration Pack (+25 Hunger, +$hpHeal HP)") }
        }
        "purified_water" -> {
          audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
          repository.consumeOrRemoveSlot(slot.slotIndex)
          repository.updatePlayerVitals(
            health = s.hp,
            maxHealth = s.maxHp,
            stamina = (s.stamina + 30).coerceAtMost(100),
            hunger = s.food,
            thirst = (s.water + 28).coerceAtMost(100),
          )
          transientState.update { it.copy(statusBanner = "Used Purified Flask (+28 Thirst, +30 Stamina)") }
        }
        "tactical_medkit" -> {
          if (s.medkits > 0) {
            audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
            repository.updateSurvivor(s.copy(medkits = s.medkits - 1, hp = s.maxHp, immunity = 100))
            transientState.update { it.copy(statusBanner = "Medkit applied — Full Health & Immunity restored!") }
          }
        }
        "field_bandage" -> {
          val hpHeal = (35 * healMult).roundToInt()
          audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
          repository.consumeOrRemoveSlot(slot.slotIndex)
          repository.updateSurvivor(
            s.copy(
              hp = (s.hp + hpHeal).coerceAtMost(s.maxHp),
              immunity = (s.immunity + 15).coerceAtMost(100),
            )
          )
          transientState.update { it.copy(statusBanner = "Applied Field Bandage (+$hpHeal HP, +15 Immunity)") }
        }
        "nano_serum" -> {
          audioEngine?.playSfx(CombatSfx.VICTORY)
          repository.consumeOrRemoveSlot(slot.slotIndex)
          repository.updateSurvivor(
            s.copy(
              hp = s.maxHp,
              stamina = (s.stamina + 45).coerceAtMost(100),
              immunity = 100,
            )
          )
          transientState.update { it.copy(statusBanner = "EPIC Nano-Regen Serum activated — Full HP, Immunity & +45 Stamina!") }
        }
        "overcharge_ammo" -> {
          audioEngine?.playSfx(CombatSfx.PLASMA_SHOT)
          repository.consumeOrRemoveSlot(slot.slotIndex)
          repository.updateSurvivor(
            s.copy(
              energyCells = s.energyCells + 24,
              stamina = (s.stamina + 30).coerceAtMost(100),
            )
          )
          transientState.update { it.copy(statusBanner = "Loaded Overcharge Cryo Rounds (+24 Plasma Cells, +30 Stamina)") }
        }
        "quantum_battery" -> {
          val hpHeal = (50 * healMult).roundToInt()
          audioEngine?.playSfx(CombatSfx.VICTORY)
          repository.consumeOrRemoveSlot(slot.slotIndex)
          repository.updateSurvivor(
            s.copy(
              energyCells = s.energyCells + 45,
              hp = (s.hp + hpHeal).coerceAtMost(s.maxHp),
              stamina = 100,
            )
          )
          transientState.update { it.copy(statusBanner = "LEGENDARY Quantum Battery installed (+45 Cells, +$hpHeal HP, 100% Stamina)") }
        }
        "barricade_kit" -> {
          audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
          repository.consumeOrRemoveSlot(slot.slotIndex)
          val maxInt = state.baseWave.maxBaseIntegrity
          transientState.update {
            it.copy(
              baseWave = it.baseWave.copy(baseIntegrity = (it.baseWave.baseIntegrity + 45).coerceAtMost(maxInt)),
              statusBanner = "Deployed Fortified Barricade Kit (+45 Base Integrity!)",
            )
          }
        }
        else -> {
          transientState.update { it.copy(statusBanner = "${slot.name} (${LootRarity.fromName(slot.rarity).label}) — combine in Crafting Station!") }
        }
      }
    }
  }

  fun salvageInventorySlot(slot: InventorySlotEntity) {
    if (slot.itemId.isEmpty() || slot.quantity <= 0 || slot.category != "RARE") return
    viewModelScope.launch {
      val s = uiState.value.survivor
      val rarity = LootRarity.fromName(slot.rarity)
      val metalGain = (16 * rarity.salvageMultiplier).roundToInt()
      val crystalGain = (4 * rarity.salvageMultiplier).roundToInt()
      audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
      repository.consumeOrRemoveSlot(slot.slotIndex)
      repository.updateSurvivor(
        s.copy(
          metal = s.metal + metalGain,
          crystals = s.crystals + crystalGain,
          xp = s.xp + (20 * rarity.salvageMultiplier).roundToInt(),
        ).let { applyLevelUp(it) }
      )
      transientState.update {
        it.copy(statusBanner = "Salvaged [${rarity.label}] ${slot.name}: +$metalGain Metal & +$crystalGain Bio-Crystals!")
      }
    }
  }

  fun dropInventorySlot(slot: InventorySlotEntity) {
    if (slot.itemId.isEmpty() || slot.quantity <= 0 || slot.category == "BUILDING") return
    viewModelScope.launch {
      val s = uiState.value.survivor
      repository.consumeOrRemoveSlot(slot.slotIndex)
      when (slot.itemId) {
        "tactical_medkit" -> repository.updateSurvivor(s.copy(medkits = (s.medkits - 1).coerceAtLeast(0)))
        "energy_cell" -> repository.updateSurvivor(s.copy(energyCells = (s.energyCells - 1).coerceAtLeast(0)))
      }
      transientState.update { it.copy(statusBanner = "Discarded 1x ${slot.name}") }
    }
  }

  fun switchBiome(biome: BiomeType) {
    viewModelScope.launch {
      val state = uiState.value
      val s = state.survivor
      repository.updateSurvivor(s.copy(activeBiomeId = biome.id))
      transientState.update {
        it.copy(
          playerX = 0.50f,
          playerY = 0.56f,
          resourceNodes = generateResourceNodes(biome, state.currentWeather),
          enemies = generateBiomeEnemies(biome, s.level, state.difficulty),
          statusBanner = "Entered ${biome.displayName} (Threat Tier ${biome.dangerTier})",
        )
      }
      audioEngine?.speak("Deploying to ${biome.displayName}.")
    }
  }

  fun cycleWeatherManual() {
    viewModelScope.launch {
      val s = uiState.value.survivor
      val next = (s.weatherIndex + 1) % WeatherType.entries.size
      repository.updateSurvivor(s.copy(weatherIndex = next, autoCycleWeather = false))
    }
  }

  fun cycleTimeManual() {
    viewModelScope.launch {
      val s = uiState.value.survivor
      val next = (s.timeOfDayIndex + 1) % TimeOfDay.entries.size
      repository.updateSurvivor(s.copy(timeOfDayIndex = next, autoCycleWeather = false))
    }
  }

  fun toggleAutoCycleWeather() {
    viewModelScope.launch {
      val s = uiState.value.survivor
      repository.updateSurvivor(s.copy(autoCycleWeather = !s.autoCycleWeather))
    }
  }

  fun movePlayerBy(dx: Float, dy: Float) {
    if (uiState.value.combat != null) return
    val movedPlayer = uiState.value.player.moveBy(dx, dy)
    transientState.update {
      it.copy(
        playerX = movedPlayer.x,
        playerY = movedPlayer.y,
        playerFacingRad = movedPlayer.facingRad,
        playerActionState = movedPlayer.actionState,
      )
    }
    checkNearbyInteraction(movedPlayer.x, movedPlayer.y)
  }

  fun movePlayerToward(targetX: Float, targetY: Float) {
    if (uiState.value.combat != null) return
    val tappedEnemy = transientState.value.enemies.firstOrNull { e ->
      hypot((e.x - targetX).toDouble(), (e.y - targetY).toDouble()) < 0.11
    }
    if (tappedEnemy != null) {
      val playerDist = hypot((tappedEnemy.x - transientState.value.playerX).toDouble(), (tappedEnemy.y - transientState.value.playerY).toDouble())
      if (playerDist < 0.42) {
        engageEnemy(tappedEnemy, openingStrike = true)
        return
      }
    }
    val movedPlayer = uiState.value.player.moveToward(targetX, targetY)
    transientState.update {
      it.copy(
        playerX = movedPlayer.x,
        playerY = movedPlayer.y,
        playerFacingRad = movedPlayer.facingRad,
        playerActionState = movedPlayer.actionState,
      )
    }
    checkNearbyInteraction(movedPlayer.x, movedPlayer.y)
  }

  private fun moveArenaPlayerBy(dx: Float, dy: Float) {
    val c = transientState.value.combat ?: return
    val nx = (c.arenaPlayerX + dx).coerceIn(0.08f, 0.88f)
    val ny = (c.arenaPlayerY + dy).coerceIn(0.24f, 0.84f)
    val facing = atan2(dy.toDouble(), dx.toDouble()).toFloat()
    transientState.update {
      it.copy(
        combat = c.copy(
          arenaPlayerX = nx,
          arenaPlayerY = ny,
          arenaPlayerFacingRad = facing,
        )
      )
    }
  }

  fun worldDashEvade() {
    val state = uiState.value
    if (state.combat != null) {
      performDodgeRoll()
      return
    }
    val s = state.survivor
    if (s.stamina < 10) {
      transientState.update { it.copy(statusBanner = "Not enough Stamina to Dash!") }
      return
    }
    val angle = state.player.facingRad
    val dashDx = cos(angle) * 0.14f
    val dashDy = sin(angle) * 0.14f
    audioEngine?.playSfx(CombatSfx.DODGE_WHOOSH)
    emitNoiseSpike(68f)
    movePlayerBy(dashDx, dashDy)
    viewModelScope.launch {
      repository.updatePlayerVitals(s.hp, s.maxHp, (s.stamina - 10).coerceAtLeast(0), s.food, s.water)
    }
  }

  fun interactNearestNode() {
    val t = transientState.value
    val nearestEnemy = t.enemies.minByOrNull { hypot((it.x - t.playerX).toDouble(), (it.y - t.playerY).toDouble()) }
    val nearestRes = t.resourceNodes.minByOrNull { hypot((it.x - t.playerX).toDouble(), (it.y - t.playerY).toDouble()) }
    val enemyDist = nearestEnemy?.let { hypot((it.x - t.playerX).toDouble(), (it.y - t.playerY).toDouble()) } ?: Double.MAX_VALUE
    val resDist = nearestRes?.let { hypot((it.x - t.playerX).toDouble(), (it.y - t.playerY).toDouble()) } ?: Double.MAX_VALUE

    if (nearestEnemy != null && (enemyDist <= 0.36 || enemyDist <= resDist)) {
      if (enemyDist <= 0.36) {
        engageEnemy(nearestEnemy, openingStrike = true)
      } else {
        movePlayerToward(nearestEnemy.x, nearestEnemy.y)
      }
    } else if (nearestRes != null) {
      if (resDist <= 0.22) {
        collectResource(nearestRes)
      } else {
        movePlayerToward(nearestRes.x, nearestRes.y)
      }
    }
  }

  private fun checkNearbyInteraction(px: Float, py: Float) {
    val hitEnemy = transientState.value.enemies.firstOrNull { e ->
      hypot((e.x - px).toDouble(), (e.y - py).toDouble()) < 0.075
    }
    if (hitEnemy != null) {
      engageEnemy(hitEnemy, openingStrike = false)
      return
    }
    val hitRes = transientState.value.resourceNodes.firstOrNull { node ->
      hypot((node.x - px).toDouble(), (node.y - py).toDouble()) < 0.080
    }
    if (hitRes != null) {
      collectResource(hitRes)
    }
  }

  private fun engageEnemy(enemy: Enemy, openingStrike: Boolean = false) {
    transientState.update {
      it.copy(enemies = it.enemies.filterNot { e -> e.id == enemy.id })
    }
    startCombat(enemy = enemy, mission = null, isCoopRaid = false)
    if (openingStrike && uiState.value.equippedWeapon?.category != "SWORD") {
      // Only ranged weapons fire an opening ballistic shot toward target; melee never grants free un-touched damage
      performPrimaryAttack()
    }
  }

  private fun collectResource(node: WorldNode) {
    val state = uiState.value
    val s = state.survivor
    audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
    viewModelScope.launch {
      val weather = state.currentWeather
      val biome = state.currentBiome
      val rarity = node.rarity
      val rarityMult = rarity.salvageMultiplier
      val crystalBonus = if (weather == WeatherType.BIO_STORM) 2 else 0
      val updated = when (node.kind) {
        NodeKind.FOOD -> {
          val qty = if (rarity.ordinal >= LootRarity.RARE.ordinal) 2 else 1
          repository.addInventoryItem("ration_pack", "MRE Ration Pack", "CONSUMABLE", qty, "Military survival ration (+25 Hunger, +12 HP).", rarity.name)
          s.copy(food = (s.food + (18 * rarityMult).toInt()).coerceAtMost(100), xp = s.xp + (14 * rarityMult).toInt())
        }
        NodeKind.WATER -> {
          val qty = if (rarity.ordinal >= LootRarity.RARE.ordinal) 2 else 1
          repository.addInventoryItem("purified_water", "Purified Flask", "CONSUMABLE", qty, "Filtered drinking water (+28 Thirst, +30 Stamina).", rarity.name)
          s.copy(water = (s.water + (20 * rarityMult).toInt()).coerceAtMost(100), xp = s.xp + (14 * rarityMult).toInt())
        }
        NodeKind.SCRAP -> {
          val gain = ((12 + biome.dangerTier * 2) * rarityMult).roundToInt()
          s.copy(metal = s.metal + gain, xp = s.xp + (16 * rarityMult).toInt())
        }
        NodeKind.TIMBER -> {
          val gain = ((12 + biome.dangerTier * 2) * rarityMult).roundToInt()
          s.copy(wood = s.wood + gain, xp = s.xp + (16 * rarityMult).toInt())
        }
        NodeKind.CRYSTAL -> {
          val gain = ((3 + crystalBonus) * rarityMult).roundToInt()
          s.copy(crystals = s.crystals + gain, xp = s.xp + (22 * rarityMult).toInt())
        }
        else -> s
      }
      var bonusDropMsg = ""
      if (rarity.ordinal >= LootRarity.UNCOMMON.ordinal) {
        if (rarity.ordinal >= LootRarity.EPIC.ordinal) {
          repository.addInventoryItem("titanium_alloy", "Titanium Alloy", "RARE", 1, "Recovered armor plating for Crafting Station or Salvage.", rarity.name)
          bonusDropMsg = " + Titanium Alloy!"
        } else {
          repository.addInventoryItem("chem_reagent", "Chem Reagent", "RARE", 1, "Sterile medical compound used in Crafting Station serums.", rarity.name)
          bonusDropMsg = " + Chem Reagent!"
        }
      }
      val leveled = applyLevelUp(updated)
      repository.updateSurvivor(leveled)
      val remaining = transientState.value.resourceNodes.filterNot { it.id == node.id }
      val finalNodes = if (remaining.size < 3) generateResourceNodes(biome, weather) else remaining
      transientState.update {
        it.copy(
          resourceNodes = finalNodes,
          statusBanner = "Scavenged [${rarity.label}] ${node.label}$bonusDropMsg",
        )
      }
    }
  }

  fun refreshWorldSector() {
    val state = uiState.value
    val biome = state.currentBiome
    emitNoiseSpike(62f)
    transientState.update {
      it.copy(
        resourceNodes = generateResourceNodes(biome, state.currentWeather),
        enemies = generateBiomeEnemies(biome, state.survivor.level, state.difficulty),
        statusBanner = "Radar sweep complete — Lv.${state.survivor.level} scaled hostiles detected!",
      )
    }
    audioEngine?.speak("Radar sweep complete. Hostiles updated.")
  }

  fun consumeFood() {
    viewModelScope.launch {
      val state = uiState.value
      val s = state.survivor
      if (s.food >= 100) {
        transientState.update { it.copy(statusBanner = "Hunger is already full.") }
        return@launch
      }
      val hpHeal = (10 * state.abilityBonuses.healMultiplier).roundToInt()
      audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
      repository.updatePlayerVitals(
        health = (s.hp + hpHeal).coerceAtMost(s.maxHp),
        maxHealth = s.maxHp,
        stamina = s.stamina,
        hunger = (s.food + 25).coerceAtMost(100),
        thirst = s.water,
      )
      transientState.update { it.copy(statusBanner = "Consumed survival ration (+25 Hunger, +$hpHeal HP)") }
    }
  }

  fun consumeWater() {
    viewModelScope.launch {
      val s = uiState.value.survivor
      if (s.water >= 100) {
        transientState.update { it.copy(statusBanner = "Thirst is already quenched.") }
        return@launch
      }
      audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
      repository.updatePlayerVitals(
        health = s.hp,
        maxHealth = s.maxHp,
        stamina = (s.stamina + 28).coerceAtMost(100),
        hunger = s.food,
        thirst = (s.water + 28).coerceAtMost(100),
      )
      transientState.update { it.copy(statusBanner = "Drank purified water (+28 Thirst, +28 Stamina)") }
    }
  }

  fun useMedkitOutofCombat() {
    viewModelScope.launch {
      val s = uiState.value.survivor
      if (s.medkits <= 0) {
        transientState.update { it.copy(statusBanner = "No Medkits left! Craft more in Armory.") }
        return@launch
      }
      audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
      repository.updateSurvivor(
        s.copy(
          medkits = s.medkits - 1,
          hp = s.maxHp,
          immunity = 100,
        )
      )
      transientState.update { it.copy(statusBanner = "Medkit applied — Health & Immunity restored!") }
    }
  }

  // --- STORY & MULTIPLAYER COMBAT ---

  fun openMissionBriefing(mission: MissionEntity) {
    if (!mission.unlocked) {
      transientState.update {
        it.copy(statusBanner = "Locked! Complete Story ${mission.storyNumber} Ch.${mission.chapterInStory - 1} / previous mission first.")
      }
      return
    }
    transientState.update { it.copy(selectedMissionForBriefing = mission) }
  }

  fun closeMissionBriefing() {
    transientState.update { it.copy(selectedMissionForBriefing = null) }
  }

  fun deployMission(mission: MissionEntity) {
    transientState.update { it.copy(selectedMissionForBriefing = null) }
    val state = uiState.value
    val biome = BiomeType.fromId(mission.biomeId)
    viewModelScope.launch {
      val s = state.survivor
      repository.updateSurvivor(s.copy(activeBiomeId = biome.id, weatherIndex = mission.weatherIndex))
    }
    val effectiveEnemyLevel = maxOf(mission.enemyLevel, state.survivor.level)
    val enemy = Enemy.createForBiome(
      id = "mission_${mission.number}",
      name = mission.enemyName,
      biome = biome,
      isBoss = mission.isBoss,
      x = 0.74f,
      y = 0.54f,
      playerLevel = effectiveEnemyLevel,
      difficulty = state.difficulty,
      customHp = mission.enemyHp,
    )
    startCombat(enemy = enemy, mission = mission, isCoopRaid = false)
  }

  fun launchCoopRaid(raidName: String, biome: BiomeType, bossHp: Int) {
    val state = uiState.value
    val activeSquad = state.squadMates.count { it.isSelected }
    if (activeSquad == 0) {
      transientState.update { it.copy(statusBanner = "Select at least 1 squadmate before deploying!") }
      return
    }
    val boss = Enemy.createForBiome(
      id = "raid_${Random.nextInt(9999)}",
      name = raidName,
      biome = biome,
      isBoss = true,
      x = 0.74f,
      y = 0.54f,
      playerLevel = state.survivor.level,
      difficulty = state.difficulty,
      customHp = bossHp,
    )
    startCombat(enemy = boss, mission = null, isCoopRaid = true)
  }

  private fun startCombat(
    enemy: Enemy,
    mission: MissionEntity?,
    isCoopRaid: Boolean,
  ) {
    combatLoopJob?.cancel()
    if (enemy.isBoss) {
      audioEngine?.playSfx(CombatSfx.BOSS_SIREN)
      audioEngine?.speak("Alert! Giant Boss ${enemy.name} detected!", force = true)
    } else {
      audioEngine?.playSfx(CombatSfx.HEAVY_IMPACT)
      audioEngine?.speak("Engaging ${enemy.name}.")
    }

    val difficulty = uiState.value.difficulty
    transientState.update {
      it.copy(
        activeCombatEnemy = enemy,
        combat = CombatState(
          enemyName = enemy.name,
          isBoss = enemy.isBoss,
          biome = enemy.biome,
          enemyHp = enemy.hp,
          enemyMaxHp = enemy.maxHp,
          enemyAttackPower = (enemy.attackPower * difficulty.damageMultiplier).toInt(),
          enemyLevel = enemy.level,
          telegraphProgress = enemy.telegraphProgress,
          dodgeWindowActive = false,
          shieldTicks = 0,
          comboCount = 0,
          squadUltimateCharge = if (isCoopRaid) 50 else 25,
          arenaPlayerX = 0.34f,
          arenaPlayerY = 0.58f,
          arenaEnemyX = 0.68f,
          arenaEnemyY = 0.54f,
          missionNumber = mission?.number,
          storyNumber = mission?.storyNumber,
          chapterInStory = mission?.chapterInStory,
          isCoopRaid = isCoopRaid,
          combatLog = listOf(
            if (enemy.isBoss) "BOSS: ${enemy.name} (Lv.${enemy.level}) [${enemy.type.specialMoveName}] engaged!"
            else "${enemy.name} (Lv.${enemy.level} • ${enemy.type.displayTag}) engaged!"
          ),
        )
      )
    }

    // High-frequency ~80ms Real-Time Arena Combat Loop!
    combatLoopJob = viewModelScope.launch {
      var tick = 0
      while (true) {
        delay(80L)
        tick++
        val currentCombat = transientState.value.combat ?: break
        val currentEnemy = transientState.value.activeCombatEnemy ?: break
        if (currentCombat.victory || currentCombat.defeat) break

        val state = uiState.value
        val s = state.survivor
        val playerModel = state.player
        val difficultyNow = state.difficulty

        // 1. Step Active Enemy Arena AI (movement, telegraph charge, danger zones, projectiles)
        val syncedEnemy = currentEnemy.copy(hp = currentCombat.enemyHp)
        val aiStep = syncedEnemy.stepCombatAi(
          enemyX = currentCombat.arenaEnemyX,
          enemyY = currentCombat.arenaEnemyY,
          playerX = currentCombat.arenaPlayerX,
          playerY = currentCombat.arenaPlayerY,
          difficulty = difficultyNow,
        )
        val steppedEnemy = aiStep.enemy
        val wasEnraged = currentCombat.isEnraged
        var nextEnemyHp = steppedEnemy.hp
        var nextUlt = currentCombat.squadUltimateCharge
        var nextEnemyStrikeSeq = currentCombat.enemyStrikeSeq
        var nextPlayerHp = s.hp
        var nextShield = (currentCombat.shieldTicks - 1).coerceAtLeast(0)
        val nextCounterTicks = (currentCombat.counterReadyTicks - 1).coerceAtLeast(0)
        var nextEnemyFlash = (currentCombat.enemyHitFlashTicks - 1).coerceAtLeast(0)
        var nextPlayerFlash = (currentCombat.playerHurtFlashTicks - 1).coerceAtLeast(0)
        var nextDamageSeq = currentCombat.playerDamageSeq
        var nextHitIntensity = currentCombat.lastHitIntensity

        val logUpdates = currentCombat.combatLog.toMutableList()
        val decayedHits = currentCombat.recentHits
          .map { it.copy(ttlTicks = it.ttlTicks - 1) }
          .filter { it.ttlTicks > 0 }
          .toMutableList()
        val decayedIndicators = currentCombat.directionalHits
          .map { it.copy(ttlTicks = it.ttlTicks - 1) }
          .filter { it.ttlTicks > 0 }
          .toMutableList()
        val activeProjectiles = currentCombat.projectiles.toMutableList()
        val activeZones = currentCombat.dangerZones.toMutableList()

        if (steppedEnemy.isEnraged && !wasEnraged) {
          logUpdates.add("${steppedEnemy.name} ENRAGED! Speed & Damage surging!")
          audioEngine?.playSfx(CombatSfx.BOSS_SIREN)
          audioEngine?.speak("Warning! Boss enraged!")
        }

        // 2. Enemy spawns telegraphed Danger Zone around player's current spot
        if (aiStep.spawnDangerZone) {
          activeZones.add(
            DangerZone(
              id = projectileIdCounter++,
              x = currentCombat.arenaPlayerX,
              y = currentCombat.arenaPlayerY,
              radius = if (steppedEnemy.isBoss) 0.14f else 0.10f,
              progress = 0.08f,
              damage = steppedEnemy.computeStrikeDamage(difficultyNow),
              label = steppedEnemy.activeSpecialMove,
            )
          )
        }

        // 3. Enemy fires real-time bio-projectile toward player (manageable dodge speed)
        if (aiStep.fireProjectile) {
          val pdx = currentCombat.arenaPlayerX - aiStep.nextX
          val pdy = currentCombat.arenaPlayerY - aiStep.nextY
          val pDist = hypot(pdx.toDouble(), pdy.toDouble()).toFloat().coerceAtLeast(0.01f)
          val projSpeed = 0.018f * difficultyNow.speedMultiplier
          activeProjectiles.add(
            ArenaProjectile(
              id = projectileIdCounter++,
              x = aiStep.nextX,
              y = aiStep.nextY,
              vx = (pdx / pDist) * projSpeed,
              vy = (pdy / pDist) * projSpeed,
              damage = (steppedEnemy.computeStrikeDamage(difficultyNow) * 0.65f).toInt().coerceAtLeast(6),
              fromPlayer = false,
            )
          )
        }

        // 4. Advance & resolve Ballistic Projectiles (ZERO damage unless projectile touches target hitbox!)
        val survivingProjectiles = mutableListOf<ArenaProjectile>()
        for (proj in activeProjectiles) {
          val nx = proj.x + proj.vx
          val ny = proj.y + proj.vy
          if (nx !in 0.02f..0.98f || ny !in 0.10f..0.94f) {
            if (proj.fromPlayer) {
              decayedHits.add(FloatingHit(hitIdCounter++, "SHOT MISSED!"))
            }
            continue
          }

          if (proj.fromPlayer) {
            val touched = CombatHitbox.doesProjectileTouchTarget(
              projX = nx,
              projY = ny,
              targetX = aiStep.nextX,
              targetY = aiStep.nextY,
              isBoss = steppedEnemy.isBoss,
              extraRadius = if (proj.isHeavy) 0.024f else 0f,
            )
            if (touched) {
              nextEnemyHp = (nextEnemyHp - proj.damage).coerceAtLeast(0)
              nextEnemyFlash = 3
              nextUlt = (nextUlt + 12).coerceAtMost(100)
              decayedHits.add(FloatingHit(hitIdCounter++, if (proj.isHeavy) "CRIT -${proj.damage}!" else "-${proj.damage}", isCrit = proj.isHeavy))
              audioEngine?.playSfx(CombatSfx.ENEMY_HIT)
              continue
            }
          } else {
            val touchedPlayer = CombatHitbox.doesProjectileTouchPlayer(
              projX = nx,
              projY = ny,
              playerX = currentCombat.arenaPlayerX,
              playerY = currentCombat.arenaPlayerY,
            )
            if (touchedPlayer) {
              val taken = playerModel.takeDamage(proj.damage, dodging = nextShield > 0)
              if (taken == 0) {
                decayedHits.add(FloatingHit(hitIdCounter++, "DODGED ORB!", isCrit = true))
              } else {
                nextPlayerHp = (nextPlayerHp - taken).coerceAtLeast(0)
                nextPlayerFlash = 5
                nextDamageSeq++
                nextHitIntensity = (taken / 22f).coerceIn(0.55f, 1.45f)
                val angle = if (hypot(proj.vx.toDouble(), proj.vy.toDouble()) > 0.001) {
                  atan2((-proj.vy).toDouble(), (-proj.vx).toDouble()).toFloat()
                } else {
                  atan2((aiStep.nextY - currentCombat.arenaPlayerY).toDouble(), (aiStep.nextX - currentCombat.arenaPlayerX).toDouble()).toFloat()
                }
                decayedIndicators.add(
                  DirectionalHitIndicator(
                    id = hitIdCounter++,
                    angleRad = angle,
                    damage = taken,
                    intensity = nextHitIntensity,
                    ttlTicks = 14,
                  )
                )
                decayedHits.add(FloatingHit(hitIdCounter++, "-$taken HP", isEnemyHit = true))
                audioEngine?.playSfx(CombatSfx.PLAYER_HURT)
              }
              continue
            }
          }
          survivingProjectiles.add(proj.copy(x = nx, y = ny))
        }

        // 5. Advance & detonate Ground Danger Zones (player can walk/dodge out with Left Thumb!)
        val survivingZones = mutableListOf<DangerZone>()
        for (zone in activeZones) {
          val nextProg = zone.progress + 0.058f * difficultyNow.speedMultiplier
          if (nextProg >= 1.0f) {
            nextEnemyStrikeSeq++
            val pDist = hypot(
              (currentCombat.arenaPlayerX - zone.x).toDouble(),
              (currentCombat.arenaPlayerY - zone.y).toDouble(),
            )
            if (pDist <= zone.radius) {
              val finalDmg = playerModel.takeDamage(zone.damage, dodging = nextShield > 0)
              if (finalDmg == 0) {
                logUpdates.add("DODGED ${zone.label}!")
                decayedHits.add(FloatingHit(hitIdCounter++, "EVADED!", isCrit = true))
              } else {
                nextPlayerHp = (nextPlayerHp - finalDmg).coerceAtLeast(0)
                nextPlayerFlash = 5
                nextDamageSeq++
                nextHitIntensity = (finalDmg / 22f).coerceIn(0.70f, 1.55f)
                val zdx = zone.x - currentCombat.arenaPlayerX
                val zdy = zone.y - currentCombat.arenaPlayerY
                val angle = if (hypot(zdx.toDouble(), zdy.toDouble()) > 0.025) {
                  atan2(zdy.toDouble(), zdx.toDouble()).toFloat()
                } else {
                  atan2((aiStep.nextY - currentCombat.arenaPlayerY).toDouble(), (aiStep.nextX - currentCombat.arenaPlayerX).toDouble()).toFloat()
                }
                decayedIndicators.add(
                  DirectionalHitIndicator(
                    id = hitIdCounter++,
                    angleRad = angle,
                    damage = finalDmg,
                    intensity = nextHitIntensity,
                    ttlTicks = 15,
                  )
                )
                logUpdates.add("${zone.label} hit for -$finalDmg HP!")
                decayedHits.add(FloatingHit(hitIdCounter++, "-$finalDmg HP", isEnemyHit = true))
                audioEngine?.playSfx(CombatSfx.PLAYER_HURT)
              }
            } else {
              decayedHits.add(FloatingHit(hitIdCounter++, "OUTMANEUVERED!", isCrit = false))
            }
          } else {
            survivingZones.add(zone.copy(progress = nextProg))
          }
        }

        // 6. Direct Melee Strike (ONLY counts if enemy/boss physically touches player hitbox!)
        if (aiStep.strikePlayerDirect) {
          nextEnemyStrikeSeq++
          val rawStrike = steppedEnemy.computeStrikeDamage(difficultyNow)
          val finalDmg = playerModel.takeDamage(rawStrike, dodging = nextShield > 0)
          if (finalDmg == 0) {
            logUpdates.add("DODGED ${steppedEnemy.activeSpecialMove}!")
            decayedHits.add(FloatingHit(hitIdCounter++, "DODGED!", isCrit = true))
          } else {
            nextPlayerHp = (nextPlayerHp - finalDmg).coerceAtLeast(0)
            nextPlayerFlash = 5
            nextDamageSeq++
            nextHitIntensity = (finalDmg / 22f).coerceIn(0.70f, 1.55f)
            val angle = atan2((aiStep.nextY - currentCombat.arenaPlayerY).toDouble(), (aiStep.nextX - currentCombat.arenaPlayerX).toDouble()).toFloat()
            decayedIndicators.add(
              DirectionalHitIndicator(
                id = hitIdCounter++,
                angleRad = angle,
                damage = finalDmg,
                intensity = nextHitIntensity,
                ttlTicks = 15,
              )
            )
            logUpdates.add("${steppedEnemy.activeSpecialMove} struck -$finalDmg HP!")
            decayedHits.add(FloatingHit(hitIdCounter++, "-$finalDmg HP", isEnemyHit = true))
            audioEngine?.playSfx(CombatSfx.PLAYER_HURT)
          }
        } else if (aiStep.missedDirectStrike) {
          nextEnemyStrikeSeq++
          audioEngine?.playSfx(CombatSfx.DODGE_WHOOSH)
          val label = if (steppedEnemy.isBoss) "BOSS MISSED!" else "ENEMY MISSED!"
          decayedHits.add(FloatingHit(hitIdCounter++, label, isCrit = false))
          logUpdates.add("${steppedEnemy.name} swung out of reach (0 DMG)!")
        }

        // 7. Co-Op Squad Mate assist fires visible ballistic bolt toward enemy
        if (currentCombat.isCoopRaid && tick % 12 == 0) {
          val selectedMates = state.squadMates.filter { it.isSelected }
          if (selectedMates.isNotEmpty()) {
            val mate = selectedMates[(tick / 12) % selectedMates.size]
            val assistDmg = (mate.power * 0.45f).toInt().coerceAtLeast(10)
            val sdx = aiStep.nextX - currentCombat.arenaPlayerX
            val sdy = (aiStep.nextY - (currentCombat.arenaPlayerY - 0.08f))
            val sDist = hypot(sdx.toDouble(), sdy.toDouble()).toFloat().coerceAtLeast(0.01f)
            survivingProjectiles.add(
              ArenaProjectile(
                id = projectileIdCounter++,
                x = currentCombat.arenaPlayerX,
                y = (currentCombat.arenaPlayerY - 0.08f).coerceAtLeast(0.18f),
                vx = (sdx / sDist) * 0.085f,
                vy = (sdy / sDist) * 0.085f,
                damage = assistDmg,
                fromPlayer = true,
                category = WeaponCategory.FUTURISTIC,
                isHeavy = false,
              )
            )
            logUpdates.add("${mate.callsign} fired assist round!")
          }
        }

        if (nextEnemyHp <= 0) {
          handleCombatVictory(currentCombat)
          break
        }

        val combatStamGain = (4 + state.currentWeather.staminaRegenDelta + state.abilityBonuses.staminaRegenBonus).coerceAtLeast(2)
        val regenStamina = if (tick % 4 == 0) (s.stamina + combatStamGain).coerceAtMost(100) else s.stamina
        if (nextPlayerHp != s.hp || regenStamina != s.stamina) {
          repository.updatePlayerVitals(nextPlayerHp, s.maxHp, regenStamina, s.food, s.water)
        }

        if (nextPlayerHp <= 0) {
          audioEngine?.speak("Survivor down. Emergency evacuation required.", force = true)
          transientState.update {
            it.copy(
              activeCombatEnemy = steppedEnemy.copy(hp = nextEnemyHp),
              combat = currentCombat.copy(
                arenaEnemyX = aiStep.nextX,
                arenaEnemyY = aiStep.nextY,
                isEnraged = steppedEnemy.isEnraged,
                enemyStrikeSeq = nextEnemyStrikeSeq,
                playerDamageSeq = nextDamageSeq,
                lastHitIntensity = nextHitIntensity,
                directionalHits = decayedIndicators.takeLast(4),
                defeat = true,
                projectiles = emptyList(),
                dangerZones = emptyList(),
                combatLog = logUpdates.takeLast(5),
                recentHits = decayedHits.takeLast(4),
              )
            )
          }
          break
        }

        val dodgeReady = steppedEnemy.aiState == EnemyAiState.TELEGRAPH ||
          steppedEnemy.telegraphProgress >= 0.60f ||
          survivingZones.any { it.progress >= 0.55f }

        if (dodgeReady && !currentCombat.dodgeWindowActive && steppedEnemy.isBoss) {
          audioEngine?.speak("Incoming strike! Evade now.")
        }

        transientState.update {
          it.copy(
            activeCombatEnemy = steppedEnemy.copy(hp = nextEnemyHp),
            combat = currentCombat.copy(
              enemyHp = nextEnemyHp,
              enemyAttackPower = (steppedEnemy.attackPower * difficultyNow.damageMultiplier).toInt(),
              telegraphProgress = steppedEnemy.telegraphProgress,
              dodgeWindowActive = dodgeReady,
              shieldTicks = nextShield,
              counterReadyTicks = nextCounterTicks,
              isEnraged = steppedEnemy.isEnraged,
              arenaEnemyX = aiStep.nextX,
              arenaEnemyY = aiStep.nextY,
              enemyHitFlashTicks = nextEnemyFlash,
              playerHurtFlashTicks = nextPlayerFlash,
              playerDamageSeq = nextDamageSeq,
              lastHitIntensity = nextHitIntensity,
              directionalHits = decayedIndicators.takeLast(4),
              projectiles = survivingProjectiles,
              dangerZones = survivingZones,
              enemyStrikeSeq = nextEnemyStrikeSeq,
              squadUltimateCharge = nextUlt,
              combatLog = logUpdates.takeLast(5),
              recentHits = decayedHits.takeLast(4),
            )
          )
        }
      }
    }
  }

  fun performPrimaryAttack() {
    executeCombatMove(CombatMoveType.PRIMARY)
  }

  fun performSprintAttack() {
    executeCombatMove(CombatMoveType.SPRINT_STRIKE)
  }

  fun performHeavyOverheadStrike() {
    executeCombatMove(CombatMoveType.HEAVY_OVERHEAD)
  }

  fun performHeavyAttack() {
    performHeavyOverheadStrike()
  }

  private fun executeCombatMove(move: CombatMoveType) {
    val combat = transientState.value.combat ?: return
    val enemy = transientState.value.activeCombatEnemy ?: return
    if (combat.victory || combat.defeat) return
    val state = uiState.value
    val s = state.survivor
    val weapon = state.equippedWeapon ?: return

    val bonuses = state.abilityBonuses
    val moveCost = state.effectiveMoveCost(move)
    val isSpecialMove = move != CombatMoveType.PRIMARY
    if (isSpecialMove && s.stamina < moveCost) {
      transientState.update {
        it.copy(statusBanner = "Need $moveCost Stamina for ${move.displayLabel}! (Dodge/Wait to regen)")
      }
      return
    }

    val actualStaminaUsed = moveCost.coerceAtMost(s.stamina)
    val lowStaminaPenalty = if (!isSpecialMove && s.stamina < 4) 0.78f else 1.0f

    val energyUsed = weapon.energyCost.coerceAtMost(s.energyCells)
    val lowEnergyPenalty = if (weapon.energyCost > 0 && s.energyCells < weapon.energyCost) 0.78f else 1.0f

    val category = WeaponCategory.fromName(weapon.category)
    val strikeNoise = when {
      category == WeaponCategory.FUTURISTIC -> 94f
      move == CombatMoveType.HEAVY_OVERHEAD -> 88f
      move == CombatMoveType.SPRINT_STRIKE -> 78f
      category == WeaponCategory.BOW -> 52f
      else -> 72f
    }
    emitNoiseSpike(strikeNoise)
    when {
      move == CombatMoveType.HEAVY_OVERHEAD -> audioEngine?.playSfx(CombatSfx.HEAVY_IMPACT)
      move == CombatMoveType.SPRINT_STRIKE -> audioEngine?.playSfx(CombatSfx.DODGE_WHOOSH)
      category == WeaponCategory.SWORD -> audioEngine?.playSfx(CombatSfx.SWORD_SLASH)
      category == WeaponCategory.BOW -> audioEngine?.playSfx(CombatSfx.BOW_SHOT)
      category == WeaponCategory.FUTURISTIC -> audioEngine?.playSfx(CombatSfx.PLASMA_SHOT)
    }

    val hasCounter = combat.counterReadyTicks > 0
    val counterMult = 1.25f + bonuses.counterDamageBonus
    val moveMult = when (move) {
      CombatMoveType.PRIMARY -> 1.0f
      CombatMoveType.SPRINT_STRIKE -> 1.48f
      CombatMoveType.HEAVY_OVERHEAD -> 2.15f
      CombatMoveType.DODGE -> 1.0f
    } * (if (hasCounter && isSpecialMove) counterMult else 1.0f)

    val armorPierce = when (move) {
      CombatMoveType.HEAVY_OVERHEAD -> 0.55f
      CombatMoveType.SPRINT_STRIKE -> 0.25f
      else -> 0f
    }

    val (rawDamage, isCrit) = state.player.computeAttackDamage(
      weapon = weapon,
      weather = state.currentWeather,
      isHeavy = move == CombatMoveType.HEAVY_OVERHEAD,
      enemyArmor = enemy.armor,
      moveMultiplier = moveMult,
      armorPierceRatio = armorPierce,
    )
    val damage = (rawDamage * lowStaminaPenalty * lowEnergyPenalty).toInt().coerceAtLeast(8)

    viewModelScope.launch {
      repository.updateSurvivor(
        s.copy(
          stamina = (s.stamina - actualStaminaUsed).coerceAtLeast(0),
          energyCells = (s.energyCells - energyUsed).coerceAtLeast(0),
        )
      )
    }

    val startX = combat.arenaPlayerX
    val startY = combat.arenaPlayerY
    val dx = combat.arenaEnemyX - startX
    val dy = combat.arenaEnemyY - startY
    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.01f)

    // Sprint Attack closes ~65%+ of the arena gap toward the enemy regardless of weapon type!
    val (postMovePlayerX, postMovePlayerY) = if (move == CombatMoveType.SPRINT_STRIKE) {
      val maxSprintClose = 0.38f + bonuses.sprintReachBonus
      val closeDist = (dist - 0.12f).coerceAtLeast(0f).coerceAtMost(maxSprintClose)
      val nx = (startX + (dx / dist) * closeDist).coerceIn(0.10f, 0.84f)
      val ny = (startY + (dy / dist) * closeDist).coerceIn(0.24f, 0.82f)
      nx to ny
    } else if (category == WeaponCategory.SWORD) {
      val lungeStep = if (move == CombatMoveType.HEAVY_OVERHEAD) 0.11f else if (dist > 0.24f) 0.08f else 0.02f
      val nx = (startX + (dx / dist) * lungeStep).coerceIn(0.08f, 0.86f)
      val ny = (startY + (dy / dist) * lungeStep).coerceIn(0.24f, 0.84f)
      nx to ny
    } else {
      startX to startY
    }

    val nextSprintSeq = combat.playerSprintSeq + if (move == CombatMoveType.SPRINT_STRIKE) 1L else 0L
    val nextOverheadSeq = combat.playerOverheadSeq + if (move == CombatMoveType.HEAVY_OVERHEAD) 1L else 0L
    val nextCounterTicks = if (hasCounter && isSpecialMove) 0 else combat.counterReadyTicks

    // Stagger interrupt: Sprint & Overhead Strikes knock back enemy telegraph meter!
    val staggerReduction = when (move) {
      CombatMoveType.HEAVY_OVERHEAD -> 0.42f
      CombatMoveType.SPRINT_STRIKE -> 0.28f
      else -> 0f
    }
    val staggeredEnemy = if (staggerReduction > 0f) {
      enemy.copy(telegraphProgress = (enemy.telegraphProgress - staggerReduction).coerceAtLeast(0f))
    } else enemy

    // Aim vector from post-move position toward target (all ranged shots travel ballistically along this ray)
    val postDx = combat.arenaEnemyX - postMovePlayerX
    val postDy = combat.arenaEnemyY - postMovePlayerY
    val postDist = hypot(postDx.toDouble(), postDy.toDouble()).toFloat().coerceAtLeast(0.001f)

    // Ranged weapons fire ballistic projectiles (ZERO damage until projectile physically touches enemy/boss hitbox!)
    if (category != WeaponCategory.SWORD) {
      when (move) {
        CombatMoveType.PRIMARY -> {
          val speed = if (category == WeaponCategory.FUTURISTIC) 0.088f else 0.078f
          val proj = ArenaProjectile(
            id = projectileIdCounter++,
            x = postMovePlayerX,
            y = postMovePlayerY,
            vx = (postDx / postDist) * speed,
            vy = (postDy / postDist) * speed,
            damage = damage,
            fromPlayer = true,
            category = category,
            isHeavy = isCrit,
          )
          transientState.update {
            it.copy(
              combat = combat.copy(
                projectiles = combat.projectiles + proj,
                playerStrikeSeq = combat.playerStrikeSeq + 1,
                lastMoveUsed = move,
                combatLog = (combat.combatLog + "${weapon.name} fired! Must hit target!").takeLast(5),
              )
            )
          }
          return
        }
        CombatMoveType.SPRINT_STRIKE -> {
          val halfDmg = (damage * 0.56f).toInt().coerceAtLeast(8)
          val speed = 0.095f
          val bolts = listOf(-0.035f, 0.035f).map { yOffset ->
            ArenaProjectile(
              id = projectileIdCounter++,
              x = postMovePlayerX,
              y = (postMovePlayerY + yOffset).coerceIn(0.18f, 0.86f),
              vx = (postDx / postDist) * speed,
              vy = (postDy / postDist) * speed,
              damage = halfDmg,
              fromPlayer = true,
              category = category,
              isHeavy = true,
            )
          }
          transientState.update {
            it.copy(
              combat = combat.copy(
                arenaPlayerX = postMovePlayerX,
                arenaPlayerY = postMovePlayerY,
                sprintTrailFromX = startX,
                sprintTrailFromY = startY,
                counterReadyTicks = nextCounterTicks,
                projectiles = combat.projectiles + bolts,
                playerStrikeSeq = combat.playerStrikeSeq + 1,
                playerSprintSeq = nextSprintSeq,
                lastMoveUsed = move,
                combatLog = (combat.combatLog + "SPRINT VOLLEY fired!").takeLast(5),
                recentHits = (combat.recentHits + FloatingHit(hitIdCounter++, "SPRINT VOLLEY!", isCrit = true)).takeLast(4),
              )
            )
          }
          return
        }
        CombatMoveType.HEAVY_OVERHEAD -> {
          val heavyBolt = ArenaProjectile(
            id = projectileIdCounter++,
            x = postMovePlayerX,
            y = postMovePlayerY,
            vx = (postDx / postDist) * 0.105f,
            vy = (postDy / postDist) * 0.105f,
            damage = damage,
            fromPlayer = true,
            category = category,
            isHeavy = true,
          )
          transientState.update {
            it.copy(
              combat = combat.copy(
                counterReadyTicks = nextCounterTicks,
                projectiles = combat.projectiles + heavyBolt,
                playerStrikeSeq = combat.playerStrikeSeq + 1,
                playerOverheadSeq = nextOverheadSeq,
                lastMoveUsed = move,
                combatLog = (combat.combatLog + "HEAVY OVERHEAD WAVE launched!").takeLast(5),
              )
            )
          }
          return
        }
        else -> Unit
      }
    }

    // Melee Weapon Strict Touch Detection: ZERO damage if swing does not touch enemy/boss hitbox!
    val meleeTouched = CombatHitbox.doesPlayerMeleeTouchTarget(
      playerX = postMovePlayerX,
      playerY = postMovePlayerY,
      enemyX = combat.arenaEnemyX,
      enemyY = combat.arenaEnemyY,
      isBoss = enemy.isBoss,
      moveType = move,
    )
    if (!meleeTouched) {
      audioEngine?.playSfx(CombatSfx.DODGE_WHOOSH)
      transientState.update {
        it.copy(
          combat = combat.copy(
            arenaPlayerX = postMovePlayerX,
            arenaPlayerY = postMovePlayerY,
            sprintTrailFromX = startX,
            sprintTrailFromY = startY,
            playerStrikeSeq = combat.playerStrikeSeq + 1,
            playerSprintSeq = nextSprintSeq,
            playerOverheadSeq = nextOverheadSeq,
            lastMoveUsed = move,
            combatLog = (combat.combatLog + "${move.displayLabel} MISSED! Move closer to touch target!").takeLast(5),
            recentHits = (combat.recentHits + FloatingHit(hitIdCounter++, "MISSED! (Too Far)")).takeLast(4),
          )
        )
      }
      return
    }

    // Melee Attack Physically Connected! Apply damage, stagger & knockback
    val newEnemyHp = (combat.enemyHp - damage).coerceAtLeast(0)
    val newCombo = combat.comboCount + 1
    val ultGain = (when (move) {
      CombatMoveType.HEAVY_OVERHEAD -> 26
      CombatMoveType.SPRINT_STRIKE -> 18
      else -> 14
    }) + bonuses.ultChargeBonus
    val newUlt = (combat.squadUltimateCharge + ultGain).coerceAtMost(100)
    val moveTag = when {
      hasCounter && isSpecialMove -> "COUNTER SMASH"
      move == CombatMoveType.HEAVY_OVERHEAD -> "OVERHEAD"
      move == CombatMoveType.SPRINT_STRIKE -> "SPRINT"
      isCrit -> "CRIT"
      else -> ""
    }
    val hitLabel = if (moveTag.isNotEmpty()) "$moveTag -$damage!" else "-$damage"
    val knockbackAmt = if (move == CombatMoveType.HEAVY_OVERHEAD) 0.08f else 0.04f
    val knockbackX = (combat.arenaEnemyX + (postDx / postDist) * knockbackAmt).coerceIn(0.18f, 0.88f)
    audioEngine?.playSfx(CombatSfx.ENEMY_HIT)

    val updatedLog = (combat.combatLog + "${move.displayLabel} (${weapon.name}) hit for $damage DMG!").takeLast(5)
    val nextCombat = combat.copy(
      enemyHp = newEnemyHp,
      arenaPlayerX = postMovePlayerX,
      arenaPlayerY = postMovePlayerY,
      sprintTrailFromX = startX,
      sprintTrailFromY = startY,
      arenaEnemyX = knockbackX,
      counterReadyTicks = nextCounterTicks,
      telegraphProgress = staggeredEnemy.telegraphProgress,
      enemyHitFlashTicks = if (move == CombatMoveType.HEAVY_OVERHEAD) 5 else 3,
      comboCount = newCombo,
      squadUltimateCharge = newUlt,
      playerStrikeSeq = combat.playerStrikeSeq + 1,
      playerSprintSeq = nextSprintSeq,
      playerOverheadSeq = nextOverheadSeq,
      lastMoveUsed = move,
      combatLog = updatedLog,
      recentHits = (combat.recentHits + FloatingHit(hitIdCounter++, hitLabel, isCrit = isCrit || isSpecialMove)).takeLast(4),
    )

    if (newEnemyHp <= 0) {
      viewModelScope.launch { handleCombatVictory(nextCombat) }
    } else {
      transientState.update {
        it.copy(
          activeCombatEnemy = staggeredEnemy.copy(hp = newEnemyHp),
          combat = nextCombat,
        )
      }
    }
  }

  fun performDodgeRoll() {
    val combat = transientState.value.combat ?: return
    if (combat.victory || combat.defeat) return
    val state = uiState.value
    val s = state.survivor
    val bonuses = state.abilityBonuses
    val cost = state.effectiveMoveCost(CombatMoveType.DODGE)
    if (s.stamina < cost) {
      transientState.update { it.copy(statusBanner = "Need $cost Stamina to Dodge!") }
      return
    }
    audioEngine?.playSfx(CombatSfx.DODGE_WHOOSH)
    val isPerfect = combat.dodgeWindowActive
    val staminaDelta = if (isPerfect) -cost + 12 else -cost
    viewModelScope.launch {
      repository.updatePlayerVitals(s.hp, s.maxHp, (s.stamina + staminaDelta).coerceIn(0, 100), s.food, s.water)
    }

    val startX = combat.arenaPlayerX
    val startY = combat.arenaPlayerY
    val dodgeSpan = 0.16f + bonuses.sprintReachBonus * 0.4f
    val dashDx = if (hypot(joystickVx.toDouble(), joystickVy.toDouble()) > 0.1) joystickVx * dodgeSpan else -(0.12f + bonuses.sprintReachBonus * 0.3f)
    val dashDy = if (hypot(joystickVx.toDouble(), joystickVy.toDouble()) > 0.1) joystickVy * dodgeSpan else 0f
    val nx = (startX + dashDx).coerceIn(0.08f, 0.88f)
    val ny = (startY + dashDy).coerceIn(0.24f, 0.84f)

    val bonusLog = if (isPerfect) {
      "PERFECT DODGE! +12 Stamina & Counter Buff Ready!"
    } else {
      "Tactical Dodge — Evasion Shield active"
    }
    val dodgeShield = (if (isPerfect) 16 else 13) + bonuses.dodgeShieldBonusTicks
    val ultAdd = (if (isPerfect) 15 else 4) + (bonuses.ultChargeBonus / 2)
    transientState.update {
      it.copy(
        combat = combat.copy(
          arenaPlayerX = nx,
          arenaPlayerY = ny,
          sprintTrailFromX = startX,
          sprintTrailFromY = startY,
          shieldTicks = dodgeShield,
          counterReadyTicks = if (isPerfect) 28 else combat.counterReadyTicks,
          squadUltimateCharge = (combat.squadUltimateCharge + ultAdd).coerceAtMost(100),
          playerSprintSeq = combat.playerSprintSeq + 1,
          lastMoveUsed = CombatMoveType.DODGE,
          combatLog = (combat.combatLog + bonusLog).takeLast(5),
          recentHits = (combat.recentHits + FloatingHit(hitIdCounter++, if (isPerfect) "PERFECT DODGE!" else "EVADE SHIELD", isCrit = isPerfect)).takeLast(4),
        )
      )
    }
  }

  fun performCombatMedkit() {
    val combat = transientState.value.combat ?: return
    if (combat.victory || combat.defeat) return
    val state = uiState.value
    val s = state.survivor
    if (s.medkits <= 0) {
      transientState.update { it.copy(statusBanner = "No Medkits remaining!") }
      return
    }
    audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
    viewModelScope.launch {
      val healAmount = (55 * state.abilityBonuses.healMultiplier).roundToInt()
      repository.updateSurvivor(
        s.copy(
          medkits = s.medkits - 1,
          hp = (s.hp + healAmount).coerceAtMost(s.maxHp),
        )
      )
      transientState.update {
        it.copy(
          combat = combat.copy(
            combatLog = (combat.combatLog + "Used Medkit (+$healAmount HP)").takeLast(5),
            recentHits = listOf(FloatingHit(hitIdCounter++, "+$healAmount HP", isHeal = true)),
          )
        )
      }
    }
  }

  fun performSquadUltimate() {
    val combat = transientState.value.combat ?: return
    if (combat.victory || combat.defeat || combat.squadUltimateCharge < 100) return
    val state = uiState.value
    val dmgPerBolt = ((145 + state.activeSquadPower) / 3).coerceAtLeast(35)
    val dx = combat.arenaEnemyX - combat.arenaPlayerX
    val dy = combat.arenaEnemyY - combat.arenaPlayerY
    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.01f)
    val barrage = listOf(-0.045f, 0f, 0.045f).map { offset ->
      ArenaProjectile(
        id = projectileIdCounter++,
        x = combat.arenaPlayerX,
        y = (combat.arenaPlayerY + offset).coerceIn(0.18f, 0.86f),
        vx = (dx / dist) * 0.115f,
        vy = (dy / dist) * 0.115f,
        damage = dmgPerBolt,
        fromPlayer = true,
        category = WeaponCategory.FUTURISTIC,
        isHeavy = true,
      )
    }
    audioEngine?.playSfx(CombatSfx.HEAVY_IMPACT)
    audioEngine?.speak("Squad barrage deployed!")
    transientState.update {
      it.copy(
        combat = combat.copy(
          projectiles = combat.projectiles + barrage,
          squadUltimateCharge = 0,
          playerStrikeSeq = combat.playerStrikeSeq + 1,
          combatLog = (combat.combatLog + "CO-OP SQUAD BARRAGE launched!").takeLast(5),
          recentHits = (combat.recentHits + FloatingHit(hitIdCounter++, "SQUAD BARRAGE!", isCrit = true)).takeLast(4),
        )
      )
    }
  }

  private suspend fun handleCombatVictory(combat: CombatState) {
    combatLoopJob?.cancel()
    val state = uiState.value
    val s = state.survivor

    val mission = combat.missionNumber?.let { num -> state.missions.firstOrNull { it.number == num } }
    val xpGain = mission?.rewardXp ?: if (combat.isCoopRaid) 240 else if (combat.isBoss) 200 else 70
    val crystalGain = mission?.rewardCrystals ?: if (combat.isCoopRaid) 18 else if (combat.isBoss) 14 else 4
    val metalGain = mission?.rewardMetal ?: if (combat.isBoss) 38 else 18

    if (mission != null) {
      repository.completeMission(mission.number)
    }

    val updatedSurvivor = applyLevelUp(
      s.copy(
        xp = s.xp + xpGain,
        crystals = s.crystals + crystalGain,
        metal = s.metal + metalGain,
        monstersSlain = s.monstersSlain + 1,
        missionsCompleted = if (mission != null && !mission.completed) s.missionsCompleted + 1 else s.missionsCompleted,
      )
    )
    repository.updateSurvivor(updatedSurvivor)
    val dropRarity = if (combat.isCoopRaid) {
      LootRarity.LEGENDARY
    } else if (combat.isBoss) {
      LootRarity.EPIC
    } else {
      LootDropTable.rollWorldNodeRarity(state.currentBiome, state.currentWeather, Random.nextInt(100))
    }
    repository.addInventoryItem(
      itemId = "titanium_alloy",
      name = "Titanium Alloy",
      category = "RARE",
      deltaQty = if (combat.isBoss) 2 else 1,
      description = "High-grade monster core & armor alloy. Craft Base Modules or Salvage in Inventory.",
      rarity = if (dropRarity.ordinal >= LootRarity.RARE.ordinal) dropRarity.name else LootRarity.RARE.name,
    )
    repository.addInventoryItem(
      itemId = "chem_reagent",
      name = "Chem Reagent",
      category = "RARE",
      deltaQty = 1,
      description = "Sterile medical compound used in Crafting Station serums.",
      rarity = LootRarity.UNCOMMON.name,
    )
    val extraLootLabel = if (combat.isBoss || combat.isCoopRaid) {
      repository.addInventoryItem(
        itemId = "apex_core",
        name = "Apex Core",
        category = "RARE",
        deltaQty = 1,
        description = "Legendary boss reactor core. Craft Quantum Batteries or salvage for massive resources.",
        rarity = LootRarity.LEGENDARY.name,
      )
      repository.addInventoryItem("ration_pack", "MRE Ration Pack", "CONSUMABLE", 1, "Military survival ration (+25 Hunger, +12 HP).", LootRarity.UNCOMMON.name)
      "Titanium + Apex Core [${dropRarity.label}]"
    } else {
      "Titanium + Reagent [${dropRarity.label}]"
    }

    audioEngine?.playSfx(CombatSfx.VICTORY)
    audioEngine?.speak(if (combat.isBoss) "Boss eliminated! Sector secured." else "Hostile neutralized.")

    val summary = "+$xpGain XP  •  +$crystalGain Crystals  •  +$metalGain Metal  •  +$extraLootLabel"
    transientState.update {
      val remRaiders = it.enemies.count { e -> e.isBaseRaider }
      it.copy(
        baseWave = it.baseWave.copy(remainingEnemiesInWave = remRaiders),
        combat = combat.copy(
          enemyHp = 0,
          projectiles = emptyList(),
          dangerZones = emptyList(),
          victory = true,
          rewardSummary = summary,
          combatLog = (combat.combatLog + "VICTORY! $summary").takeLast(5),
        )
      )
    }
    if (transientState.value.baseWave.waveActive && transientState.value.enemies.none { it.isBaseRaider }) {
      completeBaseWave(uiState.value)
    }
  }

  fun closeCombat(reviveIfDefeated: Boolean = false) {
    combatLoopJob?.cancel()
    viewModelScope.launch {
      if (reviveIfDefeated) {
        val s = uiState.value.survivor
        val metalPenalty = (s.metal * 0.20f).toInt()
        repository.updateSurvivor(
          s.copy(
            hp = (s.maxHp * 0.50f).toInt().coerceAtLeast(35),
            stamina = 60,
            metal = (s.metal - metalPenalty).coerceAtLeast(0),
          )
        )
        transientState.update {
          it.copy(
            combat = null,
            activeCombatEnemy = null,
            statusBanner = "Emergency Evac! Lost $metalPenalty Metal & revived at 50% HP.",
          )
        }
      } else {
        transientState.update { it.copy(combat = null, activeCombatEnemy = null) }
      }
    }
  }

  // --- BASE CAMP BUILDING ---

  fun collectCampYield() {
    if (!transientState.value.campYieldReady) return
    viewModelScope.launch {
      val state = uiState.value
      val s = state.survivor
      val purifierLv = state.buildings.firstOrNull { it.id == "purifier" }?.level ?: 1
      val greenhouseLv = state.buildings.firstOrNull { it.id == "greenhouse" }?.level ?: 1
      val rainMult = if (state.currentWeather == WeatherType.RAIN) 1.4f else 1f
      val waterGain = (purifierLv * 12 * rainMult).toInt()
      val foodGain = greenhouseLv * 12
      audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
      repository.updateSurvivor(
        s.copy(
          water = (s.water + waterGain).coerceAtMost(100),
          food = (s.food + foodGain).coerceAtMost(100),
          energyCells = s.energyCells + 4,
        )
      )
      transientState.update {
        it.copy(
          campYieldReady = false,
          statusBanner = "Camp Harvested: +$foodGain Hunger, +$waterGain Thirst, +4 Energy Cells",
        )
      }
    }
  }

  fun upgradeBuilding(building: BaseBuildingEntity) {
    if (building.level >= building.maxLevel) return
    viewModelScope.launch {
      val s = uiState.value.survivor
      if (s.wood < building.woodCost || s.metal < building.metalCost || s.crystals < building.crystalCost) {
        transientState.update { it.copy(statusBanner = "Need ${building.woodCost} Wood, ${building.metalCost} Metal, ${building.crystalCost} Crystals") }
        return@launch
      }
      val newLv = building.level + 1
      val nextBunkerHpBonus = if (building.id == "bunker") 25 else 0
      audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
      repository.updateSurvivor(
        applyLevelUp(
          s.copy(
            wood = s.wood - building.woodCost,
            metal = s.metal - building.metalCost,
            crystals = s.crystals - building.crystalCost,
            maxHp = s.maxHp + nextBunkerHpBonus,
            hp = s.hp + nextBunkerHpBonus,
            xp = s.xp + 40,
          )
        )
      )
      repository.upsertBuilding(
        building.copy(
          level = newLv,
          woodCost = (building.woodCost * 1.45f).toInt(),
          metalCost = (building.metalCost * 1.45f).toInt(),
          crystalCost = (building.crystalCost * 1.45f).toInt() + 2,
        )
      )
      transientState.update {
        it.copy(statusBanner = "${building.name} upgraded to Level $newLv!")
      }
    }
  }

  // --- ARMORY & CRAFTING ---

  fun craftOrUpgradeWeapon(weapon: WeaponEntity) {
    viewModelScope.launch {
      val s = uiState.value.survivor
      if (s.wood < weapon.woodCost || s.metal < weapon.metalCost || s.crystals < weapon.crystalCost) {
        transientState.update { it.copy(statusBanner = "Need ${weapon.woodCost} Wood, ${weapon.metalCost} Metal, ${weapon.crystalCost} Crystals") }
        return@launch
      }
      val updatedWeapon = if (!weapon.unlocked) {
        weapon.copy(
          unlocked = true,
          woodCost = (weapon.woodCost * 0.8f).toInt() + 10,
          metalCost = (weapon.metalCost * 0.8f).toInt() + 15,
          crystalCost = weapon.crystalCost + 5,
        )
      } else {
        weapon.copy(
          level = weapon.level + 1,
          woodCost = (weapon.woodCost * 1.35f).toInt(),
          metalCost = (weapon.metalCost * 1.35f).toInt(),
          crystalCost = weapon.crystalCost + 6,
        )
      }
      audioEngine?.playSfx(CombatSfx.SWORD_SLASH)
      repository.upsertWeapon(updatedWeapon)
      repository.updateSurvivor(
        applyLevelUp(
          s.copy(
            wood = s.wood - weapon.woodCost,
            metal = s.metal - weapon.metalCost,
            crystals = s.crystals - weapon.crystalCost,
            equippedWeaponId = weapon.id,
            xp = s.xp + 30,
          )
        )
      )
      val verb = if (!weapon.unlocked) "crafted & equipped" else "upgraded to Lv.${updatedWeapon.level}"
      transientState.update { it.copy(statusBanner = "${weapon.name} $verb!") }
    }
  }

  fun equipWeapon(weapon: WeaponEntity) {
    if (!weapon.unlocked) return
    viewModelScope.launch {
      val s = uiState.value.survivor
      repository.updateSurvivor(s.copy(equippedWeaponId = weapon.id))
      transientState.update { it.copy(statusBanner = "Equipped ${weapon.name}") }
    }
  }

  fun craftMedkit() {
    viewModelScope.launch {
      val s = uiState.value.survivor
      if (s.wood < 8 || s.crystals < 2) {
        transientState.update { it.copy(statusBanner = "Need 8 Wood & 2 Bio-Crystals to craft a Medkit") }
        return@launch
      }
      audioEngine?.playSfx(CombatSfx.HEAL_CHIME)
      repository.updateSurvivor(
        s.copy(
          wood = s.wood - 8,
          crystals = s.crystals - 2,
          medkits = s.medkits + 1,
        )
      )
      transientState.update { it.copy(statusBanner = "Crafted +1 Tactical Medkit") }
    }
  }

  fun craftEnergyCells() {
    viewModelScope.launch {
      val s = uiState.value.survivor
      if (s.metal < 10 || s.crystals < 3) {
        transientState.update { it.copy(statusBanner = "Need 10 Metal & 3 Bio-Crystals for 12 Energy Cells") }
        return@launch
      }
      audioEngine?.playSfx(CombatSfx.PLASMA_SHOT)
      repository.updateSurvivor(
        s.copy(
          metal = s.metal - 10,
          crystals = s.crystals - 3,
          energyCells = s.energyCells + 12,
        )
      )
      transientState.update { it.copy(statusBanner = "Synthesized +12 Plasma Energy Cells") }
    }
  }

  // --- SQUAD CO-OP ---

  fun toggleSquadMate(mate: SquadMateEntity) {
    viewModelScope.launch {
      repository.toggleSquadMate(mate)
    }
  }

  fun regenerateSquadCode() {
    viewModelScope.launch {
      val code = "NOVA-${Random.nextInt(1000, 9999)}"
      repository.updateSurvivor(uiState.value.survivor.copy(squadCode = code))
      transientState.update { it.copy(statusBanner = "New Squad Beacon Code: $code") }
    }
  }

  private fun applyLevelUp(s: SurvivorEntity): SurvivorEntity {
    var lv = s.level
    var xp = s.xp
    var maxHp = s.maxHp
    var hp = s.hp
    var leveledUp = false
    while (xp >= lv * 120) {
      xp -= lv * 120
      lv++
      maxHp += 15
      hp = maxHp
      leveledUp = true
    }
    if (leveledUp) {
      audioEngine?.speak("Level $lv reached! Enemy threat strength has also scaled up.")
    }
    return s.copy(level = lv, xp = xp, maxHp = maxHp, hp = hp.coerceAtMost(maxHp))
  }

  private fun generateResourceNodes(
    biome: BiomeType = BiomeType.DEAD_CITY,
    weather: WeatherType = WeatherType.CLEAR,
  ): List<WorldNode> {
    val resourceKinds = listOf(
      NodeKind.FOOD to "Food",
      NodeKind.WATER to "Water",
      NodeKind.SCRAP to "Metal",
      NodeKind.TIMBER to "Wood",
      NodeKind.CRYSTAL to "Crystal",
    )
    return resourceKinds.mapIndexed { idx, (kind, label) ->
      val rolled = LootDropTable.rollWorldNodeRarity(biome, weather, Random.nextInt(100))
      val effectiveRarity = if (kind == NodeKind.CRYSTAL && rolled == LootRarity.COMMON) LootRarity.UNCOMMON else rolled
      WorldNode(
        id = "res_${idx}_${Random.nextInt(9999)}",
        kind = kind,
        label = label,
        x = Random.nextFloat() * 0.72f + 0.14f,
        y = Random.nextFloat() * 0.52f + 0.24f,
        rarity = effectiveRarity,
      )
    }
  }

  private fun generateBiomeEnemies(
    biome: BiomeType,
    playerLevel: Int,
    difficulty: DifficultyState,
  ): List<Enemy> {
    val names = when (biome) {
      BiomeType.DEAD_CITY -> listOf("Virus Stalker", "Armored Brute", "Metro Stalker")
      BiomeType.ASHWOOD_FOREST -> listOf("Spore Ravager", "Canopy Ravager", "Blight Stalker")
      BiomeType.FROSTPEAK_RIDGE -> listOf("Frost Golem", "Glacier Golem", "Ice Stalker")
      BiomeType.SECTOR_9_LAB -> listOf("Cryo Mutant", "Lab Warden", "Bio Mutant")
    }
    val bossName = when (biome) {
      BiomeType.DEAD_CITY -> "Metro Goliath"
      BiomeType.ASHWOOD_FOREST -> "Elder Treant"
      BiomeType.FROSTPEAK_RIDGE -> "Frost Behemoth"
      BiomeType.SECTOR_9_LAB -> "Omega Leviathan"
    }
    val positions = listOf(0.16f to 0.28f, 0.84f to 0.72f, 0.20f to 0.76f)
    val mobs = positions.mapIndexed { idx, (ex, ey) ->
      Enemy.createForBiome(
        id = "mob_${idx}_${Random.nextInt(9999)}",
        name = names[idx % names.size],
        biome = biome,
        isBoss = false,
        x = ex,
        y = ey,
        playerLevel = playerLevel,
        difficulty = difficulty,
      )
    }
    val boss = Enemy.createForBiome(
      id = "boss_${Random.nextInt(9999)}",
      name = bossName,
      biome = biome,
      isBoss = true,
      x = 0.84f,
      y = 0.26f,
      playerLevel = playerLevel,
      difficulty = difficulty,
    )
    return mobs + boss
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine?.release()
  }

  class Factory(
    private val repository: SurvivalRepository,
    private val audioEngine: TacticalAudioEngine? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return GameViewModel(repository, audioEngine) as T
    }
  }
}
