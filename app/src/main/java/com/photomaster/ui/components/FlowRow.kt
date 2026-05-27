package com.photomaster.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import kotlin.math.max

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: androidx.compose.foundation.layout.Arrangement.Horizontal = androidx.compose.foundation.layout.Arrangement.Start,
    verticalArrangement: androidx.compose.foundation.layout.Arrangement.Vertical = androidx.compose.foundation.layout.Arrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 0
        val vGapPx = 0

        val rows = mutableListOf<List<Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        val itemConstraints = constraints.copy(minWidth = 0)

        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(itemConstraints)

            if (currentRow.isNotEmpty() &&
                (currentRowWidth + hGapPx + placeable.width > constraints.maxWidth ||
                        currentRow.size >= maxItemsInEachRow)) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)

                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += if (currentRow.size > 1) hGapPx + placeable.width else placeable.width
            currentRowHeight = max(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
        }

        val width = rowWidths.maxOrNull()?.coerceIn(constraints.minWidth, constraints.maxWidth)
            ?: constraints.minWidth
        val height = rowHeights.sumOf { it } + (rows.size - 1).coerceAtLeast(0) * vGapPx
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            var y = 0

            rows.forEachIndexed { rowIndex, row ->
                var x = when (horizontalArrangement) {
                    androidx.compose.foundation.layout.Arrangement.Center -> (width - rowWidths[rowIndex]) / 2
                    androidx.compose.foundation.layout.Arrangement.End -> width - rowWidths[rowIndex]
                    else -> 0
                }

                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGapPx
                }

                y += rowHeights[rowIndex] + vGapPx
            }
        }
    }
}

@Composable
fun FlowColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: androidx.compose.foundation.layout.Arrangement.Vertical = androidx.compose.foundation.layout.Arrangement.Top,
    horizontalArrangement: androidx.compose.foundation.layout.Arrangement.Horizontal = androidx.compose.foundation.layout.Arrangement.Start,
    maxItemsInEachColumn: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 0
        val vGapPx = 0

        val columns = mutableListOf<List<Placeable>>()
        val columnHeights = mutableListOf<Int>()
        val columnWidths = mutableListOf<Int>()

        val itemConstraints = constraints.copy(minHeight = 0)

        var currentColumn = mutableListOf<Placeable>()
        var currentColumnHeight = 0
        var currentColumnWidth = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(itemConstraints)

            if (currentColumn.isNotEmpty() &&
                (currentColumnHeight + vGapPx + placeable.height > constraints.maxHeight ||
                        currentColumn.size >= maxItemsInEachColumn)) {
                columns.add(currentColumn)
                columnHeights.add(currentColumnHeight)
                columnWidths.add(currentColumnWidth)

                currentColumn = mutableListOf()
                currentColumnHeight = 0
                currentColumnWidth = 0
            }

            currentColumn.add(placeable)
            currentColumnHeight += if (currentColumn.size > 1) vGapPx + placeable.height else placeable.height
            currentColumnWidth = max(currentColumnWidth, placeable.width)
        }

        if (currentColumn.isNotEmpty()) {
            columns.add(currentColumn)
            columnHeights.add(currentColumnHeight)
            columnWidths.add(currentColumnWidth)
        }

        val height = columnHeights.maxOrNull()?.coerceIn(constraints.minHeight, constraints.maxHeight)
            ?: constraints.minHeight
        val width = columnWidths.sumOf { it } + (columns.size - 1).coerceAtLeast(0) * hGapPx
            .coerceIn(constraints.minWidth, constraints.maxWidth)

        layout(width, height) {
            var x = 0

            columns.forEachIndexed { columnIndex, column ->
                var y = when (verticalArrangement) {
                    androidx.compose.foundation.layout.Arrangement.Center -> (height - columnHeights[columnIndex]) / 2
                    androidx.compose.foundation.layout.Arrangement.Bottom -> height - columnHeights[columnIndex]
                    else -> 0
                }

                column.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    y += placeable.height + vGapPx
                }

                x += columnWidths[columnIndex] + hGapPx
            }
        }
    }
}
