package com.example.pixvi.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
/**
 * A controlled vertical slider for adjusting line spacing, built correctly
 * for the state-driven VerticalSlider API by directly manipulating the state's `value` property.
 *
 * @param value The current value of the slider from the external state (ViewModel).
 * @param onValueChange A lambda invoked when the slider's value changes.
 * @param onValueChangeFinished A lambda invoked when the user stops dragging the slider.
 * @param modifier The modifier to be applied to the slider container.
 * @param sliderHeight The final height (length) of the vertical slider component.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageLineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    sliderHeight: Dp,
    onInteraction: (() -> Unit)? = null
) {
    // 1. Create the SliderState. The `value` parameter here is ONLY for the initial value
    // on first composition. It will not update the slider later.
    val sliderState = rememberSliderState(
        value = value,
        valueRange = 1.0f..2.0f,
        onValueChangeFinished = {
            onValueChangeFinished()
            onInteraction?.invoke() // Reset timer when interaction finishes
        }
    )

    // 2. Downstream Sync (ViewModel -> UI)
    // This effect runs whenever the `value` from the ViewModel changes.
    // We update the slider's internal state to match.
    LaunchedEffect(value) {
        if (sliderState.value != value) {
            sliderState.value = value
        }
    }

    LaunchedEffect(sliderState.value) {
        if (value != sliderState.value) {
            onValueChange(sliderState.value)
            onInteraction?.invoke() // Reset timer when value changes
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%.2fx".format(sliderState.value),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        VerticalSlider(
            state = sliderState,
            modifier = Modifier.height(sliderHeight),
            reverseDirection = true,
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    modifier = Modifier.width(16.dp)
                )
            }
        )
    }
}