package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BaseBuildingEntity
import com.example.model.BaseZoneRules
import com.example.model.WeatherType
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CrimsonAlert
import com.example.ui.components.ResourceCostRow
import com.example.ui.components.TacticalBadge
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.BunkerSurface
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import com.example.viewmodel.GameUiState

@Composable
fun BaseCampScreen(
  uiState: GameUiState,
  onCollectYield: () -> Unit,
  onUpgradeBuilding: (BaseBuildingEntity) -> Unit,
  onOpenInventory: () -> Unit,
  onOpenCrafting: () -> Unit,
  onDeployBaseDefense: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(168.dp)
          .clip(MaterialTheme.shapes.large)
          .border(1.dp, SteelBorder, MaterialTheme.shapes.large),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_camp_base),
          contentDescription = "Survival Camp Sanctuary",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(ObsidianBg.copy(alpha = 0.2f), ObsidianBg.copy(alpha = 0.92f))
              )
            )
        )
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(14.dp),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TacticalBadge(text = "DEFENSE ${uiState.defenseRating}", color = BioEmerald)
            TacticalBadge(text = "FORGE x${"%.1f".format(uiState.forgeBonusMultiplier)}", color = BioAmber)
          }
          Spacer(Modifier.height(6.dp))
          Text(
            text = "SANCTUARY BASE CAMP",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = if (uiState.currentWeather == WeatherType.RAIN) {
              "Acid Rain active: Rainwater Purifier +40% bonus yield!"
            } else "Construct & upgrade camp modules to survive 2045.",
            style = MaterialTheme.typography.bodyMedium,
            color = BioCyan,
          )
        }
      }
    }

    item {
      val wave = uiState.baseWave
      val turretLv = uiState.buildings.firstOrNull { it.id == "turrets" }?.level ?: 0
      val nextCount = BaseZoneRules.computeWaveEnemyCount(wave.waveNumber, uiState.difficulty)
      val nextLv = BaseZoneRules.computeWaveEnemyLevel(uiState.survivor.level, wave.waveNumber, uiState.difficulty)
      val turretDmg = BaseZoneRules.turretBarrageDamage(turretLv)
      val integrityRatio = (wave.baseIntegrity.toFloat() / wave.maxBaseIntegrity.coerceAtLeast(1)).coerceIn(0f, 1f)

      Surface(
        color = BunkerSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (wave.waveActive) CrimsonAlert else BioAmber.copy(alpha = 0.7f)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "BASE SIEGE WAVE DEFENSE",
              style = MaterialTheme.typography.titleMedium,
              color = if (wave.waveActive) CrimsonAlert else BioAmber,
              fontWeight = FontWeight.Bold,
            )
            TacticalBadge(
              text = "WAVE ${wave.waveNumber} • ${wave.wavesSurvived} CLEARED",
              color = BioCyan,
            )
          }
          LinearProgressIndicator(
            progress = { integrityRatio },
            modifier = Modifier
              .fillMaxWidth()
              .height(7.dp)
              .clip(MaterialTheme.shapes.small),
            color = if (integrityRatio < 0.35f) CrimsonAlert else BioEmerald,
            trackColor = BunkerSurfaceElevated,
          )
          Text(
            text = "Base Integrity: ${wave.baseIntegrity}/${wave.maxBaseIntegrity} • Incoming: $nextCount Raiders (Lv.$nextLv, Threat ${uiState.difficulty.threatTier.label}) • Turret Auto-Fire: $turretDmg DMG",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = onDeployBaseDefense,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("deploy_base_defense_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (wave.waveActive) CrimsonAlert else BioAmber,
              contentColor = ObsidianBg,
            ),
          ) {
            Text(
              text = if (wave.waveActive) "RETURN TO ACTIVE BASE SIEGE" else "DEPLOY TO BASE PERIMETER & START WAVE ${wave.waveNumber}",
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    item {
      // Interactive Camp Blueprint Grid & Harvest Action
      Surface(
        color = BunkerSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, SteelBorder),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "CAMP SCHEMATIC GRID",
            style = MaterialTheme.typography.labelLarge,
            color = BioCyan,
          )
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(84.dp)
              .clip(MaterialTheme.shapes.small)
              .background(BunkerSurfaceElevated)
          ) {
            val cols = uiState.buildings.size.coerceAtLeast(1)
            val slotW = size.width / cols
            uiState.buildings.forEachIndexed { index, b ->
              val active = b.level > 0
              val x = index * slotW + 8f
              val boxW = slotW - 16f
              drawRect(
                color = if (active) BioEmerald.copy(alpha = 0.24f) else SteelBorder.copy(alpha = 0.4f),
                topLeft = Offset(x, 14f),
                size = Size(boxW, size.height - 28f),
              )
              val barFill = (b.level / b.maxLevel.toFloat()).coerceIn(0f, 1f)
              drawRect(
                color = if (active) BioAmber else SteelBorder,
                topLeft = Offset(x + 4f, size.height - 20f),
                size = Size((boxW - 8f) * barFill, 6f),
              )
            }
          }

          Button(
            onClick = onCollectYield,
            enabled = uiState.campYieldReady,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("collect_camp_yield_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = BioEmerald,
              contentColor = ObsidianBg,
              disabledContainerColor = BunkerSurfaceElevated,
            ),
          ) {
            Text(
              text = if (uiState.campYieldReady) "HARVEST CAMP FOOD, WATER & CELLS" else "HARVEST RECHARGING...",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
          }

          val usedSlots = uiState.inventorySlots.count { it.itemId.isNotEmpty() && it.quantity > 0 }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onOpenInventory,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("camp_open_inventory_button"),
              border = BorderStroke(1.dp, BioCyan.copy(alpha = 0.7f)),
            ) {
              Icon(Icons.Default.Backpack, contentDescription = null, tint = BioCyan)
              Spacer(Modifier.width(6.dp))
              Text(
                text = "INVENTORY ($usedSlots/${uiState.inventorySlots.size.coerceAtLeast(20)})",
                style = MaterialTheme.typography.labelLarge,
                color = BioCyan,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
            }
            OutlinedButton(
              onClick = onOpenCrafting,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("camp_open_crafting_button"),
              border = BorderStroke(1.dp, BioAmber.copy(alpha = 0.8f)),
            ) {
              Icon(Icons.Default.Build, contentDescription = null, tint = BioAmber)
              Spacer(Modifier.width(6.dp))
              Text(
                text = "CRAFTING STATION",
                style = MaterialTheme.typography.labelLarge,
                color = BioAmber,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
            }
          }
        }
      }
    }

    items(uiState.buildings, key = { it.id }) { building ->
      val maxed = building.level >= building.maxLevel
      Surface(
        color = BunkerSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (building.level > 0) SteelBorder else BioAmber.copy(alpha = 0.5f)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = building.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = building.bonusLabel,
                style = MaterialTheme.typography.labelLarge,
                color = BioCyan,
              )
            }
            TacticalBadge(
              text = if (building.level == 0) "NOT BUILT" else "LV.${building.level}/${building.maxLevel}",
              color = if (maxed) BioEmerald else BioAmber,
            )
          }

          Text(
            text = building.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (!maxed) {
              ResourceCostRow(
                woodCost = building.woodCost,
                metalCost = building.metalCost,
                crystalCost = building.crystalCost,
                survivor = uiState.survivor,
              )
            } else {
              Text("MAX LEVEL REACHED", style = MaterialTheme.typography.labelLarge, color = BioEmerald)
            }
            Spacer(Modifier.width(8.dp))
            val canAfford = uiState.survivor.wood >= building.woodCost &&
              uiState.survivor.metal >= building.metalCost &&
              uiState.survivor.crystals >= building.crystalCost
            Button(
              onClick = { onUpgradeBuilding(building) },
              enabled = !maxed,
              modifier = Modifier
                .height(48.dp)
                .testTag("upgrade_building_${building.id}"),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (canAfford) BioAmber else BunkerSurfaceElevated,
                contentColor = if (canAfford) ObsidianBg else BioAmber,
                disabledContainerColor = BunkerSurfaceElevated,
              ),
              border = if (!maxed && !canAfford) BorderStroke(1.dp, BioAmber.copy(alpha = 0.45f)) else null,
            ) {
              Text(
                text = when {
                  maxed -> "MAXED"
                  building.level == 0 -> "BUILD"
                  else -> "UPGRADE"
                },
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}
