package com.photomaster.data.local

import android.content.Context
import android.net.Uri
import com.photomaster.data.model.DeletedItemModel
import com.photomaster.data.model.DeletedItemType
import com.photomaster.data.model.DraftModel
import com.photomaster.data.model.EditedImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 已删除项目管理器 - 管理被删除的草稿和已完成编辑
 * 使用单例模式确保全局只有一个实例
 */
class DeletedItemsManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _deletedItemsFlow = MutableStateFlow<List<DeletedItemModel>>(emptyList())

    companion object {
        private const val PREFS_NAME = "photo_master_deleted"
        private const val KEY_DELETED = "deleted_items"
        private const val DELETED_DIR = "deleted"
        private const val DELETED_THUMBNAILS_DIR = "deleted_thumbnails"
        
        @Volatile
        private var instance: DeletedItemsManager? = null
        
        fun getInstance(context: Context): DeletedItemsManager {
            return instance ?: synchronized(this) {
                instance ?: DeletedItemsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        loadDeletedItems()
    }

    /**
     * 获取所有已删除项目
     */
    fun getDeletedItems(): Flow<List<DeletedItemModel>> = _deletedItemsFlow.asStateFlow()

    /**
     * 将草稿移动到已删除
     */
    suspend fun deleteDraft(draft: DraftModel) = withContext(Dispatchers.IO) {
        // 检查是否已经在已删除列表中
        val existingItems = getDeletedItemsFromPrefs()
        if (existingItems.any { it.id == draft.id }) {
            // 已经删除过了，直接返回
            return@withContext
        }

        // 先从 DraftManager 中移除该草稿（使用单例获取）
        val draftManager = DraftManager.getInstance(context)
        draftManager.removeDraftFromPrefs(draft.id)

        // 移动草稿文件到已删除目录
        val draftsDir = File(context.filesDir, "drafts")
        val thumbnailsDir = File(context.filesDir, "thumbnails")
        val deletedDir = File(context.filesDir, DELETED_DIR).apply { mkdirs() }
        val deletedThumbnailsDir = File(context.filesDir, DELETED_THUMBNAILS_DIR).apply { mkdirs() }

        val draftFile = File(draftsDir, "${draft.id}.jpg")
        val thumbnailFile = File(thumbnailsDir, "${draft.id}_thumb.jpg")
        
        val deletedFile = File(deletedDir, "${draft.id}.jpg")
        val deletedThumbnailFile = File(deletedThumbnailsDir, "${draft.id}_thumb.jpg")

        // 移动文件（如果文件还存在的话）
        if (draftFile.exists()) {
            draftFile.renameTo(deletedFile)
        }
        if (thumbnailFile.exists()) {
            thumbnailFile.renameTo(deletedThumbnailFile)
        }

        // 创建已删除项目记录
        val deletedItem = DeletedItemModel(
            id = draft.id,
            name = draft.name,
            imageUri = if (deletedFile.exists()) Uri.fromFile(deletedFile) else draft.originalImageUri,
            thumbnailUri = if (deletedThumbnailFile.exists()) Uri.fromFile(deletedThumbnailFile) else draft.thumbnailUri,
            deletedTime = System.currentTimeMillis(),
            originalType = DeletedItemType.DRAFT,
            originalEditTime = draft.lastEditTime
        )

        // 保存到 preferences
        saveDeletedItemToPrefs(deletedItem)
        loadDeletedItems()
    }

    /**
     * 将已完成编辑移动到已删除
     */
    suspend fun deleteCompletedImage(image: EditedImageModel) = withContext(Dispatchers.IO) {
        // 检查是否已经在已删除列表中
        val existingItems = getDeletedItemsFromPrefs()
        if (existingItems.any { it.id == image.id.toString() }) {
            // 已经删除过了，直接返回
            return@withContext
        }

        // 先从 CompletedImagesManager 中移除（使用单例获取）
        val completedImagesManager = CompletedImagesManager.getInstance(context)
        completedImagesManager.removeCompletedImage(image.id)

        val deletedDir = File(context.filesDir, DELETED_DIR).apply { mkdirs() }
        val deletedThumbnailsDir = File(context.filesDir, DELETED_THUMBNAILS_DIR).apply { mkdirs() }

        // 创建已删除项目记录
        val deletedItem = DeletedItemModel(
            id = image.id.toString(),
            name = image.name,
            imageUri = image.uri,
            thumbnailUri = null, // 已完成的图片使用原图作为缩略图
            deletedTime = System.currentTimeMillis(),
            originalType = DeletedItemType.COMPLETED,
            originalEditTime = image.dateEdited
        )

        // 保存到 preferences
        saveDeletedItemToPrefs(deletedItem)
        loadDeletedItems()
    }

    /**
     * 永久删除已删除项目
     */
    suspend fun permanentlyDelete(itemId: String) = withContext(Dispatchers.IO) {
        // 删除文件
        val deletedDir = File(context.filesDir, DELETED_DIR)
        val deletedThumbnailsDir = File(context.filesDir, DELETED_THUMBNAILS_DIR)

        File(deletedDir, "${itemId}.jpg").delete()
        File(deletedThumbnailsDir, "${itemId}_thumb.jpg").delete()

        // 从 preferences 移除
        val items = getDeletedItemsFromPrefs().toMutableList()
        items.removeAll { it.id == itemId }
        saveDeletedItemsToPrefs(items)

        loadDeletedItems()
    }

    /**
     * 清空所有已删除项目
     */
    suspend fun clearAllDeleted() = withContext(Dispatchers.IO) {
        // 删除所有文件
        val deletedDir = File(context.filesDir, DELETED_DIR)
        val deletedThumbnailsDir = File(context.filesDir, DELETED_THUMBNAILS_DIR)

        deletedDir.listFiles()?.forEach { it.delete() }
        deletedThumbnailsDir.listFiles()?.forEach { it.delete() }

        // 清空 preferences
        prefs.edit().remove(KEY_DELETED).apply()

        loadDeletedItems()
    }

    /**
     * 恢复已删除项目（可选功能）
     */
    suspend fun restoreItem(itemId: String): Boolean = withContext(Dispatchers.IO) {
        // 这里可以实现恢复逻辑，如果需要的话
        // 暂时返回 false，表示不支持恢复
        false
    }

    private fun loadDeletedItems() {
        _deletedItemsFlow.value = getDeletedItemsFromPrefs()
    }

    private fun getDeletedItemsFromPrefs(): List<DeletedItemModel> {
        val jsonString = prefs.getString(KEY_DELETED, "[]") ?: "[]"
        return parseDeletedItemsJson(jsonString)
    }

    private fun saveDeletedItemToPrefs(item: DeletedItemModel) {
        val items = getDeletedItemsFromPrefs().toMutableList()
        items.add(0, item) // 添加到最前面
        saveDeletedItemsToPrefs(items)
    }

    private fun saveDeletedItemsToPrefs(items: List<DeletedItemModel>) {
        val jsonArray = JSONArray()
        items.forEach { item ->
            jsonArray.put(deletedItemToJson(item))
        }
        prefs.edit().putString(KEY_DELETED, jsonArray.toString()).apply()
    }

    private fun deletedItemToJson(item: DeletedItemModel): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("imageUri", item.imageUri.toString())
            put("thumbnailUri", item.thumbnailUri?.toString())
            put("deletedTime", item.deletedTime)
            put("originalType", item.originalType.name)
            put("originalEditTime", item.originalEditTime)
        }
    }

    private fun parseDeletedItemsJson(jsonString: String): List<DeletedItemModel> {
        val items = mutableListOf<DeletedItemModel>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    DeletedItemModel(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        imageUri = Uri.parse(obj.getString("imageUri")),
                        thumbnailUri = obj.optString("thumbnailUri")?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) },
                        deletedTime = obj.getLong("deletedTime"),
                        originalType = DeletedItemType.valueOf(obj.getString("originalType")),
                        originalEditTime = obj.getLong("originalEditTime")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }
}
