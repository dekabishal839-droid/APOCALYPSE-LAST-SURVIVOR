package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.TacticalAudioEngine
import com.example.data.local.AppDatabase
import com.example.data.repository.SurvivalRepository
import com.example.model.AbilityTreeCatalog
import com.example.model.AppTab
import com.example.ui.components.AbilityTreeDialog
import com.example.ui.components.CraftingStationDialog
import com.example.ui.components.InventoryGridDialog
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.BaseCampScreen
import com.example.ui.screens.CombatOverlay
import com.example.ui.screens.SquadScreen
import com.example.ui.screens.StoryScreen
import com.example.ui.screens.WorldScreen
import com.example.ui.theme.Aftermath2045Theme
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BunkerSurface
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.ObsidianBg
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val db = AppDatabase.getInstance(applicationContext)
    val repository = SurvivalRepository(db.gameDao(), db.playerStatsDao())
    val audioEngine = TacticalAudioEngine(applicationContext)
    setContent {
      Aftermath2045Theme {
        val viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory(repository, audioEngine))
        AftermathApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun AftermathApp(viewModel: GameViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(uiState.statusBanner) {
    if (uiState.statusBanner != null) {
      delay(4000L)
      viewModel.dismissBanner()
    }
  }

  if (uiState.isLoading) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(ObsidianBg),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(color = BioAmber)
        Text("INITIALIZING 20-STORY CAMPAIGN...", style = MaterialTheme.typography.titleLarge, color = BioCyan)
      }
    }
    return
  }

  val navItems = listOf(
    Triple(AppTab.WORLD, stringResource(R.string.nav_world), Icons.Filled.Explore to Icons.Outlined.Explore),
    Triple(AppTab.STORY, stringResource(R.string.nav_story), Icons.Filled.AutoStories to Icons.Outlined.AutoStories),
    Triple(AppTab.CAMP, stringResource(R.string.nav_camp), Icons.Filled.HomeWork to Icons.Outlined.HomeWork),
    Triple(AppTab.ARMORY, stringResource(R.string.nav_armory), Icons.Filled.Build to Icons.Outlined.Build),
    Triple(AppTab.SQUAD, stringResource(R.string.nav_squad), Icons.Filled.Groups to Icons.Outlined.Groups),
  )

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
    val useRail = maxWidth >= 560.dp

    Scaffold(
      containerColor = ObsidianBg,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!useRail && uiState.combat == null) {
          NavigationBar(
            containerColor = BunkerSurface,
            tonalElevation = 4.dp,
          ) {
            navItems.forEach { (tab, label, icons) ->
              val selected = uiState.activeTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                modifier = Modifier.testTag("nav_${tab.route}"),
                icon = {
                  Icon(
                    imageVector = if (selected) icons.first else icons.second,
                    contentDescription = label,
                  )
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = BioAmber,
                  selectedTextColor = BioAmber,
                  indicatorColor = BioAmber.copy(alpha = 0.16f),
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail && uiState.combat == null) {
          NavigationRail(
            containerColor = BunkerSurface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            navItems.forEach { (tab, label, icons) ->
              val selected = uiState.activeTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                modifier = Modifier.testTag("rail_${tab.route}"),
                icon = {
                  Icon(
                    imageVector = if (selected) icons.first else icons.second,
                    contentDescription = label,
                  )
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = BioAmber,
                  selectedTextColor = BioAmber,
                  indicatorColor = BioAmber.copy(alpha = 0.16f),
                ),
              )
            }
          }
        }

        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .widthIn(max = 1160.dp),
          ) {
            AnimatedVisibility(
              visible = uiState.statusBanner != null,
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              uiState.statusBanner?.let { msg ->
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clickable { viewModel.dismissBanner() },
                  color = BunkerSurfaceElevated,
                  shape = MaterialTheme.shapes.small,
                  border = BorderStroke(1.dp, BioCyan.copy(alpha = 0.65f)),
                ) {
                  Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = BioCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }
            }

            Box(modifier = Modifier.weight(1f)) {
              when (uiState.activeTab) {
                AppTab.WORLD -> WorldScreen(
                  uiState = uiState,
                  onSelectBiome = viewModel::switchBiome,
                  onCycleWeather = viewModel::cycleWeatherManual,
                  onCycleTime = viewModel::cycleTimeManual,
                  onToggleAutoCycle = viewModel::toggleAutoCycleWeather,
                  onJoystickVector = viewModel::setJoystickVector,
                  onTapMap = viewModel::movePlayerToward,
                  onDashEvade = viewModel::worldDashEvade,
                  onInteractNearest = viewModel::interactNearestNode,
                  onRefreshSector = viewModel::refreshWorldSector,
                  onEatFood = viewModel::consumeFood,
                  onDrinkWater = viewModel::consumeWater,
                  onUseMedkit = viewModel::useMedkitOutofCombat,
                  onToggleMute = viewModel::toggleAudioMute,
                  onOpenInventory = viewModel::openInventory,
                  onOpenCrafting = viewModel::openCraftingStation,
                  onOpenAbilityTree = viewModel::openAbilityTree,
                  onTriggerBaseWave = viewModel::triggerBaseWaveNow,
                )
                AppTab.STORY -> StoryScreen(
                  uiState = uiState,
                  onSelectMission = viewModel::openMissionBriefing,
                  onCloseBriefing = viewModel::closeMissionBriefing,
                  onDeployMission = viewModel::deployMission,
                )
                AppTab.CAMP -> BaseCampScreen(
                  uiState = uiState,
                  onCollectYield = viewModel::collectCampYield,
                  onUpgradeBuilding = viewModel::upgradeBuilding,
                  onOpenInventory = viewModel::openInventory,
                  onOpenCrafting = viewModel::openCraftingStation,
                  onDeployBaseDefense = viewModel::deployToBaseDefense,
                )
                AppTab.ARMORY -> ArmoryScreen(
                  uiState = uiState,
                  onCraftOrUpgrade = viewModel::craftOrUpgradeWeapon,
                  onEquipWeapon = viewModel::equipWeapon,
                  onCraftMedkit = viewModel::craftMedkit,
                  onCraftEnergyCells = viewModel::craftEnergyCells,
                  onOpenCrafting = viewModel::openCraftingStation,
                  onOpenAbilityTree = viewModel::openAbilityTree,
                )
                AppTab.SQUAD -> SquadScreen(
                  uiState = uiState,
                  onToggleMate = viewModel::toggleSquadMate,
                  onRegenerateCode = viewModel::regenerateSquadCode,
                  onLaunchRaid = viewModel::launchCoopRaid,
                )
              }
            }
          }
        }
      }
    }

    val activeCombat = uiState.combat
    if (activeCombat != null) {
      CombatOverlay(
        uiState = uiState,
        combat = activeCombat,
        onJoystickVector = viewModel::setJoystickVector,
        onPrimaryStrike = viewModel::performPrimaryAttack,
        onSprintStrike = viewModel::performSprintAttack,
        onHeavyStrike = viewModel::performHeavyOverheadStrike,
        onDodgeRoll = viewModel::performDodgeRoll,
        onMedkit = viewModel::performCombatMedkit,
        onSquadUltimate = viewModel::performSquadUltimate,
        onToggleMute = viewModel::toggleAudioMute,
        onExitCombat = viewModel::closeCombat,
      )
    }

    if (uiState.isInventoryOpen) {
      InventoryGridDialog(
        slots = uiState.inventorySlots,
        onUseSlot = viewModel::useInventorySlot,
        onSalvageSlot = viewModel::salvageInventorySlot,
        onDropSlot = viewModel::dropInventorySlot,
        onOpenCrafting = {
          viewModel.closeInventory()
          viewModel.openCraftingStation()
        },
        onDismiss = viewModel::closeInventory,
      )
    }

    if (uiState.isCraftingOpen) {
      CraftingStationDialog(
        survivor = uiState.survivor,
        inventorySlots = uiState.inventorySlots,
        onCraftRecipe = viewModel::craftRecipe,
        onOpenInventory = {
          viewModel.closeCraftingStation()
          viewModel.openInventory()
        },
        onDismiss = viewModel::closeCraftingStation,
      )
    }

    if (uiState.isAbilityTreeOpen) {
      AbilityTreeDialog(
        unlockedIds = uiState.unlockedAbilityIds,
        availablePoints = uiState.availableAbilityPoints,
        totalPoints = AbilityTreeCatalog.totalEarnedPoints(
          level = uiState.survivor.level,
          missionsCompleted = uiState.survivor.missionsCompleted,
        ),
        bonuses = uiState.abilityBonuses,
        onUnlockNode = viewModel::unlockAbility,
        onResetTree = viewModel::resetAbilityTree,
        onDismiss = viewModel::closeAbilityTree,
      )
    }
  }
}
