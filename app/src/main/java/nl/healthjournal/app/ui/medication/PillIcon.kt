package nl.healthjournal.app.ui.medication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.healthjournal.domain.model.medication.PillAppearance
import nl.healthjournal.domain.model.medication.PillColor
import nl.healthjournal.domain.model.medication.PillShape

fun PillColor.toColor(): Color = when (this) {
    PillColor.WHITE -> Color(0xFFF5F5F5)
    PillColor.YELLOW -> Color(0xFFFDD835)
    PillColor.ORANGE -> Color(0xFFFB8C00)
    PillColor.RED -> Color(0xFFE53935)
    PillColor.PINK -> Color(0xFFF48FB1)
    PillColor.PURPLE -> Color(0xFF8E24AA)
    PillColor.BLUE -> Color(0xFF1E88E5)
    PillColor.GREEN -> Color(0xFF43A047)
    PillColor.BROWN -> Color(0xFF8D6E63)
    PillColor.GREY -> Color(0xFF9E9E9E)
}

/**
 * The pill the user picked, drawn as a vector. A theme-coloured outline keeps a white pill visible on a light
 * surface and a dark pill visible on a dark one. Decorative: the name and dose next to it always carry the meaning.
 */
@Composable
fun PillIcon(appearance: PillAppearance?, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = appearance?.color?.toColor() ?: Color.Transparent
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 2.dp.toPx())
        val shape = appearance?.shape ?: PillShape.ROUND
        // Each shape is a filled body plus an outline, sized inside the square with room for the stroke.
        val inset = 2.dp.toPx()
        val topLeft: Offset
        val bodySize: Size
        val corner: CornerRadius
        when (shape) {
            PillShape.ROUND, PillShape.OTHER -> {
                topLeft = Offset(inset, inset); bodySize = Size(w - 2 * inset, h - 2 * inset)
                corner = CornerRadius(bodySize.width / 2, bodySize.height / 2)
            }
            PillShape.OVAL -> {
                topLeft = Offset(inset, h * 0.2f); bodySize = Size(w - 2 * inset, h * 0.6f)
                corner = CornerRadius(bodySize.width / 2, bodySize.height / 2)
            }
            PillShape.CAPSULE -> {
                topLeft = Offset(inset, h * 0.3f); bodySize = Size(w - 2 * inset, h * 0.4f)
                corner = CornerRadius(bodySize.height / 2, bodySize.height / 2)
            }
            PillShape.OBLONG -> {
                topLeft = Offset(inset, h * 0.28f); bodySize = Size(w - 2 * inset, h * 0.44f)
                corner = CornerRadius(h * 0.08f, h * 0.08f)
            }
            PillShape.SQUARE -> {
                topLeft = Offset(inset + w * 0.08f, inset + h * 0.08f)
                bodySize = Size(w - 2 * inset - w * 0.16f, h - 2 * inset - h * 0.16f)
                corner = CornerRadius(h * 0.08f, h * 0.08f)
            }
        }
        if (appearance != null) drawRoundRect(fill, topLeft, bodySize, corner)
        drawRoundRect(outline, topLeft, bodySize, corner, style = stroke)
        if (shape == PillShape.CAPSULE) {
            drawLine(outline, Offset(w / 2, topLeft.y), Offset(w / 2, topLeft.y + bodySize.height), strokeWidth = stroke.width)
        }
    }
}
