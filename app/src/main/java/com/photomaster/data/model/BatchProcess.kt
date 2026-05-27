package com.photomaster.data.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

/**
 * 批处理操作类型
 */
enum class BatchOperationType {
    RESIZE,         // 调整尺寸
    CROP,           // 裁剪
    ROTATE,         // 旋转
    FLIP,           // 翻转
    FILTER,         // 滤镜
    ADJUST,         // 基础调整
    COMPRESS,       // 压缩
    RENAME,         // 重命名
    FORMAT_CONVERT  // 格式转换
}

/**
 * 批处理操作
 */
data class BatchOperation(
    val id: String = UUID.randomUUID().toString(),
    val type: BatchOperationType,
    val params: Map<String, Any> = emptyMap(),
    val enabled: Boolean = true
)

/**
 * 批处理任务
 */
data class BatchTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "批处理任务",
    val operations: List<BatchOperation> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 添加操作
     */
    fun addOperation(operation: BatchOperation): BatchTask {
        return copy(operations = operations + operation)
    }

    /**
     * 移除操作
     */
    fun removeOperation(operationId: String): BatchTask {
        return copy(operations = operations.filter { it.id != operationId })
    }

    /**
     * 更新操作
     */
    fun updateOperation(operationId: String, newOperation: BatchOperation): BatchTask {
        return copy(operations = operations.map {
            if (it.id == operationId) newOperation else it
        })
    }

    /**
     * 启用/禁用操作
     */
    fun toggleOperation(operationId: String): BatchTask {
        return copy(operations = operations.map {
            if (it.id == operationId) it.copy(enabled = !it.enabled) else it
        })
    }

    /**
     * 移动操作顺序
     */
    fun moveOperation(operationId: String, direction: Int): BatchTask {
        val index = operations.indexOfFirst { it.id == operationId }
        if (index == -1) return this

        val newIndex = (index + direction).coerceIn(0, operations.size - 1)
        if (newIndex == index) return this

        val mutableList = operations.toMutableList()
        val operation = mutableList.removeAt(index)
        mutableList.add(newIndex, operation)

        return copy(operations = mutableList)
    }
}

/**
 * 批处理项目
 */
data class BatchItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val status: BatchItemStatus = BatchItemStatus.PENDING,
    val progress: Float = 0f,
    val resultUri: Uri? = null,
    val errorMessage: String? = null,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val processedBitmap: Bitmap? = null
)

/**
 * 批处理项目状态
 */
enum class BatchItemStatus {
    PENDING,        // 等待处理
    PROCESSING,     // 处理中
    COMPLETED,      // 已完成
    FAILED,         // 失败
    CANCELLED       // 已取消
}

/**
 * 批处理会话
 */
data class BatchSession(
    val id: String = UUID.randomUUID().toString(),
    val task: BatchTask,
    val items: List<BatchItem> = emptyList(),
    val status: BatchSessionStatus = BatchSessionStatus.IDLE,
    val currentIndex: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null
) {
    /**
     * 获取总体进度
     */
    fun getOverallProgress(): Float {
        if (items.isEmpty()) return 0f
        return items.sumOf { it.progress.toDouble() }.toFloat() / items.size
    }

    /**
     * 是否全部完成
     */
    fun isAllCompleted(): Boolean {
        return items.all { it.status == BatchItemStatus.COMPLETED }
    }

    /**
     * 获取待处理项目
     */
    fun getPendingItems(): List<BatchItem> {
        return items.filter { it.status == BatchItemStatus.PENDING }
    }
}

/**
 * 批处理会话状态
 */
enum class BatchSessionStatus {
    IDLE,           // 空闲
    RUNNING,        // 运行中
    PAUSED,         // 暂停
    COMPLETED,      // 完成
    CANCELLED       // 取消
}

/**
 * 输出设置
 */
data class OutputSettings(
    val format: OutputFormat = OutputFormat.JPEG,
    val quality: Int = 95,
    val outputDirectory: String? = null,
    val fileNamePrefix: String = "",
    val fileNameSuffix: String = "",
    val overwriteExisting: Boolean = false
)

/**
 * 输出格式
 */
enum class OutputFormat {
    JPEG, PNG, WEBP
}

/**
 * 尺寸调整模式
 */
enum class ResizeMode {
    FIT,            // 适应（保持比例）
    FILL,           // 填充（可能裁剪）
    STRETCH,        // 拉伸
    CENTER_CROP     // 居中裁剪
}

/**
 * 批量处理预设类型
 */
enum class BatchPreset {
    SOCIAL_MEDIA,   // 社交媒体
    WEB_OPTIMIZE,   // 网页优化
    PRINT_READY,    // 打印准备
    THUMBNAIL       // 缩略图
}

/**
 * 预设批处理模板
 */
object BatchPresets {

    /**
     * 社交媒体分享模板
     */
    fun socialMediaPreset(): BatchTask {
        return BatchTask(
            name = "社交媒体分享",
            operations = listOf(
                BatchOperation(
                    type = BatchOperationType.RESIZE,
                    params = mapOf(
                        "width" to 1080,
                        "height" to 1080,
                        "mode" to ResizeMode.FIT
                    )
                ),
                BatchOperation(
                    type = BatchOperationType.COMPRESS,
                    params = mapOf(
                        "quality" to 85,
                        "maxFileSize" to 5 * 1024 * 1024 // 5MB
                    )
                )
            )
        )
    }

    /**
     * 网页优化模板
     */
    fun webOptimizePreset(): BatchTask {
        return BatchTask(
            name = "网页优化",
            operations = listOf(
                BatchOperation(
                    type = BatchOperationType.RESIZE,
                    params = mapOf(
                        "width" to 1920,
                        "height" to 1080,
                        "mode" to ResizeMode.FIT
                    )
                ),
                BatchOperation(
                    type = BatchOperationType.COMPRESS,
                    params = mapOf(
                        "quality" to 80
                    )
                ),
                BatchOperation(
                    type = BatchOperationType.FORMAT_CONVERT,
                    params = mapOf(
                        "format" to OutputFormat.WEBP
                    )
                )
            )
        )
    }

    /**
     * 打印准备模板
     */
    fun printReadyPreset(): BatchTask {
        return BatchTask(
            name = "打印准备",
            operations = listOf(
                BatchOperation(
                    type = BatchOperationType.RESIZE,
                    params = mapOf(
                        "dpi" to 300,
                        "width" to 3508,  // A4 at 300dpi
                        "height" to 4961
                    )
                ),
                BatchOperation(
                    type = BatchOperationType.ADJUST,
                    params = mapOf(
                        "sharpness" to 20
                    )
                )
            )
        )
    }

    /**
     * 缩略图生成模板
     */
    fun thumbnailPreset(): BatchTask {
        return BatchTask(
            name = "生成缩略图",
            operations = listOf(
                BatchOperation(
                    type = BatchOperationType.RESIZE,
                    params = mapOf(
                        "width" to 200,
                        "height" to 200,
                        "mode" to ResizeMode.CENTER_CROP
                    )
                ),
                BatchOperation(
                    type = BatchOperationType.COMPRESS,
                    params = mapOf(
                        "quality" to 70
                    )
                )
            )
        )
    }

    /**
     * 获取所有预设
     */
    fun getAllPresets(): List<BatchTask> {
        return listOf(
            socialMediaPreset(),
            webOptimizePreset(),
            printReadyPreset(),
            thumbnailPreset()
        )
    }
}

/**
 * 批处理结果
 */
data class BatchResult(
    val sessionId: String,
    val successCount: Int,
    val failedCount: Int,
    val totalCount: Int,
    val outputUris: List<Uri>,
    val errors: List<Pair<String, String>> // (fileName, errorMessage)
)
