package com.cryptic.pixvi.artwork.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A circular page indicator that shows progress as a filling boundary
 * and the current page number in the center.
 * Includes a semi-transparent background for better visibility.
 */
@Composable
fun CircularPageProgressIndicator(
    currentPage: Int,     // Raw integer
    pageCount: Int,       // Raw integer
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    strokeWidth: Dp = 2.8.dp,
    currentPageProgressColor: Color = Color.White,
    remainingPagesProgressColor: Color = Color.Magenta,
    trackColor: Color = Color.White.copy(alpha = 0.45f),
    textColor: Color = Color.White,
    indicatorBackgroundColor: Color = Color.Black.copy(alpha = 0.4f),
    textStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
) {
    if (pageCount <= 1) return

    var showCurrentPageNumber by remember { mutableStateOf(true) }

    val textMeasurer = rememberTextMeasurer()

    val textToDraw = if (showCurrentPageNumber) {
        "${currentPage + 1}"
    } else {
        val remaining = pageCount - (currentPage + 1)
        "$remaining"
    }

    val textLayoutResult = remember(textToDraw, textStyle) {
        textMeasurer.measure(textToDraw, style = textStyle)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { showCurrentPageNumber = !showCurrentPageNumber }
            )
    ) {
        val _strokeWidthPx = strokeWidth.toPx()
        val canvasCenter = this.center

        // 1. Draw the background circle
        drawCircle(
            color = indicatorBackgroundColor,
            radius = size.toPx() / 2f,
            center = canvasCenter
        )

        val arcDiameter = this.size.minDimension - _strokeWidthPx
        val arcTopLeft = Offset(
            canvasCenter.x - arcDiameter / 2f,
            canvasCenter.y - arcDiameter / 2f
        )
        val arcSize = Size(arcDiameter, arcDiameter)

        // 2. Draw the background track arc
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = _strokeWidthPx)
        )

        // 3. Draw the progress arc
        // Determine the color based on the display state
        val currentArcColor = if (showCurrentPageNumber) {
            currentPageProgressColor
        } else {
            remainingPagesProgressColor
        }

        // The progress sweep angle still represents the current page's progress,
        // or you might want it to represent remaining pages.
        // For this example, we'll keep it reflecting current page progress,
        // as changing the arc length based on "remaining" might be confusing
        // if the text shows "5 remaining" but the arc shows 1/5th progress.
        // If you want the arc to show remaining progress, you'd adjust `progressSweepAngle` too.
        val progressSweepAngle = 360f * (currentPage + 1).toFloat() / pageCount.toFloat()

        drawArc(
            color = currentArcColor, // Use the dynamically selected color
            startAngle = -90f,
            sweepAngle = progressSweepAngle,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = _strokeWidthPx, cap = StrokeCap.Round)
        )

        // 4. Draw the page number text
        val textX = canvasCenter.x - (textLayoutResult.size.width / 2f)
        val textY = canvasCenter.y - (textLayoutResult.size.height / 2f)
        drawText(
            textLayoutResult = textLayoutResult,
            color = textColor,
            topLeft = Offset(textX, textY)
        )
    }
}