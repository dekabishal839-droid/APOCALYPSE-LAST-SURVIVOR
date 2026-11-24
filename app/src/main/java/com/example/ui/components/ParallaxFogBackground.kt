package com.example.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.BiomeType
import com.example.model.TimeOfDay
import com.example.model.WeatherType
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ObsidianBg
import kotlin.math.sin

private const val FOG_AGSL_SHADER = """
uniform float2 uResolution;
uniform float uTime;
uniform float uParallax;
uniform float uDensity;
uniform float4 uFogColor;

float hash(float2 p) {
  return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453123);
}

float noise(float2 p) {
  float2 i = floor(p);
  float2 f = fract(p);
  float2 u = f * f * (3.0 - 2.0 * f);
  return mix(
    mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
    mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
    u.y
  );
}

float fbm(float2 p) {
  float value = 0.0;
  float amp = 0.5;
  for (int i = 0; i < 4; i++) {
    value += amp * noise(p);
    p *= 2.04;
    amp *= 0.5;
  }
  return value;
}

half4 main(float2 fragCoord) {
  float2 uv = fragCoord / max(uResolution, float2(1.0));
  float2 flow1 = float2(uv.x * 3.0 + uTime * 0.12 + uParallax * 0.42, uv.y * 2.2 - uTime * 0.025);
  float2 flow2 = float2(uv.x * 5.8 - uTime * 0.08 + uParallax * 0.85, uv.y * 4.0 + uTime * 0.035);
  float n = fbm(flow1) * 0.65 + fbm(flow2) * 0.35;

  float heightMask = smoothstep(0.06, 0.54, uv.y) * (1.0 - smoothstep(0.86, 1.0, uv.y) * 0.22);
  float alpha = clamp((n - 0.26) * 1.42 * uDensity * heightMask, 0.0, 0.68);
  return half4(uFogColor.rgb * alpha, alpha);
}
"""

@Composable
fun ParallaxCityscapeWithFog(
  biome: BiomeType,
  weather: WeatherType,
  timeOfDay: TimeOfDay,
  parallaxOffset: Float,
  modifier: Modifier = Modifier,
) {
  val infinite = rememberInfiniteTransition(label = "parallax_fog_anim")
  val timePhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 100f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 60_000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "shader_time",
  )

  val fogColor = remember(biome, weather, timeOfDay) {
    when {
      biome == BiomeType.SECTOR_9_LAB -> Color(0xFF38BDF8)
      weather == WeatherType.BIO_STORM -> Color(0xFFF59E0B)
      weather == WeatherType.ASH_FOG -> Color(0xFFCBD5E1)
      timeOfDay == TimeOfDay.DUSK -> Color(0xFFFB923C)
      timeOfDay == TimeOfDay.DAWN -> Color(0xFFA78BFA)
      timeOfDay == TimeOfDay.MIDNIGHT -> Color(0xFF38BDF8)
      else -> Color(0xFF94A3B8)
    }
  }

  val fogDensity = remember(weather, timeOfDay) {
    val weatherBoost = when (weather) {
      WeatherType.ASH_FOG -> 1.12f
      WeatherType.BIO_STORM -> 0.88f
      WeatherType.RAIN -> 0.74f
      WeatherType.CLEAR -> 0.42f
    }
    val timeBoost = if (timeOfDay == TimeOfDay.MIDNIGHT || timeOfDay == TimeOfDay.DUSK) 1.08f else 0.90f
    weatherBoost * timeBoost
  }

  Box(modifier = modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRealisticPostApocalypticEnvironment(
        biome = biome,
        timeOfDay = timeOfDay,
        weather = weather,
        parallax = parallaxOffset,
        timePhase = timePhase,
      )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      AgslFogOverlay(
        timeSeconds = timePhase,
        parallax = parallaxOffset,
        density = fogDensity,
        fogColor = fogColor,
      )
    } else {
      FallbackFogOverlay(
        timeSeconds = timePhase,
        parallax = parallaxOffset,
        density = fogDensity,
        fogColor = fogColor,
      )
    }
  }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslFogOverlay(
  timeSeconds: Float,
  parallax: Float,
  density: Float,
  fogColor: Color,
) {
  val runtimeShader = remember { RuntimeShader(FOG_AGSL_SHADER) }
  Canvas(modifier = Modifier.fillMaxSize()) {
    runtimeShader.setFloatUniform("uResolution", size.width, size.height)
    runtimeShader.setFloatUniform("uTime", timeSeconds)
    runtimeShader.setFloatUniform("uParallax", parallax)
    runtimeShader.setFloatUniform("uDensity", density)
    runtimeShader.setFloatUniform("uFogColor", fogColor.red, fogColor.green, fogColor.blue, 1f)
    drawRect(brush = ShaderBrush(runtimeShader))
  }
}

@Composable
private fun FallbackFogOverlay(
  timeSeconds: Float,
  parallax: Float,
  density: Float,
  fogColor: Color,
) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    for (i in 0..4) {
      val drift = ((timeSeconds * 0.08f * (i + 1) + parallax * (0.3f + i * 0.15f)) % 1.4f) - 0.2f
      val cx = drift * w
      val cy = h * (0.28f + i * 0.14f)
      val r = w * (0.45f + (i % 2) * 0.15f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(fogColor.copy(alpha = (0.16f * density).coerceAtMost(0.36f)), Color.Transparent),
          center = Offset(cx, cy),
          radius = r,
        ),
        center = Offset(cx, cy),
        radius = r,
      )
    }
  }
}

private fun DrawScope.drawRealisticPostApocalypticEnvironment(
  biome: BiomeType,
  timeOfDay: TimeOfDay,
  weather: WeatherType,
  parallax: Float,
  timePhase: Float,
) {
  val w = size.width
  val h = size.height
  val autoDrift = sin(timePhase * 0.10f) * 0.04f
  val scroll = parallax - 0.5f + autoDrift

  // 1. Atmospheric Sky & Celestial Sun/Moon Glow
  val skyTop = when (timeOfDay) {
    TimeOfDay.DAWN -> Color(0xFF2B1B40)
    TimeOfDay.DAY -> Color(0xFF0C2338)
    TimeOfDay.DUSK -> Color(0xFF3E1810)
    TimeOfDay.MIDNIGHT -> Color(0xFF04070D)
  }
  val skyHorizon = when (biome) {
    BiomeType.SECTOR_9_LAB -> Color(0xFF0B3242)
    BiomeType.FROSTPEAK_RIDGE -> Color(0xFF21426B)
    BiomeType.ASHWOOD_FOREST -> Color(0xFF103328)
    BiomeType.DEAD_CITY -> when (timeOfDay) {
      TimeOfDay.DUSK -> Color(0xFF9A3412)
      TimeOfDay.DAWN -> Color(0xFF7E22CE)
      else -> Color(0xFF1E293B)
    }
  }
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(skyTop.copy(alpha = 0.62f), skyHorizon.copy(alpha = 0.45f), ObsidianBg.copy(alpha = 0.65f))
    )
  )

  // Moon / Low Sun atmospheric bloom
  val celestialX = w * 0.72f - scroll * 25f
  val celestialY = h * 0.16f
  val celestialColor = when (timeOfDay) {
    TimeOfDay.DUSK -> Color(0xFFFB923C)
    TimeOfDay.DAWN -> Color(0xFFFDE68A)
    TimeOfDay.DAY -> Color(0xFFBAE6FD)
    TimeOfDay.MIDNIGHT -> Color(0xFFE0F2FE)
  }
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(celestialColor.copy(alpha = 0.48f), celestialColor.copy(alpha = 0.12f), Color.Transparent),
      center = Offset(celestialX, celestialY),
      radius = w * 0.16f,
    ),
    center = Offset(celestialX, celestialY),
    radius = w * 0.16f,
  )

  // Volumetric God-Rays cutting diagonally through dust haze
  for (ray in 0..2) {
    val rx = w * (0.25f + ray * 0.24f) - scroll * 40f
    val rayPath = Path().apply {
      moveTo(rx, 0f)
      lineTo(rx + w * 0.08f, 0f)
      lineTo(rx - w * 0.10f, h * 0.68f)
      lineTo(rx - w * 0.20f, h * 0.68f)
      close()
    }
    drawPath(
      path = rayPath,
      brush = Brush.verticalGradient(
        colors = listOf(celestialColor.copy(alpha = 0.10f), Color.Transparent)
      ),
    )
  }

  // Distant Mountain / Ridge Backdrop (0.08x parallax)
  val mountainShift = -scroll * w * 0.08f
  val farHorizonY = h * 0.44f
  val ridgePath = Path().apply {
    moveTo(-w * 0.2f + mountainShift, farHorizonY + 24f)
    lineTo(w * 0.05f + mountainShift, farHorizonY - 56f)
    lineTo(w * 0.28f + mountainShift, farHorizonY - 18f)
    lineTo(w * 0.52f + mountainShift, farHorizonY - 72f)
    lineTo(w * 0.78f + mountainShift, farHorizonY - 24f)
    lineTo(w * 1.05f + mountainShift, farHorizonY - 62f)
    lineTo(w * 1.3f + mountainShift, farHorizonY + 24f)
    close()
  }
  drawPath(ridgePath, Color(0xFF0A111A).copy(alpha = 0.55f))

  // Sweeping searchlight beam
  val beamAngle = sin(timePhase * 0.30f) * 0.26f
  val beamOrigin = Offset(w * 0.28f - scroll * 45f, farHorizonY)
  val beamPath = Path().apply {
    moveTo(beamOrigin.x, beamOrigin.y)
    lineTo(beamOrigin.x + sin(beamAngle - 0.11f) * w * 0.65f, 0f)
    lineTo(beamOrigin.x + sin(beamAngle + 0.11f) * w * 0.65f, 0f)
    close()
  }
  val beamColor = if (weather == WeatherType.BIO_STORM) BioAmber else BioCyan
  drawPath(
    path = beamPath,
    brush = Brush.verticalGradient(colors = listOf(Color.Transparent, beamColor.copy(alpha = 0.13f))),
  )

  // 2. FAR LAYER (0.18x parallax) — Shattered Skyscrapers with Exposed Rebar & Distant Smoke Fires
  val farShift = -scroll * w * 0.18f
  val farBuildings = listOf(0.24f, 0.36f, 0.20f, 0.40f, 0.29f, 0.33f, 0.22f, 0.37f, 0.26f, 0.31f)
  val farColW = (w * 1.35f) / farBuildings.size
  farBuildings.forEachIndexed { idx, heightFrac ->
    val bx = -w * 0.18f + idx * farColW + farShift
    val bh = h * heightFrac
    val by = farHorizonY - bh * 0.60f
    val skew = if (idx % 2 == 0) 14f else -10f
    val towerPath = Path().apply {
      moveTo(bx, farHorizonY + 34f)
      lineTo(bx, by + skew.coerceAtLeast(0f))
      lineTo(bx + farColW * 0.42f, by + (if (idx % 3 == 0) 14f else 0f))
      lineTo(bx + farColW - 4f, by + (-skew).coerceAtLeast(0f))
      lineTo(bx + farColW - 4f, farHorizonY + 34f)
      close()
    }
    drawPath(
      path = towerPath,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF111A26).copy(alpha = 0.85f), Color(0xFF070B10).copy(alpha = 0.90f))
      ),
    )
    // Exposed steel rebar spikes at broken roofline
    if (idx % 2 == 0) {
      drawLine(Color(0xFF334155), Offset(bx + 8f, by + 6f), Offset(bx + 12f, by - 12f), strokeWidth = 1.6f)
      drawLine(Color(0xFF334155), Offset(bx + farColW * 0.6f, by + 4f), Offset(bx + farColW * 0.65f, by - 10f), strokeWidth = 1.4f)
    }

    // Distant smoldering fire glow & rising smoke plume
    if (idx == 3 || idx == 7) {
      val fireCenter = Offset(bx + farColW * 0.5f, by + 20f)
      val flicker = 0.75f + 0.25f * sin(timePhase * 5f + idx)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFFF97316).copy(alpha = 0.45f * flicker), Color.Transparent),
          center = fireCenter,
          radius = 28f,
        ),
        center = fireCenter,
        radius = 28f,
      )
      drawCircle(
        color = Color(0xFF1E293B).copy(alpha = 0.35f),
        radius = 18f,
        center = Offset(fireCenter.x - 10f, fireCenter.y - 24f - (timePhase * 6f % 20f)),
      )
    }

    if (idx % 3 == 1) {
      drawLine(Color(0xFF334155), Offset(bx + farColW * 0.5f, by), Offset(bx + farColW * 0.5f, by - 24f), strokeWidth = 2f)
      val blink = (sin(timePhase * 2.5f + idx) + 1f) * 0.5f
      drawCircle(color = CrimsonAlert.copy(alpha = 0.35f + blink * 0.6f), radius = 3f, center = Offset(bx + farColW * 0.5f, by - 24f))
    }
  }

  // 3. MID LAYER (0.42x parallax) — Detailed Concrete Ruins, Balcony Slabs & Biome Silhouettes
  val midShift = -scroll * w * 0.42f
  val midHorizonY = h * 0.52f
  val midSpecs = listOf(0.22f, 0.31f, 0.17f, 0.28f, 0.23f, 0.33f, 0.19f, 0.27f)
  val midColW = (w * 1.45f) / midSpecs.size
  midSpecs.forEachIndexed { idx, frac ->
    val bx = -w * 0.22f + idx * midColW + midShift
    val bh = h * frac
    val by = midHorizonY - bh * 0.54f
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF16202C).copy(alpha = 0.88f), Color(0xFF090E14).copy(alpha = 0.92f)),
        startY = by,
        endY = by + bh + 45f,
      ),
      topLeft = Offset(bx + 4f, by),
      size = Size(midColW - 10f, bh + 45f),
    )
    // Concrete floor slab ribs
    for (slab in 1..3) {
      val sy = by + slab * (bh * 0.24f)
      drawLine(Color(0xFF283548).copy(alpha = 0.7f), Offset(bx + 4f, sy), Offset(bx + midColW - 6f, sy), strokeWidth = 2f)
    }
    // Flickering damaged windows
    for (row in 0..3) {
      for (col in 0..2) {
        if ((idx + row * 2 + col) % 3 == 0) {
          val winAlpha = 0.18f + 0.16f * sin(timePhase * 1.7f + idx + row)
          val winColor = if ((idx + row) % 4 == 0) BioAmber else BioCyan
          drawRect(
            color = winColor.copy(alpha = winAlpha.coerceIn(0.05f, 0.45f)),
            topLeft = Offset(bx + 12f + col * 15f, by + 10f + row * 16f),
            size = Size(7f, 10f),
          )
        }
      }
    }
  }

  when (biome) {
    BiomeType.ASHWOOD_FOREST -> {
      for (i in 0..10) {
        val tx = -w * 0.15f + i * (w * 0.13f) + midShift * 1.1f
        val tree = Path().apply {
          moveTo(tx, midHorizonY - 44f)
          lineTo(tx - 24f, midHorizonY + 18f)
          lineTo(tx + 24f, midHorizonY + 18f)
          close()
        }
        drawPath(tree, BioEmerald.copy(alpha = 0.34f))
      }
    }
    BiomeType.FROSTPEAK_RIDGE -> {
      for (i in 0..5) {
        val px = -w * 0.10f + i * (w * 0.25f) + midShift * 0.8f
        val peak = Path().apply {
          moveTo(px, midHorizonY - 68f)
          lineTo(px - 68f, midHorizonY + 20f)
          lineTo(px + 68f, midHorizonY + 20f)
          close()
        }
        drawPath(peak, Color(0xFF475569).copy(alpha = 0.58f))
      }
    }
    BiomeType.SECTOR_9_LAB -> {
      drawLine(BioCyan.copy(alpha = 0.50f), Offset(0f, midHorizonY - 14f), Offset(w, midHorizonY - 14f), strokeWidth = 3f)
    }
    BiomeType.DEAD_CITY -> Unit
  }

  // Wet Asphalt / Reflective Ground Plane below midHorizonY
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color.Transparent, Color(0xFF0A1017).copy(alpha = 0.60f), ObsidianBg.copy(alpha = 0.82f)),
      startY = midHorizonY,
      endY = h,
    )
  )
  // Subtle wet puddle reflections matching skyline light
  for (p in 0..3) {
    val px = ((p * 0.28f + 0.15f - scroll * 0.55f + 2f) % 1.1f) * w
    val py = h * (0.64f + (p % 2) * 0.14f)
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(celestialColor.copy(alpha = 0.11f), Color.Transparent),
        center = Offset(px, py),
        radius = 48f,
      ),
      topLeft = Offset(px - 46f, py - 11f),
      size = Size(92f, 22f),
    )
  }

  // 4. NEAR LAYER (0.75x parallax) — Damaged Concrete Highway Piers & Embers
  val nearShift = -scroll * w * 0.75f
  for (i in 0..4) {
    val px = -w * 0.10f + i * (w * 0.30f) + nearShift
    drawRoundRect(
      color = Color(0xFF05080C).copy(alpha = 0.88f),
      topLeft = Offset(px, h * 0.54f),
      size = Size(18f, h * 0.46f),
      cornerRadius = CornerRadius(3f, 3f),
    )
    drawLine(Color(0xFF1E293B), Offset(px + 3f, h * 0.54f), Offset(px + 3f, h), strokeWidth = 2f)
  }

  // Rising glowing ash & embers
  for (i in 0..20) {
    val seedX = ((i * 53) % 100) / 100f
    val seedY = ((i * 31) % 100) / 100f
    val ex = ((seedX + timePhase * 0.024f - scroll * 0.4f + 2f) % 1f) * w
    val ey = ((seedY - timePhase * 0.042f + 2f) % 1f) * h
    drawCircle(
      color = if (i % 2 == 0) BioAmber.copy(alpha = 0.45f) else BioCyan.copy(alpha = 0.32f),
      radius = (1.5f + (i % 3)).toFloat(),
      center = Offset(ex, ey),
    )
  }
}
