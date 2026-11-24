package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AftermathColorScheme = darkColorScheme(
  primary = BioAmber,
  onPrimary = ObsidianBg,
  primaryContainer = BioAmberDark,
  onPrimaryContainer = BioAmberLight,
  secondary = BioCyan,
  onSecondary = ObsidianBg,
  secondaryContainer = BioCyanDark,
  onSecondaryContainer = BioCyanLight,
  tertiary = BioEmerald,
  onTertiary = ObsidianBg,
  tertiaryContainer = BioEmeraldDark,
  onTertiaryContainer = AshWhite,
  error = CrimsonAlert,
  onError = AshWhite,
  errorContainer = CrimsonAlertDark,
  onErrorContainer = AshWhite,
  background = ObsidianBg,
  onBackground = AshWhite,
  surface = BunkerSurface,
  onSurface = AshWhite,
  surfaceVariant = BunkerSurfaceElevated,
  onSurfaceVariant = AshMuted,
  surfaceContainerLowest = ObsidianBg,
  surfaceContainerLow = BunkerSurface,
  surfaceContainer = BunkerSurfaceElevated,
  surfaceContainerHigh = BunkerSurfaceBright,
  outline = SteelBorder,
  outlineVariant = SteelBorder,
)

private val AftermathShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun Aftermath2045Theme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = AftermathColorScheme,
    typography = AftermathTypography,
    shapes = AftermathShapes,
    content = content,
  )
}
