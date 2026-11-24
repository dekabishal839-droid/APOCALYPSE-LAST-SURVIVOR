package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.MissionEntity
import com.example.model.BiomeType
import com.example.model.WeatherType
import com.example.ui.components.TacticalBadge
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
fun StoryScreen(
  uiState: GameUiState,
  onSelectMission: (MissionEntity) -> Unit,
  onCloseBriefing: () -> Unit,
  onDeployMission: (MissionEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val missionsByStory = remember(uiState.missions) {
    uiState.missions.groupBy { it.storyNumber }
  }
  val activeMission = remember(uiState.missions) {
    uiState.missions.firstOrNull { it.unlocked && !it.completed } ?: uiState.missions.firstOrNull()
  }
  val activeStory = activeMission?.storyNumber ?: 1
  val activeBandStart = ((((activeMission?.chapterInStory ?: 1) - 1) / 10) * 10) + 1
  var selectedStory by remember(activeStory) { mutableIntStateOf(activeStory) }
  var chapterBandStart by remember(activeBandStart) { mutableIntStateOf(activeBandStart) } // 1, 11, 21, 31, 41

  val totalCompleted = uiState.missions.count { it.completed }
  val totalMissions = uiState.missions.size.coerceAtLeast(1000)

  val selectedStoryMissions = remember(missionsByStory, selectedStory) {
    missionsByStory[selectedStory].orEmpty()
  }
  val storyTitle = selectedStoryMissions.firstOrNull()?.storyTitle ?: "Story $selectedStory"
  val storyBiome = BiomeType.fromId(selectedStoryMissions.firstOrNull()?.biomeId ?: "DEAD_CITY")
  val filteredChapters = remember(selectedStoryMissions, chapterBandStart) {
    selectedStoryMissions.filter { it.chapterInStory in chapterBandStart..(chapterBandStart + 9) }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Compact Landscape Campaign Banner
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(86.dp)
        .clip(MaterialTheme.shapes.medium),
    ) {
      Image(
        painter = painterResource(
          id = if (storyBiome == BiomeType.SECTOR_9_LAB) R.drawable.img_biome_lab else R.drawable.img_hero_2045
        ),
        contentDescription = "2045 Campaign",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              listOf(ObsidianBg.copy(alpha = 0.92f), ObsidianBg.copy(alpha = 0.55f))
            )
          )
      )
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TacticalBadge(text = "20 STORIES • 1,000 CHAPTERS", color = BioAmber)
            TacticalBadge(text = "BOSS EVERY 5 CH", color = CrimsonAlert)
          }
          Text(
            text = "STORY $selectedStory: ${storyTitle.uppercase()}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
          )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(190.dp)) {
          Text(
            text = "$totalCompleted / $totalMissions COMPLETED",
            style = MaterialTheme.typography.labelLarge,
            color = BioEmerald,
          )
          Spacer(Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { totalCompleted / totalMissions.toFloat() },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = BioEmerald,
            trackColor = BunkerSurfaceElevated,
          )
        }
      }
    }

    // Story 1..20 Selector Strip
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      for (storyNum in 1..20) {
        val storyList = missionsByStory[storyNum].orEmpty()
        val done = storyList.count { it.completed }
        val isUnlocked = storyList.any { it.unlocked }
        val selected = selectedStory == storyNum
        Surface(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable {
              selectedStory = storyNum
              chapterBandStart = 1
            }
            .testTag("story_arc_$storyNum"),
          color = when {
            selected -> BioCyan.copy(alpha = 0.22f)
            isUnlocked -> BunkerSurface
            else -> BunkerSurface.copy(alpha = 0.55f)
          },
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(
            1.dp,
            when {
              selected -> BioCyan
              isUnlocked -> BioAmber.copy(alpha = 0.55f)
              else -> SteelBorder
            },
          ),
        ) {
          Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(
              text = "Story $storyNum",
              style = MaterialTheme.typography.labelLarge,
              color = if (selected) BioCyan else if (isUnlocked) BioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = if (isUnlocked) "$done/50 Ch" else "Locked",
              style = MaterialTheme.typography.labelSmall,
              color = if (done == 50) BioEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Chapter Band Filter Pills (1-10, 11-20, 21-30, 31-40, 41-50)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf(1, 11, 21, 31, 41).forEach { start ->
        val end = start + 9
        val selected = chapterBandStart == start
        Surface(
          modifier = Modifier
            .clickable { chapterBandStart = start }
            .testTag("chapter_band_$start"),
          color = if (selected) BioAmber.copy(alpha = 0.2f) else BunkerSurfaceElevated,
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, if (selected) BioAmber else SteelBorder),
        ) {
          Text(
            text = "Ch. $start–$end",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) BioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }
      Text(
        text = "Biome: ${storyBiome.displayName} • Strict Linear Unlock",
        style = MaterialTheme.typography.labelSmall,
        color = BioCyan,
      )
    }

    // 50-Chapter Mission List (showing selected 10-chapter band cleanly)
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(filteredChapters, key = { it.number }) { mission ->
        val borderColor = when {
          mission.completed -> BioEmerald.copy(alpha = 0.7f)
          mission.unlocked && mission.isBoss -> CrimsonAlert
          mission.unlocked -> BioCyan.copy(alpha = 0.75f)
          else -> SteelBorder
        }
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectMission(mission) }
            .testTag("mission_card_${mission.number}"),
          color = if (mission.isBoss) Color(0xFF1F1013) else BunkerSurface,
          shape = MaterialTheme.shapes.medium,
          border = BorderStroke(1.dp, borderColor),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TacticalBadge(
                  text = "S${mission.storyNumber} • CH.${mission.chapterInStory}",
                  color = if (mission.isBoss) CrimsonAlert else BioAmber,
                )
                if (mission.isBoss) {
                  TacticalBadge(text = "BOSS ENTRY", color = CrimsonAlert)
                }
                TacticalBadge(text = "ENEMY LV.${mission.enemyLevel}", color = BioCyan)
                Text(
                  text = WeatherType.fromIndex(mission.weatherIndex).displayName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = mission.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "${mission.objective}  •  +${mission.rewardXp} XP  +${mission.rewardCrystals} Crystals",
                style = MaterialTheme.typography.bodySmall,
                color = BioEmerald,
              )
            }
            Spacer(Modifier.width(10.dp))
            when {
              mission.completed -> Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Mission completed",
                tint = BioEmerald,
                modifier = Modifier.size(28.dp),
              )
              mission.unlocked -> Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Deploy mission",
                tint = BioAmber,
                modifier = Modifier.size(30.dp),
              )
              else -> Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Mission locked",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
              )
            }
          }
        }
      }
    }
  }

  val briefingMission = uiState.selectedMissionForBriefing
  if (briefingMission != null) {
    MissionBriefingDialog(
      mission = briefingMission,
      playerLevel = uiState.survivor.level,
      onDismiss = onCloseBriefing,
      onDeploy = { onDeployMission(briefingMission) },
    )
  }
}

@Composable
private fun MissionBriefingDialog(
  mission: MissionEntity,
  playerLevel: Int,
  onDismiss: () -> Unit,
  onDeploy: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      color = BunkerSurface,
      shape = MaterialTheme.shapes.large,
      border = BorderStroke(1.5.dp, if (mission.isBoss) CrimsonAlert else BioAmber),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TacticalBadge(text = "STORY ${mission.storyNumber} • CH ${mission.chapterInStory}/50", color = BioAmber)
          TacticalBadge(
            text = BiomeType.fromId(mission.biomeId).displayName,
            color = BioCyan,
          )
        }
        Text(
          text = mission.title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = mission.briefing,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
          color = BunkerSurfaceElevated,
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, SteelBorder),
        ) {
          val scaledLv = maxOf(mission.enemyLevel, playerLevel)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              text = "Target: ${mission.enemyName} (Scaled Lv.$scaledLv • ${mission.enemyHp} Base HP)",
              style = MaterialTheme.typography.labelLarge,
              color = CrimsonAlert,
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("briefing_cancel_button"),
          ) {
            Text("CANCEL")
          }
          Button(
            onClick = onDeploy,
            modifier = Modifier
              .weight(1.4f)
              .height(48.dp)
              .testTag("briefing_deploy_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = BioAmber,
              contentColor = ObsidianBg,
            ),
          ) {
            Text("DEPLOY NOW", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
