package com.photomaster.domain.manager

import android.content.Context
import android.os.StatFs
import com.photomaster.data.local.CompletedImagesManager
import com.photomaster.data.local.DeletedItemsManager
import com.photomaster.data.local.DraftManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 存储信息
 */
data class StorageInfo(
    val totalSpace: Long,           // 总空间
    val availableSpace: Long,       // 可用空间
    val appCacheSize: Long,         // 应用缓存大小
    val draftsSize: Long,           // 草稿占用空间
    val completedSize: Long,        // 已完成作品占用空间
    val deletedSize: Long           // 回收站占用空间
) {
    val usedSpace: Long = totalSpace - availableSpace
    val appTotalSize: Long = appCacheSize + draftsSize + completedSize + deletedSize

    fun getTotalSpaceGB(): String = formatSize(totalSpace)
    fun getAvailableSpaceGB(): String = formatSize(availableSpace)
    fun getUsedSpaceGB(): String = formatSize(usedSpace)
    fun getAppCacheSizeMB(): String = formatSize(appCacheSize)
    fun getDraftsSizeMB(): String = formatSize(draftsSize)
    fun getCompletedSizeMB(): String = formatSize(completedSize)
    fun getDeletedSizeMB(): String = formatSize(deletedSize)
    fun getAppTotalSizeMB(): String = formatSize(appTotalSize)

    private fun formatSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size.toDouble() / (1024 * 1024 * 1024))
            size >= 1024 * 1024 -> String.format("%.2f MB", size.toDouble() / (1024 * 1024))
            size >= 1024 -> String.format("%.2f KB", size.toDouble() / 1024)
            else -> "$size B"
        }
    }
}

/**
 * 存储管理器
 */
class StorageManager(private val context: Context) {

    private val draftManager = DraftManager.getInstance(context)
    private val completedManager = CompletedImagesManager.getInstance(context)
    private val deletedManager = DeletedItemsManager.getInstance(context)

    /**
     * 获取存储信息
     */
    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val statFs = StatFs(context.filesDir.path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong

        val totalSpace = totalBlocks * blockSize
        val availableSpace = availableBlocks * blockSize

        val appCacheSize = calculateCacheSize()
        val draftsSize = calculateDraftsSize()
        val completedSize = calculateCompletedSize()
        val deletedSize = calculateDeletedSize()

        StorageInfo(
            totalSpace = totalSpace,
            availableSpace = availableSpace,
            appCacheSize = appCacheSize,
            draftsSize = draftsSize,
            completedSize = completedSize,
            deletedSize = deletedSize
        )
    }

    /**
     * 清理应用缓存
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 清理内部缓存
            context.cacheDir.deleteRecursively()
            context.cacheDir.mkdirs()

            // 清理外部缓存
            context.externalCacheDir?.let { externalCache ->
                externalCache.deleteRecursively()
                externalCache.mkdirs()
            }

            // 清理临时文件
            clearTempFiles()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清理临时文件
     */
    private fun clearTempFiles() {
        val tempDir = File(context.filesDir, "temp")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000) {
                    file.delete()
                }
            }
        }
    }

    /**
     * 计算缓存大小
     */
    private fun calculateCacheSize(): Long {
        var size = 0L
        size += getFolderSize(context.cacheDir)
        size += getFolderSize(context.externalCacheDir)
        return size
    }

    /**
     * 计算草稿占用空间
     */
    private fun calculateDraftsSize(): Long {
        val draftsDir = File(context.filesDir, "drafts")
        return if (draftsDir.exists()) getFolderSize(draftsDir) else 0L
    }

    /**
     * 计算已完成作品占用空间
     */
    private fun calculateCompletedSize(): Long {
        val completedDir = File(context.filesDir, "completed")
        return if (completedDir.exists()) getFolderSize(completedDir) else 0L
    }

    /**
     * 计算回收站占用空间
     */
    private fun calculateDeletedSize(): Long {
        val deletedDir = File(context.filesDir, "deleted")
        return if (deletedDir.exists()) getFolderSize(deletedDir) else 0L
    }

    /**
     * 获取文件夹大小
     */
    private fun getFolderSize(folder: File?): Long {
        if (folder == null || !folder.exists()) return 0L

        var size = 0L
        folder.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getFolderSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    /**
     * 获取草稿数量
     */
    suspend fun getDraftsCount(): Int = withContext(Dispatchers.IO) {
        val draftsDir = File(context.filesDir, "drafts")
        if (draftsDir.exists()) {
            draftsDir.listFiles { file -> file.extension == "jpg" }?.size ?: 0
        } else {
            0
        }
    }

    /**
     * 获取已完成作品数量
     */
    suspend fun getCompletedCount(): Int = withContext(Dispatchers.IO) {
        val completedDir = File(context.filesDir, "completed")
        if (completedDir.exists()) {
            completedDir.listFiles { file -> file.extension == "jpg" }?.size ?: 0
        } else {
            0
        }
    }

    /**
     * 获取回收站项目数量
     */
    suspend fun getDeletedCount(): Int = withContext(Dispatchers.IO) {
        val deletedDir = File(context.filesDir, "deleted")
        if (deletedDir.exists()) {
            deletedDir.listFiles { file -> file.extension == "jpg" }?.size ?: 0
        } else {
            0
        }
    }
}
