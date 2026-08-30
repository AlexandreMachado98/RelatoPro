package com.relatopro.app.ui.components.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.relatopro.app.ui.theme.BorderColor
import com.relatopro.app.ui.theme.SurfaceWhite

/**
 * Reusable Shimmer Effect Modifier for Skeleton Loading
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val shimmerColors = listOf(
        Color(0xFFE2E8F0).copy(alpha = 0.6f),
        Color(0xFFF1F5F9),
        Color(0xFFE2E8F0).copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )

    background(brush)
}

/**
 * Skeleton Metric Card for Dashboard
 */
@Composable
fun SkeletonMetricCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(90.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).shimmerEffect())
            }
            Box(modifier = Modifier.width(60.dp).height(28.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Box(modifier = Modifier.width(110.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        }
    }
}

/**
 * Skeleton Report Card for MyReports and History Lists
 */
@Composable
fun SkeletonReportCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().height(110.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(180.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Box(modifier = Modifier.width(60.dp).height(20.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
            }
            Box(modifier = Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Box(modifier = Modifier.width(80.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
        }
    }
}

/**
 * Skeleton Checklist Item for Checklist Step
 */
@Composable
fun SkeletonChecklistItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().height(90.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(50.dp, 28.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                Box(modifier = Modifier.size(50.dp, 28.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                Box(modifier = Modifier.size(50.dp, 28.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
            }
        }
    }
}
