package com.cryptic.piyek.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.cryptic.piyek.R
import com.cryptic.piyek.feature.home.presentation.NavOptions

@Composable
fun VerticalHomeNavigator(
    currentWindow: NavOptions,
    availableWindow: List<NavOptions>,
    onWindowChange: (NavOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemSize = 48.dp
    val itemSpacing = 10.dp
    val verticalPadding = 16.dp

    val selectedIndex = availableWindow.indexOf(currentWindow).coerceAtLeast(0)

    val indicatorOffsetDp by animateDpAsState(
        targetValue = verticalPadding + selectedIndex * (itemSize + itemSpacing),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_indicator_offset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(34.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 5.dp)
    ) {
        // Active indicator pill placed in the layer behind the icons
        Box(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = indicatorOffsetDp.roundToPx()) }
                .size(itemSize)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                )
        )

        // Static column of IconButtons in the top layer
        Column(
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.padding(vertical = verticalPadding)
        ) {
            availableWindow.forEach { option ->
                val isSelected = option == currentWindow
                IconButton(
                    onClick = { onWindowChange(option) },
                    modifier = Modifier.size(itemSize),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (isSelected)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = when (option) {
                            is NavOptions.ILLust -> ImageVector.vectorResource(R.drawable.illust_represention)
                            is NavOptions.Manga -> ImageVector.vectorResource(R.drawable.manga_represention)
                            is NavOptions.Novel -> Icons.Outlined.Book
                        },
                        contentDescription = option.toString()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerticalHomeNavigatorPreview() {
    val options = remember {
        listOf(
            NavOptions.ILLust,
            NavOptions.Manga,
            NavOptions.Novel
        )
    }
    var selectedOption by remember { mutableStateOf<NavOptions>(NavOptions.ILLust) }

    Box(modifier = Modifier.padding(24.dp)) {
        VerticalHomeNavigator(
            currentWindow = selectedOption,
            availableWindow = options,
            onWindowChange = { selectedOption = it }
        )
    }
}