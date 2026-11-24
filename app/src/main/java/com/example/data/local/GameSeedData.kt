package com.example.data.local

object GameSeedData {
  val initialPlayerStats = PlayerStats(
    id = 1,
    health = 100,
    maxHealth = 100,
    stamina = 100,
    maxStamina = 100,
    hunger = 65,
    thirst = 60,
  )

  val initialSurvivor = SurvivorEntity(
    hp = 100,
    maxHp = 100,
    food = 65,
    water = 60,
    stamina = 100,
    immunity = 85,
    wood = 30,
    metal = 25,
    crystals = 10,
    medkits = 2,
    energyCells = 12,
  )

  val initialWeapons = listOf(
    WeaponEntity(
      id = "sword_machete",
      name = "Scrap Machete",
      category = "SWORD",
      level = 1,
      baseDamage = 24,
      critChance = 15,
      energyCost = 0,
      unlocked = true,
      woodCost = 10,
      metalCost = 15,
      crystalCost = 0,
      description = "Tempered salvage blade forged from 2045 steel girders. Reliable zero-ammo slash.",
    ),
    WeaponEntity(
      id = "sword_katana",
      name = "Carbon Ronin Katana",
      category = "SWORD",
      level = 1,
      baseDamage = 40,
      critChance = 28,
      energyCost = 0,
      unlocked = false,
      woodCost = 15,
      metalCost = 45,
      crystalCost = 8,
      description = "Razor-edge carbon-fiber blade built for lightning parries and high critical bleed.",
    ),
    WeaponEntity(
      id = "sword_thermal",
      name = "Thermal Edge Blade",
      category = "SWORD",
      level = 1,
      baseDamage = 64,
      critChance = 35,
      energyCost = 1,
      unlocked = false,
      woodCost = 10,
      metalCost = 70,
      crystalCost = 20,
      description = "Superheated plasma-edged sword that slices right through Giant Boss exoskeletons.",
    ),
    WeaponEntity(
      id = "bow_recurve",
      name = "Hunter Recurve Bow",
      category = "BOW",
      level = 1,
      baseDamage = 28,
      critChance = 25,
      energyCost = 0,
      unlocked = true,
      woodCost = 20,
      metalCost = 10,
      crystalCost = 0,
      description = "Silent composite wood bow. Excels in Ashwood Forest and foggy ambushes.",
    ),
    WeaponEntity(
      id = "bow_crossbow",
      name = "Viper Compound Crossbow",
      category = "BOW",
      level = 1,
      baseDamage = 48,
      critChance = 34,
      energyCost = 0,
      unlocked = false,
      woodCost = 35,
      metalCost = 40,
      crystalCost = 10,
      description = "High-tension steel bolts punch deep into mutated beast weak-points.",
    ),
    WeaponEntity(
      id = "bow_arc",
      name = "Shockwave Arc Bow",
      category = "BOW",
      level = 1,
      baseDamage = 72,
      critChance = 42,
      energyCost = 1,
      unlocked = false,
      woodCost = 30,
      metalCost = 55,
      crystalCost = 24,
      description = "Electrified magnetic arrows stun colossal bio-monsters on critical hits.",
    ),
    WeaponEntity(
      id = "tech_pulse",
      name = "Pulse Rifle MK-II",
      category = "FUTURISTIC",
      level = 1,
      baseDamage = 46,
      critChance = 22,
      energyCost = 1,
      unlocked = false,
      woodCost = 10,
      metalCost = 50,
      crystalCost = 12,
      description = "Pre-collapse military kinetic-pulse carbine recovered from Metro defense convoys.",
    ),
    WeaponEntity(
      id = "tech_railgun",
      name = "Helios Plasma Railgun",
      category = "FUTURISTIC",
      level = 1,
      baseDamage = 78,
      critChance = 30,
      energyCost = 2,
      unlocked = false,
      woodCost = 15,
      metalCost = 80,
      crystalCost = 28,
      description = "Fires hyper-accelerated plasma slugs capable of shattering Frostpeak armor.",
    ),
    WeaponEntity(
      id = "tech_cryo_cannon",
      name = "Sector-9 Cryo-Arc Cannon",
      category = "FUTURISTIC",
      level = 1,
      baseDamage = 108,
      critChance = 38,
      energyCost = 3,
      unlocked = false,
      woodCost = 20,
      metalCost = 110,
      crystalCost = 42,
      description = "Experimental prototype from Sector-9 Labs. Unleashes devastating thermal-arc bursts.",
    ),
  )

  val initialBuildings = listOf(
    BaseBuildingEntity(
      id = "bunker",
      name = "Command Bunker",
      level = 1,
      maxLevel = 5,
      woodCost = 25,
      metalCost = 30,
      crystalCost = 5,
      bonusLabel = "+25 Max HP / lv",
      description = "Reinforced survival headquarters. Upgrades expand vital capacity & defense.",
    ),
    BaseBuildingEntity(
      id = "purifier",
      name = "Rainwater Purifier",
      level = 1,
      maxLevel = 5,
      woodCost = 18,
      metalCost = 22,
      crystalCost = 3,
      bonusLabel = "+12 Water / harvest",
      description = "Filters toxic 2045 rain into clean drinking water. Bonus yield during Rain!",
    ),
    BaseBuildingEntity(
      id = "greenhouse",
      name = "Hydroponic Greenhouse",
      level = 1,
      maxLevel = 5,
      woodCost = 25,
      metalCost = 15,
      crystalCost = 4,
      bonusLabel = "+12 Food / harvest",
      description = "UV-lit indoor pods growing virus-free rations even in Bio-Storms.",
    ),
    BaseBuildingEntity(
      id = "forge",
      name = "Weapons Forge & Tech Lab",
      level = 1,
      maxLevel = 5,
      woodCost = 20,
      metalCost = 35,
      crystalCost = 8,
      bonusLabel = "+10% Weapon DMG / lv",
      description = "Crafts Swords, Compound Bows, and futuristic Sector-9 plasma weapons.",
    ),
    BaseBuildingEntity(
      id = "turrets",
      name = "Perimeter Laser Turrets",
      level = 0,
      maxLevel = 5,
      woodCost = 15,
      metalCost = 40,
      crystalCost = 10,
      bonusLabel = "+15 Defense Rating",
      description = "Automated defense grid protecting your camp from night creature raids.",
    ),
    BaseBuildingEntity(
      id = "radio",
      name = "Co-Op Radio Tower",
      level = 1,
      maxLevel = 5,
      woodCost = 20,
      metalCost = 30,
      crystalCost = 6,
      bonusLabel = "+15% Squad Power",
      description = "Long-range encrypted relay coordinating strikes with friend survivor squads.",
    ),
  )

  val initialSquadMates = listOf(
    SquadMateEntity(
      id = "mate_kira",
      callsign = "KIRA-07",
      role = "Vanguard Sword",
      weaponName = "Carbon Ronin Katana",
      power = 42,
      isSelected = true,
    ),
    SquadMateEntity(
      id = "mate_arjun",
      callsign = "ARJUN-X",
      role = "Recon Bow",
      weaponName = "Shockwave Arc Bow",
      power = 48,
      isSelected = true,
    ),
    SquadMateEntity(
      id = "mate_nova",
      callsign = "NOVA-9",
      role = "Plasma Heavy",
      weaponName = "Helios Plasma Railgun",
      power = 56,
      isSelected = false,
    ),
    SquadMateEntity(
      id = "mate_zara",
      callsign = "ZARA-MED",
      role = "Field Medic",
      weaponName = "Pulse Rifle MK-II",
      power = 38,
      isSelected = true,
    ),
  )

  fun initialInventorySlots(): List<InventorySlotEntity> {
    val populated = listOf(
      InventorySlotEntity(0, "salvaged_wood", "Salvaged Timber", "BUILDING", initialSurvivor.wood, "Reinforced lumber used for Camp upgrades & Bow crafting.", "COMMON"),
      InventorySlotEntity(1, "scrap_metal", "Scrap Metal", "BUILDING", initialSurvivor.metal, "Alloy plates required for Bunker walls, Turrets & Blades.", "COMMON"),
      InventorySlotEntity(2, "bio_crystal", "Bio-Crystal", "TECH", initialSurvivor.crystals, "High-energy 2045 shard powering Plasma weapons & Tech Labs.", "UNCOMMON"),
      InventorySlotEntity(3, "ration_pack", "MRE Ration Pack", "CONSUMABLE", 3, "Vacuum-sealed survival meal (+25 Hunger, +12 HP).", "COMMON"),
      InventorySlotEntity(4, "purified_water", "Purified Flask", "CONSUMABLE", 3, "Filtered clean water (+28 Thirst, +25 Stamina).", "COMMON"),
      InventorySlotEntity(5, "tactical_medkit", "Tactical Medkit", "CONSUMABLE", initialSurvivor.medkits, "Emergency trauma patch (Restores HP & Immunity).", "UNCOMMON"),
      InventorySlotEntity(6, "energy_cell", "Plasma Cell", "TECH", initialSurvivor.energyCells, "Ammunition core for Futuristic weapons & Turrets.", "UNCOMMON"),
      InventorySlotEntity(7, "titanium_alloy", "Titanium Alloy", "RARE", 2, "Recovered 2045 armor plating. Craft Base Modules or Salvage for resources.", "RARE"),
      InventorySlotEntity(8, "chem_reagent", "Chem Reagent", "RARE", 2, "Sterile medical compound used in Crafting Station serums.", "UNCOMMON"),
    )
    val empty = (populated.size until 20).map { idx ->
      InventorySlotEntity(idx, "", "Empty Slot", "EMPTY", 0, "Collect exploration loot to fill this slot.", "COMMON")
    }
    return populated + empty
  }

  private data class StoryBlueprint(
    val title: String,
    val biomeId: String,
    val mobName: String,
    val bossPrefix: String,
    val settingHook: String,
  )

  fun build1000StoryMissions(): List<MissionEntity> {
    val storyArcs = listOf(
      StoryBlueprint("Outbreak Zero: Metro Ruins", "DEAD_CITY", "Virus Stalker", "Metro Goliath", "2045 virus ke pehle ghante shattered subway lines mein survive karo."),
      StoryBlueprint("Ashwood Quarantine", "ASHWOOD_FOREST", "Spore Ravager", "Blighted Treant", "Toxic pine canopy mein camp ke liye wood aur clean water looto."),
      StoryBlueprint("Frostpeak Relay", "FROSTPEAK_RIDGE", "Frost Golem", "Glacier Titan", "Frozen mountain ridges par satellite relay signal wapas chalu karo."),
      StoryBlueprint("Sector-9 Descent", "SECTOR_9_LAB", "Cryo Mutant", "Lab Leviathan", "400m neeche underground bio-lab mein Patient Zero files recover karo."),
      StoryBlueprint("Blackout Highway", "DEAD_CITY", "Armored Brute", "Highway Colossus", "Toote flyovers aur burnt military convoys ke beech rasta saaf karo."),
      StoryBlueprint("Crimson Canopy", "ASHWOOD_FOREST", "Canopy Stalker", "Apex Vine Hydra", "Mutated jungle spores raat ko aggressive ho rahe hain—nests jalao."),
      StoryBlueprint("Avalanche Citadel", "FROSTPEAK_RIDGE", "Ice Stalker", "Permafrost Warden", "Barfeele bunker vaults mein pre-collapse plasma batteries dhoondo."),
      StoryBlueprint("Cryo Containment Breach", "SECTOR_9_LAB", "Bio-Guard Mutant", "Omega Chimera", "Sector-9 ke cryo chambers khul gaye hain—wardens ko roko."),
      StoryBlueprint("Skybridge Resistance", "DEAD_CITY", "Rooftop Ravager", "Tower Dreadnought", "High-rise glass bridges par rooftop packs ka kabza todo."),
      StoryBlueprint("Venom Dam Watershed", "ASHWOOD_FOREST", "Swamp Ravager", "Dam Behemoth", "Purifier supply bachane ke liye flooded river basin secure karo."),
      StoryBlueprint("Thunder Ridge Siege", "FROSTPEAK_RIDGE", "Volt Golem", "Storm-Peak Rex", "Lightning bio-storms ke beech high-altitude beacons defend karo."),
      StoryBlueprint("Fusion Reactor Core", "SECTOR_9_LAB", "Reactor Hound", "Core Abomination", "Overheating lab reactor ko stabilize karne ke liye mutants hatao."),
      StoryBlueprint("Subway Catacombs", "DEAD_CITY", "Tunnel Stalker", "Catacomb Tyrant", "Deep metro tunnels mein surviving civilians ke liye escape route kholo."),
      StoryBlueprint("Sawmill Stronghold", "ASHWOOD_FOREST", "Blight Stalker", "Ironwood Juggernaut", "Abandoned timber mill par alpha packs ke ambush tode."),
      StoryBlueprint("Titanium Quarry Front", "FROSTPEAK_RIDGE", "Quarry Golem", "Obsidian Crusher", "Futuristic weapons ke liye rare titanium veins mine karo."),
      StoryBlueprint("Cloning Vat Abyss", "SECTOR_9_LAB", "Apex Clone", "Genesis Sovereign", "Active cloning pods jo naye monsters paida kar rahe hain unhe destroy karo."),
      StoryBlueprint("Quarantine Wall Alpha", "DEAD_CITY", "Siege Brute", "Wall-Breaker Titan", "City perimeter defense laser turrets ko rebuild aur defend karo."),
      StoryBlueprint("Spore Eclipse", "ASHWOOD_FOREST", "Eclipse Ravager", "Spore Overlord", "Red ash fog ke beech final forest antidote herbs collect karo."),
      StoryBlueprint("Glacier Vault Omega", "FROSTPEAK_RIDGE", "Cryo Sentinel", "Zero-Kelvin Colossus", "Mountain summit ke secret vault door ko breach karo."),
      StoryBlueprint("Dawn of 2045: Final Cure", "SECTOR_9_LAB", "Omega Sentinel", "Patient Zero Prime", "Humanity ko bachane ke liye final antivirus warhead launch karo!"),
    )

    val phaseVerbs = listOf(
      "Perimeter Sweep", "Supply Intercept", "Shadow Ambush", "Signal Extraction", "Bunker Breach",
      "Toxic Patrol", "Night Stalk", "Convoy Salvage", "Reactor Run", "Alpha Hunt",
    )

    val missions = ArrayList<MissionEntity>(1000)
    var globalNumber = 1

    storyArcs.forEachIndexed { storyIdx, arc ->
      val storyNum = storyIdx + 1
      for (chapterInStory in 1..50) {
        val isBoss = chapterInStory % 5 == 0
        val bossRank = chapterInStory / 5
        val enemyLevel = (storyIdx * 3) + ((chapterInStory - 1) / 3) + 1
        val weatherIdx = (storyIdx + chapterInStory) % 4
        val phaseLabel = phaseVerbs[(chapterInStory - 1) % phaseVerbs.size]

        val title = if (isBoss) {
          "BOSS: ${arc.bossPrefix} MK-$bossRank"
        } else {
          "Ch.$chapterInStory • $phaseLabel"
        }

        val enemyName = if (isBoss) {
          "${arc.bossPrefix} [Tier $bossRank]"
        } else {
          "${arc.mobName} Lv.$enemyLevel"
        }

        val baseHp = if (isBoss) {
          360 + storyIdx * 85 + bossRank * 95
        } else {
          115 + storyIdx * 32 + chapterInStory * 9
        }

        val briefing = if (isBoss) {
          "STORY $storyNum — CHAPTER $chapterInStory BOSS! ${arc.settingHook} Giant Boss ${arc.bossPrefix} (Lv.$enemyLevel) ne sector block kar diya hai!"
        } else {
          "Story $storyNum (${arc.title}) • Ch.$chapterInStory/50: ${arc.settingHook} Next Boss in ${5 - (chapterInStory % 5)} chapters."
        }

        val objective = if (isBoss) {
          "Eliminate Boss: $enemyName"
        } else {
          "Neutralize $enemyName & secure sector"
        }

        missions.add(
          MissionEntity(
            number = globalNumber,
            storyNumber = storyNum,
            storyTitle = arc.title,
            chapter = storyNum,
            chapterInStory = chapterInStory,
            chapterTitle = "Story $storyNum: ${arc.title}",
            title = title,
            biomeId = arc.biomeId,
            weatherIndex = weatherIdx,
            isBoss = isBoss,
            enemyName = enemyName,
            enemyLevel = enemyLevel,
            enemyHp = baseHp,
            completed = false,
            unlocked = globalNumber == 1,
            briefing = briefing,
            objective = objective,
            rewardXp = if (isBoss) 220 + enemyLevel * 18 else 55 + enemyLevel * 8,
            rewardCrystals = if (isBoss) 14 + bossRank * 3 + storyIdx else 3 + (chapterInStory / 10),
            rewardMetal = if (isBoss) 38 + enemyLevel * 3 else 12 + (chapterInStory / 2) + storyIdx * 2,
          )
        )
        globalNumber++
      }
    }
    return missions
  }
}
