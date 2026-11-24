package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.InventorySlotEntity
import com.example.data.local.SurvivorEntity
import com.example.model.CraftingCategory
import com.example.model.CraftingRecipe
import com.example.model.CraftingStationCatalog
import com.example.model.LootRarity
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.BunkerSurface
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder

@Composable
fun CraftingStationDialog(
  survivor: SurvivorEntity,
  inventorySlots: List<InventorySlotEntity>,
  onCraftRecipe: (String) -> Unit,
  onOpenInventory: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  var selectedCategory by remember { mutableStateOf<CraftingCategory?>(null) }

  val filteredRecipes = remember(selectedCategory) {
    val cat = selectedCategory
    if (cat == null) CraftingStationCatalog.recipes
    else CraftingStationCatalog.recipes.filter { it.category == cat }
  }

  val titaniumCount = CraftingStationCatalog.gridQuantity(inventorySlots, "titanium_alloy")
  val reagentCount = CraftingStationCatalog.gridQuantity(inventorySlots, "chem_reagent")
  val apexCoreCount = CraftingStationCatalog.gridQuantity(inventorySlots, "apex_core")

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .fillMaxHeight(0.90f)
        .padding(vertical = 8.dp),
      color = BunkerSurface,
      shape = MaterialTheme.shapes.large,
      border = BorderStroke(1.5.dp, BioCyan.copy(alpha = 0.78f)),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Default.Build, contentDescription = "Crafting Station", tint = BioAmber, modifier = Modifier.size(22.dp))
            Text(
              text = "CRAFTING STATION WORKBENCH",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
            TacticalBadge(text = "AMMO • MEDS • BASE", color = BioCyan)
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onOpenInventory,
              modifier = Modifier
                .height(48.dp)
                .testTag("crafting_open_inventory_button"),
              border = BorderStroke(1.dp, BioCyan),
            ) {
              Icon(Icons.Default.Backpack, contentDescription = null, modifier = Modifier.size(16.dp), tint = BioCyan)
              Spacer(Modifier.width(5.dp))
              Text("INVENTORY BAG", style = MaterialTheme.typography.labelLarge, color = BioCyan)
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(48.dp)
                .testTag("close_crafting_station_button"),
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close Crafting Station", tint = MaterialTheme.colorScheme.onSurface)
            }
          }
        }

        // Live Collected Materials Strip (Synced Core + Grid Reagents)
        Surface(
          color = ObsidianBg.copy(alpha = 0.72f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, SteelBorder),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "STOCK:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold,
            )
            ResourcePill(Icons.Default.Forest, "${survivor.wood}", BioEmerald, "Wood")
            ResourcePill(Icons.Default.Hardware, "${survivor.metal}", MaterialTheme.colorScheme.onSurfaceVariant, "Metal")
            ResourcePill(Icons.Default.Diamond, "${survivor.crystals}", BioCyan, "Bio-Crystal")
            ResourcePill(Icons.Default.Star, "Ti:$titaniumCount", rarityAccentColor(LootRarity.RARE), "Titanium Alloy")
            ResourcePill(Icons.Default.LocalHospital, "Chem:$reagentCount", rarityAccentColor(LootRarity.UNCOMMON), "Chem Reagent")
            ResourcePill(Icons.Default.Bolt, "Apex:$apexCoreCount", rarityAccentColor(LootRarity.LEGENDARY), "Apex Core")
          }
        }

        // Category Filter Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CraftingFilterChip(
            label = "ALL (${CraftingStationCatalog.recipes.size})",
            selected = selectedCategory == null,
            color = BioAmber,
            onClick = { selectedCategory = null },
            testTag = "crafting_tab_all",
            modifier = Modifier.weight(1f),
          )
          CraftingCategory.entries.forEach { cat ->
            val catColor = when (cat) {
              CraftingCategory.AMMUNITION -> BioCyan
              CraftingCategory.MEDICAL -> BioEmerald
              CraftingCategory.BASE_UPGRADE -> BioAmber
            }
            CraftingFilterChip(
              label = cat.label.uppercase(),
              selected = selectedCategory == cat,
              color = catColor,
              onClick = { selectedCategory = cat },
              testTag = "crafting_tab_${cat.name.lowercase()}",
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Recipes Grid
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("crafting_recipes_grid"),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filteredRecipes, key = { it.id }) { recipe ->
            val canCraft = CraftingStationCatalog.canCraft(recipe, survivor, inventorySlots)
            CraftingRecipeCard(
              recipe = recipe,
              survivor = survivor,
              inventorySlots = inventorySlots,
              canCraft = canCraft,
              onCraft = { onCraftRecipe(recipe.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CraftingFilterChip(
  label: String,
  selected: Boolean,
  color: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .height(42.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    color = if (selected) color.copy(alpha = 0.22f) else BunkerSurfaceElevated,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, if (selected) color else SteelBorder),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun CraftingRecipeCard(
  recipe: CraftingRecipe,
  survivor: SurvivorEntity,
  inventorySlots: List<InventorySlotEntity>,
  canCraft: Boolean,
  onCraft: () -> Unit,
) {
  val rarityColor = rarityAccentColor(recipe.rarity)
  val catIcon = when (recipe.category) {
    CraftingCategory.AMMUNITION -> Icons.Default.Bolt
    CraftingCategory.MEDICAL -> Icons.Default.LocalHospital
    CraftingCategory.BASE_UPGRADE -> Icons.Default.HomeWork
  }

  Surface(
    color = BunkerSurfaceElevated,
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(if (canCraft) 1.5.dp else 1.dp, rarityColor.copy(alpha = if (canCraft) 0.85f else 0.42f)),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(catIcon, contentDescription = recipe.category.label, tint = rarityColor, modifier = Modifier.size(18.dp))
          Text(
            text = recipe.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        TacticalBadge(text = recipe.rarity.label.uppercase(), color = rarityColor)
      }

      Text(
        text = recipe.summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      // Ingredient Chips
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (recipe.woodCost > 0) {
          IngredientPill("Wood ${recipe.woodCost}", survivor.wood >= recipe.woodCost)
        }
        if (recipe.metalCost > 0) {
          IngredientPill("Metal ${recipe.metalCost}", survivor.metal >= recipe.metalCost)
        }
        if (recipe.crystalCost > 0) {
          IngredientPill("Crystal ${recipe.crystalCost}", survivor.crystals >= recipe.crystalCost)
        }
        recipe.gridIngredients.forEach { ing ->
          val have = CraftingStationCatalog.gridQuantity(inventorySlots, ing.itemId)
          IngredientPill("${ing.displayName} ${ing.quantity}", have >= ing.quantity)
        }
      }

      Button(
        onClick = onCraft,
        enabled = canCraft,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("craft_recipe_${recipe.id}"),
        colors = ButtonDefaults.buttonColors(
          containerColor = rarityColor,
          contentColor = ObsidianBg,
          disabledContainerColor = BunkerSurface,
          disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
      ) {
        Text(
          text = if (canCraft) "CRAFT • ${recipe.category.label.uppercase()}" else "MISSING MATERIALS",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun IngredientPill(
  label: String,
  satisfied: Boolean,
) {
  val color = if (satisfied) BioEmerald else CrimsonAlert
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(color.copy(alpha = 0.14f))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

fun rarityAccentColor(rarity: LootRarity): Color = when (rarity) {
  LootRarity.COMMON -> Color(0xFF94A3B8)
  LootRarity.UNCOMMON -> BioEmerald
  LootRarity.RARE -> BioCyan
  LootRarity.EPIC -> Color(0xFFA855F7)
  LootRarity.LEGENDARY -> BioAmber
}
