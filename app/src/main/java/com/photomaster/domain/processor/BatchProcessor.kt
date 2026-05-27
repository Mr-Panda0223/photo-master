package com.photomaster.domain.processor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.photomaster.data.model.BatchItem
import com.photomaster.data.model.BatchItemStatus
import com.photomaster.data.model.BatchOperation
import com.photomaster.data.model.BatchOperationType
import com.photomaster.data.model.BatchResult
import com.photomaster.data.model.BatchSession
import com.photomaster.data.model.BatchSessionStatus
import com.photomaster.data.model.BatchTask
import com.photomaster.data.model.OutputFormat
import com.photomaster.data.model.OutputSettings
import com.photomaster.data.model.ResizeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * 批量处理器
 * 处理批量图片编辑任务
 */
class BatchProcessor(private val context: Context) {

    private val _session = MutableStateFlow<BatchSession?>(null)
    val session: StateFlow<BatchSession?> = _session.asStateFlow()

    private var isCancelled = false

    /**
     * 创建批处理会话
     */
    fun createSession(task: BatchTask, imageUris: List<Uri>): BatchSession {
        val items = imageUris.map { uri ->
            BatchItem(
                uri = uri,
                name = getFileNameFromUri(uri) ?: "image_${UUID.randomUUID()}.jpg"
            )
        }

        val newSession = BatchSession(
            task = task,
            items = items
        )

        _session.value = newSession
        return newSession
    }

    /**
     * 开始批处理
     */
    suspend fun startProcessing(outputSettings: OutputSettings): BatchResult? {
        val currentSession = _session.value ?: return null
        isCancelled = false

        val updatedSession = currentSession.copy(
            status = BatchSessionStatus.RUNNING,
            startedAt = System.currentTimeMillis()
        )
        _session.value = updatedSession

        val outputUris = mutableListOf<Uri>()
        val errors = mutableListOf<Pair<String, String>>()
        var successCount = 0
        var failedCount = 0

        updatedSession.items.forEachIndexed { index, item ->
            if (isCancelled) {
                _session.value = updatedSession.copy(
                    status = BatchSessionStatus.CANCELLED,
                    completedAt = System.currentTimeMillis()
                )
                return null
            }

            // 更新当前处理项状态
            updateItemStatus(item.id, BatchItemStatus.PROCESSING, 0f)

            try {
                val resultUri = processItem(item, updatedSession.task, outputSettings, index)

                if (resultUri != null) {
                    outputUris.add(resultUri)
                    updateItemStatus(item.id, BatchItemStatus.COMPLETED, 1f, resultUri)
                    successCount++
                } else {
                    updateItemStatus(item.id, BatchItemStatus.FAILED, 0f, errorMessage = "处理失败")
                    errors.add(item.name to "处理失败")
                    failedCount++
                }
            } catch (e: Exception) {
                updateItemStatus(item.id, BatchItemStatus.FAILED, 0f, errorMessage = e.message)
                errors.add(item.name to (e.message ?: "未知错误"))
                failedCount++
            }

            // 更新会话进度
            _session.value = _session.value?.copy(
                currentIndex = index + 1,
                completedCount = successCount,
                failedCount = failedCount
            )
        }

        // 完成会话
        _session.value = _session.value?.copy(
            status = if (failedCount == 0) BatchSessionStatus.COMPLETED else BatchSessionStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )

        return BatchResult(
            sessionId = currentSession.id,
            successCount = successCount,
            failedCount = failedCount,
            totalCount = updatedSession.items.size,
            outputUris = outputUris,
            errors = errors
        )
    }

    /**
     * 处理单个项目
     */
    private suspend fun processItem(
        item: BatchItem,
        task: BatchTask,
        outputSettings: OutputSettings,
        index: Int
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // 加载原始图片
            val bitmap = loadBitmapFromUri(item.uri) ?: return@withContext null

            var processedBitmap = bitmap
            val operations = task.operations.filter { it.enabled }
            val totalOperations = operations.size

            // 按顺序应用所有操作
            operations.forEachIndexed { opIndex, operation ->
                if (isCancelled) return@withContext null

                processedBitmap = applyOperation(processedBitmap, operation)

                // 更新进度
                val progress = (opIndex + 1).toFloat() / totalOperations
                updateItemProgress(item.id, progress * 0.9f) // 预留10%给保存
            }

            // 保存处理后的图片
            val outputUri = saveBitmap(processedBitmap, item.name, outputSettings, index)
            updateItemProgress(item.id, 1f)

            outputUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 应用单个操作
     */
    private fun applyOperation(bitmap: Bitmap, operation: BatchOperation): Bitmap {
        return when (operation.type) {
            BatchOperationType.RESIZE -> applyResize(bitmap, operation.params)
            BatchOperationType.CROP -> applyCrop(bitmap, operation.params)
            BatchOperationType.ROTATE -> applyRotate(bitmap, operation.params)
            BatchOperationType.FLIP -> applyFlip(bitmap, operation.params)
            BatchOperationType.FILTER -> applyFilter(bitmap, operation.params)
            BatchOperationType.ADJUST -> applyAdjust(bitmap, operation.params)
            BatchOperationType.COMPRESS -> bitmap // 压缩在保存时处理
            BatchOperationType.RENAME -> bitmap // 重命名在保存时处理
            BatchOperationType.FORMAT_CONVERT -> bitmap // 格式转换在保存时处理
        }
    }

    /**
     * 应用尺寸调整
     */
    private fun applyResize(bitmap: Bitmap, params: Map<String, Any>): Bitmap {
        val targetWidth = (params["width"] as? Int) ?: bitmap.width
        val targetHeight = (params["height"] as? Int) ?: bitmap.height
        val mode = (params["mode"] as? ResizeMode) ?: ResizeMode.FIT

        return when (mode) {
            ResizeMode.FIT -> {
                val scale = minOf(
                    targetWidth.toFloat() / bitmap.width,
                    targetHeight.toFloat() / bitmap.height
                )
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }
            ResizeMode.FILL -> {
                val scale = maxOf(
                    targetWidth.toFloat() / bitmap.width,
                    targetHeight.toFloat() / bitmap.height
                )
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                // 裁剪到目标尺寸
                val x = (scaled.width - targetWidth) / 2
                val y = (scaled.height - targetHeight) / 2
                Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)
            }
            ResizeMode.STRETCH -> {
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            }
            ResizeMode.CENTER_CROP -> {
                val scale = maxOf(
                    targetWidth.toFloat() / bitmap.width,
                    targetHeight.toFloat() / bitmap.height
                )
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                val x = (scaled.width - targetWidth) / 2
                val y = (scaled.height - targetHeight) / 2
                Bitmap.createBitmap(scaled, x.coerceAtLeast(0), y.coerceAtLeast(0),
                    minOf(targetWidth, scaled.width), minOf(targetHeight, scaled.height))
            }
        }
    }

    /**
     * 应用裁剪
     */
    private fun applyCrop(bitmap: Bitmap, params: Map<String, Any>): Bitmap {
        val x = (params["x"] as? Int) ?: 0
        val y = (params["y"] as? Int) ?: 0
        val width = (params["width"] as? Int) ?: bitmap.width
        val height = (params["height"] as? Int) ?: bitmap.height

        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        val safeWidth = width.coerceAtMost(bitmap.width - safeX)
        val safeHeight = height.coerceAtMost(bitmap.height - safeY)

        return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth.coerceAtLeast(1), safeHeight.coerceAtLeast(1))
    }

    /**
     * 应用旋转
     */
    private fun applyRotate(bitmap: Bitmap, params: Map<String, Any>): Bitmap {
        val degrees = (params["degrees"] as? Float) ?: 0f
        if (degrees == 0f) return bitmap

        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 应用翻转
     */
    private fun applyFlip(bitmap: Bitmap, params: Map<String, Any>): Bitmap {
        val horizontal = (params["horizontal"] as? Boolean) ?: false
        val vertical = (params["vertical"] as? Boolean) ?: false

        if (!horizontal && !vertical) return bitmap

        val matrix = Matrix()
        if (horizontal) matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        if (vertical) matrix.postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 应用滤镜
     */
    private fun applyFilter(bitmap: Bitmap, params: Map<String, Any>): Bitmap {
        val filterType = (params["filterType"] as? Int) ?: 0
        val imageProcessor = ImageProcessor()
        return imageProcessor.applyFilter(bitmap, mapOf("filterType" to filterType.toFloat()))
    }

    /**
     * 应用调整
     */
    private fun applyAdjust(bitmap: Bitmap, params: Map<String, Any>): Bitmap {
        val brightness = (params["brightness"] as? Float) ?: 0f
        val contrast = (params["contrast"] as? Float) ?: 1f
        val saturation = (params["saturation"] as? Float) ?: 1f

        val imageProcessor = ImageProcessor()
        return imageProcessor.applyAdjustments(bitmap, com.photomaster.ui.editor.AdjustParams(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation
        ))
    }

    /**
     * 保存位图到文件
     */
    private fun saveBitmap(
        bitmap: Bitmap,
        originalName: String,
        settings: OutputSettings,
        index: Int
    ): Uri? {
        val format = when (settings.format) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP -> Bitmap.CompressFormat.WEBP
        }

        val extension = when (settings.format) {
            OutputFormat.JPEG -> "jpg"
            OutputFormat.PNG -> "png"
            OutputFormat.WEBP -> "webp"
        }

        // 生成文件名
        val baseName = originalName.substringBeforeLast(".")
        val newName = "${settings.fileNamePrefix}${baseName}${settings.fileNameSuffix}_${index}.${extension}"

        return try {
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
                put(MediaStore.MediaColumns.MIME_TYPE, when (settings.format) {
                    OutputFormat.JPEG -> "image/jpeg"
                    OutputFormat.PNG -> "image/png"
                    OutputFormat.WEBP -> "image/webp"
                })
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoPS_Batch")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(format, settings.quality, outputStream)
                }
            }

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从URI加载位图
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从URI获取文件名
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                File(path).name
            }
        }
        return result
    }

    /**
     * 更新项目状态
     */
    private fun updateItemStatus(
        itemId: String,
        status: BatchItemStatus,
        progress: Float,
        resultUri: Uri? = null,
        errorMessage: String? = null
    ) {
        _session.value = _session.value?.let { session ->
            session.copy(
                items = session.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            status = status,
                            progress = progress,
                            resultUri = resultUri,
                            errorMessage = errorMessage
                        )
                    } else item
                }
            )
        }
    }

    /**
     * 更新项目进度
     */
    private fun updateItemProgress(itemId: String, progress: Float) {
        _session.value = _session.value?.let { session ->
            session.copy(
                items = session.items.map { item ->
                    if (item.id == itemId) item.copy(progress = progress) else item
                }
            )
        }
    }

    /**
     * 取消批处理
     */
    fun cancel() {
        isCancelled = true
        _session.value = _session.value?.copy(status = BatchSessionStatus.CANCELLED)
    }

    /**
     * 暂停批处理
     */
    fun pause() {
        _session.value = _session.value?.copy(status = BatchSessionStatus.PAUSED)
    }

    /**
     * 恢复批处理
     */
    fun resume() {
        _session.value = _session.value?.copy(status = BatchSessionStatus.RUNNING)
    }

    /**
     * 重置会话
     */
    fun reset() {
        isCancelled = false
        _session.value = null
    }
}
