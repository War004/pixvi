package com.cryptic.piyek.feature.iLLust.presentation.home

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
 * A circular page indicator that displays progress as a filling arc
 * and the current page number (or remaining count) in the center.
 *
 * Tapping toggles between showing the current page number and the
 * number of remaining pages.
 *
 * A semi-transparent backdrop ensures readability over any image.
 */
@Composable
fun CircularPageProgressIndicator(
    currentPage: Int,
    pageCount: Int,
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
        fontWeight = FontWeight.Bold,
    ),
) {
    if (pageCount <= 1) return

    var showCurrentPageNumber by remember { mutableStateOf(true) }

    val textMeasurer = rememberTextMeasurer()

    val textToDraw = if (showCurrentPageNumber) {
        "${currentPage + 1}"
    } else {
        "${pageCount - (currentPage + 1)}"
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
                onClick = { showCurrentPageNumber = !showCurrentPageNumber },
            ),
    ) {
        val strokeWidthPx = strokeWidth.toPx()
        val canvasCenter = this.center

        // 1. Background circle
        drawCircle(
            color = indicatorBackgroundColor,
            radius = size.toPx() / 2f,
            center = canvasCenter,
        )

        val arcDiameter = this.size.minDimension - strokeWidthPx
        val arcTopLeft = Offset(
            canvasCenter.x - arcDiameter / 2f,
            canvasCenter.y - arcDiameter / 2f,
        )
        val arcSize = Size(arcDiameter, arcDiameter)

        // 2. Track arc (full circle, dimmed)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx),
        )

        // 3. Progress arc
        val arcColor = if (showCurrentPageNumber) {
            currentPageProgressColor
        } else {
            remainingPagesProgressColor
        }
        val progressSweepAngle = 360f * (currentPage + 1).toFloat() / pageCount.toFloat()

        drawArc(
            color = arcColor,
            startAngle = -90f,
            sweepAngle = progressSweepAngle,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        )

        // 4. Center text
        val textX = canvasCenter.x - (textLayoutResult.size.width / 2f)
        val textY = canvasCenter.y - (textLayoutResult.size.height / 2f)
        drawText(
            textLayoutResult = textLayoutResult,
            color = textColor,
            topLeft = Offset(textX, textY),
        )
    }
}
