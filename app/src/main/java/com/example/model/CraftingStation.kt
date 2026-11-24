package com.example.model

import com.example.data.local.InventorySlotEntity
import com.example.data.local.SurvivorEntity

enum class CraftingCategory(val label: String) {
  AMMUNITION("Ammunition"),
  MEDICAL("Medical Supplies"),
  BASE_UPGRADE("Base Upgrades"),
}

data class CraftingIngredient(
  val itemId: String,
  val displayName: String,
  val quantity: Int,
)

data class CraftingRecipe(
  val id: String,
  val name: String,
  val category: CraftingCategory,
  val outputItemId: String,
  val outputCategory: String,
  val outputQty: Int,
  val rarity: LootRarity,
  val woodCost: Int = 0,
  val metalCost: Int = 0,
  val crystalCost: Int = 0,
  val gridIngredients: List<CraftingIngredient> = emptyList(),
  val targetBuildingId: String? = null,
  val summary: String,
)

object CraftingStationCatalog {
  val recipes: List<CraftingRecipe> = listOf(
    // --- AMMUNITION ---
    CraftingRecipe(
      id = "craft_plasma_cells",
      name = "Plasma Energy Cells (x14)",
      category = CraftingCategory.AMMUNITION,
      outputItemId = "energy_cell",
      outputCategory = "TECH",
      outputQty = 14,
      rarity = LootRarity.UNCOMMON,
      metalCost = 8,
      crystalCost = 2,
      summary = "Combines Scrap Metal & Bio-Crystals into +14 Plasma Cells for Futuristic weapons & Turrets.",
    ),
    CraftingRecipe(
      id = "craft_overcharge_ammo",
      name = "Overcharge Cryo Rounds (x2)",
      category = CraftingCategory.AMMUNITION,
      outputItemId = "overcharge_ammo",
      outputCategory = "CONSUMABLE",
      outputQty = 2,
      rarity = LootRarity.RARE,
      metalCost = 10,
      crystalCost = 3,
      gridIngredients = listOf(CraftingIngredient("titanium_alloy", "Titanium Alloy", 1)),
      summary = "Stores high-density ammo packs in your Grid. Use for +24 Plasma Cells & +30 Stamina.",
    ),
    CraftingRecipe(
      id = "craft_quantum_battery",
      name = "Apex Quantum Battery (x1)",
      category = CraftingCategory.AMMUNITION,
      outputItemId = "quantum_battery",
      outputCategory = "CONSUMABLE",
      outputQty = 1,
      rarity = LootRarity.LEGENDARY,
      metalCost = 14,
      crystalCost = 5,
      gridIngredients = listOf(CraftingIngredient("apex_core", "Apex Core", 1)),
      summary = "Boss-grade power cell in Inventory Grid. Use for +45 Plasma Cells, +50 HP & full Stamina.",
    ),

    // --- MEDICAL SUPPLIES ---
    CraftingRecipe(
      id = "craft_field_bandage",
      name = "Field Trauma Bandages (x2)",
      category = CraftingCategory.MEDICAL,
      outputItemId = "field_bandage",
      outputCategory = "CONSUMABLE",
      outputQty = 2,
      rarity = LootRarity.COMMON,
      woodCost = 8,
      metalCost = 2,
      summary = "Adds 2 Field Bandages to your Grid Inventory. Use for +35 HP & +15 Immunity.",
    ),
    CraftingRecipe(
      id = "craft_tactical_medkit",
      name = "Tactical Medkit (+1)",
      category = CraftingCategory.MEDICAL,
      outputItemId = "tactical_medkit",
      outputCategory = "CONSUMABLE",
      outputQty = 1,
      rarity = LootRarity.UNCOMMON,
      woodCost = 8,
      crystalCost = 2,
      summary = "Emergency trauma patch synced to your Medkit reserve & Grid (+55 HP in combat / full heal outside).",
    ),
    CraftingRecipe(
      id = "craft_nano_serum",
      name = "Nano-Regen Serum (x1)",
      category = CraftingCategory.MEDICAL,
      outputItemId = "nano_serum",
      outputCategory = "CONSUMABLE",
      outputQty = 1,
      rarity = LootRarity.EPIC,
      crystalCost = 4,
      gridIngredients = listOf(
        CraftingIngredient("chem_reagent", "Chem Reagent", 1),
        CraftingIngredient("titanium_alloy", "Titanium Alloy", 1),
      ),
      summary = "Military bio-serum stored in Grid. Restores 100% HP, Immunity & +45 Stamina.",
    ),

    // --- BASE UPGRADES ---
    CraftingRecipe(
      id = "craft_barricade_kit",
      name = "Fortified Barricade Kit (x1)",
      category = CraftingCategory.BASE_UPGRADE,
      outputItemId = "barricade_kit",
      outputCategory = "CONSUMABLE",
      outputQty = 1,
      rarity = LootRarity.RARE,
      woodCost = 12,
      metalCost = 14,
      crystalCost = 2,
      summary = "Stores a deployable defense kit in your Grid. Use anytime to reinforce Base Integrity (+45 HP).",
    ),
    CraftingRecipe(
      id = "craft_bunker_bulkhead",
      name = "Command Bunker Bulkhead (+1 Lv)",
      category = CraftingCategory.BASE_UPGRADE,
      outputItemId = "bunker_upgrade",
      outputCategory = "BUILDING",
      outputQty = 1,
      rarity = LootRarity.RARE,
      woodCost = 16,
      metalCost = 16,
      gridIngredients = listOf(CraftingIngredient("titanium_alloy", "Titanium Alloy", 1)),
      targetBuildingId = "bunker",
      summary = "Upgrades Command Bunker (+1 Lv, +25 Max HP & Base HP) using discounted Titanium Alloy.",
    ),
    CraftingRecipe(
      id = "craft_turret_module",
      name = "Laser Turret Capacitor (+1 Lv)",
      category = CraftingCategory.BASE_UPGRADE,
      outputItemId = "turret_upgrade",
      outputCategory = "BUILDING",
      outputQty = 1,
      rarity = LootRarity.EPIC,
      metalCost = 14,
      crystalCost = 5,
      gridIngredients = listOf(CraftingIngredient("titanium_alloy", "Titanium Alloy", 1)),
      targetBuildingId = "turrets",
      summary = "Upgrades Perimeter Laser Turrets (+1 Lv) for automated base wave defense.",
    ),
  )

  fun findById(id: String): CraftingRecipe? = recipes.firstOrNull { it.id == id }

  fun gridQuantity(slots: List<InventorySlotEntity>, itemId: String): Int =
    slots.filter { it.itemId == itemId }.sumOf { it.quantity }

  fun canCraft(
    recipe: CraftingRecipe,
    survivor: SurvivorEntity,
    slots: List<InventorySlotEntity>,
  ): Boolean {
    if (survivor.wood < recipe.woodCost) return false
    if (survivor.metal < recipe.metalCost) return false
    if (survivor.crystals < recipe.crystalCost) return false
    return recipe.gridIngredients.all { ing ->
      gridQuantity(slots, ing.itemId) >= ing.quantity
    }
  }
}

object LootDropTable {
  fun rollWorldNodeRarity(
    biome: BiomeType,
    weather: WeatherType,
    rollPercent: Int,
  ): LootRarity {
    val biomeShift = (biome.dangerTier - 1) * 7
    val weatherShift = if (weather == WeatherType.BIO_STORM) 8 else if (weather == WeatherType.ASH_FOG) 4 else 0
    val adjusted = (rollPercent + biomeShift + weatherShift).coerceIn(0, 120)
    return when {
      adjusted >= 106 -> LootRarity.LEGENDARY
      adjusted >= 93 -> LootRarity.EPIC
      adjusted >= 76 -> LootRarity.RARE
      adjusted >= 50 -> LootRarity.UNCOMMON
      else -> LootRarity.COMMON
    }
  }
}
