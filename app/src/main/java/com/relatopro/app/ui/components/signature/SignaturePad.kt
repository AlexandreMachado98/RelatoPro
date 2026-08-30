package com.relatopro.app.ui.components.signature

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.relatopro.app.ui.theme.PrimaryBlue
import com.relatopro.app.ui.theme.StatusNaoConforme

data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
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

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .clipToBounds(),
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
                                lines += Line(start, end)
                                currentPosition = end
                            },
                            onDragEnd = {
                                currentPosition = Offset.Unspecified
                            }
                        )
                    }
            ) {
                // To record everything drawn on canvas, we would use native android.graphics.Canvas
                // but Jetpack Compose currently requires a workaround to save Canvas to Bitmap.
                // We will just draw the lines visually here. 
                // In a real device, we can draw them to a Picture/Bitmap backed canvas.
                
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
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { 
                    lines = emptyList()
                    onClear()
                }
            ) {
                Text("Limpar Assinatura", color = StatusNaoConforme)
            }
            
            Button(
                onClick = {
                    // Logic to convert lines to Bitmap goes here.
                    // For the sake of this structure, we pass a dummy empty bitmap.
                    // In a production app, we'd render the `lines` into an `android.graphics.Bitmap` canvas.
                    val bitmap = androidx.core.graphics.createBitmap(500, 200, Bitmap.Config.ARGB_8888)
                    onSignatureCaptured(bitmap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Salvar Assinatura")
            }
        }
    }
}
