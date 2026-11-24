package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.model.BiomeType
import com.example.model.CombatHitbox
import com.example.model.CombatMoveType
import com.example.model.CombatState
import com.example.model.WeaponCategory
import com.example.ui.components.DirectionalHitMarkerOverlay
import com.example.ui.components.ParallaxCityscapeWithFog
import com.example.ui.components.TacticalBadge
import com.example.ui.components.ThreatMeterHud
import com.example.ui.components.ThumbActionButton
import com.example.ui.components.VirtualJoystick
import com.example.ui.components.WeatherAtmosphereOverlay
import com.example.ui.components.WeatherImpactHud
import com.example.ui.components.rememberDamageShakeState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.BunkerSurface
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import com.example.viewmodel.GameUiState

@Composable
fun CombatOverlay(
  uiState: GameUiState,
  combat: CombatState,
  onJoystickVector: (Float, Float) -> Unit,
  onPrimaryStrike: () -> Unit,
  onSprintStrike: () -> Unit,
  onHeavyStrike: () -> Unit,
  onDodgeRoll: () -> Unit,
  onMedkit: () -> Unit,
  onSquadUltimate: () -> Unit,
  onToggleMute: () -> Unit,
  onExitCombat: (Boolean) -> Unit,
) {
  BackHandler {
    onExitCombat(combat.defeat)
  }

  val infinite = rememberInfiniteTransition(label = "combat_anim")
  val idleBob by infinite.animateFloat(
    initialValue = -6f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
    label = "idle_bob",
  )

  val strikeAnim = remember { Animatable(0f) }
  LaunchedEffect(combat.playerStrikeSeq) {
    if (combat.playerStrikeSeq > 0L) {
      strikeAnim.snapTo(1f)
      strikeAnim.animateTo(0f, animationSpec = tween(220))
    }
  }

  val sprintAnim = remember { Animatable(0f) }
  LaunchedEffect(combat.playerSprintSeq) {
    if (combat.playerSprintSeq > 0L) {
      sprintAnim.snapTo(1f)
      sprintAnim.animateTo(0f, animationSpec = tween(260))
    }
  }

  val overheadAnim = remember { Animatable(0f) }
  LaunchedEffect(combat.playerOverheadSeq) {
    if (combat.playerOverheadSeq > 0L) {
      overheadAnim.snapTo(1f)
      overheadAnim.animateTo(0f, animationSpec = tween(320))
    }
  }

  val enemyLungeAnim = remember { Animatable(0f) }
  LaunchedEffect(combat.enemyStrikeSeq) {
    if (combat.enemyStrikeSeq > 0L) {
      enemyLungeAnim.snapTo(1f)
      enemyLungeAnim.animateTo(0f, animationSpec = tween(240))
    }
  }

  val weapon = uiState.equippedWeapon
  val weaponCategory = WeaponCategory.fromName(weapon?.category ?: "SWORD")
  val activeEnemy = uiState.activeCombatEnemy

  val bgRes = when {
    combat.isBoss -> R.drawable.img_boss_titan
    combat.biome == BiomeType.SECTOR_9_LAB -> R.drawable.img_biome_lab
    combat.biome == BiomeType.ASHWOOD_FOREST || combat.biome == BiomeType.FROSTPEAK_RIDGE -> R.drawable.img_camp_base
    else -> R.drawable.img_hero_2045
  }

  val shakeState = rememberDamageShakeState(
    damageSeq = combat.playerDamageSeq,
    intensity = combat.lastHitIntensity,
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    // Camera-shaken arena stage (world backdrop + combatants + directional hit markers)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          translationX = shakeState.translationX
          translationY = shakeState.translationY
          rotationZ = shakeState.rotationDeg
        }
    ) {
      Image(
        painter = painterResource(id = bgRes),
        contentDescription = combat.enemyName,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = 1.14f
            scaleY = 1.14f
            translationX = (0.5f - combat.arenaPlayerX) * 58f
          }
          .alpha(if (combat.isBoss) 0.50f else 0.40f),
      )

      // Parallax scrolls smoothly with live player X position inside the combat arena!
      ParallaxCityscapeWithFog(
        biome = combat.biome,
        weather = uiState.currentWeather,
        timeOfDay = uiState.currentTimeOfDay,
        parallaxOffset = combat.arenaPlayerX + strikeAnim.value * 0.08f - enemyLungeAnim.value * 0.08f,
      )

      WeatherAtmosphereOverlay(weather = uiState.currentWeather)

      // Real-Time Moving Arena Stage Canvas (Player + Enemy + HP Bars + Danger Zones + Projectiles; tap stage to attack!)
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(combat.victory, combat.defeat) {
            detectTapGestures {
              if (!combat.victory && !combat.defeat) {
                onPrimaryStrike()
              }
            }
          }
      ) {
        drawRealTimeCombatArena(
          combat = combat,
          weaponCategory = weaponCategory,
          idleBob = idleBob,
          strikeProgress = strikeAnim.value,
          sprintProgress = sprintAnim.value,
          overheadProgress = overheadAnim.value,
          enemyLungeProgress = enemyLungeAnim.value,
        )
      }

      DirectionalHitMarkerOverlay(
        hits = combat.directionalHits,
        flashAlpha = shakeState.flashAlpha,
        lowHpWarning = uiState.player.health <= uiState.player.maxHealth * 0.25f,
      )
    }

    // TOP LANDSCAPE COMBAT BAR (Player Vitals + Stamina | Enemy Boss HP | Weather Impact)
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        // Left Top: Player HP, Stamina Bar, Counter Badge + Global Threat Meter
        Surface(
          modifier = Modifier.weight(1f),
          color = BunkerSurface.copy(alpha = 0.86f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, if (combat.counterReadyTicks > 0) BioAmber else BioCyan.copy(alpha = 0.6f)),
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${uiState.survivor.callsign} LV.${uiState.survivor.level}",
                style = MaterialTheme.typography.labelLarge,
                color = BioCyan,
              )
              if (combat.counterReadyTicks > 0) {
                val counterPct = 25 + (uiState.abilityBonuses.counterDamageBonus * 100f).toInt()
                TacticalBadge(text = "COUNTER +$counterPct%", color = BioAmber)
              }
              Text(
                text = "HP ${uiState.survivor.hp}/${uiState.survivor.maxHp} • ST ${uiState.survivor.stamina}%",
                style = MaterialTheme.typography.labelSmall,
                color = BioEmerald,
              )
            }
            LinearProgressIndicator(
              progress = { (uiState.survivor.hp.toFloat() / uiState.survivor.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = BioEmerald,
              trackColor = BunkerSurfaceElevated,
            )
            LinearProgressIndicator(
              progress = { (uiState.survivor.stamina / 100f).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .testTag("combat_stamina_bar"),
              color = BioCyan,
              trackColor = BunkerSurfaceElevated,
            )
            ThreatMeterHud(difficulty = uiState.difficulty)
          }
        }

      // Right Top: Enemy / Boss HP & Telegraph Warning + Audio/Retreat
      Surface(
        modifier = Modifier.weight(1.15f),
        color = BunkerSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (combat.dodgeWindowActive) CrimsonAlert else SteelBorder),
      ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TacticalBadge(
                text = if (combat.isBoss) "BOSS LV.${combat.enemyLevel}" else "LV.${combat.enemyLevel}",
                color = if (combat.isBoss) BioAmber else CrimsonAlert,
              )
              Text(
                text = combat.enemyName.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "COMBO x${combat.comboCount}",
                style = MaterialTheme.typography.labelSmall,
                color = BioAmber,
              )
              IconButton(onClick = onToggleMute, modifier = Modifier.size(30.dp)) {
                Icon(
                  imageVector = if (uiState.audioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = "Toggle AI Sound",
                  tint = if (uiState.audioMuted) CrimsonAlert else BioCyan,
                  modifier = Modifier.size(16.dp),
                )
              }
              IconButton(
                onClick = { onExitCombat(combat.defeat) },
                modifier = Modifier
                  .size(30.dp)
                  .testTag("combat_retreat_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                  contentDescription = "Retreat from combat",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }

          LinearProgressIndicator(
            progress = { (combat.enemyHp.toFloat() / combat.enemyMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = CrimsonAlert,
            trackColor = BunkerSurfaceElevated,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "HP ${combat.enemyHp}/${combat.enemyMaxHp} • ATK ${combat.enemyAttackPower}",
              style = MaterialTheme.typography.labelSmall,
              color = CrimsonAlert,
            )
            Text(
              text = if (combat.dodgeWindowActive) "INCOMING STRIKE — MOVE OR DODGE!" else "${activeEnemy?.aiState?.badge ?: "ACTIVE"}",
              style = MaterialTheme.typography.labelSmall,
              color = if (combat.dodgeWindowActive) CrimsonAlert else BioAmber,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
      }
      WeatherImpactHud(
        weather = uiState.currentWeather,
        timeOfDay = uiState.currentTimeOfDay,
        audioMuted = uiState.audioMuted,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )
    }

    // Floating Hit Numbers anchored over Player (enemy hits) or Enemy/Boss (player attacks)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val stageW = constraints.maxWidth.toFloat()
      val stageH = constraints.maxHeight.toFloat()
      combat.recentHits.forEachIndexed { idx, hit ->
        val hitColor = when {
          hit.isHeal -> BioEmerald
          hit.isEnemyHit -> CrimsonAlert
          hit.isCrit -> BioAmber
          else -> BioCyan
        }
        val anchorX = if (hit.isEnemyHit || hit.isHeal) combat.arenaPlayerX else combat.arenaEnemyX
        val anchorY = if (hit.isEnemyHit || hit.isHeal) combat.arenaPlayerY else combat.arenaEnemyY
        val px = (anchorX * stageW).roundToInt() - 46
        val py = ((anchorY - 0.16f) * stageH).roundToInt() - idx * 28
        Surface(
          color = ObsidianBg.copy(alpha = 0.78f),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, hitColor.copy(alpha = 0.65f)),
          modifier = Modifier.offset {
            IntOffset(
              x = px.coerceIn(16, (stageW.toInt() - 140).coerceAtLeast(16)),
              y = py.coerceIn(72, (stageH.toInt() - 72).coerceAtLeast(72)),
            )
          },
        ) {
          Text(
            text = hit.text,
            style = MaterialTheme.typography.titleMedium,
            color = hitColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
          )
        }
      }
    }

    // BOTTOM CONTROLS OR VICTORY/DEFEAT DIALOG
    if (combat.victory || combat.defeat) {
      Surface(
        color = BunkerSurface.copy(alpha = 0.96f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, if (combat.victory) BioEmerald else CrimsonAlert),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth(0.72f)
          .padding(bottom = 16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = if (combat.victory) "THREAT ELIMINATED!" else "SURVIVOR DOWN",
            style = MaterialTheme.typography.displaySmall,
            color = if (combat.victory) BioEmerald else CrimsonAlert,
          )
          Text(
            text = if (combat.victory) combat.rewardSummary else "Overwhelmed in ${combat.biome.displayName}. Emergency evac costs 20% Scrap Metal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Button(
            onClick = { onExitCombat(combat.defeat) },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("combat_continue_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (combat.victory) BioEmerald else BioAmber,
              contentColor = ObsidianBg,
            ),
          ) {
            Text(
              text = if (combat.victory) "CLAIM REWARDS & CONTINUE" else "EMERGENCY EVACUATE",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    } else {
      // Compact Combat Log pill at bottom center
      Surface(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 10.dp),
        color = ObsidianBg.copy(alpha = 0.78f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SteelBorder),
      ) {
        Text(
          text = combat.combatLog.lastOrNull() ?: "Use Left Joystick to Dodge, Right Buttons to Fight!",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
      }

      // LEFT THUMB ZONE: Virtual Joystick + Tactical Dodge Button
      val dodgeCost = uiState.effectiveMoveCost(CombatMoveType.DODGE)
      val sprintCost = uiState.effectiveMoveCost(CombatMoveType.SPRINT_STRIKE)
      val heavyCost = uiState.effectiveMoveCost(CombatMoveType.HEAVY_OVERHEAD)
      val hasDodgeStam = uiState.survivor.stamina >= dodgeCost
      val hasSprintStam = uiState.survivor.stamina >= sprintCost
      val hasHeavyStam = uiState.survivor.stamina >= heavyCost

      Row(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        VirtualJoystick(
          onVectorChange = onJoystickVector,
          size = 114.dp,
          accentColor = BioCyan,
          testTag = "combat_left_joystick",
        )
        ThumbActionButton(
          label = if (combat.dodgeWindowActive) "DODGE!" else "DODGE",
          icon = Icons.Default.Shield,
          color = when {
            !hasDodgeStam -> Color(0xFF64748B)
            combat.dodgeWindowActive -> CrimsonAlert
            else -> BioCyan
          },
          onClick = onDodgeRoll,
          testTag = "combat_dodge_button",
          size = 60.dp,
          filled = combat.dodgeWindowActive && hasDodgeStam,
          staminaCost = dodgeCost,
        )
      }

      // RIGHT THUMB ZONE: Tactical Special Moves (Sprint Attack, Heavy Overhead), Primary Strike, Heal, Squad Ult
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalAlignment = Alignment.End,
        ) {
          ThumbActionButton(
            label = if (combat.squadUltimateCharge >= 100) "ULT!" else "${combat.squadUltimateCharge}%",
            icon = Icons.Default.Bolt,
            color = Color(0xFFF97316),
            onClick = onSquadUltimate,
            testTag = "combat_ultimate_button",
            size = 50.dp,
            enabled = combat.squadUltimateCharge >= 100,
          )
          ThumbActionButton(
            label = "HEAL ${uiState.survivor.medkits}",
            icon = Icons.Default.LocalHospital,
            color = BioEmerald,
            onClick = onMedkit,
            testTag = "combat_medkit_button",
            size = 54.dp,
          )
        }

        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          ThumbActionButton(
            label = "SPRINT",
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            color = if (hasSprintStam) BioEmerald else Color(0xFF64748B),
            onClick = onSprintStrike,
            testTag = "combat_sprint_button",
            size = 60.dp,
            filled = hasSprintStam && combat.counterReadyTicks > 0,
            staminaCost = sprintCost,
          )
          ThumbActionButton(
            label = "OVERHEAD",
            icon = Icons.Default.Whatshot,
            color = if (hasHeavyStam) BioCyan else Color(0xFF64748B),
            onClick = onHeavyStrike,
            testTag = "combat_heavy_button",
            size = 60.dp,
            filled = hasHeavyStam,
            staminaCost = heavyCost,
          )
        }

        val strikeLabel = when (weaponCategory) {
          WeaponCategory.SWORD -> "SLASH"
          WeaponCategory.BOW -> "SHOOT"
          WeaponCategory.FUTURISTIC -> "FIRE"
        }
        ThumbActionButton(
          label = strikeLabel,
          icon = if (weaponCategory == WeaponCategory.FUTURISTIC) Icons.Default.FlashOn else Icons.Default.SportsMma,
          color = BioAmber,
          onClick = onPrimaryStrike,
          testTag = "combat_strike_button",
          size = 72.dp,
          filled = true,
          staminaCost = CombatMoveType.PRIMARY.staminaCost,
        )
      }
    }
  }
}

private fun DrawScope.drawRealTimeCombatArena(
  combat: CombatState,
  weaponCategory: WeaponCategory,
  idleBob: Float,
  strikeProgress: Float,
  sprintProgress: Float,
  overheadProgress: Float,
  enemyLungeProgress: Float,
) {
  val w = size.width
  val h = size.height

  // Arena Floor Grid
  val floorY = h * 0.58f
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.72f)),
      startY = floorY - 30f,
      endY = h,
    )
  )
  drawLine(BioCyan.copy(alpha = 0.25f), Offset(0f, floorY), Offset(w, floorY), strokeWidth = 2f)

  // 1. Telegraphed Red Ground Danger Zones (Player can physically move out with Left Thumb!)
  combat.dangerZones.forEach { zone ->
    val zx = zone.x * w
    val zy = zone.y * h
    val r = zone.radius * size.minDimension
    drawCircle(
      color = CrimsonAlert.copy(alpha = 0.16f + zone.progress * 0.22f),
      radius = r,
      center = Offset(zx, zy),
    )
    drawCircle(
      color = CrimsonAlert.copy(alpha = 0.45f),
      radius = r * zone.progress.coerceIn(0.1f, 1f),
      center = Offset(zx, zy),
    )
    drawCircle(
      color = CrimsonAlert,
      radius = r,
      center = Offset(zx, zy),
      style = Stroke(width = 2.5f),
    )
  }

  // 2. Live Player Survivor at arenaPlayerX, arenaPlayerY (with Sprint Dash Trails & Overhead Leap)
  val leapLift = sin(overheadProgress * Math.PI).toFloat() * 34f
  val px = combat.arenaPlayerX * w + strikeProgress * 28f
  val py = combat.arenaPlayerY * h + idleBob * 0.35f - leapLift
  val playerTint = if (combat.playerHurtFlashTicks > 0) CrimsonAlert else BioCyan

  if (sprintProgress > 0.03f) {
    val sx = combat.sprintTrailFromX * w
    val sy = combat.sprintTrailFromY * h
    for (step in 1..3) {
      val t = step / 4f
      val gx = sx + (px - sx) * t
      val gy = sy + (py - sy) * t
      drawCircle(
        color = BioEmerald.copy(alpha = 0.28f * sprintProgress * t),
        radius = 24f,
        center = Offset(gx, gy),
      )
    }
    drawLine(
      brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, BioEmerald.copy(alpha = 0.85f * sprintProgress)),
        start = Offset(sx, sy),
        end = Offset(px, py),
      ),
      start = Offset(sx, sy),
      end = Offset(px, py),
      strokeWidth = 10f * sprintProgress,
      cap = StrokeCap.Round,
    )
  }

  drawOval(
    color = Color.Black.copy(alpha = 0.65f),
    topLeft = Offset(px - 30f, py + 34f + leapLift),
    size = Size(60f, 16f),
  )

  if (combat.counterReadyTicks > 0) {
    drawCircle(
      color = BioAmber.copy(alpha = 0.72f),
      radius = 48f,
      center = Offset(px, py),
      style = Stroke(width = 2.5f),
    )
  }

  if (weaponCategory == WeaponCategory.SWORD && !combat.victory && !combat.defeat) {
    val inTouchRange = CombatHitbox.doesPlayerMeleeTouchTarget(
      playerX = combat.arenaPlayerX,
      playerY = combat.arenaPlayerY,
      enemyX = combat.arenaEnemyX,
      enemyY = combat.arenaEnemyY,
      isBoss = combat.isBoss,
      moveType = CombatMoveType.PRIMARY,
    )
    val reachPx = CombatHitbox.playerMeleeTouchRadius(CombatMoveType.PRIMARY, combat.isBoss) * size.minDimension * 0.65f
    drawCircle(
      color = if (inTouchRange) BioEmerald.copy(alpha = 0.42f) else SteelBorder.copy(alpha = 0.30f),
      radius = reachPx,
      center = Offset(px, py),
      style = Stroke(width = if (inTouchRange) 2.2f else 1.4f),
    )
  }

  if (combat.shieldTicks > 0) {
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color.Transparent, BioEmerald.copy(alpha = 0.38f)),
        center = Offset(px, py),
        radius = 58f,
      ),
      center = Offset(px, py),
      radius = 58f,
    )
    drawCircle(color = BioEmerald, radius = 58f, center = Offset(px, py), style = Stroke(width = 3f))
  }

  drawLine(Color(0xFF334155), Offset(px - 10f, py + 12f), Offset(px - 14f, py + 38f), strokeWidth = 9f)
  drawLine(Color(0xFF334155), Offset(px + 10f, py + 12f), Offset(px + 14f, py + 38f), strokeWidth = 9f)
  drawRoundRect(
    color = Color(0xFF1E293B),
    topLeft = Offset(px - 20f, py - 20f),
    size = Size(40f, 36f),
    cornerRadius = CornerRadius(8f, 8f),
  )
  drawRoundRect(
    color = playerTint,
    topLeft = Offset(px - 20f, py - 20f),
    size = Size(40f, 36f),
    cornerRadius = CornerRadius(8f, 8f),
    style = Stroke(width = 2.5f),
  )
  drawCircle(color = Color(0xFF0F172A), radius = 16f, center = Offset(px, py - 34f))
  drawCircle(color = playerTint, radius = 16f, center = Offset(px, py - 34f), style = Stroke(width = 2f))
  drawRoundRect(
    color = playerTint,
    topLeft = Offset(px - 3f, py - 38f),
    size = Size(14f, 6f),
    cornerRadius = CornerRadius(2f, 2f),
  )

  when (weaponCategory) {
    WeaponCategory.SWORD -> drawLine(BioAmber, Offset(px + 15f, py - 6f), Offset(px + 54f, py - 38f), strokeWidth = 6f)
    WeaponCategory.BOW -> drawArc(
      color = BioAmber,
      startAngle = -70f,
      sweepAngle = 140f,
      useCenter = false,
      topLeft = Offset(px + 10f, py - 32f),
      size = Size(24f, 48f),
      style = Stroke(width = 4.5f),
    )
    WeaponCategory.FUTURISTIC -> drawRoundRect(
      color = BioCyan,
      topLeft = Offset(px + 10f, py - 12f),
      size = Size(44f, 10f),
      cornerRadius = CornerRadius(3f, 3f),
    )
  }

  // 3. Active Moving Enemy or Giant Boss at arenaEnemyX, arenaEnemyY
  if (!combat.victory) {
    val scale = if (combat.isBoss) 1.32f else 0.96f
    val ex = combat.arenaEnemyX * w - enemyLungeProgress * 32f
    val ey = combat.arenaEnemyY * h + idleBob * 0.7f
    val bodyColor = when {
      combat.enemyHitFlashTicks > 0 -> Color.White
      combat.isEnraged -> CrimsonAlert
      combat.isBoss -> Color(0xFFF97316)
      else -> CrimsonAlert
    }

    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(bodyColor.copy(alpha = if (combat.dodgeWindowActive) 0.42f else 0.22f), Color.Transparent),
        center = Offset(ex, ey),
        radius = 96f * scale,
      ),
      center = Offset(ex, ey),
      radius = 96f * scale,
    )

    drawOval(
      color = Color.Black.copy(alpha = 0.65f),
      topLeft = Offset(ex - 40f * scale, ey + 48f * scale),
      size = Size(80f * scale, 18f * scale),
    )

    val hornL = Path().apply {
      moveTo(ex - 14f * scale, ey - 36f * scale)
      lineTo(ex - 36f * scale, ey - 66f * scale)
      lineTo(ex - 5f * scale, ey - 42f * scale)
      close()
    }
    val hornR = Path().apply {
      moveTo(ex + 14f * scale, ey - 36f * scale)
      lineTo(ex + 36f * scale, ey - 66f * scale)
      lineTo(ex + 5f * scale, ey - 42f * scale)
      close()
    }
    drawPath(hornL, bodyColor)
    drawPath(hornR, bodyColor)

    drawRoundRect(
      color = Color(0xFF24080B),
      topLeft = Offset(ex - 30f * scale, ey - 22f * scale),
      size = Size(60f * scale, 56f * scale),
      cornerRadius = CornerRadius(14f, 14f),
    )
    drawRoundRect(
      color = bodyColor,
      topLeft = Offset(ex - 30f * scale, ey - 22f * scale),
      size = Size(60f * scale, 56f * scale),
      cornerRadius = CornerRadius(14f, 14f),
      style = Stroke(width = 2.8f),
    )
    for (r in 0..2) {
      val ry = ey - 8f * scale + r * 12f * scale
      drawLine(bodyColor, Offset(ex - 15f * scale, ry), Offset(ex + 15f * scale, ry), strokeWidth = 3f)
    }

    drawLine(bodyColor, Offset(ex - 30f * scale, ey - 10f * scale), Offset(ex - 52f * scale, ey + 20f * scale), strokeWidth = 8f * scale)
    drawLine(bodyColor, Offset(ex + 30f * scale, ey - 10f * scale), Offset(ex + 52f * scale, ey + 20f * scale), strokeWidth = 8f * scale)

    drawCircle(color = Color(0xFF1A0507), radius = 20f * scale, center = Offset(ex, ey - 36f * scale))
    drawCircle(color = bodyColor, radius = 20f * scale, center = Offset(ex, ey - 36f * scale), style = Stroke(width = 2f))
    drawCircle(color = Color(0xFFFACC15), radius = 5f * scale, center = Offset(ex - 8f * scale, ey - 38f * scale))
    drawCircle(color = Color(0xFFFACC15), radius = 5f * scale, center = Offset(ex + 8f * scale, ey - 38f * scale))

    // Floating HP Bar directly over the moving Enemy / Boss sprite
    val hpBarW = if (combat.isBoss) 118f else 84f
    val hpBarH = if (combat.isBoss) 10f else 8f
    val hpBarX = ex - hpBarW / 2f
    val hpBarY = ey - 78f * scale
    val enemyHpRatio = (combat.enemyHp.toFloat() / combat.enemyMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    drawRoundRect(
      color = ObsidianBg.copy(alpha = 0.92f),
      topLeft = Offset(hpBarX - 2f, hpBarY - 2f),
      size = Size(hpBarW + 4f, hpBarH + 4f),
      cornerRadius = CornerRadius(4f, 4f),
    )
    drawRoundRect(
      color = if (combat.isBoss) Color(0xFFF97316) else CrimsonAlert,
      topLeft = Offset(hpBarX, hpBarY),
      size = Size(hpBarW * enemyHpRatio, hpBarH),
      cornerRadius = CornerRadius(3f, 3f),
    )

    // Radial impact sparks when enemy takes a hit
    if (combat.enemyHitFlashTicks > 0) {
      val sparkColor = if (weaponCategory == WeaponCategory.FUTURISTIC) BioCyan else BioAmber
      for (sIdx in 0 until 8) {
        val ang = (sIdx * 0.785f) + 0.25f
        val rInner = 24f * scale
        val rOuter = 68f * scale
        drawLine(
          color = sparkColor,
          start = Offset(ex + cos(ang) * rInner, ey + sin(ang) * rInner),
          end = Offset(ex + cos(ang) * rOuter, ey + sin(ang) * rOuter),
          strokeWidth = 3.5f,
        )
      }
    }

    // Sword arc VFX on strike
    if (strikeProgress > 0.05f && weaponCategory == WeaponCategory.SWORD) {
      drawArc(
        color = BioAmber.copy(alpha = strikeProgress),
        startAngle = -60f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(px - 20f, py - 55f),
        size = Size(130f, 110f),
        style = Stroke(width = 10f * strikeProgress),
      )
    }

    // Heavy Overhead Strike Shockwave Crescent + Ground Slam Ring
    if (overheadProgress > 0.03f) {
      val shockRadius = 42f + (1f - overheadProgress) * 95f
      drawCircle(
        color = BioAmber.copy(alpha = 0.65f * overheadProgress),
        radius = shockRadius,
        center = Offset(ex, ey),
        style = Stroke(width = 8f * overheadProgress),
      )
      drawArc(
        color = BioCyan.copy(alpha = overheadProgress),
        startAngle = -110f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(px - 10f, py - 92f),
        size = Size(165f, 155f),
        style = Stroke(width = 14f * overheadProgress, cap = StrokeCap.Round),
      )
    }
  }

  // 4. Flying Real-Time Projectiles (Player Arrows/Plasma + Enemy Bio-Orbs)
  combat.projectiles.forEach { proj ->
    val cx = proj.x * w
    val cy = proj.y * h
    if (proj.fromPlayer) {
      val color = if (proj.category == WeaponCategory.FUTURISTIC) BioCyan else BioAmber
      drawCircle(color = color.copy(alpha = 0.35f), radius = if (proj.isHeavy) 16f else 11f, center = Offset(cx, cy))
      drawCircle(color = color, radius = if (proj.isHeavy) 8f else 5.5f, center = Offset(cx, cy))
    } else {
      drawCircle(color = CrimsonAlert.copy(alpha = 0.38f), radius = 15f, center = Offset(cx, cy))
      drawCircle(color = Color(0xFFFACC15), radius = 6.5f, center = Offset(cx, cy))
      drawCircle(color = CrimsonAlert, radius = 10f, center = Offset(cx, cy), style = Stroke(width = 2.5f))
    }
  }
}
