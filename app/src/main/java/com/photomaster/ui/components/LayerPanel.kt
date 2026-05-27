package com.photomaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomaster.data.model.BlendMode
import com.photomaster.data.model.Layer
import com.photomaster.data.model.LayerState
import com.photomaster.data.model.LayerType
import com.photomaster.domain.manager.LayerManager

/**
 * 图层面板组件
 */
@Composable
fun LayerPanel(
    layerState: LayerState,
    layerManager: LayerManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "图层管理",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            // 操作按钮
            Row {
                IconButton(
                    onClick = { layerManager.undo() },
                    enabled = layerManager.canUndo()
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "撤销"
                    )
                }
                IconButton(
                    onClick = { layerManager.redo() },
                    enabled = layerManager.canRedo()
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "重做"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        // 图层列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = layerState.layers.sortedByDescending { it.zIndex },
                key = { it.id }
            ) { layer ->
                LayerItem(
                    layer = layer,
                    isSelected = layer.id == layerState.selectedLayerId,
                    onClick = { layerManager.selectLayer(layer.id) },
                    onVisibilityToggle = { layerManager.toggleLayerVisibility(layer.id) },
                    onDelete = { layerManager.removeLayer(layer.id) },
                    onDuplicate = { layerManager.duplicateLayer(layer.id) },
                    onMoveUp = { layerManager.moveLayer(layer.id, 1) },
                    onMoveDown = { layerManager.moveLayer(layer.id, -1) },
                    onBringToFront = { layerManager.bringToFront(layer.id) },
                    onSendToBack = { layerManager.sendToBack(layer.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        // 选中图层的属性编辑
        layerState.getSelectedLayer()?.let { selectedLayer ->
            LayerPropertiesEditor(
                layer = selectedLayer,
                onOpacityChange = { layerManager.updateLayerOpacity(selectedLayer.id, it) },
                onBlendModeChange = { layerManager.updateLayerBlendMode(selectedLayer.id, it) }
            )
        }
    }
}

/**
 * 图层列表项
 */
@Composable
private fun LayerItem(
    layer: Layer,
    isSelected: Boolean,
    onClick: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick)
            .alpha(if (layer.visible) 1f else 0.5f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图层类型图标
            Icon(
                imageVector = when (layer.type) {
                    LayerType.IMAGE -> Icons.Default.Image
                    LayerType.TEXT -> Icons.Default.TextFields
                    LayerType.STICKER -> Icons.Default.EmojiEmotions
                    LayerType.SHAPE -> Icons.Default.ShapeLine
                    LayerType.ADJUSTMENT -> Icons.Default.Tune
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 图层名称
            Text(
                text = layer.getDisplayName(),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // 不透明度指示
            Text(
                text = "${(layer.opacity * 100).toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 可见性切换
            IconButton(
                onClick = onVisibilityToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (layer.visible)
                        Icons.Default.Visibility
                    else
                        Icons.Default.VisibilityOff,
                    contentDescription = if (layer.visible) "隐藏" else "显示",
                    modifier = Modifier.size(18.dp)
                )
            }

            // 更多操作菜单
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("上移一层") },
                        onClick = {
                            onMoveUp()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowUpward, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("下移一层") },
                        onClick = {
                            onMoveDown()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowDownward, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("置于顶层") },
                        onClick = {
                            onBringToFront()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.VerticalAlignTop, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("置于底层") },
                        onClick = {
                            onSendToBack()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.VerticalAlignBottom, null)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("复制图层") },
                        onClick = {
                            onDuplicate()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, null)
                        }
                    )
                    if (!layer.isLocked) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("删除图层") },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图层属性编辑器
 */
@Composable
private fun LayerPropertiesEditor(
    layer: Layer,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (BlendMode) -> Unit
) {
    var showBlendModeMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "图层属性",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 不透明度滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "不透明度",
                fontSize = 12.sp,
                modifier = Modifier.width(60.dp)
            )
            Slider(
                value = layer.opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(layer.opacity * 100).toInt()}%",
                fontSize = 12.sp,
                modifier = Modifier.width(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 混合模式选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "混合模式",
                fontSize = 12.sp,
                modifier = Modifier.width(60.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showBlendModeMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(getBlendModeDisplayName(layer.blendMode))
                }

                DropdownMenu(
                    expanded = showBlendModeMenu,
                    onDismissRequest = { showBlendModeMenu = false }
                ) {
                    BlendMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(getBlendModeDisplayName(mode)) },
                            onClick = {
                                onBlendModeChange(mode)
                                showBlendModeMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 获取混合模式显示名称
 */
private fun getBlendModeDisplayName(mode: BlendMode): String {
    return when (mode) {
        BlendMode.NORMAL -> "正常"
        BlendMode.MULTIPLY -> "正片叠底"
        BlendMode.SCREEN -> "滤色"
        BlendMode.OVERLAY -> "叠加"
        BlendMode.SOFT_LIGHT -> "柔光"
        BlendMode.HARD_LIGHT -> "强光"
        BlendMode.COLOR_DODGE -> "颜色减淡"
        BlendMode.COLOR_BURN -> "颜色加深"
        BlendMode.DARKEN -> "变暗"
        BlendMode.LIGHTEN -> "变亮"
        BlendMode.DIFFERENCE -> "差值"
        BlendMode.EXCLUSION -> "排除"
    }
}

/**
 * 图层工具栏
 */
@Composable
fun LayerToolbar(
    onAddTextLayer: () -> Unit,
    onAddStickerLayer: () -> Unit,
    onMergeVisibleLayers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 添加文字图层
        OutlinedButton(
            onClick = onAddTextLayer
        ) {
            Icon(
                imageVector = Icons.Default.TextFields,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("文字", fontSize = 12.sp)
        }

        // 添加贴纸图层
        OutlinedButton(
            onClick = onAddStickerLayer
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEmotions,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("贴纸", fontSize = 12.sp)
        }

        // 合并可见图层
        OutlinedButton(
            onClick = onMergeVisibleLayers
        ) {
            Icon(
                imageVector = Icons.Default.MergeType,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("合并", fontSize = 12.sp)
        }
    }
}
