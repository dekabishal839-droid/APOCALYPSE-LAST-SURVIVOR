package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BunkerSurfaceElevated
import com.example.ui.theme.ObsidianBg
import kotlin.math.hypot

/**
 * Left-thumb virtual analog joystick for two-handed landscape movement and combat dodging.
 */
@Composable
fun VirtualJoystick(
  onVectorChange: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 112.dp,
  accentColor: Color = BioCyan,
  testTag: String = "left_virtual_joystick",
) {
  var thumbOffset by remember { mutableStateOf(Offset.Zero) }
  var isDragging by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .testTag(testTag)
      .pointerInput(Unit) {
        val maxRadius = (this.size.width / 2f) * 0.72f
        detectDragGestures(
          onDragStart = { startOffset ->
            isDragging = true
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val delta = startOffset - center
            val dist = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
            val clamped = if (dist > maxRadius && dist > 0f) delta * (maxRadius / dist) else delta
            thumbOffset = clamped
            onVectorChange((clamped.x / maxRadius).coerceIn(-1f, 1f), (clamped.y / maxRadius).coerceIn(-1f, 1f))
          },
          onDrag = { change, dragAmount ->
            change.consume()
            val next = thumbOffset + dragAmount
            val dist = hypot(next.x.toDouble(), next.y.toDouble()).toFloat()
            val clamped = if (dist > maxRadius && dist > 0f) next * (maxRadius / dist) else next
            thumbOffset = clamped
            onVectorChange((clamped.x / maxRadius).coerceIn(-1f, 1f), (clamped.y / maxRadius).coerceIn(-1f, 1f))
          },
          onDragEnd = {
            isDragging = false
            thumbOffset = Offset.Zero
            onVectorChange(0f, 0f)
          },
          onDragCancel = {
            isDragging = false
            thumbOffset = Offset.Zero
            onVectorChange(0f, 0f)
          },
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val outerR = this.size.minDimension / 2f - 4f

      // Translucent base pad
      drawCircle(
        color = ObsidianBg.copy(alpha = 0.68f),
        radius = outerR,
        center = center,
      )
      drawCircle(
        color = accentColor.copy(alpha = if (isDragging) 0.85f else 0.42f),
        radius = outerR,
        center = center,
        style = Stroke(width = if (isDragging) 3f else 2f),
      )

      // Crosshair tick lines
      drawLine(accentColor.copy(alpha = 0.25f), Offset(center.x - outerR * 0.5f, center.y), Offset(center.x + outerR * 0.5f, center.y), strokeWidth = 1.5f)
      drawLine(accentColor.copy(alpha = 0.25f), Offset(center.x, center.y - outerR * 0.5f), Offset(center.x, center.y + outerR * 0.5f), strokeWidth = 1.5f)

      // Movable thumb stick puck
      val puckCenter = center + thumbOffset
      drawCircle(
        color = accentColor.copy(alpha = if (isDragging) 0.42f else 0.24f),
        radius = outerR * 0.40f,
        center = puckCenter,
      )
      drawCircle(
        color = accentColor,
        radius = outerR * 0.36f,
        center = puckCenter,
        style = Stroke(width = 2.5f),
      )
    }
  }
}

/**
 * Right-thumb ergonomic circular arcade action button (minimum 48dp touch target).
 */
@Composable
fun ThumbActionButton(
  label: String,
  icon: ImageVector,
  color: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  filled: Boolean = false,
  enabled: Boolean = true,
  staminaCost: Int? = null,
) {
  val bgColor = when {
    !enabled -> BunkerSurfaceElevated.copy(alpha = 0.55f)
    filled -> color
    else -> ObsidianBg.copy(alpha = 0.84f)
  }
  val contentColor = when {
    !enabled -> Color(0xFF64748B)
    filled -> ObsidianBg
    else -> color
  }

  Surface(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .size(size)
      .clip(CircleShape)
      .border(1.5.dp, if (enabled) color else Color(0xFF334155), CircleShape)
      .background(bgColor)
      .clickable(enabled = enabled, onClick = onClick)
      .testTag(testTag),
    color = bgColor,
    shape = CircleShape,
    border = BorderStroke(1.5.dp, if (enabled) color else Color(0xFF334155)),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(if (size >= 62.dp) 21.dp else 16.dp),
      )
      Spacer(Modifier.height(1.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = if (staminaCost != null) 9.sp else 10.sp,
          lineHeight = 10.sp,
        ),
        color = contentColor,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
      if (staminaCost != null) {
        Text(
          text = "${staminaCost}ST",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 8.sp,
            lineHeight = 9.sp,
          ),
          color = contentColor.copy(alpha = 0.88f),
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }
  }
}
