package com.lzacademy.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes

/**
 * Building blocks of the Settings screen: a titled card, a labelled switch row and a tappable
 * row. They are deliberately generic so every role renders its own settings with the same look.
 */

/** Titled card that groups settings rows. */
@Composable
fun LZSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    val c = LZ.colors
    LZCard(modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(22.dp))
            }
            LZText(title, style = LZ.type.cardTitle)
        }
        Spacer(Modifier.height(4.dp))
        content()
    }
}

/** Labelled ON/OFF switch: title, explanation, and the control itself. */
@Composable
fun LZSettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LZ.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier.size(38.dp).background(c.surfaceSoft, LZShapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LZText(title, style = LZ.type.bodyStrong, maxLines = 2)
            LZText(description, style = LZ.type.secondary, color = c.textSecondary, maxLines = 3)
            if (trailing != null) {
                Spacer(Modifier.height(6.dp))
                trailing()
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.primary,
                checkedBorderColor = c.primary,
                uncheckedThumbColor = c.textSecondary,
                uncheckedTrackColor = c.surfaceSoft,
                uncheckedBorderColor = c.border,
            ),
        )
    }
}

/** Tappable settings row that opens something else (a screen, a dialog). */
@Composable
fun LZSettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    tint: Color? = null,
) {
    val c = LZ.colors
    val accent = tint ?: c.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(LZShapes.small, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier.size(38.dp).background(c.surfaceSoft, LZShapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LZText(title, style = LZ.type.bodyStrong, color = tint ?: c.textPrimary, maxLines = 2)
            if (description != null) {
                LZText(description, style = LZ.type.secondary, color = c.textSecondary, maxLines = 3)
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
