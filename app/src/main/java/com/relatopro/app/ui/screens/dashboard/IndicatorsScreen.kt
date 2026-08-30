package com.relatopro.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relatopro.app.ui.theme.*

@Composable
fun IndicatorsScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                }
                Text("Indicadores", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            
            Box(
                modifier = Modifier
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Últimos 30 dias", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
        HorizontalDivider(color = BorderColor)
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IndicatorCard(Modifier.weight(1f), "Relatórios", "32", "+12%", StatusConforme)
                    IndicatorCard(Modifier.weight(1f), "Concluídos", "24", "+8%", StatusConforme)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IndicatorCard(Modifier.weight(1f), "Não Conformes", "7", "-5%", StatusNaoConforme)
                    IndicatorCard(Modifier.weight(1f), "Pendentes", "3", "+2%", StatusConforme)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                LineChartCardFullWidth()
            }
        }
    }
}

@Composable
fun IndicatorCard(modifier: Modifier, title: String, value: String, percentage: String, percentColor: Color) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text(percentage, color = percentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun LineChartCardFullWidth() {
    Card(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("Relatórios por período", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = listOf(10f, 15f, 25f, 18f, 40f, 25f, 20f)
                    val maxPoint = 40f
                    val stepX = size.width / (points.size - 1)
                    
                    val path = Path()
                    val fillPath = Path()
                    
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - (value / maxPoint * size.height)
                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, size.height)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                        
                        if (index == points.lastIndex) {
                            fillPath.lineTo(x, size.height)
                            fillPath.close()
                        }
                    }
                    
                    // Draw filled area
                    drawPath(
                        path = fillPath,
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        style = Fill
                    )
                    
                    // Draw Line
                    drawPath(
                        path = path,
                        color = PrimaryBlue,
                        style = Stroke(width = 3f.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Draw Points
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - (value / maxPoint * size.height)
                        drawCircle(color = SurfaceWhite, radius = 6f.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = PrimaryBlue, radius = 4f.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            // X-Axis Labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("01/08", "06/08", "12/08", "18/08", "24/08", "30/08").forEach { label ->
                    Text(label, color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}
