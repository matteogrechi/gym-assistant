package it.matteogrechi.gymassystant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import it.matteogrechi.gymassystant.R

enum class ButtonEmphasis { Filled, Tonal, Outlined, Text }

@Composable
fun GymButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    emphasis: ButtonEmphasis = ButtonEmphasis.Filled,
    enabled: Boolean = true, icon: ImageVector? = null,
    horizontalPadding: Dp? = null,
) {
    val colors = GymColors
    val container = when (emphasis) {
        ButtonEmphasis.Filled -> colors.accent
        ButtonEmphasis.Tonal -> colors.tonalContainer
        else -> Color.Transparent
    }
    val foreground = if (emphasis == ButtonEmphasis.Filled) colors.onAccent else colors.primary
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = container, contentColor = foreground,
        disabledContainerColor = container.copy(alpha = if (container == Color.Transparent) 0f else .38f),
        disabledContentColor = foreground.copy(alpha = .38f),
    )
    val target = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
    val padding = PaddingValues(horizontal = horizontalPadding ?: if (emphasis == ButtonEmphasis.Text) 12.dp else 24.dp,
        vertical = 10.dp)
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
    when (emphasis) {
        ButtonEmphasis.Filled -> Button(onClick, target, enabled, shape = CircleShape,
            colors = buttonColors, elevation = null, contentPadding = padding, content = content)
        ButtonEmphasis.Tonal -> FilledTonalButton(onClick, target, enabled, shape = CircleShape,
            colors = buttonColors, elevation = null, contentPadding = padding, content = content)
        ButtonEmphasis.Outlined -> OutlinedButton(onClick, target, enabled, shape = CircleShape,
            colors = buttonColors,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = if (enabled) 1f else .38f)),
            contentPadding = padding, content = content)
        ButtonEmphasis.Text -> TextButton(onClick, target, enabled, shape = CircleShape,
            colors = buttonColors, contentPadding = padding, content = content)
    }
}

@Composable
fun Heading(
    title: String, back: (() -> Unit)? = null, modifier: Modifier = Modifier,
    backLabel: String? = null, actions: @Composable RowScope.() -> Unit = {},
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val inset = if (back != null) 12.dp else 0.dp
        Row(Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
            .requiredWidth(maxWidth + inset).offset(x = -inset).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (back != null) {
                IconButton(back, modifier = Modifier.size(48.dp)) {
                    Icon(GymIcons.Back, backLabel ?: stringResource(R.string.common_back),
                        modifier = Modifier.size(24.dp), tint = GymColors.primary)
                }
            }
            Text(title, modifier = Modifier.weight(1f).semantics { heading() },
                style = titleStyle.let {
                    if (LocalConfiguration.current.screenWidthDp < 350 && titleStyle == MaterialTheme.typography.headlineLarge) {
                        it.copy(fontSize = 25.sp, lineHeight = 31.25.sp)
                    } else it
                }, color = GymColors.onSurface)
            actions()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Selector(
    labels: List<String>, selected: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
) {
    if (labels.isEmpty()) return
    val colors = GymColors
    val focus = remember(labels.size) { List(labels.size) { FocusRequester() } }
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selected == index, onClick = { onSelect(index) }, enabled = enabled,
                modifier = Modifier.weight(1f).fillMaxHeight().sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .focusRequester(focus[index]).onPreviewKeyEvent { event ->
                        if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val next = when (event.key) {
                            Key.DirectionLeft -> (index + labels.size - 1) % labels.size
                            Key.DirectionRight -> (index + 1) % labels.size
                            Key.MoveHome -> 0
                            Key.MoveEnd -> labels.lastIndex
                            else -> return@onPreviewKeyEvent false
                        }
                        onSelect(next)
                        focus[next].requestFocus()
                        true
                    },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size, CircleShape),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.tonalContainer, activeContentColor = colors.primary,
                    activeBorderColor = colors.outline, inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = colors.primary, inactiveBorderColor = colors.outline,
                ),
                icon = {}, label = {
                    Column(Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth().height(2.dp)
                            .background(if (selected == index) colors.primary else Color.Transparent))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningTabs(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = GymColors
    val labels = listOf(stringResource(R.string.common_plans), stringResource(R.string.common_exercises))
    SecondaryTabRow(
        selectedTabIndex = selected.coerceIn(labels.indices), modifier = modifier,
        containerColor = Color.Transparent, contentColor = colors.focus,
        divider = { HorizontalDivider(color = colors.outlineVariant) },
        indicator = {
            if (selected in labels.indices) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(selected, matchContentSize = false),
                    height = 2.dp, color = colors.focus,
                )
            }
        },
    ) {
        labels.forEachIndexed { index, label ->
            Tab(selected == index, { onSelect(index) },
                modifier = Modifier.heightIn(min = 48.dp), selectedContentColor = colors.focus,
                unselectedContentColor = colors.onSurfaceVariant) {
                Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun LibraryRow(
    title: String, subtitle: String, badge: String? = null,
    onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val colors = GymColors
    Column(modifier) {
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            color = Color.Transparent, contentColor = colors.onSurface) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold, lineHeight = 18.2.sp))
                    Spacer(Modifier.height(6.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.3.sp),
                        color = colors.onSurfaceVariant)
                    if (badge != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(color = colors.tonalContainer, contentColor = colors.onSurface,
                            shape = CircleShape) {
                            Text(badge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Icon(GymIcons.ChevronRight, null, modifier = Modifier.size(24.dp), tint = colors.primary)
            }
        }
        HorizontalDivider(color = colors.outlineVariant)
    }
}

@Composable
fun Disclosure(
    title: String, modifier: Modifier = Modifier, initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val colors = GymColors
    val state = stringResource(if (expanded) R.string.common_expanded else R.string.common_collapsed)
    val action = stringResource(if (expanded) R.string.common_collapse else R.string.common_expand)
    Column(modifier.padding(vertical = 4.dp)) {
        Surface(onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .semantics { stateDescription = state; onClick(label = action, action = null) },
            shape = RoundedCornerShape(12.dp), color = colors.surfaceContainer,
            contentColor = colors.onSurface) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Box(Modifier.size(48.dp).background(colors.tonalContainer, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(GymIcons.ExpandMore, null, modifier = Modifier.size(24.dp)
                        .rotate(if (expanded) 180f else 0f))
                }
            }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            content()
        }
        HorizontalDivider(color = colors.outlineVariant)
    }
}

@Composable
fun Field(
    label: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, readOnly: Boolean = false,
    singleLine: Boolean = true, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: String? = null, isError: Boolean = false, placeholder: String? = null,
    containerColor: Color? = null,
) {
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        if (readOnly) {
            ReadOnlyValue(label, value)
        } else {
            NativeInput(label, value, onValueChange, enabled = enabled, singleLine = singleLine,
                keyboardOptions = keyboardOptions, isError = isError, placeholder = placeholder,
                containerColor = containerColor)
        }
        FieldHelp(supportingText, isError)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, readOnly: Boolean = false,
    supportingText: String? = null, isError: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val available = enabled && options.isNotEmpty()
    val value = options.getOrNull(selected).orEmpty()
    val colors = GymColors
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        if (readOnly) {
            ReadOnlyValue(label, value)
        } else {
            ExposedDropdownMenuBox(expanded = expanded && available,
                onExpandedChange = { if (available) expanded = it }) {
                NativeInput(label, value, {},
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, available),
                    enabled = available, readOnly = true, isError = isError,
                    horizontalPadding = 16,
                    trailingIcon = {
                        Icon(GymIcons.ExpandMore, null, modifier = Modifier.size(24.dp)
                            .rotate(if (expanded && available) 180f else 0f),
                            tint = colors.onSurfaceVariant)
                    })
                ExposedDropdownMenu(expanded = expanded && available,
                    onDismissRequest = { expanded = false },
                    containerColor = colors.surfaceBright, shape = RoundedCornerShape(12.dp),
                    tonalElevation = 0.dp, border = BorderStroke(1.dp, colors.outlineVariant)) {
                    options.forEachIndexed { index, option ->
                        DropdownMenuItem(text = {
                            Text(option, style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal)
                        }, onClick = { expanded = false; onSelect(index) },
                            trailingIcon = if (selected == index) {
                                { Icon(GymIcons.Check, null, modifier = Modifier.size(24.dp), tint = colors.focus) }
                            } else null,
                            modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp)
                                .background(if (selected == index) colors.tonalContainer else Color.Transparent,
                                    RoundedCornerShape(8.dp)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp))
                    }
                }
            }
        }
        FieldHelp(supportingText, isError)
    }
}

@Composable
private fun FieldLabel(label: String) {
    if (label.isNotEmpty()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = GymColors.onSurface)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ReadOnlyValue(label: String, value: String) {
    SelectionContainer {
        Text(value, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .padding(vertical = 12.dp).semantics { contentDescription = label },
            style = MaterialTheme.typography.bodyLarge, color = GymColors.onSurface)
    }
}

@Composable
private fun FieldHelp(text: String?, isError: Boolean) {
    if (!text.isNullOrEmpty()) {
        Text(text, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else GymColors.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeInput(
    label: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, readOnly: Boolean = false,
    singleLine: Boolean = true, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false, placeholder: String? = null, horizontalPadding: Int = 12,
    trailingIcon: (@Composable () -> Unit)? = null,
    containerColor: Color? = null,
) {
    val colors = GymColors
    val interaction = remember { MutableInteractionSource() }
    val errorMessage = stringResource(R.string.common_invalid_value)
    val fill = containerColor ?: colors.surfaceBright
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fill, unfocusedContainerColor = fill,
        disabledContainerColor = fill.copy(alpha = .38f), errorContainerColor = fill,
        focusedBorderColor = colors.focus, unfocusedBorderColor = colors.outline,
        disabledBorderColor = colors.outline.copy(alpha = .38f),
        focusedTextColor = colors.onSurface, unfocusedTextColor = colors.onSurface,
        cursorColor = colors.focus,
        focusedPlaceholderColor = colors.onSurfaceVariant,
        unfocusedPlaceholderColor = colors.onSurfaceVariant,
    )
    // Material's default 56dp field padding differs from the mockup's 48dp field.
    BasicTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
            .semantics { contentDescription = label; if (isError) error(errorMessage) },
        enabled = enabled, readOnly = readOnly, singleLine = singleLine,
        keyboardOptions = keyboardOptions, interactionSource = interaction,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = colors.onSurface.copy(alpha = if (enabled) 1f else .38f)),
        cursorBrush = SolidColor(colors.focus),
        decorationBox = { inner ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value, innerTextField = inner, enabled = enabled, singleLine = singleLine,
                visualTransformation = VisualTransformation.None, interactionSource = interaction,
                isError = isError, trailingIcon = trailingIcon,
                placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
                contentPadding = PaddingValues(horizontal = horizontalPadding.dp, vertical = 12.dp),
                colors = fieldColors,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled, isError = isError, interactionSource = interaction,
                        colors = fieldColors, shape = RoundedCornerShape(8.dp),
                    )
                },
            )
        },
    )
}
