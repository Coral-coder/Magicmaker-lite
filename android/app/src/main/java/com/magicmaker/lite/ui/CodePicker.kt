package com.magicmaker.lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.magicmaker.lite.core.WandCode
import com.magicmaker.lite.core.WandType

/**
 * Searchable list of every code for the current wand. Tap a row to arm it, tap the star to
 * keep it on the quick strip.
 */
@Composable
fun CodePickerDialog(
    codes: List<WandCode>,
    selectedId: Long,
    favorites: Set<String>,
    onSelect: (WandCode) -> Unit,
    onToggleFavorite: (WandCode) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(codes, query) {
        val q = query.trim().uppercase()
        if (q.isEmpty()) codes
        else codes.filter { it.name.uppercase().contains(q) || it.hex.uppercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel,
        title = {
            Text(
                "ALL CODES · ${codes.size}",
                style = techHeader(),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("SEARCH NAME OR CODE", style = techLabel()) },
                )
                Text(
                    "${filtered.size} SHOWN",
                    style = techLabel(),
                    color = AppColors.TextDim,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                HorizontalDivider(color = AppColors.PanelLine, thickness = 0.5.dp)
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = { it.id }) { code ->
                        CodeRow(
                            code = code,
                            selected = code.id == selectedId,
                            favorite = code.hex.uppercase() in favorites,
                            onSelect = { onSelect(code) },
                            onToggleFavorite = { onToggleFavorite(code) },
                        )
                        HorizontalDivider(color = AppColors.PanelLine, thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", style = techBody(), color = AppColors.Cyan)
            }
        },
    )
}

@Composable
private fun CodeRow(
    code: WandCode,
    selected: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ColorDot(code.colorIndex, size = 18.dp)
        Column(Modifier.weight(1f)) {
            Text(
                code.name.uppercase(),
                style = techBody(),
                color = if (selected) AppColors.Cyan else AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (code.wand == WandType.NEW) "CODE ${code.tail}" else code.hex.uppercase(),
                style = techLabel(),
                color = AppColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            if (favorite) "★" else "☆",
            style = techBody(),
            color = if (favorite) AppColors.Amber else AppColors.TextDim,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onToggleFavorite)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** The palette color as a filled dot — the fastest way to recognize a code at a glance. */
@Composable
fun ColorDot(colorIndex: Int, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(starlightGlowColor(colorIndex))
            .border(0.5.dp, AppColors.PanelLine, CircleShape),
    )
}

/** One code on the quick strip. [label] disambiguates the many same-named palette entries. */
@Composable
fun CodeChip(
    code: WandCode,
    selected: Boolean,
    favorite: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AppColors.PanelLine else AppColors.Panel)
            .border(
                if (selected) 1.dp else 0.5.dp,
                if (selected) AppColors.Cyan else AppColors.PanelLine,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ColorDot(code.colorIndex, size = 14.dp)
        Text(
            label,
            style = techLabel(),
            color = if (selected) AppColors.Cyan else AppColors.TextPrimary,
            maxLines = 1,
        )
        if (favorite) Text("★", style = techLabel(), color = AppColors.Amber)
    }
}

/** Padding shared by the strip so chips don't sit flush against the screen edge. */
val stripPadding = PaddingValues(horizontal = AppDimens.pad)
