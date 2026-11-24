package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AbilityBonusSummary
import com.example.model.AbilityBranch
import com.example.model.AbilityNode
import com.example.model.AbilityTreeCatalog
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.BunkerSurface
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import kotlin.math.roundToInt

@Composable
fun AbilityTreeDialog(
  unlockedIds: Set<String>,
  availablePoints: Int,
  totalPoints: Int,
  bonuses: AbilityBonusSummary,
  onUnlockNode: (String) -> Unit,
  onResetTree: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

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
      border = BorderStroke(1.5.dp, BioAmber.copy(alpha = 0.78f)),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Header Row: Title, Available AP, Reset Button, Close Button
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
            Icon(
              imageVector = Icons.Default.AccountTree,
              contentDescription = "Player Ability Tree",
              tint = BioAmber,
              modifier = Modifier.size(22.dp),
            )
            Text(
              text = "PLAYER ABILITY TREE",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
            TacticalBadge(
              text = "$availablePoints AP FREE ($totalPoints TOTAL)",
              color = if (availablePoints > 0) BioEmerald else BioCyan,
              modifier = Modifier.testTag("ability_points_badge"),
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onResetTree,
              enabled = unlockedIds.isNotEmpty(),
              modifier = Modifier
                .height(48.dp)
                .testTag("reset_ability_tree_button"),
              border = BorderStroke(1.dp, if (unlockedIds.isNotEmpty()) BioAmber else SteelBorder),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = BioAmber)
              Spacer(Modifier.width(5.dp))
              Text("RESET AP", style = MaterialTheme.typography.labelLarge, color = BioAmber)
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(48.dp)
                .testTag("close_ability_tree_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Ability Tree",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        // Active Synergy Strip
        Surface(
          color = ObsidianBg.copy(alpha = 0.72f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, SteelBorder),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "ACTIVE BUFFS:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold,
            )
            AbilityBuffPill("ATK", "+${((bonuses.damageMultiplier - 1f) * 100f).roundToInt()}%", BioAmber)
            AbilityBuffPill("CRIT", "+${bonuses.critBonusPct}%", BioAmber)
            AbilityBuffPill("ST DISC", "-${(bonuses.specialStaminaDiscount * 100f).roundToInt()}%", BioCyan)
            AbilityBuffPill("ARMOR", "+${(bonuses.damageMitigationBonus * 100f).roundToInt()}%", BioEmerald)
            AbilityBuffPill("TURRET", "+${((bonuses.turretDamageMultiplier - 1f) * 100f).roundToInt()}%", BioEmerald)
            AbilityBuffPill("MAX HP", "+${bonuses.maxHpBonus}", BioEmerald)
          }
        }

        // 3 Branches Side-by-Side (Vanguard | Phantom | Bio-Guard)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          AbilityBranch.entries.forEach { branch ->
            val (branchColor, branchIcon) = when (branch) {
              AbilityBranch.VANGUARD -> BioAmber to Icons.Default.SportsMma
              AbilityBranch.PHANTOM -> BioCyan to Icons.Default.Bolt
              AbilityBranch.BIO_GUARD -> BioEmerald to Icons.Default.Shield
            }
            val branchNodes = AbilityTreeCatalog.nodes
              .filter { it.branch == branch }
              .sortedBy { it.tier }

            Surface(
              modifier = Modifier.weight(1f),
              color = BunkerSurfaceElevated,
              shape = MaterialTheme.shapes.medium,
              border = BorderStroke(1.dp, branchColor.copy(alpha = 0.55f)),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(branchIcon, contentDescription = branch.title, tint = branchColor, modifier = Modifier.size(16.dp))
                  Text(
                    text = branch.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = branchColor,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Text(
                  text = branch.subtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))

                branchNodes.forEachIndexed { idx, node ->
                  val isUnlocked = node.id in unlockedIds
                  val prereqMet = node.prereqId == null || node.prereqId in unlockedIds
                  val canUnlock = AbilityTreeCatalog.canUnlock(node, unlockedIds, availablePoints)

                  if (idx > 0) {
                    val connectorActive = isUnlocked || prereqMet
                    Canvas(
                      modifier = Modifier
                        .width(12.dp)
                        .height(18.dp),
                    ) {
                      drawLine(
                        color = if (isUnlocked) branchColor else if (connectorActive) branchColor.copy(alpha = 0.5f) else SteelBorder,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = if (isUnlocked) 5f else 3f,
                        pathEffect = if (isUnlocked) null else PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                      )
                    }
                  }

                  AbilityNodeCard(
                    node = node,
                    branchColor = branchColor,
                    isUnlocked = isUnlocked,
                    prereqMet = prereqMet,
                    canUnlock = canUnlock,
                    onUnlock = { onUnlockNode(node.id) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AbilityNodeCard(
  node: AbilityNode,
  branchColor: Color,
  isUnlocked: Boolean,
  prereqMet: Boolean,
  canUnlock: Boolean,
  onUnlock: () -> Unit,
) {
  val borderColor = when {
    isUnlocked -> branchColor
    canUnlock -> branchColor.copy(alpha = 0.78f)
    else -> SteelBorder
  }
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .then(if (canUnlock) Modifier.clickable(onClick = onUnlock) else Modifier),
    color = if (isUnlocked) branchColor.copy(alpha = 0.14f) else ObsidianBg.copy(alpha = 0.72f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(if (isUnlocked) 1.5.dp else 1.dp, borderColor),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "T${node.tier} • ${node.name}",
          style = MaterialTheme.typography.titleSmall,
          color = if (isUnlocked || prereqMet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
          contentDescription = if (isUnlocked) "Unlocked" else "Locked",
          tint = if (isUnlocked) branchColor else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp),
        )
      }

      Text(
        text = node.effectLabel,
        style = MaterialTheme.typography.labelLarge,
        color = branchColor,
        fontWeight = FontWeight.Bold,
      )

      Text(
        text = node.description,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(Modifier.height(2.dp))

      when {
        isUnlocked -> {
          Surface(
            color = branchColor.copy(alpha = 0.22f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "ACTIVE",
                style = MaterialTheme.typography.labelLarge,
                color = branchColor,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
        else -> {
          Button(
            onClick = onUnlock,
            enabled = canUnlock,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("unlock_ability_${node.id}"),
            colors = ButtonDefaults.buttonColors(
              containerColor = branchColor,
              contentColor = ObsidianBg,
              disabledContainerColor = BunkerSurface,
              disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
          ) {
            Text(
              text = when {
                !prereqMet -> "REQUIRES T${node.tier - 1}"
                canUnlock -> "UNLOCK (${node.pointCost} AP)"
                else -> "NEED ${node.pointCost} AP"
              },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AbilityBuffPill(
  tag: String,
  value: String,
  color: Color,
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(color.copy(alpha = 0.14f))
      .padding(horizontal = 7.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(tag, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
  }
}
