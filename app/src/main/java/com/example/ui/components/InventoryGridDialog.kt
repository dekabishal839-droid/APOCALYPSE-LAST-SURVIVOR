package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.InventorySlotEntity
import com.example.model.LootRarity
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.BunkerSurface
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import kotlin.math.roundToInt

@Composable
fun InventoryGridDialog(
  slots: List<InventorySlotEntity>,
  onUseSlot: (InventorySlotEntity) -> Unit,
  onSalvageSlot: (InventorySlotEntity) -> Unit,
  onDropSlot: (InventorySlotEntity) -> Unit,
  onOpenCrafting: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  var selectedSlotIndex by remember { mutableIntStateOf(0) }
  val selectedSlot = slots.firstOrNull { it.slotIndex == selectedSlotIndex } ?: slots.firstOrNull()
  val usedCount = slots.count { it.itemId.isNotEmpty() && it.quantity > 0 }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.90f)
        .padding(vertical = 12.dp),
      color = BunkerSurface,
      shape = MaterialTheme.shapes.large,
      border = BorderStroke(1.5.dp, BioCyan.copy(alpha = 0.7f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Backpack, contentDescription = null, tint = BioAmber, modifier = Modifier.size(22.dp))
            Text(
              text = "SURVIVOR PACK ($usedCount / ${slots.size} SLOTS)",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            TacticalBadge(text = "LOOT RARITY", color = BioCyan)
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = onOpenCrafting,
              modifier = Modifier
                .height(48.dp)
                .testTag("inventory_open_crafting_button"),
              border = BorderStroke(1.dp, BioAmber),
            ) {
              Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = BioAmber)
              Spacer(Modifier.width(5.dp))
              Text("CRAFT STATION", style = MaterialTheme.typography.labelLarge, color = BioAmber)
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(48.dp)
                .testTag("close_inventory_button"),
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close inventory", tint = MaterialTheme.colorScheme.onSurface)
            }
          }
        }

        // Landscape Split: Left = 5x4 Grid, Right = Selected Slot Inspector & Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
              .weight(1.35f)
              .height(232.dp)
              .testTag("inventory_grid"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(slots, key = { it.slotIndex }) { slot ->
              val isSelected = slot.slotIndex == selectedSlotIndex
              val isOccupied = slot.itemId.isNotEmpty() && slot.quantity > 0
              val rarity = LootRarity.fromName(slot.rarity)
              val rarityColor = rarityAccentColor(rarity)
              val catColor = categoryColor(slot.category)
              Box(
                modifier = Modifier
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isOccupied) BunkerSurfaceElevated else ObsidianBg.copy(alpha = 0.55f))
                  .border(
                    width = if (isSelected) 2.2.dp else 1.2.dp,
                    color = if (isSelected) BioAmber else if (isOccupied) rarityColor.copy(alpha = 0.78f) else SteelBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                  )
                  .clickable { selectedSlotIndex = slot.slotIndex }
                  .testTag("inventory_slot_${slot.slotIndex}")
                  .padding(4.dp),
              ) {
                if (isOccupied) {
                  if (rarity != LootRarity.COMMON) {
                    Text(
                      text = rarity.shortTag,
                      style = MaterialTheme.typography.labelSmall,
                      color = rarityColor,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.align(Alignment.TopStart),
                    )
                  }
                  Icon(
                    imageVector = itemIcon(slot.itemId, slot.category),
                    contentDescription = slot.name,
                    tint = catColor,
                    modifier = Modifier
                      .size(22.dp)
                      .align(Alignment.Center),
                  )
                  Surface(
                    color = ObsidianBg.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd),
                  ) {
                    Text(
                      text = "x${slot.quantity}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                  }
                } else {
                  Text(
                    text = "${slot.slotIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SteelBorder,
                    modifier = Modifier.align(Alignment.Center),
                  )
                }
              }
            }
          }

          // Right Inspector Panel
          Surface(
            modifier = Modifier
              .weight(1f)
              .height(232.dp),
            color = BunkerSurfaceElevated,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, SteelBorder),
          ) {
            if (selectedSlot != null && selectedSlot.itemId.isNotEmpty() && selectedSlot.quantity > 0) {
              val catColor = categoryColor(selectedSlot.category)
              val rarity = LootRarity.fromName(selectedSlot.rarity)
              val rarityColor = rarityAccentColor(rarity)
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(itemIcon(selectedSlot.itemId, selectedSlot.category), contentDescription = null, tint = rarityColor, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = selectedSlot.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "${selectedSlot.category} • QTY ${selectedSlot.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = catColor,
                      )
                    }
                    TacticalBadge(text = rarity.label.uppercase(), color = rarityColor)
                  }
                  Text(
                    text = selectedSlot.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  if (selectedSlot.category == "CONSUMABLE") {
                    Button(
                      onClick = { onUseSlot(selectedSlot) },
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("inventory_use_button"),
                      colors = ButtonDefaults.buttonColors(containerColor = BioEmerald, contentColor = ObsidianBg),
                    ) {
                      Text("USE ITEM", fontWeight = FontWeight.Bold)
                    }
                  } else if (selectedSlot.category == "RARE") {
                    val metalOut = (16 * rarity.salvageMultiplier).roundToInt()
                    val crysOut = (4 * rarity.salvageMultiplier).roundToInt()
                    Button(
                      onClick = { onSalvageSlot(selectedSlot) },
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("inventory_salvage_button"),
                      colors = ButtonDefaults.buttonColors(containerColor = rarityColor, contentColor = ObsidianBg),
                    ) {
                      Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("SALVAGE (+${metalOut}M +${crysOut}C)", fontWeight = FontWeight.Bold)
                    }
                  } else {
                    Text(
                      text = "Combine in Crafting Station, Base Upgrades, or Armory.",
                      style = MaterialTheme.typography.labelSmall,
                      color = BioCyan,
                    )
                  }

                  if (selectedSlot.category != "BUILDING") {
                    OutlinedButton(
                      onClick = { onDropSlot(selectedSlot) },
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("inventory_drop_button"),
                      border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f)),
                    ) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("DISCARD 1", style = MaterialTheme.typography.labelSmall, color = CrimsonAlert)
                    }
                  }
                }
              }
            } else {
              Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                Text(
                  text = "Empty Grid Slot.\nExplore the wasteland or slay monsters to collect survival & base-building resources.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun categoryColor(category: String): Color = when (category) {
  "BUILDING" -> BioAmber
  "CONSUMABLE" -> BioEmerald
  "TECH" -> BioCyan
  "RARE" -> Color(0xFFA855F7)
  else -> SteelBorder
}

private fun itemIcon(itemId: String, category: String): ImageVector = when {
  itemId.contains("wood") -> Icons.Default.Forest
  itemId.contains("metal") -> Icons.Default.Hardware
  itemId.contains("crystal") -> Icons.Default.Diamond
  itemId.contains("ration") -> Icons.Default.Restaurant
  itemId.contains("water") -> Icons.Default.WaterDrop
  itemId.contains("medkit") -> Icons.Default.LocalHospital
  itemId.contains("cell") -> Icons.Default.Bolt
  category == "RARE" -> Icons.Default.Star
  else -> Icons.Default.Backpack
}
