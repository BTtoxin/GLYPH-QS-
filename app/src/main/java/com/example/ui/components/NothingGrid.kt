package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun NothingCSSGrid(
    modifier: Modifier = Modifier,
    columns: Int = 2,
    spacing: Dp = 10.dp,
    content: @Composable NothingGridScope.() -> Unit
) {
    val scope = NothingGridScope()
    scope.content()
    val items = scope.gridItems
    
    val rows = mutableListOf<List<GridItemData>>()
    var currentRow = mutableListOf<GridItemData>()
    var currentSpanSum = 0
    
    for (item in items) {
        val span = minOf(item.colSpan, columns)
        if (currentSpanSum + span > columns) {
            rows.add(currentRow)
            currentRow = mutableListOf()
            currentSpanSum = 0
        }
        currentRow.add(item)
        currentSpanSum += span
    }
    if (currentRow.isNotEmpty()) {
        rows.add(currentRow)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NothingBlack)
            .border(BorderStroke(1.2.dp, NothingBorder), shape = RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                for (item in row) {
                    val weight = item.colSpan.toFloat()
                    Box(
                        modifier = Modifier
                            .weight(weight)
                    ) {
                        item.content()
                    }
                }
                
                val rowSpanSum = row.sumOf { it.colSpan }
                if (rowSpanSum < columns) {
                    Spacer(
                        modifier = Modifier.weight((columns - rowSpanSum).toFloat())
                    )
                }
            }
        }
    }
}

class GridItemData(
    val colSpan: Int,
    val content: @Composable () -> Unit
)

class NothingGridScope {
    val gridItems = mutableListOf<GridItemData>()
    
    fun item(colSpan: Int = 1, content: @Composable () -> Unit) {
        gridItems.add(GridItemData(colSpan, content))
    }
}
