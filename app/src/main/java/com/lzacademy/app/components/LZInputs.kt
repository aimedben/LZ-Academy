package com.lzacademy.app.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Single line text field with LZ styling: leading icon, focus ring, error state. */
@Composable
fun LZTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    errorMessage: String? = null,
    singleLine: Boolean = true,
) {
    val c = LZ.colors
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        when {
            errorMessage != null -> c.error
            focused -> c.primary
            else -> c.border
        },
        label = "inputBorder",
    )
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(c.surface, LZShapes.standard)
                .border(if (focused) 1.5.dp else 1.dp, borderColor, LZShapes.standard)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = if (focused) c.primary else c.textSecondary, modifier = Modifier.size(22.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                singleLine = singleLine,
                textStyle = LZ.type.body.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                visualTransformation = visualTransformation,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) LZText(placeholder, color = c.textSecondary, maxLines = 1)
                        inner()
                    }
                },
            )
            if (trailing != null) trailing()
        }
        if (errorMessage != null) {
            LZText(errorMessage, style = LZ.type.label, color = c.error, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        }
    }
}

/** Multi-line input for a message body (SMS, notes) — same look as [LZTextInput]. */
@Composable
fun LZMultilineInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 120.dp,
) {
    val c = LZ.colors
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) c.primary else c.border,
        label = "multilineBorder",
    )
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(c.surface, LZShapes.standard)
            .border(if (focused) 1.5.dp else 1.dp, borderColor, LZShapes.standard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            singleLine = false,
            textStyle = LZ.type.body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        LZText(placeholder, color = c.textSecondary, style = LZ.type.body)
                    }
                    inner()
                }
            },
        )
    }
}

/** Password field with a show/hide toggle. */
@Composable
fun LZPasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Mot de passe",
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    var visible by remember { mutableStateOf(false) }
    LZTextInput(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        leadingIcon = Icons.Rounded.Lock,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        errorMessage = errorMessage,
        trailing = {
            LZIconButton(
                icon = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = if (visible) "Masquer le mot de passe" else "Afficher le mot de passe",
                onClick = { visible = !visible },
                container = Color.Transparent,
                tint = LZ.colors.textSecondary,
                size = 40.dp,
            )
        },
    )
}

/** Rounded search field with clear button. */
@Composable
fun LZSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Rechercher...",
) {
    val c = LZ.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(c.lavenderSoft, LZShapes.pill)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Search, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = LZ.type.body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) LZText(placeholder, color = c.textSecondary, maxLines = 1)
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            LZIconButton(
                Icons.Rounded.Close, "Effacer", { onValueChange("") },
                container = Color.Transparent, tint = c.textSecondary, size = 36.dp,
            )
        }
    }
}

/** Form dropdown (label, current value, menu). */
@Composable
fun LZDropdownField(
    options: List<String>,
    selected: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Sélectionner",
    leadingIcon: ImageVector? = null,
) {
    val c = LZ.colors
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(c.surface, LZShapes.standard)
                .border(1.dp, if (expanded) c.primary else c.border, LZShapes.standard)
                .clip(LZShapes.standard)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) Icon(leadingIcon, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
            LZText(
                selected ?: placeholder,
                color = if (selected == null) c.textSecondary else c.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Icon(Icons.Rounded.ExpandMore, null, tint = c.textSecondary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(c.surface)) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { LZText(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Small pill dropdown for chart cards ("12 mois ⌄"). */
@Composable
fun LZCompactDropdown(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .background(c.surfaceSoft, LZShapes.pill)
                .pressable(LZShapes.pill, role = Role.DropdownList) { expanded = true }
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LZText(options[selectedIndex], style = LZ.type.label, color = c.primary, maxLines = 1)
            Icon(Icons.Rounded.ExpandMore, "Changer la période", tint = c.primary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(c.surface)) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { LZText(option) },
                    onClick = {
                        onSelect(i)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Date field opening a Material 3 date picker. Requires API 26+ (java.time). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LZDatePickerField(
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "jj/mm/aaaa",
) {
    val c = LZ.colors
    var open by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(c.surface, LZShapes.standard)
            .border(1.dp, c.border, LZShapes.standard)
            .clip(LZShapes.standard)
            .clickable(role = Role.Button) { open = true }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.CalendarToday, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
        LZText(
            date?.format(DateFormat) ?: placeholder,
            color = if (date == null) c.textSecondary else c.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        onDateSelected(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("Valider", color = c.primary) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Annuler", color = c.textSecondary) } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Filter chip ("Tous", "Maths", "Physique"). */
@Composable
fun LZFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    val bg by animateColorAsState(if (selected) c.primary else c.surfaceSoft, label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else c.textSecondary, label = "chipFg")
    Box(
        modifier
            .defaultMinSize(minHeight = 36.dp)
            .background(bg, LZShapes.pill)
            .pressable(LZShapes.pill, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        LZText(label, style = LZ.type.label, color = fg, maxLines = 1)
    }
}

/** Segmented control with an animated thumb ("Semaine | Mois"). */
@Composable
fun LZSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(c.surfaceSoft, LZShapes.pill)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val thumbOffset by animateDpAsState(segment * selectedIndex, label = "segmentOffset")
        Box(
            Modifier
                .offset(x = thumbOffset)
                .width(segment)
                .fillMaxHeight()
                .background(c.primary, LZShapes.pill),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, option ->
                val fg by animateColorAsState(if (i == selectedIndex) Color.White else c.textSecondary, label = "segmentFg")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(LZShapes.pill)
                        .clickable(role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { LZText(option, style = LZ.type.bodyStrong, color = fg, maxLines = 1) }
            }
        }
    }
}

/**
 * Mockup category tabs: selected = filled emerald pill with white icon + text,
 * inactive = lavender pill with violet text. Shared by every Gestion tab.
 */
@Composable
fun LZCategoryTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector> = emptyList(),
) {
    val c = LZ.colors
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { i, option ->
            val selected = i == selectedIndex
            val bg by animateColorAsState(if (selected) c.primary else c.lavenderSoft, label = "tabBg")
            val fg by animateColorAsState(if (selected) c.onPrimary else c.purple, label = "tabFg")
            Row(
                Modifier
                    .height(40.dp)
                    .background(bg, LZShapes.pill)
                    .pressable(LZShapes.pill, role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (i < icons.size) {
                    Icon(icons[i], null, tint = fg, modifier = Modifier.size(16.dp))
                }
                LZText(option, style = LZ.type.label, color = fg, maxLines = 1)
            }
        }
    }
}

/** Mockup floating action button: emerald circle, 56dp, white "+". */
@Composable
fun LZFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    contentDescription: String = "Ajouter",
) {
    Box(
        modifier
            .size(56.dp)
            .softShadow(18.dp, CircleShape)
            .background(LZ.colors.primary, CircleShape)
            .pressable(CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
