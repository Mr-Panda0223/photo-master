package com.photomaster.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photomaster.ui.editor.EditorState
import com.photomaster.ui.editor.EditorViewModel
import com.photomaster.ui.editor.ToolType
import com.photomaster.ui.editor.AdjustParams
import com.photomaster.ui.editor.LightShadowParams
import com.photomaster.ui.editor.LocalStretchParams
import com.photomaster.ui.editor.BeautyParams
import com.photomaster.ui.editor.FilterParams
import com.photomaster.ui.editor.CropEdge
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageUri: String,
    draftId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToExport: (String) -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val editorState by viewModel.editorState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    val uri = remember(imageUri) {
        Uri.parse(imageUri)
    }

    // 设置草稿ID（如果是从草稿继续编辑）
    LaunchedEffect(draftId) {
        viewModel.setDraftId(draftId)
    }

    // Load image when screen is first composed
    LaunchedEffect(uri) {
        viewModel.loadImage(uri, context)
    }

    BackHandler {
        showExitDialog = true
    }

    // 删除确认对话框
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑") },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val currentState = editorState
                    if (currentState is EditorState.Ready) {
                        // 上一步 (Undo)
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = currentState.canUndo
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "上一步",
                                tint = if (currentState.canUndo) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        // 下一步 (Redo)
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = currentState.canRedo
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "下一步",
                                tint = if (currentState.canRedo) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                    // 删除按钮（仅当编辑的是已有草稿时显示）
                    if (draftId != null) {
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // 仅保存 - 保存编辑但不导出到相册，保存后返回首页
                    TextButton(
                        onClick = {
                            viewModel.saveImage(
                                context = context,
                                originalUri = uri,
                                onSuccess = { draftId ->
                                    // 保存成功后返回首页
                                    onNavigateBack()
                                },
                                onError = { error ->
                                    // Show error message
                                }
                            )
                        },
                        enabled = editorState is EditorState.Ready
                    ) {
                        Text("保存")
                    }
                    // 导出 - 保存编辑并导出到系统相册
                    TextButton(
                        onClick = {
                            viewModel.exportImage(
                                context = context,
                                originalUri = uri,
                                onSuccess = { exportedUri ->
                                    // 导出成功后返回首页
                                    onNavigateBack()
                                },
                                onError = { error ->
                                    // Show error message
                                }
                            )
                        },
                        enabled = editorState is EditorState.Ready
                    ) {
                        Text("导出")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = editorState) {
                is EditorState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is EditorState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is EditorState.Ready -> {
                    EditorContent(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出编辑") },
            text = { Text("确定要退出编辑吗？未保存的更改将会丢失。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("继续编辑")
                }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除草稿") },
            text = { Text("确定要删除这个编辑中的草稿吗？删除后会移动到已删除列表。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteDraft(
                            context = context,
                            onSuccess = {
                                // 删除成功后返回首页
                                onNavigateBack()
                            },
                            onError = { error ->
                                // 显示错误信息，可以在这里添加 Toast
                            }
                        )
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun EditorContent(
    state: EditorState.Ready,
    viewModel: EditorViewModel
) {
    val textBoxState by viewModel.textBoxState.collectAsState()
    val stickerBoxState by viewModel.stickerBoxState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image preview area with crop overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Show crop overlay when in crop mode
            if (state.currentTool == ToolType.CROP) {
                CropImageView(
                    bitmap = state.currentBitmap,
                    viewModel = viewModel
                )
            } else {
                // Normal image display - 使用直接渲染避免闪烁
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DirectBitmapImage(
                        bitmap = state.currentBitmap,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 显示文本框（当文字工具激活且文本框可见时）
                    if (state.currentTool == ToolType.TEXT && textBoxState.isVisible) {
                        TextBoxOverlay(
                            textBoxState = textBoxState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 显示贴纸框（当贴纸工具激活且贴纸框可见时）
                    if (state.currentTool == ToolType.STICKER && stickerBoxState.isVisible) {
                        StickerBoxOverlay(
                            stickerBoxState = stickerBoxState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (state.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Tools panel
        EditorToolsPanel(
            currentTool = state.currentTool,
            onToolSelected = { viewModel.selectTool(it) },
            viewModel = viewModel
        )
    }
}

/**
 * 直接渲染 Bitmap，避免使用 AsyncImage 的 crossfade 效果
 * 这样可以减少调整时的闪烁感
 */
@Composable
private fun DirectBitmapImage(
    bitmap: android.graphics.Bitmap,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 使用 remember 缓存 ImageBitmap，避免重复创建
    val imageBitmap = remember(bitmap) {
        bitmap.asImageBitmap()
    }
    
    Image(
        bitmap = imageBitmap,
        contentDescription = "编辑中的图片",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun CropImageView(
    bitmap: android.graphics.Bitmap,
    viewModel: EditorViewModel
) {
    var imageSize by remember { mutableStateOf(Size.Zero) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }

    // Calculate the displayed image size and position
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    imageSize = coordinates.size.toSize()
                    imageOffset = coordinates.positionInParent()
                }
        ) {
            // 使用直接渲染避免闪烁
            DirectBitmapImage(
                bitmap = bitmap,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Crop overlay
        CropOverlay(
            imageSize = imageSize,
            viewModel = viewModel
        )
    }
}

@Composable
private fun CropOverlay(
    imageSize: Size,
    viewModel: EditorViewModel
) {
    val cropState by viewModel.cropState.collectAsState()
    var overlaySize by remember { mutableStateOf(Size.Zero) }

    // Initialize crop rect when first composed
    LaunchedEffect(imageSize) {
        if (imageSize.width > 0 && imageSize.height > 0) {
            viewModel.initCropRect(imageSize.width, imageSize.height)
        }
    }

    val cropRect = cropState.cropRect
    if (cropRect.width <= 0 || cropRect.height <= 0) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlaySize = coordinates.size.toSize()
            }
    ) {
        // Dark overlay outside crop area
        with(LocalDensity.current) {
            // Top overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cropRect.top.toDp())
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // Middle section with left and right overlays
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cropRect.height.toDp())
                    .offset(y = cropRect.top.toDp())
            ) {
                // Left overlay
                Box(
                    modifier = Modifier
                        .width(cropRect.left.toDp())
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // Crop frame (transparent center)
                Box(
                    modifier = Modifier
                        .width(cropRect.width.toDp())
                        .fillMaxHeight()
                        .border(2.dp, Color.White)
                )

                // Right overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            // Bottom overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((overlaySize.height - cropRect.bottom).toDp().coerceAtLeast(0.dp))
                    .offset(y = cropRect.bottom.toDp())
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // Corner handles for resizing
            val handleSize = 24.dp
            val handleOffset = (-12).dp

            // Top-left handle
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .offset(
                        x = (cropRect.left + handleOffset.value).dp,
                        y = (cropRect.top + handleOffset.value).dp
                    )
                    .background(Color.White, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.resizeCropRect(
                                dx = dragAmount.x,
                                dy = dragAmount.y,
                                edge = CropEdge.TOP_LEFT,
                                maxWidth = overlaySize.width,
                                maxHeight = overlaySize.height
                            )
                        }
                    }
            )

            // Top-right handle
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .offset(
                        x = (cropRect.right - handleOffset.value - handleSize.value).dp,
                        y = (cropRect.top + handleOffset.value).dp
                    )
                    .background(Color.White, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.resizeCropRect(
                                dx = dragAmount.x,
                                dy = dragAmount.y,
                                edge = CropEdge.TOP_RIGHT,
                                maxWidth = overlaySize.width,
                                maxHeight = overlaySize.height
                            )
                        }
                    }
            )

            // Bottom-left handle
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .offset(
                        x = (cropRect.left + handleOffset.value).dp,
                        y = (cropRect.bottom - handleOffset.value - handleSize.value).dp
                    )
                    .background(Color.White, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.resizeCropRect(
                                dx = dragAmount.x,
                                dy = dragAmount.y,
                                edge = CropEdge.BOTTOM_LEFT,
                                maxWidth = overlaySize.width,
                                maxHeight = overlaySize.height
                            )
                        }
                    }
            )

            // Bottom-right handle
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .offset(
                        x = (cropRect.right - handleOffset.value - handleSize.value).dp,
                        y = (cropRect.bottom - handleOffset.value - handleSize.value).dp
                    )
                    .background(Color.White, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.resizeCropRect(
                                dx = dragAmount.x,
                                dy = dragAmount.y,
                                edge = CropEdge.BOTTOM_RIGHT,
                                maxWidth = overlaySize.width,
                                maxHeight = overlaySize.height
                            )
                        }
                    }
            )

            // Draggable crop area
            Box(
                modifier = Modifier
                    .size(
                        width = cropRect.width.toDp(),
                        height = cropRect.height.toDp()
                    )
                    .offset(
                        x = cropRect.left.toDp(),
                        y = cropRect.top.toDp()
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.moveCropRect(
                                dx = dragAmount.x,
                                dy = dragAmount.y,
                                maxWidth = overlaySize.width,
                                maxHeight = overlaySize.height
                            )
                        }
                    }
            )
        }
    }
}



@Composable
private fun EditorToolsPanel(
    currentTool: ToolType,
    onToolSelected: (ToolType) -> Unit,
    viewModel: EditorViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Tool options area
        when (currentTool) {
            ToolType.CROP -> CropToolPanel(viewModel = viewModel)
            ToolType.ROTATE -> RotateToolPanel(viewModel = viewModel)
            ToolType.FLIP -> FlipToolPanel(viewModel = viewModel)
            ToolType.ADJUST -> AdjustToolPanel(viewModel = viewModel)
            ToolType.LIGHT_SHADOW -> LightShadowToolPanel(viewModel = viewModel)
            ToolType.LOCAL_STRETCH -> LocalStretchToolPanel(viewModel = viewModel)
            ToolType.FILTER -> FilterToolPanel(viewModel = viewModel)
            ToolType.BEAUTY -> BeautyToolPanel(viewModel = viewModel)
            ToolType.TEXT -> TextToolPanel(viewModel = viewModel)
            ToolType.STICKER -> StickerToolPanel(viewModel = viewModel)
            ToolType.BRUSH -> BrushToolPanel(viewModel = viewModel)
            ToolType.NONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "选择下方工具开始编辑",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        HorizontalDivider()

        // Tool icons row - 使用水平滚动以支持更多工具
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            ToolIcon(
                icon = Icons.Default.Crop,
                label = "裁剪",
                isSelected = currentTool == ToolType.CROP,
                onClick = { onToolSelected(ToolType.CROP) }
            )
            ToolIcon(
                icon = Icons.Default.RotateRight,
                label = "旋转",
                isSelected = currentTool == ToolType.ROTATE,
                onClick = { onToolSelected(ToolType.ROTATE) }
            )
            ToolIcon(
                icon = Icons.Default.Flip,
                label = "翻转",
                isSelected = currentTool == ToolType.FLIP,
                onClick = { onToolSelected(ToolType.FLIP) }
            )
            ToolIcon(
                icon = Icons.Default.Tune,
                label = "调整",
                isSelected = currentTool == ToolType.ADJUST,
                onClick = { onToolSelected(ToolType.ADJUST) }
            )
            ToolIcon(
                icon = Icons.Default.WbSunny,
                label = "光影",
                isSelected = currentTool == ToolType.LIGHT_SHADOW,
                onClick = { onToolSelected(ToolType.LIGHT_SHADOW) }
            )
            ToolIcon(
                icon = Icons.Default.TouchApp,
                label = "局部",
                isSelected = currentTool == ToolType.LOCAL_STRETCH,
                onClick = { onToolSelected(ToolType.LOCAL_STRETCH) }
            )
            ToolIcon(
                icon = Icons.Default.PhotoFilter,
                label = "滤镜",
                isSelected = currentTool == ToolType.FILTER,
                onClick = { onToolSelected(ToolType.FILTER) }
            )
            ToolIcon(
                icon = Icons.Default.Face,
                label = "美颜",
                isSelected = currentTool == ToolType.BEAUTY,
                onClick = { onToolSelected(ToolType.BEAUTY) }
            )
            ToolIcon(
                icon = Icons.Default.TextFields,
                label = "文字",
                isSelected = currentTool == ToolType.TEXT,
                onClick = { onToolSelected(ToolType.TEXT) }
            )
            ToolIcon(
                icon = Icons.Default.EmojiEmotions,
                label = "贴纸",
                isSelected = currentTool == ToolType.STICKER,
                onClick = { onToolSelected(ToolType.STICKER) }
            )
            ToolIcon(
                icon = Icons.Default.Brush,
                label = "画笔",
                isSelected = currentTool == ToolType.BRUSH,
                onClick = { onToolSelected(ToolType.BRUSH) }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun ToolIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CropToolPanel(viewModel: EditorViewModel) {
    val cropState by viewModel.cropState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "拖动裁剪框调整位置，拖动角落调整大小",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Aspect ratio options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CropAspectButton("自由", null, cropState.aspectRatio, viewModel)
            CropAspectButton("1:1", 1f, cropState.aspectRatio, viewModel)
            CropAspectButton("4:3", 4f / 3f, cropState.aspectRatio, viewModel)
            CropAspectButton("16:9", 16f / 9f, cropState.aspectRatio, viewModel)
            CropAspectButton("3:4", 3f / 4f, cropState.aspectRatio, viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { viewModel.resetCrop() }
            ) {
                Text("重置")
            }
            Button(
                onClick = { viewModel.applyCropFromState() }
            ) {
                Text("应用裁剪")
            }
        }
    }
}

@Composable
private fun CropAspectButton(
    label: String,
    ratio: Float?,
    currentRatio: Float?,
    viewModel: EditorViewModel
) {
    val isSelected = if (ratio == null) currentRatio == null else kotlin.math.abs(ratio - (currentRatio ?: 0f)) < 0.01f

    Button(
        onClick = {
            viewModel.setCropAspectRatio(ratio)
        },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RotateToolPanel(viewModel: EditorViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { viewModel.applyRotation(-90f) },
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.RotateLeft, contentDescription = "向左旋转")
            Text("向左90°", modifier = Modifier.padding(start = 8.dp))
        }
        Button(
            onClick = { viewModel.applyRotation(90f) },
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.RotateRight, contentDescription = "向右旋转")
            Text("向右90°", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun FlipToolPanel(viewModel: EditorViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { viewModel.applyFlip(horizontal = true) },
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Flip, contentDescription = "水平翻转")
            Text("水平翻转", modifier = Modifier.padding(start = 8.dp))
        }
        Button(
            onClick = { viewModel.applyFlip(horizontal = false) },
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Flip, contentDescription = "垂直翻转")
            Text("垂直翻转", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

// 调整项类型
private enum class AdjustType {
    NONE, BRIGHTNESS, CONTRAST, SATURATION, TEMPERATURE, TINT
}

@Composable
private fun AdjustToolPanel(viewModel: EditorViewModel) {
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var temperature by remember { mutableFloatStateOf(0f) }
    var tint by remember { mutableFloatStateOf(0f) }
    var selectedAdjust by remember { mutableStateOf(AdjustType.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (selectedAdjust) {
            AdjustType.NONE -> {
                // 显示所有调整选项
                Text(
                    text = "选择调整项",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AdjustOptionButton(
                        label = "亮度",
                        value = brightness,
                        isSelected = false,
                        onClick = { selectedAdjust = AdjustType.BRIGHTNESS }
                    )
                    AdjustOptionButton(
                        label = "对比度",
                        value = contrast,
                        isSelected = false,
                        onClick = { selectedAdjust = AdjustType.CONTRAST }
                    )
                    AdjustOptionButton(
                        label = "饱和度",
                        value = saturation,
                        isSelected = false,
                        onClick = { selectedAdjust = AdjustType.SATURATION }
                    )
                    AdjustOptionButton(
                        label = "色温",
                        value = temperature,
                        isSelected = false,
                        onClick = { selectedAdjust = AdjustType.TEMPERATURE }
                    )
                    AdjustOptionButton(
                        label = "色调",
                        value = tint,
                        isSelected = false,
                        onClick = { selectedAdjust = AdjustType.TINT }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        brightness = 0f
                        contrast = 1f
                        saturation = 1f
                        temperature = 0f
                        tint = 0f
                        viewModel.reset()
                    }) {
                        Text("重置")
                    }
                    Button(onClick = { viewModel.applyAdjustments() }) {
                        Text("应用")
                    }
                }
            }

            AdjustType.BRIGHTNESS -> {
                // 亮度调整界面
                SingleAdjustSlider(
                    label = "亮度",
                    value = brightness,
                    valueRange = -100f..100f,
                    onValueChange = {
                        brightness = it
                        viewModel.updateAdjustParams(
                            AdjustParams(
                                brightness = it,
                                contrast = contrast,
                                saturation = saturation,
                                temperature = temperature,
                                tint = tint
                            )
                        )
                    },
                    onBack = { selectedAdjust = AdjustType.NONE }
                )
            }

            AdjustType.CONTRAST -> {
                // 对比度调整界面
                SingleAdjustSlider(
                    label = "对比度",
                    value = contrast,
                    valueRange = 0f..2f,
                    onValueChange = {
                        contrast = it
                        viewModel.updateAdjustParams(
                            AdjustParams(
                                brightness = brightness,
                                contrast = it,
                                saturation = saturation,
                                temperature = temperature,
                                tint = tint
                            )
                        )
                    },
                    onBack = { selectedAdjust = AdjustType.NONE }
                )
            }

            AdjustType.SATURATION -> {
                // 饱和度调整界面
                SingleAdjustSlider(
                    label = "饱和度",
                    value = saturation,
                    valueRange = 0f..2f,
                    onValueChange = {
                        saturation = it
                        viewModel.updateAdjustParams(
                            AdjustParams(
                                brightness = brightness,
                                contrast = contrast,
                                saturation = it,
                                temperature = temperature,
                                tint = tint
                            )
                        )
                    },
                    onBack = { selectedAdjust = AdjustType.NONE }
                )
            }

            AdjustType.TEMPERATURE -> {
                // 色温调整界面
                SingleAdjustSlider(
                    label = "色温",
                    value = temperature,
                    valueRange = -100f..100f,
                    onValueChange = {
                        temperature = it
                        viewModel.updateAdjustParams(
                            AdjustParams(
                                brightness = brightness,
                                contrast = contrast,
                                saturation = saturation,
                                temperature = it,
                                tint = tint
                            )
                        )
                    },
                    onBack = { selectedAdjust = AdjustType.NONE }
                )
            }

            AdjustType.TINT -> {
                // 色调调整界面
                SingleAdjustSlider(
                    label = "色调",
                    value = tint,
                    valueRange = -100f..100f,
                    onValueChange = {
                        tint = it
                        viewModel.updateAdjustParams(
                            AdjustParams(
                                brightness = brightness,
                                contrast = contrast,
                                saturation = saturation,
                                temperature = temperature,
                                tint = it
                            )
                        )
                    },
                    onBack = { selectedAdjust = AdjustType.NONE }
                )
            }
        }
    }
}

@Composable
private fun AdjustOptionButton(
    label: String,
    value: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = String.format("%.1f", value),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SingleAdjustSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    Column {
        // 返回按钮和标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = String.format("%.1f", value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(50.dp)
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 光影调整项类型
private enum class LightShadowType {
    NONE, SHADOWS, HIGHLIGHTS, EXPOSURE
}

@Composable
private fun LightShadowToolPanel(viewModel: EditorViewModel) {
    var shadows by remember { mutableFloatStateOf(0f) }
    var highlights by remember { mutableFloatStateOf(0f) }
    var exposure by remember { mutableFloatStateOf(0f) }
    var selectedType by remember { mutableStateOf(LightShadowType.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (selectedType) {
            LightShadowType.NONE -> {
                // 显示所有调整选项
                Text(
                    text = "选择光影调整项",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AdjustOptionButton(
                        label = "阴影",
                        value = shadows,
                        isSelected = false,
                        onClick = { selectedType = LightShadowType.SHADOWS }
                    )
                    AdjustOptionButton(
                        label = "高光",
                        value = highlights,
                        isSelected = false,
                        onClick = { selectedType = LightShadowType.HIGHLIGHTS }
                    )
                    AdjustOptionButton(
                        label = "曝光",
                        value = exposure,
                        isSelected = false,
                        onClick = { selectedType = LightShadowType.EXPOSURE }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        shadows = 0f
                        highlights = 0f
                        exposure = 0f
                        viewModel.updateLightShadowParams(LightShadowParams())
                    }) {
                        Text("重置")
                    }
                    Button(onClick = { viewModel.applyLightShadow() }) {
                        Text("应用")
                    }
                }
            }

            LightShadowType.SHADOWS -> {
                SingleAdjustSlider(
                    label = "阴影",
                    value = shadows,
                    valueRange = -100f..100f,
                    onValueChange = {
                        shadows = it
                        viewModel.updateLightShadowParams(
                            LightShadowParams(shadows = it, highlights = highlights, exposure = exposure)
                        )
                    },
                    onBack = { selectedType = LightShadowType.NONE }
                )
            }

            LightShadowType.HIGHLIGHTS -> {
                SingleAdjustSlider(
                    label = "高光",
                    value = highlights,
                    valueRange = -100f..100f,
                    onValueChange = {
                        highlights = it
                        viewModel.updateLightShadowParams(
                            LightShadowParams(shadows = shadows, highlights = it, exposure = exposure)
                        )
                    },
                    onBack = { selectedType = LightShadowType.NONE }
                )
            }

            LightShadowType.EXPOSURE -> {
                SingleAdjustSlider(
                    label = "曝光",
                    value = exposure,
                    valueRange = -100f..100f,
                    onValueChange = {
                        exposure = it
                        viewModel.updateLightShadowParams(
                            LightShadowParams(shadows = shadows, highlights = highlights, exposure = it)
                        )
                    },
                    onBack = { selectedType = LightShadowType.NONE }
                )
            }
        }
    }
}

// 局部拉伸调整项类型
private enum class LocalStretchType {
    NONE, RADIUS, STRENGTH, DIRECTION_X, DIRECTION_Y
}

@Composable
private fun LocalStretchToolPanel(viewModel: EditorViewModel) {
    var centerX by remember { mutableFloatStateOf(0.5f) }
    var centerY by remember { mutableFloatStateOf(0.5f) }
    var radius by remember { mutableFloatStateOf(100f) }
    var strength by remember { mutableFloatStateOf(0f) }
    var directionX by remember { mutableFloatStateOf(0f) }
    var directionY by remember { mutableFloatStateOf(0f) }
    var selectedType by remember { mutableStateOf(LocalStretchType.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (selectedType) {
            LocalStretchType.NONE -> {
                // 显示所有调整选项
                Text(
                    text = "选择局部调整项",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "点击图像选择中心点，然后调整参数",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AdjustOptionButton(
                        label = "影响半径",
                        value = radius,
                        isSelected = false,
                        onClick = { selectedType = LocalStretchType.RADIUS }
                    )
                    AdjustOptionButton(
                        label = "膨胀/收缩",
                        value = strength,
                        isSelected = false,
                        onClick = { selectedType = LocalStretchType.STRENGTH }
                    )
                    AdjustOptionButton(
                        label = "水平方向",
                        value = directionX,
                        isSelected = false,
                        onClick = { selectedType = LocalStretchType.DIRECTION_X }
                    )
                    AdjustOptionButton(
                        label = "垂直方向",
                        value = directionY,
                        isSelected = false,
                        onClick = { selectedType = LocalStretchType.DIRECTION_Y }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        centerX = 0.5f
                        centerY = 0.5f
                        radius = 100f
                        strength = 0f
                        directionX = 0f
                        directionY = 0f
                        viewModel.updateLocalStretchParams(LocalStretchParams())
                    }) {
                        Text("重置")
                    }
                    Button(onClick = { viewModel.applyLocalStretch() }) {
                        Text("应用")
                    }
                }
            }

            LocalStretchType.RADIUS -> {
                SingleAdjustSlider(
                    label = "影响半径",
                    value = radius,
                    valueRange = 50f..300f,
                    onValueChange = {
                        radius = it
                        viewModel.updateLocalStretchParams(
                            LocalStretchParams(
                                centerX = centerX,
                                centerY = centerY,
                                radius = it,
                                strength = strength,
                                directionX = directionX,
                                directionY = directionY
                            )
                        )
                    },
                    onBack = { selectedType = LocalStretchType.NONE }
                )
            }

            LocalStretchType.STRENGTH -> {
                SingleAdjustSlider(
                    label = "膨胀/收缩",
                    value = strength,
                    valueRange = -100f..100f,
                    onValueChange = {
                        strength = it
                        viewModel.updateLocalStretchParams(
                            LocalStretchParams(
                                centerX = centerX,
                                centerY = centerY,
                                radius = radius,
                                strength = it,
                                directionX = directionX,
                                directionY = directionY
                            )
                        )
                    },
                    onBack = { selectedType = LocalStretchType.NONE }
                )
            }

            LocalStretchType.DIRECTION_X -> {
                SingleAdjustSlider(
                    label = "水平方向",
                    value = directionX,
                    valueRange = -1f..1f,
                    onValueChange = {
                        directionX = it
                        viewModel.updateLocalStretchParams(
                            LocalStretchParams(
                                centerX = centerX,
                                centerY = centerY,
                                radius = radius,
                                strength = strength,
                                directionX = it,
                                directionY = directionY
                            )
                        )
                    },
                    onBack = { selectedType = LocalStretchType.NONE }
                )
            }

            LocalStretchType.DIRECTION_Y -> {
                SingleAdjustSlider(
                    label = "垂直方向",
                    value = directionY,
                    valueRange = -1f..1f,
                    onValueChange = {
                        directionY = it
                        viewModel.updateLocalStretchParams(
                            LocalStretchParams(
                                centerX = centerX,
                                centerY = centerY,
                                radius = radius,
                                strength = strength,
                                directionX = directionX,
                                directionY = it
                            )
                        )
                    },
                    onBack = { selectedType = LocalStretchType.NONE }
                )
            }
        }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ${String.format("%.1f", value)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 滤镜调整项类型
private enum class FilterAdjustType {
    NONE, HUE, SATURATION, BRIGHTNESS, CONTRAST, WARMTH, FADE, GRAIN, VIGNETTE
}

@Composable
private fun FilterToolPanel(viewModel: EditorViewModel) {
    val filters = listOf(
        "原图" to 0,
        "黑白" to 1,
        "复古" to 2,
        "清新" to 3,
        "冷色" to 4,
        "暖色" to 5,
        "胶片" to 6,
        "美食" to 7
    )

    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var fade by remember { mutableFloatStateOf(0f) }
    var grain by remember { mutableFloatStateOf(0f) }
    var vignette by remember { mutableFloatStateOf(0f) }
    var selectedAdjust by remember { mutableStateOf(FilterAdjustType.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (selectedAdjust) {
            FilterAdjustType.NONE -> {
                // 显示预设滤镜和自定义调整选项
                Text(
                    text = "预设滤镜",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filters.forEach { (name, type) ->
                        FilterThumbnail(
                            filterName = name,
                            onClick = { viewModel.applyFilter(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "自定义滤镜参数",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 第一行：色相、饱和度、亮度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterOptionButton(
                        label = "色相",
                        value = hue,
                        displayValue = "${hue.toInt()}°",
                        isSelected = false,
                        onClick = { selectedAdjust = FilterAdjustType.HUE }
                    )
                    FilterOptionButton(
                        label = "饱和度",
                        value = saturation,
                        displayValue = String.format("%.1f", saturation),
                        isSelected = false,
                        onClick = { selectedAdjust = FilterAdjustType.SATURATION }
                    )
                    FilterOptionButton(
                        label = "亮度",
                        value = brightness,
                        displayValue = "${brightness.toInt()}",
                        isSelected = false,
                        onClick = { selectedAdjust = FilterAdjustType.BRIGHTNESS }
                    )
                    FilterOptionButton(
                        label = "对比度",
                        value = contrast,
                        displayValue = String.format("%.1f", contrast),
                        isSelected = false,
                        onClick = { selectedAdjust = FilterAdjustType.CONTRAST }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 第二行：色温、褪色、颗粒、暗角
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterOptionButton(
                        label = "色温",
                        value = warmth,
                        displayValue = "${warmth.toInt()}",
                        isSelected = false,
                        onClick = { selectedAdjust = FilterAdjustType.WARMTH }
                    )
                    FilterOptionButton(
                        label = "褪色",
                        value = fade,
                        displayValue = "${fade.toInt()}",
                        isSelected = false,
                        onClick = { selectedAdjust = FilterAdjustType.FADE }
                    )
                    FilterOptionButton(
                        label = "颗粒",
                        value = grain,
                        displayValue = "${grain.toInt()}",
                        isSelected = false,
                        onClick = { selectedAdjust = FilterAdjustType.GRAIN }
                    )
                    FilterOptionButton(
                        label = "暗角",
                        value = vignette,
                        displayValue = "${vignette.toInt()}",
                        isSelected = false,
                        onClick = { selectedAdjust = FilterAdjustType.VIGNETTE }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        hue = 0f
                        saturation = 1f
                        brightness = 0f
                        contrast = 1f
                        warmth = 0f
                        fade = 0f
                        grain = 0f
                        vignette = 0f
                        viewModel.updateFilterParams(FilterParams())
                    }) {
                        Text("重置")
                    }
                    Button(onClick = { viewModel.applyCustomFilter() }) {
                        Text("应用")
                    }
                }
            }

            FilterAdjustType.HUE -> {
                SingleAdjustSlider(
                    label = "色相",
                    value = hue,
                    valueRange = -180f..180f,
                    onValueChange = {
                        hue = it
                        viewModel.updateFilterParams(
                            FilterParams(
                                hue = it,
                                saturation = saturation,
                                brightness = brightness,
                                contrast = contrast,
                                warmth = warmth,
                                fade = fade,
                                grain = grain,
                                vignette = vignette
                            )
                        )
                    },
                    onBack = { selectedAdjust = FilterAdjustType.NONE }
                )
            }

            FilterAdjustType.SATURATION -> {
                SingleAdjustSlider(
                    label = "饱和度",
                    value = saturation,
                    valueRange = 0f..2f,
                    onValueChange = {
                        saturation = it
                        viewModel.updateFilterParams(
                            FilterParams(
                                hue = hue,
                                saturation = it,
                                brightness = brightness,
                                contrast = contrast,
                                warmth = warmth,
                                fade = fade,
                                grain = grain,
                                vignette = vignette
                            )
                        )
                    },
                    onBack = { selectedAdjust = FilterAdjustType.NONE }
                )
            }

            FilterAdjustType.BRIGHTNESS -> {
                SingleAdjustSlider(
                    label = "亮度",
                    value = brightness,
                    valueRange = -100f..100f,
                    onValueChange = {
                        brightness = it
                        viewModel.updateFilterParams(
                            FilterParams(
                                hue = hue,
                                saturation = saturation,
                                brightness = it,
                                contrast = contrast,
                                warmth = warmth,
                                fade = fade,
                                grain = grain,
                                vignette = vignette
                            )
                        )
                    },
                    onBack = { selectedAdjust = FilterAdjustType.NONE }
                )
            }

            FilterAdjustType.CONTRAST -> {
                SingleAdjustSlider(
                    label = "对比度",
                    value = contrast,
                    valueRange = 0f..2f,
                    onValueChange = {
                        contrast = it
                        viewModel.updateFilterParams(
                            FilterParams(
                                hue = hue,
                                saturation = saturation,
                                brightness = brightness,
                                contrast = it,
                                warmth = warmth,
                                fade = fade,
                                grain = grain,
                                vignette = vignette
                            )
                        )
                    },
                    onBack = { selectedAdjust = FilterAdjustType.NONE }
                )
            }

            FilterAdjustType.WARMTH -> {
                SingleAdjustSlider(
                    label = "色温",
                    value = warmth,
                    valueRange = -100f..100f,
                    onValueChange = {
                        warmth = it
                        viewModel.updateFilterParams(
                            FilterParams(
                                hue = hue,
                                saturation = saturation,
                                brightness = brightness,
                                contrast = contrast,
                                warmth = it,
                                fade = fade,
                                grain = grain,
                                vignette = vignette
                            )
                        )
                    },
                    onBack = { selectedAdjust = FilterAdjustType.NONE }
                )
            }

            FilterAdjustType.FADE -> {
                SingleAdjustSlider(
                    label = "褪色",
                    value = fade,
                    valueRange = 0f..100f,
                    onValueChange = {
                        fade = it
                        viewModel.updateFilterParams(
                            FilterParams(
                                hue = hue,
                                saturation = saturation,
                                brightness = brightness,
                                contrast = contrast,
                                warmth = warmth,
                                fade = it,
                                grain = grain,
                                vignette = vignette
                            )
                        )
                    },
                    onBack = { selectedAdjust = FilterAdjustType.NONE }
                )
            }

            FilterAdjustType.GRAIN -> {
                SingleAdjustSlider(
                    label = "颗粒",
                    value = grain,
                    valueRange = 0f..100f,
                    onValueChange = {
                        grain = it
                        viewModel.updateFilterParams(
                            FilterParams(
                                hue = hue,
                                saturation = saturation,
                                brightness = brightness,
                                contrast = contrast,
                                warmth = warmth,
                                fade = fade,
                                grain = it,
                                vignette = vignette
                            )
                        )
                    },
                    onBack = { selectedAdjust = FilterAdjustType.NONE }
                )
            }

            FilterAdjustType.VIGNETTE -> {
                SingleAdjustSlider(
                    label = "暗角",
                    value = vignette,
                    valueRange = 0f..100f,
                    onValueChange = {
                        vignette = it
                        viewModel.updateFilterParams(
                            FilterParams(
                                hue = hue,
                                saturation = saturation,
                                brightness = brightness,
                                contrast = contrast,
                                warmth = warmth,
                                fade = fade,
                                grain = grain,
                                vignette = it
                            )
                        )
                    },
                    onBack = { selectedAdjust = FilterAdjustType.NONE }
                )
            }
        }
    }
}

@Composable
private fun FilterOptionButton(
    label: String,
    value: Float,
    displayValue: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = displayValue,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun FilterThumbnail(filterName: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhotoFilter,
                contentDescription = filterName,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = filterName,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// 文字工具子选项类型
private enum class TextToolSubType {
    NONE, SIZE, COLOR, ROTATION
}

// 贴纸工具子选项类型
private enum class StickerToolSubType {
    NONE, SIZE, ROTATION, ALPHA
}

@Composable
private fun StickerBoxOverlay(
    stickerBoxState: com.photomaster.ui.editor.StickerBoxState,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerSize = coordinates.size.toSize()
            }
    ) {
        // 贴纸框显示 - 使用绝对像素位置
        if (containerSize.width > 0 && containerSize.height > 0) {
            val xOffset = (stickerBoxState.x * containerSize.width).dp
            val yOffset = (stickerBoxState.y * containerSize.height).dp

            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // 计算新的相对位置
                            val newX = stickerBoxState.x + dragAmount.x / containerSize.width
                            val newY = stickerBoxState.y + dragAmount.y / containerSize.height
                            viewModel.updateStickerBoxPosition(newX, newY)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stickerBoxState.stickerCode,
                    fontSize = stickerBoxState.size.sp,
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp),
                    color = Color.White.copy(alpha = stickerBoxState.alpha)
                )
            }
        }
    }
}

@Composable
private fun TextBoxOverlay(
    textBoxState: com.photomaster.ui.editor.TextBoxState,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerSize = coordinates.size.toSize()
            }
    ) {
        // 文本框显示 - 使用绝对像素位置
        if (containerSize.width > 0 && containerSize.height > 0) {
            val xOffset = (textBoxState.x * containerSize.width).dp
            val yOffset = (textBoxState.y * containerSize.height).dp

            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // 计算新的相对位置
                            val newX = textBoxState.x + dragAmount.x / containerSize.width
                            val newY = textBoxState.y + dragAmount.y / containerSize.height
                            viewModel.updateTextBoxPosition(newX, newY)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = textBoxState.text.ifBlank { "点击输入文字" },
                    fontSize = textBoxState.textSize.sp,
                    color = Color(textBoxState.textColor),
                    modifier = Modifier
                        .border(
                            width = if (textBoxState.isEditing) 2.dp else 1.dp,
                            color = if (textBoxState.isEditing) MaterialTheme.colorScheme.primary else Color.White,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun TextToolPanel(viewModel: EditorViewModel) {
    val textBoxState by viewModel.textBoxState.collectAsState()
    var selectedSubType by remember { mutableStateOf(TextToolSubType.NONE) }

    val colors = listOf(
        android.graphics.Color.WHITE to "白色",
        android.graphics.Color.BLACK to "黑色",
        android.graphics.Color.RED to "红色",
        android.graphics.Color.GREEN to "绿色",
        android.graphics.Color.BLUE to "蓝色",
        android.graphics.Color.YELLOW to "黄色",
        android.graphics.Color.MAGENTA to "紫色",
        android.graphics.Color.CYAN to "青色"
    )

    // 当切换到文字工具时，如果没有文本框则创建一个
    LaunchedEffect(Unit) {
        if (!textBoxState.isVisible) {
            viewModel.createTextBox()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (selectedSubType) {
            TextToolSubType.NONE -> {
                // 显示所有选项
                Text(
                    text = "编辑文字",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 文字输入框
                OutlinedTextField(
                    value = textBoxState.text,
                    onValueChange = { viewModel.updateTextBoxText(it) },
                    label = { Text("输入文字") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "拖拽图片上的文本框可移动位置",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 子选项按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextSubOptionButton(
                        label = "字号",
                        value = "${textBoxState.textSize.toInt()}",
                        onClick = { selectedSubType = TextToolSubType.SIZE }
                    )
                    TextSubOptionButton(
                        label = "颜色",
                        value = "",
                        color = Color(textBoxState.textColor),
                        onClick = { selectedSubType = TextToolSubType.COLOR }
                    )
                    TextSubOptionButton(
                        label = "旋转",
                        value = "${textBoxState.rotation.toInt()}°",
                        onClick = { selectedSubType = TextToolSubType.ROTATION }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { viewModel.cancelTextBox() }) {
                        Text("取消")
                    }
                    Button(onClick = { viewModel.applyTextFromBox() }) {
                        Text("应用")
                    }
                }
            }

            TextToolSubType.SIZE -> {
                // 字号调整
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedSubType = TextToolSubType.NONE }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                        Text(
                            text = "字号",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${textBoxState.textSize.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(40.dp)
                        )
                        Slider(
                            value = textBoxState.textSize,
                            onValueChange = { viewModel.updateTextBoxStyle(textSize = it) },
                            valueRange = 20f..150f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            TextToolSubType.COLOR -> {
                // 颜色选择
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedSubType = TextToolSubType.NONE }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                        Text(
                            text = "文字颜色",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colors.forEach { (color, name) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    viewModel.updateTextBoxStyle(textColor = color)
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            width = if (textBoxState.textColor == color) 3.dp else 1.dp,
                                            color = if (textBoxState.textColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            TextToolSubType.ROTATION -> {
                // 旋转调整
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedSubType = TextToolSubType.NONE }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                        Text(
                            text = "旋转角度",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${textBoxState.rotation.toInt()}°",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(40.dp)
                        )
                        Slider(
                            value = textBoxState.rotation,
                            onValueChange = { viewModel.updateTextBoxStyle(rotation = it) },
                            valueRange = -180f..180f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextSubOptionButton(
    label: String,
    value: String,
    color: Color? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (color != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            } else {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun StickerToolPanel(viewModel: EditorViewModel) {
    val stickerBoxState by viewModel.stickerBoxState.collectAsState()
    var selectedSubType by remember { mutableStateOf(StickerToolSubType.NONE) }

    val stickersRow1 = listOf(
        "😀" to "开心",
        "😍" to "喜欢",
        "🎉" to "庆祝",
        "❤️" to "爱心",
        "⭐" to "星星",
        "🔥" to "火热"
    )
    val stickersRow2 = listOf(
        "🌈" to "彩虹",
        "🎨" to "艺术",
        "😎" to "酷",
        "🥳" to "派对",
        "🤩" to "惊艳",
        "💯" to "满分"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (!stickerBoxState.isVisible) {
            // 选择贴纸阶段
            Text(
                text = "选择贴纸",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 贴纸选择 - 第一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stickersRow1.forEach { (sticker, name) ->
                    StickerItem(
                        sticker = sticker,
                        name = name,
                        isSelected = false,
                        onClick = { viewModel.createStickerBox(sticker) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 贴纸选择 - 第二行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stickersRow2.forEach { (sticker, name) ->
                    StickerItem(
                        sticker = sticker,
                        name = name,
                        isSelected = false,
                        onClick = { viewModel.createStickerBox(sticker) }
                    )
                }
            }
        } else {
            // 调整贴纸阶段
            when (selectedSubType) {
                StickerToolSubType.NONE -> {
                    Text(
                        text = "调整贴纸 - 拖拽移动位置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 子选项按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StickerSubOptionButton(
                            label = "大小",
                            value = stickerBoxState.size.toInt(),
                            isSelected = false,
                            onClick = { selectedSubType = StickerToolSubType.SIZE }
                        )
                        StickerSubOptionButton(
                            label = "旋转",
                            value = stickerBoxState.rotation.toInt(),
                            isSelected = false,
                            onClick = { selectedSubType = StickerToolSubType.ROTATION }
                        )
                        StickerSubOptionButton(
                            label = "透明",
                            value = (stickerBoxState.alpha * 100).toInt(),
                            isSelected = false,
                            onClick = { selectedSubType = StickerToolSubType.ALPHA }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 应用/取消按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { viewModel.cancelStickerBox() }
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = { viewModel.applyStickerFromBox() }
                        ) {
                            Text("应用")
                        }
                    }
                }
                StickerToolSubType.SIZE -> {
                    Text(
                        text = "调整大小",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    AdjustSlider(
                        label = "大小",
                        value = stickerBoxState.size,
                        valueRange = 50f..200f,
                        onValueChange = { viewModel.updateStickerBoxStyle(size = it) }
                    )

                    TextButton(
                        onClick = { selectedSubType = StickerToolSubType.NONE },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("返回")
                    }
                }
                StickerToolSubType.ROTATION -> {
                    Text(
                        text = "调整旋转",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    AdjustSlider(
                        label = "旋转角度",
                        value = stickerBoxState.rotation,
                        valueRange = -180f..180f,
                        onValueChange = { viewModel.updateStickerBoxStyle(rotation = it) }
                    )

                    TextButton(
                        onClick = { selectedSubType = StickerToolSubType.NONE },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("返回")
                    }
                }
                StickerToolSubType.ALPHA -> {
                    Text(
                        text = "调整透明度",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    AdjustSlider(
                        label = "透明度",
                        value = stickerBoxState.alpha,
                        valueRange = 0.1f..1f,
                        onValueChange = { viewModel.updateStickerBoxStyle(alpha = it) }
                    )

                    TextButton(
                        onClick = { selectedSubType = StickerToolSubType.NONE },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("返回")
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerSubOptionButton(
    label: String,
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$value",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun StickerItem(
    sticker: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = sticker,
            fontSize = 28.sp
        )
        Text(
            text = name,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun BrushToolPanel(viewModel: EditorViewModel) {
    val colors = listOf(
        android.graphics.Color.BLACK to "黑色",
        android.graphics.Color.WHITE to "白色",
        android.graphics.Color.RED to "红色",
        android.graphics.Color.GREEN to "绿色",
        android.graphics.Color.BLUE to "蓝色",
        android.graphics.Color.YELLOW to "黄色",
        android.graphics.Color.MAGENTA to "紫色",
        android.graphics.Color.CYAN to "青色"
    )

    var selectedColor by remember { mutableStateOf(android.graphics.Color.BLACK) }
    var brushSize by remember { mutableFloatStateOf(10f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "画笔工具",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "在图片上滑动手指进行涂鸦",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "画笔颜色",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colors.forEach { (color, name) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        selectedColor = color
                        viewModel.setBrushColor(color)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                width = if (selectedColor == color) 3.dp else 1.dp,
                                color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = name,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 画笔大小
        AdjustSlider(
            label = "画笔大小",
            value = brushSize,
            valueRange = 2f..50f,
            onValueChange = {
                brushSize = it
                viewModel.setBrushSize(it)
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {
                viewModel.clearBrushPoints()
            }) {
                Text("清除路径")
            }
            Button(onClick = { viewModel.applyBrush() }) {
                Text("应用涂鸦")
            }
        }
    }
}

// 美颜调整项类型
private enum class BeautyType {
    NONE, SMOOTH, WHITEN, THIN_FACE, BIG_EYES, SLIM_NOSE
}

@Composable
private fun BeautyToolPanel(viewModel: EditorViewModel) {
    var smooth by remember { mutableFloatStateOf(0f) }
    var whiten by remember { mutableFloatStateOf(0f) }
    var thinFace by remember { mutableFloatStateOf(0f) }
    var bigEyes by remember { mutableFloatStateOf(0f) }
    var slimNose by remember { mutableFloatStateOf(0f) }
    var selectedBeauty by remember { mutableStateOf(BeautyType.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (selectedBeauty) {
            BeautyType.NONE -> {
                // 显示所有美颜选项
                Text(
                    text = "选择美颜项",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BeautyOptionButton(
                        label = "磨皮",
                        value = smooth,
                        isSelected = false,
                        onClick = { selectedBeauty = BeautyType.SMOOTH }
                    )
                    BeautyOptionButton(
                        label = "美白",
                        value = whiten,
                        isSelected = false,
                        onClick = { selectedBeauty = BeautyType.WHITEN }
                    )
                    BeautyOptionButton(
                        label = "瘦脸",
                        value = thinFace,
                        isSelected = false,
                        onClick = { selectedBeauty = BeautyType.THIN_FACE }
                    )
                    BeautyOptionButton(
                        label = "大眼",
                        value = bigEyes,
                        isSelected = false,
                        onClick = { selectedBeauty = BeautyType.BIG_EYES }
                    )
                    BeautyOptionButton(
                        label = "瘦鼻",
                        value = slimNose,
                        isSelected = false,
                        onClick = { selectedBeauty = BeautyType.SLIM_NOSE }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        smooth = 0f
                        whiten = 0f
                        thinFace = 0f
                        bigEyes = 0f
                        slimNose = 0f
                        viewModel.updateBeautyParams(BeautyParams())
                    }) {
                        Text("重置")
                    }
                    Button(onClick = { viewModel.applyBeauty() }) {
                        Text("应用")
                    }
                }
            }

            BeautyType.SMOOTH -> {
                SingleAdjustSlider(
                    label = "磨皮",
                    value = smooth,
                    valueRange = 0f..100f,
                    onValueChange = {
                        smooth = it
                        viewModel.updateBeautyParams(
                            BeautyParams(
                                smooth = it,
                                whiten = whiten,
                                thinFace = thinFace,
                                bigEyes = bigEyes,
                                slimNose = slimNose
                            )
                        )
                    },
                    onBack = { selectedBeauty = BeautyType.NONE }
                )
            }

            BeautyType.WHITEN -> {
                SingleAdjustSlider(
                    label = "美白",
                    value = whiten,
                    valueRange = 0f..100f,
                    onValueChange = {
                        whiten = it
                        viewModel.updateBeautyParams(
                            BeautyParams(
                                smooth = smooth,
                                whiten = it,
                                thinFace = thinFace,
                                bigEyes = bigEyes,
                                slimNose = slimNose
                            )
                        )
                    },
                    onBack = { selectedBeauty = BeautyType.NONE }
                )
            }

            BeautyType.THIN_FACE -> {
                SingleAdjustSlider(
                    label = "瘦脸",
                    value = thinFace,
                    valueRange = 0f..100f,
                    onValueChange = {
                        thinFace = it
                        viewModel.updateBeautyParams(
                            BeautyParams(
                                smooth = smooth,
                                whiten = whiten,
                                thinFace = it,
                                bigEyes = bigEyes,
                                slimNose = slimNose
                            )
                        )
                    },
                    onBack = { selectedBeauty = BeautyType.NONE }
                )
            }

            BeautyType.BIG_EYES -> {
                SingleAdjustSlider(
                    label = "大眼",
                    value = bigEyes,
                    valueRange = 0f..100f,
                    onValueChange = {
                        bigEyes = it
                        viewModel.updateBeautyParams(
                            BeautyParams(
                                smooth = smooth,
                                whiten = whiten,
                                thinFace = thinFace,
                                bigEyes = it,
                                slimNose = slimNose
                            )
                        )
                    },
                    onBack = { selectedBeauty = BeautyType.NONE }
                )
            }

            BeautyType.SLIM_NOSE -> {
                SingleAdjustSlider(
                    label = "瘦鼻",
                    value = slimNose,
                    valueRange = 0f..100f,
                    onValueChange = {
                        slimNose = it
                        viewModel.updateBeautyParams(
                            BeautyParams(
                                smooth = smooth,
                                whiten = whiten,
                                thinFace = thinFace,
                                bigEyes = bigEyes,
                                slimNose = it
                            )
                        )
                    },
                    onBack = { selectedBeauty = BeautyType.NONE }
                )
            }
        }
    }
}

@Composable
private fun BeautyOptionButton(
    label: String,
    value: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${value.toInt()}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
