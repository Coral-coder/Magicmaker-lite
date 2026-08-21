package com.magicmaker.lite.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.magicmaker.lite.core.TransmitTiming
import kotlin.math.roundToInt

/**
 * Transmission period, 200–1000 ms in 50 ms steps.
 *
 * Drag state is kept inside the slider so the screen isn't recomposed mid-gesture (parent
 * recomposition cancels a Compose slider drag after one step); [onCommit] fires on release.
 * The − / + buttons are there for precise single-step changes without fighting the thumb.
 */
@Composable
fun PeriodSlider(
    periodMs: Int,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val min = TransmitTiming.MIN_PERIOD_MS.toFloat()
    val max = TransmitTiming.MAX_PERIOD_MS.toFloat()
    val step = TransmitTiming.PERIOD_STEP_MS.toFloat()

    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableFloatStateOf(periodMs.toFloat()) }

    LaunchedEffect(periodMs) {
        if (!dragging) draft = periodMs.toFloat()
    }

    fun snap(v: Float): Int = ((v / step).roundToInt() * step).coerceIn(min, max).roundToInt()

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("TRANSMIT PERIOD", style = techHeader(), modifier = Modifier.weight(1f))
            Text("${snap(draft)} MS", style = techBody(), color = AppColors.TextPrimary)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onCommit(snap(periodMs - step)) },
                enabled = periodMs > TransmitTiming.MIN_PERIOD_MS,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(44.dp),
            ) { Text("–", style = techBody(), color = AppColors.Cyan) }
            Slider(
                value = draft,
                onValueChange = { next ->
                    dragging = true
                    draft = next
                },
                onValueChangeFinished = {
                    dragging = false
                    onCommit(snap(draft))
                },
                valueRange = min..max,
                // 200..1000 in 50 ms steps = 17 stops, i.e. 15 ticks between the ends.
                steps = ((max - min) / step).roundToInt() - 1,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.Cyan,
                    activeTrackColor = AppColors.Cyan,
                    inactiveTrackColor = AppColors.PanelLine,
                ),
            )
            OutlinedButton(
                onClick = { onCommit(snap(periodMs + step)) },
                enabled = periodMs < TransmitTiming.MAX_PERIOD_MS,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(44.dp),
            ) { Text("+", style = techBody(), color = AppColors.Cyan) }
        }
        Text(
            "${TransmitTiming.MIN_PERIOD_MS}–${TransmitTiming.MAX_PERIOD_MS} MS · HOW LONG EACH CODE STAYS ON AIR",
            style = techLabel(),
            color = AppColors.TextDim,
        )
    }
}
