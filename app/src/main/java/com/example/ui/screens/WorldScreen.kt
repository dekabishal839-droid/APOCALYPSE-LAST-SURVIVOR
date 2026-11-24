package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BaseWaveState
import com.example.model.BaseZoneRules
import com.example.model.BiomeType
import com.example.model.DifficultyState
import com.example.model.Enemy
import com.example.model.EnemyAiState
import com.example.model.LootRarity
import com.example.model.NodeKind
import com.example.model.Player
import com.example.model.TimeOfDay
import com.example.model.WeaponCategory
import com.example.model.WorldNode
import com.example.ui.components.BaseWaveHud
import com.example.ui.components.DirectionalHitMarkerOverlay
import com.example.ui.components.ParallaxCityscapeWithFog
import com.example.ui.components.SurvivalVitalsHud
import com.example.ui.components.TacticalBadge
import com.example.ui.components.ThreatMeterOverlayHud
import com.example.ui.components.ThumbActionButton
import com.example.ui.components.VirtualJoystick
import com.example.ui.components.WeatherAtmosphereOverlay
import com.example.ui.components.WeatherImpactHud
import com.example.ui.components.rarityAccentColor
import com.example.ui.components.rememberDamageShakeState
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.BunkerSurface
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import com.example.viewmodel.GameUiState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WorldScreen(
  uiState: GameUiState,
  onSelectBiome: (BiomeType) -> Unit,
  onCycleWeather: () -> Unit,
  onCycleTime: () -> Unit,
  onToggleAutoCycle: () -> Unit,
  onJoystickVector: (Float, Float) -> Unit,
  onTapMap: (Float, Float) -> Unit,
  onDashEvade: () -> Unit,
  onInteractNearest: () -> Unit,
  onRefreshSector: () -> Unit,
  onEatFood: () -> Unit,
  onDrinkWater: () -> Unit,
  onUseMedkit: () -> Unit,
  onToggleMute: () -> Unit,
  onOpenInventory: () -> Unit,
  onOpenCrafting: () -> Unit,
  onOpenAbilityTree: () -> Unit,
  onTriggerBaseWave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infinite = rememberInfiniteTransition(label = "world_pulse")
  val pulse by infinite.animateFloat(
    initialValue = 0.72f,
    targetValue = 1.18f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "node_pulse",
  )

  val weaponCategory = WeaponCategory.fromName(uiState.equippedWeapon?.category ?: "SWORD")
  val worldShake = rememberDamageShakeState(
    damageSeq = uiState.worldDamageSeq,
    intensity = uiState.worldHitIntensity,
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Top Vitals + Global Threat Meter + Audio Toggle + Inventory
    val usedSlots = uiState.inventorySlots.count { it.itemId.isNotEmpty() && it.quantity > 0 }
    SurvivalVitalsHud(
      survivor = uiState.survivor,
      difficulty = uiState.difficulty,
      audioMuted = uiState.audioMuted,
      onToggleMute = onToggleMute,
      usedInventorySlots = usedSlots,
      totalInventorySlots = uiState.inventorySlots.size.coerceAtLeast(20),
      onOpenInventory = onOpenInventory,
      onOpenCrafting = onOpenCrafting,
      availableAbilityPoints = uiState.availableAbilityPoints,
      onOpenAbilityTree = onOpenAbilityTree,
      onEatFood = onEatFood,
      onDrinkWater = onDrinkWater,
      onUseMedkit = onUseMedkit,
    )

    // Single sleek Biome + Dynamic Weather strip for landscape economy
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BiomeType.entries.forEach { biome ->
        val selected = uiState.currentBiome == biome
        Surface(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable { onSelectBiome(biome) }
            .testTag("biome_${biome.id.lowercase()}"),
          color = if (selected) BioAmber.copy(alpha = 0.18f) else BunkerSurface,
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, if (selected) BioAmber else SteelBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = biome.displayName,
              style = MaterialTheme.typography.labelLarge,
              color = if (selected) BioAmber else MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "T${biome.dangerTier}",
              style = MaterialTheme.typography.labelSmall,
              color = if (selected) BioCyan else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Surface(
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onCycleTime)
          .testTag("cycle_time_button"),
        color = BunkerSurfaceElevated,
        border = BorderStroke(1.dp, SteelBorder),
        shape = RoundedCornerShape(8.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (uiState.currentTimeOfDay == TimeOfDay.MIDNIGHT || uiState.currentTimeOfDay == TimeOfDay.DUSK) {
              Icons.Default.NightsStay
            } else Icons.Default.WbSunny,
            contentDescription = "Cycle Day or Night",
            tint = BioAmber,
            modifier = Modifier.size(15.dp),
          )
          Spacer(Modifier.width(5.dp))
          Text(uiState.currentTimeOfDay.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
      }

      Surface(
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onCycleWeather)
          .testTag("cycle_weather_button"),
        color = BunkerSurfaceElevated,
        border = BorderStroke(1.dp, SteelBorder),
        shape = RoundedCornerShape(8.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Default.Thunderstorm, contentDescription = "Cycle Weather", tint = BioCyan, modifier = Modifier.size(15.dp))
          Spacer(Modifier.width(5.dp))
          Text(uiState.currentWeather.displayName, style = MaterialTheme.typography.labelSmall, color = BioCyan)
        }
      }

      TacticalBadge(
        text = if (uiState.survivor.autoCycleWeather) "AUTO" else "MANUAL",
        color = if (uiState.survivor.autoCycleWeather) BioEmerald else BioAmber,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clickable(onClick = onToggleAutoCycle)
          .testTag("toggle_weather_auto"),
      )
    }

    // Full-Remaining-Height Landscape Dual-Thumb Open World Viewport with Damage Shake
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .graphicsLayer {
          translationX = worldShake.translationX
          translationY = worldShake.translationY
          rotationZ = worldShake.rotationDeg
        }
        .clip(MaterialTheme.shapes.large)
        .border(1.5.dp, SteelBorder, MaterialTheme.shapes.large)
        .testTag("open_world_canvas"),
    ) {
      val mapW = constraints.maxWidth.toFloat()
      val mapH = constraints.maxHeight.toFloat()

      val bgRes = when (uiState.currentBiome) {
        BiomeType.SECTOR_9_LAB -> R.drawable.img_biome_lab
        BiomeType.ASHWOOD_FOREST, BiomeType.FROSTPEAK_RIDGE -> R.drawable.img_camp_base
        BiomeType.DEAD_CITY -> R.drawable.img_hero_2045
      }
      Image(
        painter = painterResource(id = bgRes),
        contentDescription = "${uiState.currentBiome.displayName} environment",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = 1.14f
            scaleY = 1.14f
            translationX = (0.5f - uiState.player.x) * 56f
          }
          .alpha(0.42f),
      )

      ParallaxCityscapeWithFog(
        biome = uiState.currentBiome,
        weather = uiState.currentWeather,
        timeOfDay = uiState.currentTimeOfDay,
        parallaxOffset = uiState.player.x,
      )

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures { offset ->
              val nx = (offset.x / size.width).coerceIn(0.06f, 0.94f)
              val ny = (offset.y / size.height).coerceIn(0.14f, 0.88f)
              onTapMap(nx, ny)
            }
          }
      ) {
        val turretLv = uiState.buildings.firstOrNull { it.id == "turrets" }?.level ?: 0
        drawWorldEntitiesLayer(
          biome = uiState.currentBiome,
          timeOfDay = uiState.currentTimeOfDay,
          difficulty = uiState.difficulty,
          baseWave = uiState.baseWave,
          turretLevel = turretLv,
          resourceNodes = uiState.resourceNodes,
          enemies = uiState.enemies,
          player = uiState.player,
          weaponCategory = weaponCategory,
          pulse = pulse,
        )
      }

      // Floating Resource Labels with Loot Rarity tags
      uiState.resourceNodes.forEach { node ->
        val baseColor = when (node.kind) {
          NodeKind.FOOD -> BioAmber
          NodeKind.WATER -> BioCyan
          NodeKind.TIMBER -> BioEmerald
          NodeKind.CRYSTAL -> Color(0xFF38BDF8)
          else -> Color(0xFFE2E8F0)
        }
        val labelColor = if (node.rarity != LootRarity.COMMON) rarityAccentColor(node.rarity) else baseColor
        val nodeText = if (node.rarity != LootRarity.COMMON) "[${node.rarity.shortTag}] ${node.label}" else node.label
        Box(
          modifier = Modifier.offset {
            IntOffset(
              x = ((node.x * mapW).roundToInt() - 32).coerceIn(8, (mapW.toInt() - 96).coerceAtLeast(8)),
              y = ((node.y * mapH).roundToInt() - 36).coerceIn(10, (mapH.toInt() - 32).coerceAtLeast(10)),
            )
          }
        ) {
          Surface(
            color = ObsidianBg.copy(alpha = 0.86f),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, labelColor.copy(alpha = 0.8f)),
          ) {
            Text(
              text = nodeText,
              style = MaterialTheme.typography.labelSmall,
              color = labelColor,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
          }
        }
      }

      // Floating Enemy AI State + Level Tags
      uiState.enemies.forEach { enemy ->
        val enemyColor = when {
          enemy.isBoss -> Color(0xFFF97316)
          enemy.aiState == EnemyAiState.CHASE -> CrimsonAlert
          enemy.aiState == EnemyAiState.ALERT -> BioAmber
          else -> CrimsonAlert.copy(alpha = 0.92f)
        }
        Box(
          modifier = Modifier.offset {
            IntOffset(
              x = ((enemy.x * mapW).roundToInt() - 56).coerceIn(8, (mapW.toInt() - 140).coerceAtLeast(8)),
              y = ((enemy.y * mapH).roundToInt() - if (enemy.isBoss) 68 else 52).coerceIn(10, (mapH.toInt() - 32).coerceAtLeast(10)),
            )
          }
        ) {
          Surface(
            color = ObsidianBg.copy(alpha = 0.88f),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, enemyColor),
          ) {
            Text(
              text = "${if (enemy.isBoss) "BOSS " else ""}${enemy.name} Lv.${enemy.level} (${enemy.hp}/${enemy.maxHp})",
              style = MaterialTheme.typography.labelSmall,
              color = enemyColor,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
          }
        }
      }

      // Floating Player Tag
      Box(
        modifier = Modifier.offset {
          IntOffset(
            x = ((uiState.player.x * mapW).roundToInt() - 28).coerceIn(8, (mapW.toInt() - 72).coerceAtLeast(8)),
            y = ((uiState.player.y * mapH).roundToInt() - 62).coerceIn(10, (mapH.toInt() - 32).coerceAtLeast(10)),
          )
        }
      ) {
        Surface(color = BioCyan, shape = RoundedCornerShape(4.dp)) {
          Text(
            text = "YOU Lv.${uiState.player.level}",
            style = MaterialTheme.typography.labelSmall,
            color = ObsidianBg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
          )
        }
      }

      WeatherAtmosphereOverlay(weather = uiState.currentWeather)

      DirectionalHitMarkerOverlay(
        hits = uiState.worldDirectionalHits,
        flashAlpha = worldShake.flashAlpha,
        lowHpWarning = uiState.player.health <= uiState.player.maxHealth * 0.25f,
      )

      // Top-Left Stack: Equipped Weapon Pill + Live Base Sanctuary Wave HUD
      val turretLvHud = uiState.buildings.firstOrNull { it.id == "turrets" }?.level ?: 0
      Column(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Surface(
          color = ObsidianBg.copy(alpha = 0.82f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, SteelBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = BioAmber, modifier = Modifier.size(14.dp))
            Text(
              text = "${uiState.equippedWeapon?.name ?: "Blade"} • ${uiState.enemies.size} Hostiles • ${uiState.resourceNodes.size} Loot",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
        BaseWaveHud(
          baseWave = uiState.baseWave,
          turretLevel = turretLvHud,
          onTriggerWaveNow = onTriggerBaseWave,
        )
      }

      // Top-Right Weather Impact UI (tap to expand full combat/stamina/loot/audio modifiers)
      WeatherImpactHud(
        weather = uiState.currentWeather,
        timeOfDay = uiState.currentTimeOfDay,
        audioMuted = uiState.audioMuted,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
      )

      // LEFT THUMB OVERLAY: Virtual Analog Joystick + Quick Dash
      Row(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        VirtualJoystick(
          onVectorChange = onJoystickVector,
          size = 104.dp,
          accentColor = BioCyan,
          testTag = "world_left_joystick",
        )
        ThumbActionButton(
          label = "DASH",
          icon = Icons.Default.DirectionsRun,
          color = BioEmerald,
          onClick = onDashEvade,
          testTag = "world_dash_button",
          size = 52.dp,
        )
      }

      // BOTTOM-CENTER REAL-TIME THREAT METER HUD OVERLAY (Enemy Proximity + Acoustic Noise)
      ThreatMeterOverlayHud(
        difficulty = uiState.difficulty,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 8.dp),
      )

      // RIGHT THUMB OVERLAY: Scavenge/Fight + Radar Scan
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ThumbActionButton(
          label = "SCAN",
          icon = Icons.Default.Radar,
          color = BioCyan,
          onClick = onRefreshSector,
          testTag = "radar_sweep_button",
          size = 52.dp,
        )
        ThumbActionButton(
          label = "ACTION",
          icon = Icons.Default.SportsMma,
          color = BioAmber,
          onClick = onInteractNearest,
          testTag = "engage_nearest_button",
          size = 64.dp,
          filled = true,
        )
      }
    }
  }
}

private fun DrawScope.drawWorldEntitiesLayer(
  biome: BiomeType,
  timeOfDay: TimeOfDay,
  difficulty: DifficultyState,
  baseWave: BaseWaveState,
  turretLevel: Int,
  resourceNodes: List<WorldNode>,
  enemies: List<Enemy>,
  player: Player,
  weaponCategory: WeaponCategory,
  pulse: Float,
) {
  val w = size.width
  val h = size.height

  val horizonY = h * 0.42f
  val gridColor = when (biome) {
    BiomeType.SECTOR_9_LAB -> BioCyan.copy(alpha = 0.24f)
    BiomeType.FROSTPEAK_RIDGE -> Color(0xFF93C5FD).copy(alpha = 0.22f)
    BiomeType.ASHWOOD_FOREST -> BioEmerald.copy(alpha = 0.22f)
    BiomeType.DEAD_CITY -> BioAmber.copy(alpha = 0.20f)
  }
  for (r in 0..5) {
    val t = r / 5f
    val y = horizonY + (h - horizonY) * (t * t * 0.45f + t * 0.55f)
    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
  }
  for (c in 0..10) {
    val t = c / 10f
    drawLine(gridColor, Offset(w * (0.18f + t * 0.64f), horizonY), Offset(w * (-0.15f + t * 1.3f), h), strokeWidth = 1.2f)
  }

  // Sanctuary Base Zone Perimeter & Automated Turret Grid
  val bx = BaseZoneRules.BASE_CENTER_X * w
  val by = BaseZoneRules.BASE_CENTER_Y * h
  val basePxRadius = BaseZoneRules.BASE_RADIUS * size.minDimension * 0.95f
  val baseColor = when {
    baseWave.waveActive -> CrimsonAlert
    baseWave.inBaseArea -> BioEmerald
    else -> BioCyan
  }
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(baseColor.copy(alpha = 0.16f), Color.Transparent),
      center = Offset(bx, by),
      radius = basePxRadius,
    ),
    radius = basePxRadius,
    center = Offset(bx, by),
  )
  drawCircle(
    color = baseColor.copy(alpha = if (baseWave.waveActive) 0.72f else 0.48f),
    radius = basePxRadius * (if (baseWave.waveActive) (0.96f + 0.04f * pulse) else 1f),
    center = Offset(bx, by),
    style = Stroke(width = 2.6f),
  )
  // Bunker Command Core Pad at Base Center
  drawRoundRect(
    color = Color(0xFF0F172A).copy(alpha = 0.85f),
    topLeft = Offset(bx - 16f, by - 12f),
    size = Size(32f, 24f),
    cornerRadius = CornerRadius(4f, 4f),
  )
  drawRoundRect(
    color = baseColor,
    topLeft = Offset(bx - 16f, by - 12f),
    size = Size(32f, 24f),
    cornerRadius = CornerRadius(4f, 4f),
    style = Stroke(width = 1.8f),
  )
  if (turretLevel > 0) {
    val turretOffsets = listOf(-basePxRadius * 0.78f to 0f, basePxRadius * 0.78f to 0f)
    turretOffsets.forEach { (ox, oy) ->
      drawCircle(color = BioCyan, radius = 6f, center = Offset(bx + ox, by + oy))
      drawCircle(color = BioAmber, radius = 3f, center = Offset(bx + ox, by + oy))
    }
    if (baseWave.waveActive) {
      val nearestRaider = enemies.filter { it.isBaseRaider }.minByOrNull {
        kotlin.math.hypot((it.x - BaseZoneRules.BASE_CENTER_X).toDouble(), (it.y - BaseZoneRules.BASE_CENTER_Y).toDouble())
      }
      if (nearestRaider != null) {
        drawLine(
          color = BioCyan.copy(alpha = 0.75f),
          start = Offset(bx, by - 8f),
          end = Offset(nearestRaider.x * w, nearestRaider.y * h),
          strokeWidth = 2.5f,
        )
      }
    }
  }

  resourceNodes.forEach { node ->
    drawMapResourceNode(node.x * w, node.y * h, node.kind, node.rarity, pulse)
  }

  enemies.forEach { enemy ->
    drawMapEnemy(enemy, enemy.x * w, enemy.y * h, difficulty, pulse)
  }

  val px = player.x * w
  val py = player.y * h
  if (timeOfDay == TimeOfDay.MIDNIGHT || timeOfDay == TimeOfDay.DUSK || biome == BiomeType.SECTOR_9_LAB) {
    val coneLen = 150f
    val spread = 0.42f
    val conePath = Path().apply {
      moveTo(px, py)
      lineTo(px + cos(player.facingRad - spread) * coneLen, py + sin(player.facingRad - spread) * coneLen)
      lineTo(px + cos(player.facingRad + spread) * coneLen, py + sin(player.facingRad + spread) * coneLen)
      close()
    }
    drawPath(
      path = conePath,
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFDE68A).copy(alpha = 0.34f), Color.Transparent),
        center = Offset(px, py),
        radius = coneLen,
      ),
    )
  }

  drawMapPlayer(px, py, player, weaponCategory)
}

private fun DrawScope.drawMapPlayer(
  px: Float,
  py: Float,
  player: Player,
  weaponCategory: WeaponCategory,
) {
  drawOval(
    color = Color.Black.copy(alpha = 0.6f),
    topLeft = Offset(px - 22f, py + 12f),
    size = Size(44f, 14f),
  )
  drawCircle(
    color = BioCyan.copy(alpha = 0.30f),
    radius = 28f,
    center = Offset(px, py),
    style = Stroke(width = 2.5f),
  )
  drawLine(Color(0xFF334155), Offset(px - 6f, py + 4f), Offset(px - 8f, py + 18f), strokeWidth = 5f)
  drawLine(Color(0xFF334155), Offset(px + 6f, py + 4f), Offset(px + 8f, py + 18f), strokeWidth = 5f)
  drawRoundRect(
    color = Color(0xFF1E293B),
    topLeft = Offset(px - 12f, py - 12f),
    size = Size(24f, 20f),
    cornerRadius = CornerRadius(5f, 5f),
  )
  drawRoundRect(
    color = BioCyan,
    topLeft = Offset(px - 12f, py - 12f),
    size = Size(24f, 20f),
    cornerRadius = CornerRadius(5f, 5f),
    style = Stroke(width = 2f),
  )
  drawCircle(color = Color(0xFF0F172A), radius = 10f, center = Offset(px, py - 20f))
  drawCircle(color = BioCyan, radius = 10f, center = Offset(px, py - 20f), style = Stroke(width = 1.8f))
  drawRoundRect(
    color = BioCyan,
    topLeft = Offset(px - 6f, py - 23f),
    size = Size(12f, 5f),
    cornerRadius = CornerRadius(2f, 2f),
  )

  val dirX = cos(player.facingRad)
  val dirY = sin(player.facingRad)
  val handStart = Offset(px + 10f, py - 4f)
  when (weaponCategory) {
    WeaponCategory.SWORD -> drawLine(BioAmber, handStart, Offset(handStart.x + 18f, handStart.y - 18f), strokeWidth = 4f)
    WeaponCategory.BOW -> drawArc(
      color = BioAmber,
      startAngle = -75f,
      sweepAngle = 150f,
      useCenter = false,
      topLeft = Offset(px + 4f, py - 18f),
      size = Size(16f, 28f),
      style = Stroke(width = 3f),
    )
    WeaponCategory.FUTURISTIC -> drawLine(BioCyan, handStart, Offset(handStart.x + dirX * 22f, handStart.y + dirY * 22f), strokeWidth = 5f)
  }
}

private fun DrawScope.drawMapEnemy(
  enemy: Enemy,
  nx: Float,
  ny: Float,
  difficulty: DifficultyState,
  pulse: Float,
) {
  val scale = if (enemy.isBoss) 1.40f else 1.0f
  val mainColor = when {
    enemy.isBoss -> Color(0xFFF97316)
    enemy.aiState == EnemyAiState.CHASE -> CrimsonAlert
    enemy.aiState == EnemyAiState.ALERT -> BioAmber
    else -> Color(0xFFDC2626)
  }

  // Scaled Detection Aggro Ring visible around active monsters
  val detectionPx = (enemy.type.aggroRadius * difficulty.detectionRangeMultiplier * size.minDimension * 0.48f)
  drawCircle(
    color = mainColor.copy(alpha = if (enemy.aiState == EnemyAiState.CHASE) 0.28f else 0.11f),
    radius = detectionPx * (if (enemy.aiState == EnemyAiState.CHASE) pulse else 1f),
    center = Offset(nx, ny),
    style = Stroke(width = if (enemy.aiState == EnemyAiState.CHASE) 2.2f else 1.2f),
  )

  drawOval(
    color = Color.Black.copy(alpha = 0.6f),
    topLeft = Offset(nx - 22f * scale, ny + 12f * scale),
    size = Size(44f * scale, 14f * scale),
  )

  val leftHorn = Path().apply {
    moveTo(nx - 8f * scale, ny - 16f * scale)
    lineTo(nx - 18f * scale, ny - 30f * scale)
    lineTo(nx - 3f * scale, ny - 20f * scale)
    close()
  }
  val rightHorn = Path().apply {
    moveTo(nx + 8f * scale, ny - 16f * scale)
    lineTo(nx + 18f * scale, ny - 30f * scale)
    lineTo(nx + 3f * scale, ny - 20f * scale)
    close()
  }
  drawPath(leftHorn, mainColor)
  drawPath(rightHorn, mainColor)

  drawRoundRect(
    color = Color(0xFF2B090C),
    topLeft = Offset(nx - 14f * scale, ny - 14f * scale),
    size = Size(28f * scale, 24f * scale),
    cornerRadius = CornerRadius(8f, 8f),
  )
  drawRoundRect(
    color = mainColor,
    topLeft = Offset(nx - 14f * scale, ny - 14f * scale),
    size = Size(28f * scale, 24f * scale),
    cornerRadius = CornerRadius(8f, 8f),
    style = Stroke(width = 2.2f),
  )
  drawLine(mainColor, Offset(nx - 14f * scale, ny - 4f * scale), Offset(nx - 24f * scale, ny + 8f * scale), strokeWidth = 3.5f * scale)
  drawLine(mainColor, Offset(nx + 14f * scale, ny - 4f * scale), Offset(nx + 24f * scale, ny + 8f * scale), strokeWidth = 3.5f * scale)

  drawCircle(color = Color(0xFF1F0608), radius = 10f * scale, center = Offset(nx, ny - 16f * scale))
  drawCircle(color = Color(0xFFFACC15), radius = 2.8f * scale, center = Offset(nx - 4f * scale, ny - 17f * scale))
  drawCircle(color = Color(0xFFFACC15), radius = 2.8f * scale, center = Offset(nx + 4f * scale, ny - 17f * scale))

  // Overhead Enemy / Giant Boss Health Bar directly on the World Canvas
  val barW = if (enemy.isBoss) 72f else 48f
  val barH = if (enemy.isBoss) 7f else 5f
  val barX = nx - barW / 2f
  val barY = ny - (if (enemy.isBoss) 44f else 34f)
  val hpRatio = (enemy.hp.toFloat() / enemy.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
  drawRoundRect(
    color = Color(0xFF090D16).copy(alpha = 0.92f),
    topLeft = Offset(barX - 1f, barY - 1f),
    size = Size(barW + 2f, barH + 2f),
    cornerRadius = CornerRadius(3f, 3f),
  )
  drawRoundRect(
    color = if (enemy.isBoss) Color(0xFFF97316) else CrimsonAlert,
    topLeft = Offset(barX, barY),
    size = Size(barW * hpRatio, barH),
    cornerRadius = CornerRadius(2.5f, 2.5f),
  )
}

private fun DrawScope.drawMapResourceNode(
  nx: Float,
  ny: Float,
  kind: NodeKind,
  rarity: LootRarity,
  pulse: Float,
) {
  val color = when (kind) {
    NodeKind.FOOD -> BioAmber
    NodeKind.WATER -> BioCyan
    NodeKind.SCRAP -> Color(0xFFCBD5E1)
    NodeKind.TIMBER -> BioEmerald
    NodeKind.CRYSTAL -> Color(0xFF38BDF8)
    else -> BioAmber
  }
  val rarityColor = rarityAccentColor(rarity)
  if (rarity != LootRarity.COMMON) {
    drawCircle(
      color = rarityColor.copy(alpha = 0.28f),
      radius = 22f * pulse,
      center = Offset(nx, ny),
    )
    drawCircle(
      color = rarityColor.copy(alpha = 0.82f),
      radius = 18f,
      center = Offset(nx, ny),
      style = Stroke(width = 1.8f),
    )
  }
  drawCircle(color = color.copy(alpha = 0.22f), radius = 16f * pulse, center = Offset(nx, ny))
  if (kind == NodeKind.CRYSTAL) {
    val diamond = Path().apply {
      moveTo(nx, ny - 11f)
      lineTo(nx + 9f, ny)
      lineTo(nx, ny + 11f)
      lineTo(nx - 9f, ny)
      close()
    }
    drawPath(diamond, color)
  } else {
    drawRoundRect(
      color = color,
      topLeft = Offset(nx - 9f, ny - 9f),
      size = Size(18f, 18f),
      cornerRadius = CornerRadius(4f, 4f),
    )
    drawRoundRect(
      color = ObsidianBg,
      topLeft = Offset(nx - 4f, ny - 4f),
      size = Size(8f, 8f),
      cornerRadius = CornerRadius(2f, 2f),
    )
  }
}
