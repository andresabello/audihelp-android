package com.audihelp.app.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.HearingThreshold
import kotlin.math.log10

private val AUDIOGRAM_FREQS = listOf(250, 500, 1000, 2000, 4000, 8000)
private const val DB_TOP = -80f    // top of chart = quietest = best hearing
private const val DB_BOTTOM = 0f   // bottom of chart = loudest = most loss
private const val DB_RANGE = DB_BOTTOM - DB_TOP   // 80f

// Audiogram zones: from top (normal) to bottom (profound loss)
// Our dBFS scale: -80 = perfect, 0 = profound
private val ZONE_NORMAL    = Color(0xFFE8F5E9)  // -80 to -70: green
private val ZONE_MILD      = Color(0xFFF9FBE7)  // -70 to -55: light yellow-green
private val ZONE_MODERATE  = Color(0xFFFFF9C4)  // -55 to -40: yellow
private val ZONE_SEVERE    = Color(0xFFFFE0B2)  // -40 to -20: orange
private val ZONE_PROFOUND  = Color(0xFFFFCDD2)  // -20 to  0: red

private val COLOR_RIGHT = Color(0xFFD32F2F) // red — right ear (circles)
private val COLOR_LEFT  = Color(0xFF1976D2) // blue — left ear (X marks)

private fun freqFraction(freq: Int): Float {
    val logMin = log10(250f)
    val logMax = log10(8000f)
    return (log10(freq.toFloat()) - logMin) / (logMax - logMin)
}

private fun dbFraction(db: Float): Float = (db - DB_TOP) / DB_RANGE

/**
 * Standard audiogram chart.
 *
 * X axis: frequency on a log scale (250 → 8000 Hz).
 * Y axis: dBFS threshold, -80 at top (normal hearing) → 0 at bottom (profound loss).
 *
 * Right ear: red circles (O)
 * Left  ear: blue X marks (×)
 *
 * Background zones indicate severity of hearing loss.
 */
@Composable
fun AudigramChart(
    thresholds: List<HearingThreshold>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 4.dp),
    ) {
        val padL = 52f
        val padR = 12f
        val padT = 20f
        val padB = 36f
        val chartW = size.width - padL - padR
        val chartH = size.height - padT - padB

        fun xOf(freq: Int)  = padL + freqFraction(freq) * chartW
        fun yOf(db: Float)  = padT + dbFraction(db) * chartH

        // ── Background zones ──────────────────────────────────────────────────
        data class Zone(val topDb: Float, val botDb: Float, val color: Color)
        listOf(
            Zone(-80f, -70f, ZONE_NORMAL),
            Zone(-70f, -55f, ZONE_MILD),
            Zone(-55f, -40f, ZONE_MODERATE),
            Zone(-40f, -20f, ZONE_SEVERE),
            Zone(-20f,   0f, ZONE_PROFOUND),
        ).forEach { z ->
            val top = yOf(z.topDb)
            val bot = yOf(z.botDb)
            drawRect(z.color, topLeft = Offset(padL, top), size = Size(chartW, bot - top))
        }

        // ── Grid ──────────────────────────────────────────────────────────────
        val gridColor = Color(0xFFBDBDBD)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 24f
            color = android.graphics.Color.parseColor("#616161")
        }

        // Vertical frequency lines + labels
        AUDIOGRAM_FREQS.forEach { freq ->
            val x = xOf(freq)
            drawLine(gridColor, Offset(x, padT), Offset(x, padT + chartH), strokeWidth = 1f)
            val label = if (freq >= 1000) "${freq / 1000}k" else "$freq"
            drawContext.canvas.nativeCanvas.drawText(label, x - 14f, padT + chartH + 28f, paint)
        }

        // Horizontal dB lines + labels
        listOf(0f, -10f, -20f, -30f, -40f, -50f, -60f, -70f, -80f).forEach { db ->
            val y = yOf(db)
            drawLine(gridColor, Offset(padL, y), Offset(padL + chartW, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText("${db.toInt()}", 4f, y + 8f, paint)
        }

        // Chart border
        drawRect(
            Color(0xFF424242),
            topLeft = Offset(padL, padT),
            size = Size(chartW, chartH),
            style = Stroke(2f),
        )

        // Axis labels
        val smallPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 19f
            color = android.graphics.Color.parseColor("#9E9E9E")
        }
        drawContext.canvas.nativeCanvas.drawText("Hz", padL + chartW - 16f, padT + chartH + 28f, smallPaint)
        drawContext.canvas.nativeCanvas.drawText("dBFS", 0f, padT + 10f, smallPaint)

        // ── Threshold data per ear ────────────────────────────────────────────
        listOf(EarSide.RIGHT to COLOR_RIGHT, EarSide.LEFT to COLOR_LEFT).forEach { (ear, color) ->
            val pts = thresholds.filter { it.ear == ear }.sortedBy { it.frequencyHz }
            if (pts.isEmpty()) return@forEach

            // Connecting line
            if (pts.size > 1) {
                val path = Path()
                pts.forEachIndexed { i, t ->
                    if (i == 0) path.moveTo(xOf(t.frequencyHz), yOf(t.levelDbfs))
                    else path.lineTo(xOf(t.frequencyHz), yOf(t.levelDbfs))
                }
                drawPath(path, color, style = Stroke(width = 2.5f))
            }

            // Symbols at each data point
            pts.forEach { t ->
                val cx = xOf(t.frequencyHz)
                val cy = yOf(t.levelDbfs)
                val r = 10f

                // Dashed/hollow symbols for BARELY_HEAR; dotted for FEEL; skip for NOTHING (draw ×)
                when (t.response) {
                    TrialResponse.NOTHING -> {
                        // Strike-through dash — couldn't hear at all
                        drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 3f)
                    }
                    else -> {
                        if (ear == EarSide.RIGHT) {
                            // Circle (O)
                            val filled = t.response == TrialResponse.HEAR_CLEARLY
                            if (filled) drawCircle(color.copy(alpha = 0.3f), radius = r, center = Offset(cx, cy))
                            drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(2.5f))
                        } else {
                            // X mark
                            drawLine(color, Offset(cx - r, cy - r), Offset(cx + r, cy + r), strokeWidth = 2.5f)
                            drawLine(color, Offset(cx + r, cy - r), Offset(cx - r, cy + r), strokeWidth = 2.5f)
                        }
                    }
                }
            }
        }

        // ── Legend ────────────────────────────────────────────────────────────
        val legendPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 22f
        }
        // Right ear legend (circle)
        legendPaint.color = COLOR_RIGHT.hashCode()
        drawCircle(COLOR_RIGHT, radius = 7f, center = Offset(padL + 8f, padT - 8f), style = Stroke(2f))
        legendPaint.color = android.graphics.Color.parseColor("#D32F2F")
        drawContext.canvas.nativeCanvas.drawText("R", padL + 20f, padT - 2f, legendPaint)

        // Left ear legend (X)
        drawLine(COLOR_LEFT, Offset(padL + 46f, padT - 15f), Offset(padL + 60f, padT - 1f), strokeWidth = 2f)
        drawLine(COLOR_LEFT, Offset(padL + 60f, padT - 15f), Offset(padL + 46f, padT - 1f), strokeWidth = 2f)
        legendPaint.color = android.graphics.Color.parseColor("#1976D2")
        drawContext.canvas.nativeCanvas.drawText("L", padL + 66f, padT - 2f, legendPaint)
    }
}
