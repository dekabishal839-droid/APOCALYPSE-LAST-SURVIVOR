package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.WeaponEntity
import com.example.model.WeaponCategory
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
fun ArmoryScreen(
  uiState: GameUiState,
  onCraftOrUpgrade: (WeaponEntity) -> Unit,
  onEquipWeapon: (WeaponEntity) -> Unit,
  onCraftMedkit: () -> Unit,
  onCraftEnergyCells: () -> Unit,
  onOpenCrafting: () -> Unit,
  onOpenAbilityTree: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedCategory by remember { mutableStateOf(WeaponCategory.SWORD) }
  val filteredWeapons = remember(uiState.weapons, selectedCategory) {
    uiState.weapons.filter { it.category == selectedCategory.name }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Survival Gear Crafting & Ability Tree Strip
    Surface(
      color = BunkerSurface,
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.dp, SteelBorder),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "SURVIVAL FIELD CRAFTING & SKILLS",
            style = MaterialTheme.typography.labelLarge,
            color = BioCyan,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
              onClick = onOpenCrafting,
              modifier = Modifier
                .height(48.dp)
                .testTag("armory_open_crafting_button"),
              border = BorderStroke(1.dp, BioCyan),
            ) {
              Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = BioCyan)
              Spacer(Modifier.width(6.dp))
              Text(
                text = "CRAFT STATION",
                style = MaterialTheme.typography.labelLarge,
                color = BioCyan,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedButton(
              onClick = onOpenAbilityTree,
              modifier = Modifier
                .height(48.dp)
                .testTag("armory_skills_button"),
              border = BorderStroke(1.dp, if (uiState.availableAbilityPoints > 0) BioEmerald else BioAmber),
            ) {
              Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp), tint = BioAmber)
              Spacer(Modifier.width(6.dp))
              Text(
                text = "ABILITY TREE (${uiState.availableAbilityPoints} AP)",
                style = MaterialTheme.typography.labelLarge,
                color = if (uiState.availableAbilityPoints > 0) BioEmerald else BioAmber,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onCraftMedkit,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("craft_medkit_button"),
            border = BorderStroke(1.dp, BioEmerald),
          ) {
            Text(
              text = "+1 Medkit (8W 2C)",
              style = MaterialTheme.typography.labelLarge,
              color = BioEmerald,
            )
          }
          OutlinedButton(
            onClick = onCraftEnergyCells,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("craft_cells_button"),
            border = BorderStroke(1.dp, BioCyan),
          ) {
            Text(
              text = "+12 Cells (10M 3C)",
              style = MaterialTheme.typography.labelLarge,
              color = BioCyan,
            )
          }
        }
      }
    }

    // Category Tabs: Swords, Bows, Futuristic
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      WeaponCategory.entries.forEach { cat ->
        val selected = selectedCategory == cat
        Surface(
          modifier = Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .clickable { selectedCategory = cat }
            .testTag("weapon_tab_${cat.name.lowercase()}"),
          color = if (selected) BioAmber.copy(alpha = 0.2f) else BunkerSurface,
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, if (selected) BioAmber else SteelBorder),
        ) {
          Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = cat.displayName,
              style = MaterialTheme.typography.titleLarge,
              color = if (selected) BioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(filteredWeapons, key = { it.id }) { weapon ->
        val equipped = uiState.survivor.equippedWeaponId == weapon.id
        val effectiveDmg = (weapon.baseDamage * (1f + (weapon.level - 1) * 0.22f) * uiState.forgeBonusMultiplier).toInt()
        Surface(
          color = BunkerSurface,
          shape = MaterialTheme.shapes.medium,
          border = BorderStroke(1.dp, if (equipped) BioEmerald else SteelBorder),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = weapon.name,
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "DMG $effectiveDmg  •  CRIT ${weapon.critChance}%  •  CELLS ${weapon.energyCost}",
                  style = MaterialTheme.typography.labelLarge,
                  color = BioCyan,
                )
              }
              TacticalBadge(
                text = when {
                  equipped -> "EQUIPPED"
                  weapon.unlocked -> "LV.${weapon.level}"
                  else -> "LOCKED"
                },
                color = if (equipped) BioEmerald else if (weapon.unlocked) BioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Text(
              text = weapon.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ResourceCostRow(
              woodCost = weapon.woodCost,
              metalCost = weapon.metalCost,
              crystalCost = weapon.crystalCost,
              survivor = uiState.survivor,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (weapon.unlocked && !equipped) {
                OutlinedButton(
                  onClick = { onEquipWeapon(weapon) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("equip_weapon_${weapon.id}"),
                  border = BorderStroke(1.dp, BioEmerald),
                ) {
                  Text("EQUIP", color = BioEmerald, fontWeight = FontWeight.Bold)
                }
              }
              val canAfford = uiState.survivor.wood >= weapon.woodCost &&
                uiState.survivor.metal >= weapon.metalCost &&
                uiState.survivor.crystals >= weapon.crystalCost
              Button(
                onClick = { onCraftOrUpgrade(weapon) },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("upgrade_weapon_${weapon.id}"),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (canAfford) BioAmber else BunkerSurfaceElevated,
                  contentColor = if (canAfford) ObsidianBg else BioAmber,
                ),
                border = if (!canAfford) BorderStroke(1.dp, BioAmber.copy(alpha = 0.45f)) else null,
              ) {
                Text(
                  text = if (!weapon.unlocked) "CRAFT WEAPON" else "UPGRADE LV.${weapon.level + 1}",
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }
}
