package com.lzacademy.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lzacademy.app.theme.LZSerifFamily

/** The LZ Academy monogram: indigo diamond, serif "LZ" and a red swoosh. Drawn natively, no assets. */
@Composable
fun LZLogo(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    val corner = size * 0.12f
    val shape = RoundedCornerShape(corner)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size * 0.72f)
                .rotate(45f)
                .background(Brush.linearGradient(listOf(Color(0xFF4A2D63), Color(0xFF1E1230))), shape)
                .border(2.dp, Color.White.copy(alpha = 0.92f), shape),
        )
        LZText(
            "LZ",
            style = TextStyle(
                fontFamily = LZSerifFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.30f).sp,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        )
        Canvas(Modifier.fillMaxSize()) {
            val p = Path().apply {
                moveTo(this@Canvas.size.width * 0.34f, this@Canvas.size.height * 0.74f)
                quadraticBezierTo(
                    this@Canvas.size.width * 0.52f, this@Canvas.size.height * 0.80f,
                    this@Canvas.size.width * 0.70f, this@Canvas.size.height * 0.55f,
                )
            }
            drawPath(p, Color(0xFFE53935), style = Stroke(width = size.toPx() * 0.04f, cap = StrokeCap.Round))
        }
    }
}

/** Flat illustration of a graduation cap on a stack of books (hero cards, empty states). */
@Composable
fun LZGraduationIllustration(modifier: Modifier = Modifier, size: Dp = 110.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun rr(x: Float, y: Float, bw: Float, bh: Float, color: Color) {
            drawRoundRect(color, Offset(x, y), Size(bw, bh), CornerRadius(h * 0.03f, h * 0.03f))
        }
        // books
        rr(w * 0.10f, h * 0.80f, w * 0.80f, h * 0.14f, Color(0xFF2E7D6B))
        rr(w * 0.16f, h * 0.66f, w * 0.72f, h * 0.14f, Color(0xFFF5B63A))
        rr(w * 0.08f, h * 0.52f, w * 0.76f, h * 0.14f, Color(0xFFF08A3C))
        rr(w * 0.10f, h * 0.575f, w * 0.72f, h * 0.012f, Color.White.copy(alpha = 0.55f))
        rr(w * 0.18f, h * 0.715f, w * 0.68f, h * 0.012f, Color.White.copy(alpha = 0.55f))
        // cap base
        val base = Path().apply {
            moveTo(w * 0.28f, h * 0.30f)
            lineTo(w * 0.28f, h * 0.44f)
            quadraticBezierTo(w * 0.50f, h * 0.55f, w * 0.72f, h * 0.44f)
            lineTo(w * 0.72f, h * 0.30f)
            close()
        }
        drawPath(base, Color(0xFF241533))
        // cap board
        val board = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            lineTo(w * 0.96f, h * 0.28f)
            lineTo(w * 0.50f, h * 0.46f)
            lineTo(w * 0.04f, h * 0.28f)
            close()
        }
        drawPath(board, Brush.linearGradient(listOf(Color(0xFF3B2456), Color(0xFF1E1230))))
        // tassel
        drawLine(
            Color(0xFFF5B63A),
            Offset(w * 0.88f, h * 0.31f), Offset(w * 0.88f, h * 0.50f),
            strokeWidth = w * 0.025f, cap = StrokeCap.Round,
        )
        drawCircle(Color(0xFFF5B63A), w * 0.035f, Offset(w * 0.88f, h * 0.52f))
    }
}
