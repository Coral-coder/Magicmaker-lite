package com.magicmaker.lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.magicmaker.lite.core.NewWandCatalog
import com.magicmaker.lite.core.StarlightColors
import com.magicmaker.lite.core.StarlightSerial
import com.magicmaker.lite.core.WandCatalog
import com.magicmaker.lite.core.WandCode
import com.magicmaker.lite.core.WandTransmitter
import com.magicmaker.lite.core.WandType
import com.magicmaker.lite.data.LitePrefs

/** Palette index for "Off" — the quick button both wands share. */
private const val OFF_COLOR_INDEX = 0x1D

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WandScreen(
    prefs: LitePrefs,
    transmitter: WandTransmitter,
    advertiseGranted: Boolean,
    permissionBlocked: Boolean,
    bluetoothOn: Boolean,
    stopTick: Int,
    status: String,
    onRequestPermission: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    var wandType by remember { mutableStateOf(prefs.wandType) }
    var serial by remember { mutableStateOf(prefs.serial) }
    var periodMs by remember { mutableIntStateOf(prefs.periodMs) }
    var favorites by remember { mutableStateOf(prefs.favorites()) }
    var repeating by remember { mutableStateOf(false) }
    var pulseTick by remember { mutableIntStateOf(0) }
    var pickerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }

    val codes = remember(wandType, serial) { WandCatalog.forType(wandType, serial) }
    var selectedId by remember(wandType) { mutableLongStateOf(prefs.selectedId(wandType)) }
    val selected = remember(codes, selectedId) {
        codes.firstOrNull { it.id == selectedId } ?: codes.first()
    }
    val canTransmit = advertiseGranted && bluetoothOn

    // Favourites float to the front of the quick strip; everything else keeps catalog order.
    val strip = remember(codes, favorites) {
        val starred = codes.filter { it.hex.uppercase() in favorites }
        starred + codes.filterNot { it.hex.uppercase() in favorites }
    }
    val stripState = rememberLazyListState()

    LaunchedEffect(selected.id, strip) {
        val idx = strip.indexOfFirst { it.id == selected.id }
        if (idx >= 0) stripState.animateScrollToItem(idx)
    }

    // The activity stops the radio when the app leaves the screen (or Bluetooth goes off);
    // clear REPEAT to match, so the button never claims to be broadcasting when it isn't.
    LaunchedEffect(stopTick) {
        if (stopTick > 0) repeating = false
    }

    // Leaving the app must not leave the radio broadcasting.
    DisposableEffect(Unit) {
        onDispose {
            transmitter.stop()
        }
    }

    fun transmit(code: WandCode) {
        if (!canTransmit) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        pulseTick++
        if (repeating) transmitter.startRepeat(code.hex, periodMs)
        else transmitter.sendOnce(code.hex, periodMs)
    }

    fun select(code: WandCode) {
        selectedId = code.id
        prefs.setSelectedId(wandType, code.id)
        // Selecting is also sending — one tap, like waving the wand.
        transmit(code)
    }

    Scaffold(
        containerColor = AppColors.Navy,
        topBar = {
            TopAppBar(
                title = { Text("MAGICMAKER LITE", style = techHeader()) },
                actions = {
                    TextButton(onClick = { settingsOpen = true }) {
                        Text("SETUP", style = techLabel(), color = AppColors.TextDim)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Panel,
                    titleContentColor = AppColors.Cyan,
                ),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize(),
        ) {
            if (!advertiseGranted) {
                Banner(
                    message = if (permissionBlocked) "NEARBY DEVICES PERMISSION IS BLOCKED"
                    else "NEARBY DEVICES PERMISSION NEEDED TO TRANSMIT",
                    actionLabel = if (permissionBlocked) "SETTINGS" else "GRANT",
                    onAction = onRequestPermission,
                )
            } else if (!bluetoothOn) {
                Banner(
                    message = "BLUETOOTH IS OFF",
                    actionLabel = "SETTINGS",
                    onAction = onOpenBluetoothSettings,
                )
            }

            WandStage(
                colorIndex = selected.colorIndex,
                pulseTick = pulseTick,
                onTap = { transmit(selected) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 160.dp)
                    .fillMaxWidth(),
            )

            Column(
                Modifier.padding(horizontal = AppDimens.pad),
                verticalArrangement = Arrangement.spacedBy(AppDimens.gap),
            ) {
                SelectedCodeCard(
                    code = selected,
                    favorite = selected.hex.uppercase() in favorites,
                    onToggleFavorite = { favorites = prefs.toggleFavorite(selected.hex) },
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.gap),
                ) {
                    Button(
                        onClick = { transmit(selected) },
                        enabled = canTransmit,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Cyan,
                            contentColor = AppColors.Navy,
                            disabledContainerColor = AppColors.PanelLine,
                            disabledContentColor = AppColors.TextDim,
                        ),
                    ) {
                        Text(if (repeating) "SEND · REPEATING" else "SEND", style = techHeader().copy(color = AppColors.Navy))
                    }
                    OutlinedButton(
                        onClick = {
                            val next = !repeating
                            repeating = next
                            if (!canTransmit) return@OutlinedButton
                            if (next) {
                                pulseTick++
                                transmitter.startRepeat(selected.hex, periodMs)
                            } else {
                                transmitter.stop()
                            }
                        },
                        enabled = canTransmit,
                        modifier = Modifier.height(56.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Text(
                            if (repeating) "STOP" else "REPEAT",
                            style = techBody(),
                            color = if (repeating) AppColors.Amber else AppColors.Cyan,
                        )
                    }
                }

                PeriodSlider(
                    periodMs = periodMs,
                    onCommit = { ms ->
                        periodMs = ms
                        prefs.periodMs = ms
                        // A live repeat picks up the new period straight away.
                        if (repeating && canTransmit) transmitter.startRepeat(selected.hex, ms)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.gap),
                ) {
                    WandType.entries.forEach { type ->
                        OutlinedButton(
                            onClick = {
                                if (wandType == type) return@OutlinedButton
                                if (repeating) {
                                    repeating = false
                                    transmitter.stop()
                                }
                                wandType = type
                                prefs.wandType = type
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (wandType == type) AppColors.Panel else AppColors.Navy,
                            ),
                        ) {
                            Text(
                                type.label,
                                style = techBody(),
                                color = if (wandType == type) AppColors.Cyan else AppColors.TextDim,
                            )
                        }
                    }
                }
            }

            Text(
                if (wandType == WandType.NEW) "TAP A CODE TO SEND IT · ★ PINS IT HERE"
                else "TAP A COLOR TO SEND IT · ★ PINS IT HERE",
                style = techLabel(),
                color = AppColors.TextDim,
                modifier = Modifier.padding(horizontal = AppDimens.pad, vertical = 6.dp),
            )
            LazyRow(
                state = stripState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = stripPadding,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(strip, key = { it.id }) { code ->
                    CodeChip(
                        code = code,
                        selected = code.id == selected.id,
                        favorite = code.hex.uppercase() in favorites,
                        label = chipLabel(code),
                        onClick = { select(code) },
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.pad),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { pickerOpen = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Text("ALL CODES · ${codes.size}", style = techBody(), color = AppColors.Cyan)
                }
                OutlinedButton(
                    onClick = {
                        val off = offCodeFor(wandType, codes)
                        if (off != null) select(off)
                    },
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text("OFF", style = techBody(), color = AppColors.Amber)
                }
            }

            HorizontalDivider(color = AppColors.PanelLine, thickness = 0.5.dp)
            Text(
                status.uppercase().ifEmpty { "READY" },
                style = techLabel(),
                color = AppColors.TextDim,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            )
        }
    }

    if (pickerOpen) {
        CodePickerDialog(
            codes = codes,
            selectedId = selected.id,
            favorites = favorites,
            onSelect = { code ->
                selectedId = code.id
                prefs.setSelectedId(wandType, code.id)
                transmit(code)
                pickerOpen = false
            },
            onToggleFavorite = { code -> favorites = prefs.toggleFavorite(code.hex) },
            onDismiss = { pickerOpen = false },
        )
    }

    if (settingsOpen) {
        SerialDialog(
            serial = serial,
            onSave = { next ->
                serial = StarlightSerial.validatedSerial(next)
                prefs.serial = serial
                if (repeating) {
                    repeating = false
                    transmitter.stop()
                }
                settingsOpen = false
            },
            onDismiss = { settingsOpen = false },
        )
    }
}

/**
 * Half the palette shares a name (seven Pinks, four Cyans), so a Starlight chip carries its
 * palette index too; a New Wand chip is identified by its code tail.
 */
private fun chipLabel(code: WandCode): String = when (code.wand) {
    WandType.STARLIGHT -> "${code.name.uppercase()} ${"%02X".format(code.colorIndex)}"
    WandType.NEW -> code.tail
}

/** Each wand's "Off" code: the Starlight palette entry 0x1D, or the New Wand 6F1D code. */
private fun offCodeFor(type: WandType, codes: List<WandCode>): WandCode? = when (type) {
    WandType.STARLIGHT -> codes.firstOrNull { it.colorIndex == OFF_COLOR_INDEX }
    WandType.NEW -> codes.firstOrNull { it.hex.equals(NewWandCatalog.OFF_HEX, ignoreCase = true) }
}

@Composable
private fun SelectedCodeCard(
    code: WandCode,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Panel)
            .border(0.5.dp, AppColors.PanelLine, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ColorDot(code.colorIndex, size = 22.dp)
        Column(Modifier.weight(1f)) {
            Text(
                code.name.uppercase(),
                style = techHeader(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${code.wand.label} · ${code.hex.uppercase()}",
                style = techLabel(),
                color = AppColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onToggleFavorite) {
            Text(
                if (favorite) "★" else "☆",
                style = techHeader().copy(color = if (favorite) AppColors.Amber else AppColors.TextDim),
            )
        }
    }
}

@Composable
private fun Banner(message: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppColors.Panel)
            .padding(horizontal = AppDimens.pad, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.gap),
    ) {
        Text(message, style = techLabel(), color = AppColors.Amber, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onAction, modifier = Modifier.height(36.dp)) {
            Text(actionLabel, style = techLabel(), color = AppColors.Cyan)
        }
    }
}

/**
 * The Starlight codes are built from a wand serial. The app ships with the default one, and this
 * is where someone with their own captured serial can paste it in.
 */
@Composable
private fun SerialDialog(
    serial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(serial) }
    val valid = StarlightSerial.normalizeSerial(draft) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel,
        title = { Text("STARLIGHT WAND SERIAL", style = techHeader()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The Starlight codes are this serial plus a color byte. Leave it as the " +
                        "default unless you have captured your own wand's serial.",
                    style = techLabel(),
                    color = AppColors.TextDim,
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    isError = !valid,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("${StarlightSerial.SERIAL_HEX_LEN} HEX CHARACTERS", style = techLabel()) },
                )
                Text(
                    if (valid) "EXAMPLE · ${StarlightSerial.buildHex(draft, 0x1B).uppercase()}"
                    else "NEEDS ${StarlightSerial.SERIAL_HEX_LEN} HEX CHARACTERS",
                    style = techLabel(),
                    color = if (valid) AppColors.TextDim else AppColors.Amber,
                    maxLines = 2,
                )
                TextButton(onClick = { draft = StarlightSerial.DEFAULT_SERIAL }) {
                    Text("RESET TO DEFAULT", style = techLabel(), color = AppColors.Cyan)
                }
                HorizontalDivider(color = AppColors.PanelLine, thickness = 0.5.dp)
                Text(
                    "New Wand codes are fixed — ${NewWandCatalog.codes.size} of them — and are not " +
                        "affected by this serial. Starlight has ${StarlightColors.palette.size} colors.",
                    style = techLabel(),
                    color = AppColors.TextDim,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }, enabled = valid) {
                Text("SAVE", style = techBody(), color = if (valid) AppColors.Cyan else AppColors.TextDim)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = techBody(), color = AppColors.TextDim)
            }
        },
    )
}
