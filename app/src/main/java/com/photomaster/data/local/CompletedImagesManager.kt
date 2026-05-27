package com.photomaster.data.local

import android.content.Context
import android.net.Uri
import com.photomaster.data.model.EditedImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 已完成图片管理器 - 管理已编辑完成并导出的图片
 * 使用单例模式确保全局只有一个实例
 */
class CompletedImagesManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _completedImagesFlow = MutableStateFlow<List<EditedImageModel>>(emptyList())

    companion object {
        private const val PREFS_NAME = "photo_master_completed"
        private const val KEY_COMPLETED = "completed_images"
        
        @Volatile
        private var instance: CompletedImagesManager? = null
        
        fun getInstance(context: Context): CompletedImagesManager {
            return instance ?: synchronized(this) {
                instance ?: CompletedImagesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        loadCompletedImages()
    }

    /**
     * 获取所有已完成图片
     */
    fun getCompletedImages(): Flow<List<EditedImageModel>> = _completedImagesFlow.asStateFlow()

    /**
     * 保存已完成图片
     */
    suspend fun saveCompletedImage(image: EditedImageModel) = withContext(Dispatchers.IO) {
        val images = getCompletedImagesFromPrefs().toMutableList()
        // 如果已存在则更新，否则添加
        val index = images.indexOfFirst { it.id == image.id }
        if (index != -1) {
            images[index] = image
        } else {
            images.add(0, image)
        }
        saveCompletedImagesToPrefs(images)
        loadCompletedImages()
    }

    /**
     * 删除已完成图片（从列表中移除，供 DeletedItemsManager 调用）
     */
    fun removeCompletedImage(imageId: Long) {
        val images = getCompletedImagesFromPrefs().toMutableList()
        images.removeAll { it.id == imageId }
        saveCompletedImagesToPrefs(images)
        loadCompletedImages()
    }

    /**
     * 根据ID获取已完成图片
     */
    fun getCompletedImageById(id: Long): EditedImageModel? {
        return getCompletedImagesFromPrefs().find { it.id == id }
    }

    private fun loadCompletedImages() {
        _completedImagesFlow.value = getCompletedImagesFromPrefs()
    }

    private fun getCompletedImagesFromPrefs(): List<EditedImageModel> {
        val jsonString = prefs.getString(KEY_COMPLETED, "[]") ?: "[]"
        return parseCompletedImagesJson(jsonString)
    }

    private fun saveCompletedImagesToPrefs(images: List<EditedImageModel>) {
        val jsonArray = JSONArray()
        images.forEach { image ->
            jsonArray.put(completedImageToJson(image))
        }
        prefs.edit().putString(KEY_COMPLETED, jsonArray.toString()).apply()
    }

    private fun completedImageToJson(image: EditedImageModel): JSONObject {
        return JSONObject().apply {
            put("id", image.id)
            put("name", image.name)
            put("uri", image.uri.toString())
            put("originalUri", image.originalImageUri.toString())
            put("dateEdited", image.dateEdited)
            put("width", image.width)
            put("height", image.height)
        }
    }

    private fun parseCompletedImagesJson(jsonString: String): List<EditedImageModel> {
        val images = mutableListOf<EditedImageModel>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                images.add(
                    EditedImageModel(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        uri = Uri.parse(obj.getString("uri")),
                        originalImageUri = Uri.parse(obj.getString("originalUri")),
                        dateEdited = obj.getLong("dateEdited"),
                        width = obj.getInt("width"),
                        height = obj.getInt("height")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return images
    }
}
