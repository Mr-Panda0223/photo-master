package com.photomaster.ui.screens.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.photomaster.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    imageUri: String,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportFormat by remember { mutableStateOf(ExportFormat.JPEG) }
    var quality by remember { mutableIntStateOf(90) }
    var maxWidth by remember { mutableIntStateOf(0) } // 0 = 原始尺寸
    var maxHeight by remember { mutableIntStateOf(0) }
    var addWatermark by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("PhotoMaster") }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var showShareDialog by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出图片") },
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
            // 预览区域
            ExportPreview(
                imageUri = imageUri,
                addWatermark = addWatermark,
                watermarkText = watermarkText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 导出设置
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // 格式选择
                SectionTitle("导出格式")
                FormatSelector(
                    selectedFormat = exportFormat,
                    onFormatSelected = { exportFormat = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 质量调节
                SectionTitle("图片质量")
                QualitySlider(
                    quality = quality,
                    onQualityChange = { quality = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 尺寸调整
                SectionTitle("图片尺寸")
                SizeSelector(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    onSizeSelected = { width, height ->
                        maxWidth = width
                        maxHeight = height
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 水印设置
                SectionTitle("水印")
                WatermarkSettings(
                    enabled = addWatermark,
                    text = watermarkText,
                    onEnabledChange = { addWatermark = it },
                    onTextChange = { watermarkText = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 导出按钮
            Button(
                onClick = {
                    scope.launch {
                        isExporting = true
                        exportProgress = 0f

                        val uri = exportImage(
                            context = context,
                            imageUri = imageUri,
                            format = exportFormat,
                            quality = quality,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            addWatermark = addWatermark,
                            watermarkText = watermarkText,
                            onProgress = { exportProgress = it }
                        )

                        exportedUri = uri
                        isExporting = false
                        showShareDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isExporting
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导出中 ${(exportProgress * 100).toInt()}%")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导出图片")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 返回首页按钮
            OutlinedButton(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("返回首页")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 导出完成分享对话框
    if (showShareDialog && exportedUri != null) {
        ExportSuccessDialog(
            onDismiss = { showShareDialog = false },
            onShare = { shareImage(context, exportedUri!!) },
            onSave = { showShareDialog = false },
            exportedUri = exportedUri!!
        )
    }
}

@Composable
private fun ExportPreview(
    imageUri: String,
    addWatermark: Boolean,
    watermarkText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "预览",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // 水印预览
        if (addWatermark) {
            Text(
                text = watermarkText,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun FormatSelector(
    selectedFormat: ExportFormat,
    onFormatSelected: (ExportFormat) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExportFormat.values().forEach { format ->
            FormatOption(
                format = format,
                isSelected = format == selectedFormat,
                onClick = { onFormatSelected(format) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FormatOption(
    format: ExportFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Primary.copy(alpha = 0.1f) else BackgroundGray
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Primary else BorderGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = format.name,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Primary else TextPrimary
        )
        Text(
            text = format.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun QualitySlider(
    quality: Int,
    onQualityChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("低质量", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                "$quality%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Text("高质量", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        Slider(
            value = quality.toFloat(),
            onValueChange = { onQualityChange(it.toInt()) },
            valueRange = 50f..100f,
            steps = 9,
            modifier = Modifier.padding(horizontal = 0.dp)
        )
    }
}

@Composable
private fun SizeSelector(
    maxWidth: Int,
    maxHeight: Int,
    onSizeSelected: (Int, Int) -> Unit
) {
    val sizeOptions = listOf(
        Triple("原始尺寸", 0, 0),
        Triple("2K (2560×1440)", 2560, 1440),
        Triple("1080P (1920×1080)", 1920, 1080),
        Triple("720P (1280×720)", 1280, 720),
        Triple("正方形 (1080×1080)", 1080, 1080)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sizeOptions.forEach { (name, width, height) ->
            val isSelected = maxWidth == width && maxHeight == height
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Primary.copy(alpha = 0.1f) else BackgroundGray)
                    .clickable { onSizeSelected(width, height) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSizeSelected(width, height) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    color = if (isSelected) Primary else TextPrimary
                )
            }
        }
    }
}

@Composable
private fun WatermarkSettings(
    enabled: Boolean,
    text: String,
    onEnabledChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "添加水印",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Primary,
                    checkedTrackColor = Primary.copy(alpha = 0.5f)
                )
            )
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("水印文字") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun ExportSuccessDialog(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    exportedUri: Uri
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出成功！") },
        text = {
            Column {
                Text("图片已成功保存到相册")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "路径: $exportedUri",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("分享")
            }
        },
        dismissButton = {
            TextButton(onClick = onSave) {
                Text("完成")
            }
        }
    )
}

// 导出格式枚举
enum class ExportFormat(val mimeType: String, val extension: String, val description: String) {
    JPEG("image/jpeg", "jpg", "兼容性好"),
    PNG("image/png", "png", "无损压缩"),
    WEBP("image/webp", "webp", "体积更小")
}

// 导出图片
private suspend fun exportImage(
    context: Context,
    imageUri: String,
    format: ExportFormat,
    quality: Int,
    maxWidth: Int,
    maxHeight: Int,
    addWatermark: Boolean,
    watermarkText: String,
    onProgress: (Float) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    try {
        onProgress(0.1f)

        // 加载原图
        val sourceBitmap = loadBitmapFromUri(context, Uri.parse(imageUri)) ?: return@withContext null
        onProgress(0.3f)

        // 调整尺寸
        val resizedBitmap = if (maxWidth > 0 && maxHeight > 0) {
            resizeBitmap(sourceBitmap, maxWidth, maxHeight)
        } else {
            sourceBitmap
        }
        onProgress(0.5f)

        // 添加水印
        val finalBitmap = if (addWatermark && watermarkText.isNotEmpty()) {
            addWatermarkToBitmap(resizedBitmap, watermarkText)
        } else {
            resizedBitmap
        }
        onProgress(0.7f)

        // 保存到相册
        val uri = saveBitmapToGallery(
            context = context,
            bitmap = finalBitmap,
            format = format,
            quality = quality
        )
        onProgress(1f)

        // 清理临时资源
        if (sourceBitmap != resizedBitmap) {
            sourceBitmap.recycle()
        }
        if (resizedBitmap != finalBitmap) {
            resizedBitmap.recycle()
        }

        uri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 从 URI 加载 Bitmap
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 调整 Bitmap 尺寸
private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val scale = minOf(
        maxWidth.toFloat() / width,
        maxHeight.toFloat() / height,
        1f
    )

    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

// 添加水印
private fun addWatermarkToBitmap(bitmap: Bitmap, text: String): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val paint = Paint().apply {
        color = Color.WHITE
        alpha = 128 // 50% 透明度
        textSize = bitmap.width * 0.05f
        isAntiAlias = true
    }

    // 计算文字位置（右下角）
    val textBounds = Rect()
    paint.getTextBounds(text, 0, text.length, textBounds)

    val x = bitmap.width - textBounds.width() - 50f
    val y = bitmap.height - 50f

    canvas.drawText(text, x, y, paint)

    return result
}

// 保存 Bitmap 到相册
private fun saveBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    format: ExportFormat,
    quality: Int
): Uri? {
    val filename = "PhotoMaster_${System.currentTimeMillis()}.${format.extension}"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoMaster")
    }

    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: return null

    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
            val compressFormat = when (format) {
                ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(compressFormat, quality, outputStream)
        }
        return uri
    } catch (e: Exception) {
        e.printStackTrace()
        context.contentResolver.delete(uri, null, null)
        return null
    }
}

// 分享图片
private fun shareImage(context: Context, uri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(shareIntent, "分享图片")
    context.startActivity(chooser)
}
