package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SquadMateEntity
import com.example.model.BiomeType
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

private data class CoopRaidSpec(
  val id: String,
  val title: String,
  val biome: BiomeType,
  val bossHp: Int,
  val rewardNote: String,
)

private val coopRaids = listOf(
  CoopRaidSpec("raid_metro", "Metro Overlord Siege", BiomeType.DEAD_CITY, 520, "+220 XP • +16 Crystals"),
  CoopRaidSpec("raid_ash", "Blighted Forest Swarm", BiomeType.ASHWOOD_FOREST, 680, "+220 XP • +16 Crystals"),
  CoopRaidSpec("raid_frost", "Frostpeak Titan Raid", BiomeType.FROSTPEAK_RIDGE, 840, "+220 XP • +16 Crystals"),
  CoopRaidSpec("raid_omega", "Sector-9 Omega Extinction", BiomeType.SECTOR_9_LAB, 1050, "+220 XP • +16 Crystals"),
)

@Composable
fun SquadScreen(
  uiState: GameUiState,
  onToggleMate: (SquadMateEntity) -> Unit,
  onRegenerateCode: () -> Unit,
  onLaunchRaid: (String, BiomeType, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        color = BunkerSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, BioCyan.copy(alpha = 0.6f)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Groups, contentDescription = null, tint = BioCyan, modifier = Modifier.size(22.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                text = "CO-OP STRIKE SQUAD",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            TacticalBadge(text = "PWR ${uiState.activeSquadPower}", color = BioAmber)
          }

          Surface(
            color = BunkerSurfaceElevated,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, SteelBorder),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text("SQUAD BEACON CODE", style = MaterialTheme.typography.labelSmall, color = BioCyan)
                Text(uiState.survivor.squadCode, style = MaterialTheme.typography.titleLarge, color = BioAmber)
              }
              IconButton(
                onClick = onRegenerateCode,
                modifier = Modifier.testTag("regenerate_squad_code"),
              ) {
                Icon(Icons.Default.Refresh, contentDescription = "Generate new squad code", tint = BioCyan)
              }
            }
          }

          Text(
            text = stringResource(R.string.local_coop_notice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      Text(
        text = "SELECT SQUADMATES (${uiState.squadMates.count { it.isSelected }} ACTIVE)",
        style = MaterialTheme.typography.labelLarge,
        color = BioAmber,
      )
    }

    items(uiState.squadMates, key = { it.id }) { mate ->
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggleMate(mate) }
          .testTag("squad_mate_${mate.id}"),
        color = BunkerSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (mate.isSelected) BioEmerald else SteelBorder),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (mate.isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = if (mate.isSelected) "Squadmate active" else "Squadmate benched",
            tint = if (mate.isSelected) BioEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(mate.callsign, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("${mate.role} • ${mate.weaponName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          TacticalBadge(text = "ATK ${mate.power}", color = BioCyan)
        }
      }
    }

    item {
      Text(
        text = "CO-OP BOSS RAIDS",
        style = MaterialTheme.typography.labelLarge,
        color = CrimsonAlert,
      )
    }

    items(coopRaids, key = { it.id }) { raid ->
      Surface(
        color = BunkerSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.65f)),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(raid.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("${raid.biome.displayName} • ${raid.bossHp} HP Boss", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(raid.rewardNote, style = MaterialTheme.typography.labelSmall, color = BioEmerald)
          }
          Spacer(Modifier.width(10.dp))
          Button(
            onClick = { onLaunchRaid(raid.title, raid.biome, raid.bossHp) },
            modifier = Modifier
              .height(48.dp)
              .testTag("launch_raid_${raid.id}"),
            colors = ButtonDefaults.buttonColors(
              containerColor = CrimsonAlert,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          ) {
            Text("DEPLOY", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
