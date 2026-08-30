package com.relatopro.app.ui.components.signature

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.PrimaryBlue
import com.relatopro.app.ui.theme.StatusNaoConforme
import com.relatopro.app.ui.theme.TextSecondary

data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color(0xFF0F172A),
    val strokeWidth: Float = 5f,
)

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureCaptured: (Bitmap) -> Unit,
    onClear: () -> Unit,
) {
    var lines by remember { mutableStateOf(emptyList<Line>()) }
    var currentPosition by remember { mutableStateOf(Offset.Unspecified) }
    var canvasSize by remember { mutableStateOf(IntSize(600, 240)) }

    fun captureSignature(currentLines: List<Line>) {
        if (currentLines.isEmpty()) return
        val w = if (canvasSize.width > 0) canvasSize.width else 600
        val h = if (canvasSize.height > 0) canvasSize.height else 240
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val androidCanvas = AndroidCanvas(bitmap)
        androidCanvas.drawColor(android.graphics.Color.WHITE)

        val paint = AndroidPaint().apply {
            color = android.graphics.Color.parseColor("#0F172A")
            strokeWidth = 6f
            style = AndroidPaint.Style.STROKE
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
            isAntiAlias = true
        }

        for (line in currentLines) {
            androidCanvas.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, paint)
        }

        onSignatureCaptured(bitmap)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.5.dp, if (lines.isNotEmpty()) PrimaryBlue.copy(alpha = 0.5f) else Color.LightGray, RoundedCornerShape(8.dp))
                .clipToBounds()
                .onSizeChanged { canvasSize = it },
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPosition = offset
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val start = currentPosition
                                val end = currentPosition + dragAmount
                                lines = lines + Line(start, end)
                                currentPosition = end
                            },
                            onDragEnd = {
                                currentPosition = Offset.Unspecified
                                captureSignature(lines)
                            }
                        )
                    }
            ) {
                lines.forEach { line ->
                    drawLine(
                        color = line.color,
                        start = line.start,
                        end = line.end,
                        strokeWidth = line.strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (lines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✍️ Assine aqui",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lines.isNotEmpty()) {
                TextButton(
                    onClick = {
                        lines = emptyList()
                        onClear()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = StatusNaoConforme, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Limpar", color = StatusNaoConforme, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Text(
                text = if (lines.isNotEmpty()) "Assinatura capturada automaticamente ✓" else "Toque e arraste para assinar",
                color = if (lines.isNotEmpty()) Color(0xFF16A34A) else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (lines.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
