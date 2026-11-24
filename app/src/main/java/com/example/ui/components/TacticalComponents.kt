package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SurvivorEntity
import com.example.model.BaseWaveState
import com.example.model.DifficultyState
import com.example.model.DirectionalHitIndicator
import com.example.model.ThreatTier
import com.example.model.TimeOfDay
import com.example.model.WeatherType
import kotlin.math.cos
import kotlin.math.sin
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.BunkerSurface
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import java.util.Locale

@Composable
fun ThreatMeterHud(
  difficulty: DifficultyState,
  modifier: Modifier = Modifier,
) {
  val tierColor = when (difficulty.threatTier) {
    ThreatTier.LOW -> BioEmerald
    ThreatTier.ELEVATED -> BioCyan
    ThreatTier.HIGH -> BioAmber
    ThreatTier.EXTREME, ThreatTier.NIGHTMARE -> CrimsonAlert
  }
  Surface(
    modifier = modifier.testTag("threat_meter_hud"),
    color = BunkerSurfaceElevated,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.7f)),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(Icons.Default.Warning, contentDescription = "Global Threat", tint = tierColor, modifier = Modifier.size(13.dp))
        Text(
          text = "THREAT ${difficulty.threatTier.label} (${String.format(Locale.US, "%.2fx", difficulty.overallThreatMultiplier)})",
          style = MaterialTheme.typography.labelSmall,
          color = tierColor,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
        Text(
          text = "SPD +${difficulty.speedBonusPct}% • DMG +${difficulty.damageBonusPct}% • RNG +${difficulty.rangeBonusPct}%",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
      Spacer(Modifier.height(3.dp))
      LinearProgressIndicator(
        progress = { difficulty.threatProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = tierColor,
        trackColor = BunkerSurface,
      )
    }
  }
}

@Composable
fun ThreatMeterOverlayHud(
  difficulty: DifficultyState,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val tierColor = when (difficulty.threatTier) {
    ThreatTier.LOW -> BioEmerald
    ThreatTier.ELEVATED -> BioCyan
    ThreatTier.HIGH -> BioAmber
    ThreatTier.EXTREME, ThreatTier.NIGHTMARE -> CrimsonAlert
  }
  val proxColor = when {
    difficulty.proximityPercent >= 70 -> CrimsonAlert
    difficulty.proximityPercent >= 35 -> BioAmber
    else -> BioEmerald
  }
  val noiseColor = when {
    difficulty.noiseLevel >= 70 -> CrimsonAlert
    difficulty.noiseLevel >= 40 -> BioAmber
    else -> BioCyan
  }

  Surface(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clickable { expanded = !expanded }
      .testTag("threat_meter_overlay_hud"),
    color = ObsidianBg.copy(alpha = 0.88f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.2.dp, tierColor.copy(alpha = 0.82f)),
  ) {
    Column(
      modifier = Modifier
        .width(248.dp)
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Threat Meter",
            tint = tierColor,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = "THREAT ${difficulty.threatTier.label} (${String.format(Locale.US, "%.2fx", difficulty.overallThreatMultiplier)})",
            style = MaterialTheme.typography.labelSmall,
            color = tierColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
        Text(
          text = if (difficulty.nearbyHostileCount > 0) "${difficulty.nearbyHostileCount} NEAR" else "CLEAR",
          style = MaterialTheme.typography.labelSmall,
          color = if (difficulty.nearbyHostileCount > 0) CrimsonAlert else BioEmerald,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }

      LinearProgressIndicator(
        progress = { difficulty.threatProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = tierColor,
        trackColor = BunkerSurfaceElevated,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Real-time Enemy Proximity gauge
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Radar,
            contentDescription = "Enemy Proximity",
            tint = proxColor,
            modifier = Modifier.size(12.dp),
          )
          Text(
            text = "PROX ${difficulty.nearestEnemyDistanceMeters}m",
            style = MaterialTheme.typography.labelSmall,
            color = proxColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.testTag("threat_proximity_label"),
          )
        }

        // Real-time Player Acoustic Noise gauge
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Noise Level",
            tint = noiseColor,
            modifier = Modifier.size(12.dp),
          )
          Text(
            text = "NOISE ${difficulty.noiseLevel}dB",
            style = MaterialTheme.typography.labelSmall,
            color = noiseColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.testTag("threat_noise_label"),
          )
        }
      }

      if (expanded) {
        Text(
          text = "VITALS +${difficulty.vitalsPenaltyPercent}% • TIME +${difficulty.timeBonusPercent}% • SPD +${difficulty.speedBonusPct}% • DMG +${difficulty.damageBonusPct}% • RNG +${difficulty.rangeBonusPct}%",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          modifier = Modifier.testTag("threat_overlay_details"),
        )
      }
    }
  }
}

@Composable
fun SurvivalVitalsHud(
  survivor: SurvivorEntity,
  onEatFood: () -> Unit,
  onDrinkWater: () -> Unit,
  onUseMedkit: () -> Unit,
  modifier: Modifier = Modifier,
  difficulty: DifficultyState? = null,
  audioMuted: Boolean = false,
  onToggleMute: (() -> Unit)? = null,
  usedInventorySlots: Int = 0,
  totalInventorySlots: Int = 20,
  onOpenInventory: (() -> Unit)? = null,
  onOpenCrafting: (() -> Unit)? = null,
  availableAbilityPoints: Int = 0,
  onOpenAbilityTree: (() -> Unit)? = null,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = BunkerSurface,
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(1.dp, SteelBorder),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TacticalBadge(
            text = "LV.${survivor.level}",
            color = BioAmber,
          )
          Text(
            text = survivor.callsign,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (difficulty != null) {
            val mins = difficulty.elapsedSeconds / 60
            val secs = difficulty.elapsedSeconds % 60
            Text(
              text = String.format(Locale.US, "%02d:%02d", mins, secs),
              style = MaterialTheme.typography.labelSmall,
              color = BioCyan,
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ResourcePill(Icons.Default.Forest, survivor.wood.toString(), BioEmerald, "Wood")
          ResourcePill(Icons.Default.Hardware, survivor.metal.toString(), MaterialTheme.colorScheme.onSurfaceVariant, "Metal")
          ResourcePill(Icons.Default.Diamond, survivor.crystals.toString(), BioCyan, "Bio-Crystals")
          if (onToggleMute != null) {
            IconButton(
              onClick = onToggleMute,
              modifier = Modifier
                .size(34.dp)
                .testTag("toggle_audio_button"),
            ) {
              Icon(
                imageVector = if (audioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (audioMuted) "Unmute AI Audio" else "Mute AI Audio",
                tint = if (audioMuted) CrimsonAlert else BioCyan,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      if (difficulty != null) {
        Spacer(Modifier.height(6.dp))
        ThreatMeterHud(difficulty = difficulty, modifier = Modifier.fillMaxWidth())
      }

      Spacer(Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        VitalBar(
          label = "HP ${survivor.hp}/${survivor.maxHp}",
          progress = (survivor.hp.toFloat() / survivor.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f),
          color = CrimsonAlert,
          icon = Icons.Default.Favorite,
          modifier = Modifier.weight(1f),
        )
        VitalBar(
          label = "STM ${survivor.stamina}%",
          progress = survivor.stamina / 100f,
          color = BioEmerald,
          icon = Icons.Default.Bolt,
          modifier = Modifier.weight(1f),
        )
        VitalBar(
          label = "FOOD ${survivor.food}%",
          progress = survivor.food / 100f,
          color = BioAmber,
          icon = Icons.Default.Restaurant,
          modifier = Modifier.weight(1f),
        )
        VitalBar(
          label = "THIRST ${survivor.water}%",
          progress = survivor.water / 100f,
          color = BioCyan,
          icon = Icons.Default.WaterDrop,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        QuickActionChip(
          icon = Icons.Default.Restaurant,
          label = "Eat (+25)",
          tint = BioAmber,
          onClick = onEatFood,
          testTag = "hud_eat_button",
          modifier = Modifier.weight(1f),
        )
        QuickActionChip(
          icon = Icons.Default.WaterDrop,
          label = "Drink (+28)",
          tint = BioCyan,
          onClick = onDrinkWater,
          testTag = "hud_drink_button",
          modifier = Modifier.weight(1f),
        )
        QuickActionChip(
          icon = Icons.Default.LocalHospital,
          label = "Medkit (${survivor.medkits})",
          tint = BioEmerald,
          onClick = onUseMedkit,
          testTag = "hud_medkit_button",
          modifier = Modifier.weight(1f),
        )
        if (onOpenInventory != null) {
          QuickActionChip(
            icon = Icons.Default.Backpack,
            label = "Bag $usedInventorySlots/$totalInventorySlots",
            tint = Color(0xFFA855F7),
            onClick = onOpenInventory,
            testTag = "hud_inventory_button",
            modifier = Modifier.weight(1f),
          )
        }
        if (onOpenCrafting != null) {
          QuickActionChip(
            icon = Icons.Default.Build,
            label = "Craft",
            tint = BioAmber,
            onClick = onOpenCrafting,
            testTag = "hud_craft_button",
            modifier = Modifier.weight(1f),
          )
        }
        if (onOpenAbilityTree != null) {
          QuickActionChip(
            icon = Icons.Default.AccountTree,
            label = "Skills ${availableAbilityPoints}AP",
            tint = if (availableAbilityPoints > 0) BioEmerald else BioAmber,
            onClick = onOpenAbilityTree,
            testTag = "hud_skills_button",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun VitalBar(
  label: String,
  progress: Float,
  color: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = color,
        modifier = Modifier.size(12.dp),
      )
      Spacer(Modifier.width(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
    }
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp)),
      color = color,
      trackColor = BunkerSurfaceElevated,
    )
  }
}

@Composable
private fun QuickActionChip(
  icon: ImageVector,
  label: String,
  tint: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .height(40.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(BunkerSurfaceElevated)
      .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(15.dp),
    )
    Spacer(Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
    )
  }
}

@Composable
fun ResourcePill(
  icon: ImageVector,
  value: String,
  tint: Color,
  description: String,
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(BunkerSurfaceElevated)
      .padding(horizontal = 7.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = description,
      tint = tint,
      modifier = Modifier.size(13.dp),
    )
    Spacer(Modifier.width(4.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
fun TacticalBadge(
  text: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(color.copy(alpha = 0.16f))
      .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      color = color,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
fun ResourceCostRow(
  woodCost: Int,
  metalCost: Int,
  crystalCost: Int,
  survivor: SurvivorEntity,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
    CostChip(Icons.Default.Forest, woodCost, survivor.wood >= woodCost, "Wood cost $woodCost")
    CostChip(Icons.Default.Hardware, metalCost, survivor.metal >= metalCost, "Metal cost $metalCost")
    if (crystalCost > 0) {
      CostChip(Icons.Default.Diamond, crystalCost, survivor.crystals >= crystalCost, "Crystal cost $crystalCost")
    }
  }
}

@Composable
private fun CostChip(
  icon: ImageVector,
  cost: Int,
  affordable: Boolean,
  desc: String,
) {
  val tint = if (affordable) BioEmerald else CrimsonAlert
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(13.dp))
    Spacer(Modifier.width(3.dp))
    Text(
      text = "$cost",
      style = MaterialTheme.typography.labelLarge,
      color = tint,
    )
  }
}

@Composable
fun WeatherAtmosphereOverlay(
  weather: WeatherType,
  modifier: Modifier = Modifier,
) {
  if (weather == WeatherType.CLEAR) return
  val infinite = rememberInfiniteTransition(label = "weather_anim")
  val phase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = if (weather == WeatherType.BIO_STORM) 850 else 1300, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "weather_phase",
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    when (weather) {
      WeatherType.RAIN -> {
        val dropCount = 46
        for (i in 0 until dropCount) {
          val seedX = ((i * 73) % 100) / 100f
          val seedY = ((i * 37) % 100) / 100f
          val x = seedX * w
          val y = ((seedY + phase) % 1f) * h
          drawLine(
            color = BioCyan.copy(alpha = 0.35f),
            start = Offset(x, y),
            end = Offset(x - 10f, y + 26f),
            strokeWidth = 1.6f,
          )
        }
      }
      WeatherType.BIO_STORM -> {
        val dropCount = 64
        for (i in 0 until dropCount) {
          val seedX = ((i * 59) % 100) / 100f
          val seedY = ((i * 41) % 100) / 100f
          val x = ((seedX - phase * 0.25f + 1f) % 1f) * w
          val y = ((seedY + phase * 1.4f) % 1f) * h
          drawLine(
            color = BioAmber.copy(alpha = 0.42f),
            start = Offset(x, y),
            end = Offset(x - 18f, y + 32f),
            strokeWidth = 2f,
          )
        }
        // Periodic lightning flash
        if (phase in 0.82f..0.88f) {
          drawRect(color = Color.White.copy(alpha = 0.16f))
        }
      }
      WeatherType.ASH_FOG -> {
        val moteCount = 34
        for (i in 0 until moteCount) {
          val seedX = ((i * 67) % 100) / 100f
          val seedY = ((i * 29) % 100) / 100f
          val x = ((seedX + phase * 0.35f) % 1f) * w
          val y = ((seedY + phase * 0.15f) % 1f) * h
          drawCircle(
            color = Color(0xFFCBD5E1).copy(alpha = 0.26f),
            radius = if (i % 3 == 0) 4.5f else 2.5f,
            center = Offset(x, y),
          )
        }
        drawRect(color = Color(0xFF475569).copy(alpha = 0.16f))
      }
      WeatherType.CLEAR -> Unit
    }
  }
}

data class DamageShakeState(
  val translationX: Float = 0f,
  val translationY: Float = 0f,
  val rotationDeg: Float = 0f,
  val flashAlpha: Float = 0f,
)

@Composable
fun rememberDamageShakeState(
  damageSeq: Long,
  intensity: Float = 1f,
): DamageShakeState {
  val shakeAnim = remember { Animatable(0f) }
  val haptic = LocalHapticFeedback.current
  LaunchedEffect(damageSeq) {
    if (damageSeq > 0L) {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      shakeAnim.snapTo(1f)
      shakeAnim.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
      )
    }
  }
  val t = shakeAnim.value
  if (t <= 0.001f) return DamageShakeState()
  val amp = 18f * intensity.coerceIn(0.45f, 1.6f)
  return DamageShakeState(
    translationX = sin(t * 29f) * amp * t,
    translationY = cos(t * 37f) * (amp * 0.72f) * t,
    rotationDeg = sin(t * 23f) * 1.15f * intensity * t,
    flashAlpha = (t * 0.42f * intensity).coerceIn(0f, 0.52f),
  )
}

@Composable
fun DirectionalHitMarkerOverlay(
  hits: List<DirectionalHitIndicator>,
  flashAlpha: Float,
  lowHpWarning: Boolean = false,
  modifier: Modifier = Modifier,
) {
  if (hits.isEmpty() && flashAlpha <= 0.01f && !lowHpWarning) return
  Canvas(
    modifier = modifier
      .fillMaxSize()
      .then(if (hits.isNotEmpty()) Modifier.testTag("directional_hit_overlay") else Modifier),
  ) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    // Peripheral red hit flash / low-HP danger vignette
    val edgeAlpha = (flashAlpha + if (lowHpWarning) 0.14f else 0f).coerceIn(0f, 0.55f)
    if (edgeAlpha > 0.01f) {
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(Color.Transparent, Color.Transparent, CrimsonAlert.copy(alpha = edgeAlpha)),
          center = Offset(cx, cy),
          radius = size.maxDimension * 0.62f,
        )
      )
    }

    // Active directional hit chevrons & radial threat arcs
    hits.forEach { hit ->
      val life = (hit.ttlTicks / 14f).coerceIn(0.08f, 1f)
      val angle = hit.angleRad
      val angleDeg = Math.toDegrees(angle.toDouble()).toFloat()
      val ringRx = w * 0.28f
      val ringRy = h * 0.34f

      // Soft crimson perimeter splash oriented toward incoming attack source
      val splashCenter = Offset(
        x = (cx + cos(angle) * w * 0.46f).coerceIn(0f, w),
        y = (cy + sin(angle) * h * 0.46f).coerceIn(0f, h),
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(CrimsonAlert.copy(alpha = 0.45f * life), Color.Transparent),
          center = splashCenter,
          radius = size.minDimension * 0.36f,
        ),
        radius = size.minDimension * 0.36f,
        center = splashCenter,
      )

      // Tactical arc around screen center pointing toward attacker
      val arcTopLeft = Offset(cx - ringRx, cy - ringRy)
      val arcSize = Size(ringRx * 2f, ringRy * 2f)
      drawArc(
        color = CrimsonAlert.copy(alpha = 0.36f * life),
        startAngle = angleDeg - 26f,
        sweepAngle = 52f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = 16f * hit.intensity.coerceIn(0.7f, 1.4f), cap = StrokeCap.Round),
      )
      drawArc(
        color = Color(0xFFFF6B6B).copy(alpha = 0.92f * life),
        startAngle = angleDeg - 16f,
        sweepAngle = 32f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = 5.5f, cap = StrokeCap.Round),
      )

      // Sharp directional chevron arrow pointing directly at hit origin
      val tipDist = 18f
      val wingDist = 14f
      val ux = cos(angle)
      val uy = sin(angle)
      val px = -uy
      val py = ux
      val anchorX = cx + ux * (ringRx + 6f)
      val anchorY = cy + uy * (ringRy + 6f)
      val chevron = Path().apply {
        moveTo(anchorX + ux * tipDist, anchorY + uy * tipDist)
        lineTo(anchorX - ux * 6f + px * wingDist, anchorY - uy * 6f + py * wingDist)
        lineTo(anchorX + ux * 3f, anchorY + uy * 3f)
        lineTo(anchorX - ux * 6f - px * wingDist, anchorY - uy * 6f - py * wingDist)
        close()
      }
      drawPath(chevron, color = Color(0xFFFFE4E6).copy(alpha = 0.95f * life))
      drawPath(chevron, color = CrimsonAlert.copy(alpha = life), style = Stroke(width = 2.5f))
    }
  }
}

@Composable
fun WeatherImpactHud(
  weather: WeatherType,
  timeOfDay: TimeOfDay,
  audioMuted: Boolean = false,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val (weatherColor, weatherIcon) = when (weather) {
    WeatherType.CLEAR -> BioEmerald to if (timeOfDay == TimeOfDay.MIDNIGHT || timeOfDay == TimeOfDay.DUSK) Icons.Default.NightsStay else Icons.Default.WbSunny
    WeatherType.RAIN -> BioCyan to Icons.Default.WaterDrop
    WeatherType.BIO_STORM -> BioAmber to Icons.Default.Thunderstorm
    WeatherType.ASH_FOG -> Color(0xFF94A3B8) to Icons.Default.CloudQueue
  }

  Surface(
    modifier = modifier
      .clickable { expanded = !expanded }
      .testTag("weather_impact_hud"),
    color = BunkerSurface.copy(alpha = 0.88f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, weatherColor.copy(alpha = 0.72f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = weatherIcon,
          contentDescription = weather.displayName,
          tint = weatherColor,
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = "${weather.displayName.uppercase()} • ${timeOfDay.displayName.uppercase()}",
          style = MaterialTheme.typography.labelSmall,
          color = weatherColor,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
        Text(
          text = "| ${weather.combatImpactLabel}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
        )
        Text(
          text = if (audioMuted) "• AUDIO OFF" else "• ${weather.ambientAudioLabel}",
          style = MaterialTheme.typography.labelSmall,
          color = if (audioMuted) CrimsonAlert else weatherColor.copy(alpha = 0.9f),
          maxLines = 1,
          modifier = Modifier.testTag("weather_ambient_audio_label"),
        )
      }
      if (expanded) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          WeatherEffectChip("STAMINA", weather.survivalImpactLabel, BioEmerald)
          WeatherEffectChip("LOOT", weather.lootImpactLabel, BioCyan)
          WeatherEffectChip("AMBIENT", if (audioMuted) "Muted" else weather.ambientAudioLabel, weatherColor)
          if (timeOfDay == TimeOfDay.MIDNIGHT || timeOfDay == TimeOfDay.DUSK) {
            WeatherEffectChip("OPTICS", "Night Beam ON", BioAmber)
          }
        }
      }
    }
  }
}

@Composable
private fun WeatherEffectChip(
  tag: String,
  value: String,
  color: Color,
) {
  Surface(
    color = color.copy(alpha = 0.14f),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
  ) {
    Text(
      text = "$tag: $value",
      style = MaterialTheme.typography.labelSmall,
      color = color,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
  }
}

@Composable
fun BaseWaveHud(
  baseWave: BaseWaveState,
  turretLevel: Int,
  onTriggerWaveNow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accentColor = when {
    baseWave.waveActive && baseWave.isBossWave -> Color(0xFFF97316)
    baseWave.waveActive -> CrimsonAlert
    baseWave.inBaseArea -> BioEmerald
    else -> SteelBorder
  }
  val integrityRatio = (baseWave.baseIntegrity.toFloat() / baseWave.maxBaseIntegrity.coerceAtLeast(1)).coerceIn(0f, 1f)

  Surface(
    modifier = modifier.testTag("base_wave_hud"),
    color = ObsidianBg.copy(alpha = 0.88f),
    shape = RoundedCornerShape(9.dp),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.82f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "Base Sanctuary Defense",
          tint = accentColor,
          modifier = Modifier.size(14.dp),
        )
        val statusText = when {
          baseWave.waveActive && baseWave.isBossWave ->
            "SIEGE WAVE ${baseWave.waveNumber} (BOSS) • ${baseWave.remainingEnemiesInWave} LEFT"
          baseWave.waveActive ->
            "BASE WAVE ${baseWave.waveNumber} • ${baseWave.remainingEnemiesInWave} RAIDERS"
          baseWave.inBaseArea ->
            "IN BASE • WAVE ${baseWave.waveNumber} IN ${baseWave.countdownSeconds}s"
          else ->
            "BASE SAFE ZONE (${baseWave.wavesSurvived} WAVES)"
        }
        Text(
          text = statusText,
          style = MaterialTheme.typography.labelSmall,
          color = accentColor,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
        if (!baseWave.waveActive) {
          Surface(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onTriggerWaveNow)
              .testTag("trigger_base_wave_button"),
            color = BioAmber.copy(alpha = 0.22f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, BioAmber),
          ) {
            Text(
              text = "START",
              style = MaterialTheme.typography.labelSmall,
              color = BioAmber,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        LinearProgressIndicator(
          progress = { integrityRatio },
          modifier = Modifier
            .width(120.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (integrityRatio < 0.35f) CrimsonAlert else BioEmerald,
          trackColor = BunkerSurfaceElevated,
        )
        Text(
          text = "BASE ${baseWave.baseIntegrity}/${baseWave.maxBaseIntegrity}" +
            if (turretLevel > 0) " • TURRET Lv.$turretLevel" else " • NO TURRET",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
    }
  }
}
