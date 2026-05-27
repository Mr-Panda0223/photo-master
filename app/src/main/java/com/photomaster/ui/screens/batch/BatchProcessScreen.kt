package com.photomaster.ui.screens.batch

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photomaster.data.model.BatchOperationType
import com.photomaster.data.model.BatchPreset
import com.photomaster.data.model.BatchTask
import com.photomaster.data.model.BatchPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchProcessScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current

    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedOperations by remember { mutableStateOf<List<BatchOperationType>>(emptyList()) }
    var selectedPreset by remember { mutableStateOf<BatchPreset?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = uris
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量处理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 图片选择区域
            ImageSelectionArea(
                selectedImages = selectedImages,
                onAddImages = { imagePickerLauncher.launch("image/*") },
                onRemoveImage = { uri ->
                    selectedImages = selectedImages.filter { it != uri }
                },
                onClearAll = { selectedImages = emptyList() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 预设模板选择
            SectionTitle("快速预设")
            PresetSelector(
                selectedPreset = selectedPreset,
                onPresetSelected = { preset ->
                    selectedPreset = preset
                    // 根据预设设置操作
                    selectedOperations = when (preset) {
                        BatchPreset.SOCIAL_MEDIA -> listOf(BatchOperationType.RESIZE, BatchOperationType.COMPRESS)
                        BatchPreset.WEB_OPTIMIZE -> listOf(BatchOperationType.RESIZE, BatchOperationType.COMPRESS, BatchOperationType.FORMAT_CONVERT)
                        BatchPreset.PRINT_READY -> listOf(BatchOperationType.RESIZE, BatchOperationType.ADJUST)
                        BatchPreset.THUMBNAIL -> listOf(BatchOperationType.RESIZE, BatchOperationType.COMPRESS)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 自定义操作
            SectionTitle("自定义操作")
            OperationSelector(
                selectedOperations = selectedOperations,
                onOperationToggle = { operation ->
                    selectedOperations = if (selectedOperations.contains(operation)) {
                        selectedOperations.filter { it != operation }
                    } else {
                        selectedOperations + operation
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 输出设置
            SectionTitle("输出设置")
            OutputSettingsCard()

            Spacer(modifier = Modifier.height(24.dp))

            // 开始处理按钮
            Button(
                onClick = {
                    isProcessing = true
                    // 模拟处理进度
                    // 实际应该调用 BatchProcessor
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedImages.isNotEmpty() && selectedOperations.isNotEmpty() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("处理中 ${(progress * 100).toInt()}%")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始处理 (${selectedImages.size}张图片)")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ImageSelectionArea(
    selectedImages: List<Uri>,
    onAddImages: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选择 ${selectedImages.size} 张图片",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            if (selectedImages.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedImages.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onAddImages),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击选择图片",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 显示已选择的图片
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(selectedImages) { uri ->
                    SelectedImageItem(
                        uri = uri,
                        onRemove = { onRemoveImage(uri) }
                    )
                }

                // 添加更多按钮
                item {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onAddImages),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedImageItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier.aspectRatio(1f)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // 删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.error)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PresetSelector(
    selectedPreset: BatchPreset?,
    onPresetSelected: (BatchPreset) -> Unit
) {
    val presets = listOf(
        BatchPreset.SOCIAL_MEDIA to "社交媒体" to Icons.Default.Share,
        BatchPreset.WEB_OPTIMIZE to "网页优化" to Icons.Default.Language,
        BatchPreset.PRINT_READY to "打印准备" to Icons.Default.Print,
        BatchPreset.THUMBNAIL to "缩略图" to Icons.Default.PhotoSizeSelectSmall
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { (presetPair, icon) ->
            val (preset, name) = presetPair
            PresetCard(
                name = name,
                icon = icon,
                isSelected = preset == selectedPreset,
                onClick = { onPresetSelected(preset) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PresetCard(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) 
        MaterialTheme.colorScheme.primaryContainer 
    else 
        MaterialTheme.colorScheme.surfaceVariant
    
    val borderColor = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.outline
    
    val contentColor = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
private fun OperationSelector(
    selectedOperations: List<BatchOperationType>,
    onOperationToggle: (BatchOperationType) -> Unit
) {
    val operations = listOf(
        BatchOperationType.RESIZE to "调整尺寸" to Icons.Default.PhotoSizeSelectLarge,
        BatchOperationType.CROP to "裁剪" to Icons.Default.Crop,
        BatchOperationType.ROTATE to "旋转" to Icons.Default.RotateRight,
        BatchOperationType.FLIP to "翻转" to Icons.Default.Flip,
        BatchOperationType.FILTER to "滤镜" to Icons.Default.PhotoFilter,
        BatchOperationType.ADJUST to "调整" to Icons.Default.Tune,
        BatchOperationType.COMPRESS to "压缩" to Icons.Default.Compress,
        BatchOperationType.FORMAT_CONVERT to "格式转换" to Icons.Default.Transform
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        operations.chunked(4).forEach { rowOperations ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOperations.forEach { (operationPair, icon) ->
                    val (operation, name) = operationPair
                    OperationChip(
                        name = name,
                        icon = icon,
                        isSelected = selectedOperations.contains(operation),
                        onClick = { onOperationToggle(operation) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OperationChip(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) 
        MaterialTheme.colorScheme.primaryContainer 
    else 
        MaterialTheme.colorScheme.surfaceVariant
    
    val borderColor = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.outline
    
    val contentColor = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
private fun OutputSettingsCard() {
    var outputFormat by remember { mutableStateOf("JPEG") }
    var quality by remember { mutableIntStateOf(90) }
    var outputDirectory by remember { mutableStateOf("默认相册") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 输出格式
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "输出格式",
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("JPEG", "PNG", "WebP").forEach { format ->
                        FilterChip(
                            selected = outputFormat == format,
                            onClick = { outputFormat = format },
                            label = { Text(format) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 质量设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "质量",
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$quality%",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = quality.toFloat(),
                onValueChange = { quality = it.toInt() },
                valueRange = 50f..100f,
                steps = 9
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 输出目录
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "输出目录",
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = outputDirectory,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
