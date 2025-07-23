package com.example.pixvi.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorPickerBottomSheet(
    title: String,
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // This state now properly controls if the custom picker is shown.
    var isCustomPickerVisible by remember(selectedColor) {
        mutableStateOf(colors.none { it.toArgb() == selectedColor.toArgb() })
    }


    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                colors.forEach { color ->
                    val isSelected = color.toArgb() == selectedColor.toArgb() && !isCustomPickerVisible
                    ColorCircle(
                        color = color,
                        isSelected = isSelected,
                        onClick = {
                            isCustomPickerVisible = false
                            onColorSelected(color)
                        }
                    )
                }
                AddCustomColorCircle(
                    isSelected = isCustomPickerVisible,
                    onClick = { isCustomPickerVisible = true }
                )
            }

            AnimatedVisibility(visible = isCustomPickerVisible) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                    CustomColorPicker(
                        // Use selectedColor as the initial color for the sliders
                        initialColor = selectedColor,
                        onColorChanged = { newColor -> onColorSelected(newColor) }
                    )
                }
            }
        }
    }
}


@Composable
private fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val colorDescription = "Color with hex value #${color.value.toULong().toString(16)}"
    val selectionDescription = if (isSelected) ", selected" else ", not selected"

    Box(
        modifier = Modifier
            .size(50.dp)
            .semantics {
                contentDescription = colorDescription + selectionDescription
            }
            .clip(CircleShape)
            .background(color)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                    alpha = 0.5f
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isSelected) {
            val checkmarkColor = if (color.luminance() > 0.5f) Color.Black else Color.White
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null, // Description is handled by the parent Box
                tint = checkmarkColor
            )
        }
    }
}

@Composable
private fun AddCustomColorCircle(isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Select a custom color",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CustomColorPicker(initialColor: Color, onColorChanged: (Color) -> Unit) {
    // These states are now the single source of truth during interaction.
    val hsv = remember { FloatArray(3) }
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0f) }
    var value by remember { mutableStateOf(0f) }

    // This LaunchedEffect now ONLY runs when the initialColor is truly different
    // from the color currently represented by the sliders. This breaks the feedback loop.
    // It will still update if you click a swatch, but not from its own output.
    LaunchedEffect(initialColor) {
        val currentColorFromSliders = Color.hsv(hue, saturation, value)
        if (initialColor != currentColorFromSliders) {
            android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val currentColor = Color.hsv(hue, saturation, value)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(currentColor)
                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        )

        // Hue Slider
        ColorSlider(
            label = "Hue",
            value = hue,
            onValueChange = {
                hue = it
                onColorChanged(Color.hsv(it, saturation, value))
            },
            valueRange = 0f..360f,
            trackBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
            )
        )

        // Saturation Slider
        ColorSlider(
            label = "Saturation",
            value = saturation,
            onValueChange = {
                saturation = it
                onColorChanged(Color.hsv(hue, it, value))
            },
            trackBrush = Brush.horizontalGradient(
                colors = listOf(Color.hsv(hue, 0f, value), Color.hsv(hue, 1f, value))
            )
        )

        // Brightness/Value Slider
        ColorSlider(
            label = "Brightness",
            value = value,
            onValueChange = {
                value = it
                onColorChanged(Color.hsv(hue, saturation, it))
            },
            trackBrush = Brush.horizontalGradient(
                colors = listOf(Color.hsv(hue, saturation, 0f), Color.hsv(hue, saturation, 1f))
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    trackBrush: Brush
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        // Use the official Material3 Slider for proper theming and touch targets
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            // The track is now a composable lambda
            track = { sliderState ->
                // The Box now holds both the gradient and the transparent M3 track.
                // It ensures they are laid out correctly on top of each other.
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp), // Use the standard 4.dp track height
                    contentAlignment = Alignment.CenterStart
                ) {
                    // This is the custom gradient track that fills the space
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .background(trackBrush, CircleShape)
                    )
                    // This is the invisible M3 track that provides the correct state (e.g. for ticks)
                    // We draw it on top of our gradient but make it transparent.
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.fillMaxSize(),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            activeTickColor = Color.White.copy(alpha = 0.5f),
                            inactiveTickColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            },
            thumb = {
                // Use the official M3 thumb for correct sizing and behavior
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = SliderDefaults.colors(thumbColor = Color.White)
                )
            }
        )
    }
}

// Utility function to determine text/icon color based on background
fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

val pageColorPalette = listOf(
    Color.Black,
    Color(0xFF4E342E), // Dark Brown (Sepia-like)
    Color(0xFFFDF5E6), // Old Paper
    Color(0xFFF5F5F5), // Off-white
)

val textColorPalette = listOf(
    Color.White.copy(alpha = 0.95f),
    Color(0xFFE0E0E0), // Light Grey
    Color.Black.copy(alpha = 0.9f),
    Color(0xFF3E2723)  // Dark Brown
)