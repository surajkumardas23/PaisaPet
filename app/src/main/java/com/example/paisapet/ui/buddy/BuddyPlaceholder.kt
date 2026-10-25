package com.example.paisapet.ui.buddy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.example.paisapet.budget.BuddyState
import com.example.paisapet.ui.theme.InkText

private val Cheek = Color(0x99FF8FA3)
private val SweatDrop = Color(0xFF6EC6FF)
private val ThermometerRed = Color(0xFFE53935)

/**
 * PLACEHOLDER: Phase 7 replaces this with the real art.
 * The only place Buddy is drawn: a round face whose colour and expression follow [state].
 */
@Composable
fun BuddyPlaceholder(size: Dp, modifier: Modifier = Modifier, state: BuddyState = BuddyState.THRIVING) {
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Buddy, ${buddyName(state)}" },
    ) {
        val r = this.size.minDimension / 2
        val c = center
        drawCircle(buddyBody(state), r, c)
        drawCircle(Color.White.copy(alpha = 0.3f), r * 0.2f, Offset(c.x - r * 0.4f, c.y - r * 0.5f))
        for (side in listOf(-1f, 1f)) {
            drawCircle(Cheek, r * 0.1f, Offset(c.x + side * r * 0.56f, c.y + r * 0.14f))
        }
        when (state) {
            BuddyState.THRIVING -> {
                roundEyes(r, c)
                smile(r, c, width = 0.36f, depth = 0.26f)
            }
            BuddyState.FINE -> {
                roundEyes(r, c)
                smile(r, c, width = 0.24f, depth = 0.12f)
            }
            BuddyState.WORRIED -> {
                roundEyes(r, c)
                line(r, c, from = Offset(-0.16f, 0.22f), to = Offset(0.16f, 0.22f))
                sweatDrop(r, c)
            }
            BuddyState.SICK -> {
                roundEyes(r, c, small = true)
                wavyMouth(r, c)
                thermometer(r, c)
            }
            BuddyState.FAINTED -> {
                xEyes(r, c)
                drawCircle(InkText, r * 0.09f, Offset(c.x, c.y + r * 0.26f), style = Stroke(width = r * 0.06f))
            }
        }
    }
}

private fun DrawScope.roundEyes(r: Float, c: Offset, small: Boolean = false) {
    val eyeR = if (small) r * 0.1f else r * 0.13f
    for (side in listOf(-1f, 1f)) {
        val eye = Offset(c.x + side * r * 0.32f, c.y - r * 0.08f)
        drawCircle(InkText, eyeR, eye)
        drawCircle(Color.White, r * 0.045f, Offset(eye.x - r * 0.04f, eye.y - r * 0.05f))
    }
}

private fun DrawScope.xEyes(r: Float, c: Offset) {
    val d = r * 0.1f
    for (side in listOf(-1f, 1f)) {
        val eye = Offset(c.x + side * r * 0.32f, c.y - r * 0.08f)
        drawLine(InkText, Offset(eye.x - d, eye.y - d), Offset(eye.x + d, eye.y + d), r * 0.06f, StrokeCap.Round)
        drawLine(InkText, Offset(eye.x - d, eye.y + d), Offset(eye.x + d, eye.y - d), r * 0.06f, StrokeCap.Round)
    }
}

private fun DrawScope.smile(r: Float, c: Offset, width: Float, depth: Float) {
    val w = r * width
    drawArc(
        color = InkText,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(c.x - w / 2, c.y + r * 0.02f),
        size = Size(w, r * depth),
        style = Stroke(width = r * 0.07f, cap = StrokeCap.Round),
    )
}

/** [from] and [to] are offsets from the centre in units of the radius. */
private fun DrawScope.line(r: Float, c: Offset, from: Offset, to: Offset) {
    drawLine(
        InkText,
        Offset(c.x + from.x * r, c.y + from.y * r),
        Offset(c.x + to.x * r, c.y + to.y * r),
        strokeWidth = r * 0.07f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.wavyMouth(r: Float, c: Offset) {
    val y = c.y + r * 0.24f
    val step = r * 0.08f
    val path = Path().apply {
        moveTo(c.x - step * 2, y)
        for (i in 0 until 4) {
            val dy = if (i % 2 == 0) -r * 0.05f else r * 0.05f
            quadraticTo(c.x - step * 2 + step * i + step / 2, y + dy, c.x - step * 2 + step * (i + 1), y)
        }
    }
    drawPath(path, InkText, style = Stroke(width = r * 0.06f, cap = StrokeCap.Round))
}

private fun DrawScope.sweatDrop(r: Float, c: Offset) {
    val x = c.x + r * 0.62f
    val y = c.y - r * 0.5f
    val path = Path().apply {
        moveTo(x, y - r * 0.16f)
        quadraticTo(x + r * 0.11f, y, x, y + r * 0.08f)
        quadraticTo(x - r * 0.11f, y, x, y - r * 0.16f)
        close()
    }
    drawPath(path, SweatDrop)
}

/** A thermometer poking out of the mouth's corner. */
private fun DrawScope.thermometer(r: Float, c: Offset) {
    val start = Offset(c.x + r * 0.18f, c.y + r * 0.3f)
    val end = Offset(c.x + r * 0.62f, c.y + r * 0.52f)
    drawLine(Color.White, start, end, strokeWidth = r * 0.1f, cap = StrokeCap.Round)
    drawLine(ThermometerRed, start, Offset((start.x + end.x) / 2, (start.y + end.y) / 2), strokeWidth = r * 0.05f, cap = StrokeCap.Round)
    drawCircle(ThermometerRed, r * 0.07f, start)
}
