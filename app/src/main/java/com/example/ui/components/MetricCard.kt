package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.MetricItem
import com.example.models.TrendDirection
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricCard(
    metric: MetricItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = NothingCardBg
        ),
        border = BorderStroke(1.2.dp, NothingBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Metric Category / Label
            Text(
                text = metric.label,
                color = NothingGrey,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Numeric reading container
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.alignByBaseline()
                ) {
                    Text(
                        text = String.format("%.1f", metric.value),
                        color = NothingWhite,
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = metric.unit,
                        color = NothingGrey,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                
                // Mini Trend Graphic Badge with custom state-driven conditional colors
                TrendBadge(trend = metric.trend)
            }
        }
    }
}

@Composable
fun TrendBadge(trend: TrendDirection) {
    val (icon, tintColor, text) = when (trend) {
        TrendDirection.POSITIVE -> Triple(Icons.Filled.KeyboardArrowUp, MetricUpTrend, "UP")
        TrendDirection.NEGATIVE -> Triple(Icons.Filled.KeyboardArrowDown, MetricDownTrend, "DOWN")
        TrendDirection.NEUTRAL -> Triple(null, NothingGrey, "STABLE")
    }

    Surface(
        color = tintColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Trend direction indicator ($text)",
                    tint = tintColor,
                    modifier = Modifier.size(11.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 2.dp)
                        .background(tintColor)
                )
            }
            Text(
                text = text,
                color = tintColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
