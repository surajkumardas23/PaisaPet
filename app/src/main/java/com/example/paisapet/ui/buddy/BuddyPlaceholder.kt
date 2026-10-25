package com.example.paisapet.ui.buddy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.example.paisapet.ui.theme.BuddyGreen
import com.example.paisapet.ui.theme.InkText

private val Cheek = Color(0x99FF8FA3)

/**
 * PLACEHOLDER: Phase 7 replaces this with the real art.
 * The only place Buddy is drawn: a green circle with eyes, cheeks and a smile.
 */
@Composable
fun BuddyPlaceholder(size: Dp, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Buddy" },
    ) {
        val r = this.size.minDimension / 2
        val c = center
        drawCircle(BuddyGreen, r, c)
        drawCircle(Color.White.copy(alpha = 0.3f), r * 0.2f, Offset(c.x - r * 0.4f, c.y - r * 0.5f))

        val eyeY = c.y - r * 0.08f
        val eyeDx = r * 0.32f
        for (side in listOf(-1f, 1f)) {
            val eye = Offset(c.x + side * eyeDx, eyeY)
            drawCircle(InkText, r * 0.13f, eye)
            drawCircle(Color.White, r * 0.045f, Offset(eye.x - r * 0.04f, eye.y - r * 0.05f))
            drawCircle(Cheek, r * 0.1f, Offset(c.x + side * r * 0.56f, c.y + r * 0.14f))
        }

        val smileWidth = r * 0.36f
        drawArc(
            color = InkText,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(c.x - smileWidth / 2, c.y + r * 0.02f),
            size = Size(smileWidth, r * 0.26f),
            style = Stroke(width = r * 0.07f, cap = StrokeCap.Round),
        )
    }
}
