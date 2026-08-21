package com.magicmaker.lite.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.magicmaker.lite.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WAND_ASPECT = 600f / 750f
private const val UPPER_GLOW_FRACTION = 0.94f
private val STARLIGHT_ORANGE = Color(0xFFFF9800)

/** On-screen color for each 5-bit palette index — the wand glow and the swatches share it. */
fun starlightGlowColor(index: Int): Color {
    val i = index and 0x1F
    return when (i) {
        0x00, 0x16, 0x17, 0x18 -> Color(0xFF00E5FF)
        0x01, 0x05, 0x07 -> Color(0xFFBB86FC)
        0x02, 0x03, 0x04 -> Color(0xFF448AFF)
        0x06 -> Color(0xFFE1BEE7)
        in 0x08..0x0E -> Color(0xFFFF4081)
        0x0F, 0x11 -> Color(0xFFFFAB40)
        0x10 -> Color(0xFFFFF59D)
        0x12, 0x1A -> Color(0xFFCCFF90)
        0x13, 0x14 -> STARLIGHT_ORANGE
        0x15 -> Color(0xFFFF5252)
        0x19 -> Color(0xFF69F0AE)
        0x1B, 0x1C -> Color(0xFFD1F0FF)
        0x1D -> Color(0xFF505050)
        0x1E -> Color(0xFFCE93D8)
        else -> Color(0xFFB388FF)
    }
}

private data class WandGlowEnvelope(
    val centerY: Float,
    val rx: Float,
    val ry: Float,
    val peak: Float,
)

private val wandAuraEnvelopes = listOf(
    WandGlowEnvelope(0.28f, 0.115f, 0.30f, 0.30f),
    WandGlowEnvelope(0.28f, 0.062f, 0.265f, 0.55f),
)

private val wandTintEnvelope = WandGlowEnvelope(0.28f, 0.085f, 0.275f, 0.72f)

/**
 * The wand itself, glowing in the selected color. Tapping it transmits — the big obvious
 * target, with the SEND button below for anyone who wants a labelled one.
 */
@Composable
fun WandStage(
    colorIndex: Int,
    pulseTick: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse1 = remember { Animatable(0f) }
    val pulse2 = remember { Animatable(0f) }

    // Every transmit bumps the tick, which re-fires this effect: two staggered pulses of the
    // aura so you can see the code actually went out.
    LaunchedEffect(pulseTick) {
        if (pulseTick == 0) return@LaunchedEffect
        pulse1.snapTo(0f)
        pulse2.snapTo(0f)
        launch { pulse1.animateTo(1f, tween(420)) }
        delay(180)
        pulse2.animateTo(1f, tween(420))
    }

    val glow = starlightGlowColor(colorIndex)

    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onTap,
        ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val maxH = maxHeight * 0.94f
            val maxW = maxWidth * 0.74f
            val wandHeight = minOf(maxH, maxW / WAND_ASPECT)
            val wandWidth = wandHeight * WAND_ASPECT

            Canvas(
                Modifier
                    .width(wandWidth)
                    .height(wandHeight)
                    .blur(3.5.dp),
            ) {
                drawWandAuraLayer(glow = glow, pulse1 = pulse1.value, pulse2 = pulse2.value)
            }

            Image(
                painter = painterResource(R.drawable.starlight_wand),
                contentDescription = "Wand",
                modifier = Modifier
                    .width(wandWidth)
                    .height(wandHeight),
                contentScale = ContentScale.Fit,
            )

            Canvas(
                Modifier
                    .width(wandWidth)
                    .height(wandHeight)
                    .blur(1.2.dp),
            ) {
                drawWandTintLayer(glow = glow)
            }
        }
    }
}

private fun DrawScope.drawWandAuraLayer(glow: Color, pulse1: Float, pulse2: Float) {
    val cx = size.width / 2f
    val glowBottom = size.height * UPPER_GLOW_FRACTION

    clipRect(left = 0f, top = 0f, right = size.width, bottom = glowBottom) {
        wandAuraEnvelopes.forEach { env ->
            drawSoftAura(
                glow = glow,
                cx = cx,
                centerY = size.height * env.centerY,
                rx = size.width * env.rx,
                ry = size.height * env.ry,
                peak = env.peak,
            )
        }

        listOf(pulse1 to 0.38f, pulse2 to 0.24f).forEach { (p, alpha) ->
            if (p > 0f) {
                val scale = 1f + p * 0.06f
                val env = wandAuraEnvelopes[0]
                drawSoftAura(
                    glow = glow,
                    cx = cx,
                    centerY = size.height * env.centerY,
                    rx = size.width * env.rx * scale,
                    ry = size.height * env.ry * scale,
                    peak = alpha * (1f - p * 0.4f),
                )
            }
        }
    }
}

private fun DrawScope.drawSoftAura(
    glow: Color,
    cx: Float,
    centerY: Float,
    rx: Float,
    ry: Float,
    peak: Float,
) {
    val radius = maxOf(rx, ry)
    drawOval(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to glow.copy(alpha = peak * 0.45f),
                0.28f to glow.copy(alpha = peak * 0.82f),
                0.42f to glow.copy(alpha = peak),
                0.58f to glow.copy(alpha = peak * 0.62f),
                0.72f to glow.copy(alpha = peak * 0.22f),
                0.84f to glow.copy(alpha = peak * 0.05f),
                0.96f to Color.Transparent,
            ),
            center = Offset(cx, centerY),
            radius = radius,
        ),
        topLeft = Offset(cx - rx, centerY - ry),
        size = Size(rx * 2f, ry * 2f),
        blendMode = BlendMode.Plus,
    )
}

private fun DrawScope.drawWandTintLayer(glow: Color) {
    val cx = size.width / 2f
    val glowBottom = size.height * UPPER_GLOW_FRACTION
    val env = wandTintEnvelope

    clipRect(left = 0f, top = 0f, right = size.width, bottom = glowBottom) {
        val centerY = size.height * env.centerY
        val rx = size.width * env.rx
        val ry = size.height * env.ry
        val radius = maxOf(rx, ry)
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to glow.copy(alpha = env.peak * 0.55f),
                    0.35f to glow.copy(alpha = env.peak * 0.85f),
                    0.58f to glow.copy(alpha = env.peak * 0.45f),
                    0.76f to glow.copy(alpha = env.peak * 0.12f),
                    0.92f to Color.Transparent,
                ),
                center = Offset(cx, centerY),
                radius = radius,
            ),
            topLeft = Offset(cx - rx, centerY - ry),
            size = Size(rx * 2f, ry * 2f),
            blendMode = BlendMode.Multiply,
            alpha = 0.78f,
        )
    }
}
