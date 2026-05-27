package com.photomaster.data.local

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.photomaster.data.model.DraftModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 草稿管理器 - 管理编辑中的图片草稿
 * 使用单例模式确保全局只有一个实例
 */
class DraftManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _draftsFlow = MutableStateFlow<List<DraftModel>>(emptyList())

    companion object {
        private const val PREFS_NAME = "photo_master_drafts"
        private const val KEY_DRAFTS = "drafts"
        private const val DRAFTS_DIR = "drafts"
        private const val THUMBNAILS_DIR = "thumbnails"
        
        @Volatile
        private var instance: DraftManager? = null
        
        fun getInstance(context: Context): DraftManager {
            return instance ?: synchronized(this) {
                instance ?: DraftManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        // Load drafts on init
        loadDrafts()
    }

    /**
     * 获取所有草稿
     */
    fun getDrafts(): Flow<List<DraftModel>> = _draftsFlow.asStateFlow()

    /**
     * 保存草稿
     * @param existingDraftId 如果提供，则更新已有草稿；否则创建新草稿
     */
    suspend fun saveDraft(
        originalUri: Uri,
        editedBitmap: Bitmap,
        name: String = "编辑_${System.currentTimeMillis()}",
        existingDraftId: String? = null
    ): DraftModel = withContext(Dispatchers.IO) {
        // 如果有existingDraftId，先删除旧文件
        existingDraftId?.let { draftId ->
            val draftsDir = File(context.filesDir, DRAFTS_DIR)
            val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR)
            File(draftsDir, "${draftId}.jpg").delete()
            File(thumbnailsDir, "${draftId}_thumb.jpg").delete()
        }

        val draftId = existingDraftId ?: UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Save edited image
        val draftsDir = File(context.filesDir, DRAFTS_DIR).apply { mkdirs() }
        val imageFile = File(draftsDir, "${draftId}.jpg")

        FileOutputStream(imageFile).use { out ->
            editedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        // Save thumbnail
        val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR).apply { mkdirs() }
        val thumbnailFile = File(thumbnailsDir, "${draftId}_thumb.jpg")

        val thumbnailBitmap = Bitmap.createScaledBitmap(editedBitmap, 200, 200, true)
        FileOutputStream(thumbnailFile).use { out ->
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        val draft = DraftModel(
            id = draftId,
            name = name,
            originalImageUri = originalUri,
            lastEditTime = timestamp,
            thumbnailUri = Uri.fromFile(thumbnailFile)
        )

        // Save to preferences
        if (existingDraftId != null) {
            updateDraftInPrefs(draft)
        } else {
            saveDraftToPrefs(draft)
        }

        // Update flow
        loadDrafts()

        draft
    }

    /**
     * 删除草稿（永久删除）
     */
    suspend fun deleteDraft(draftId: String) = withContext(Dispatchers.IO) {
        // Delete files
        val draftsDir = File(context.filesDir, DRAFTS_DIR)
        val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR)

        File(draftsDir, "${draftId}.jpg").delete()
        File(thumbnailsDir, "${draftId}_thumb.jpg").delete()

        // Remove from preferences
        removeDraftFromPrefs(draftId)
    }

    /**
     * 从 preferences 中移除草稿（供 DeletedItemsManager 调用）
     */
    fun removeDraftFromPrefs(draftId: String) {
        val drafts = getDraftsFromPrefs().toMutableList()
        drafts.removeAll { it.id == draftId }
        saveDraftsToPrefs(drafts)

        // Update flow
        loadDrafts()
    }

    /**
     * 获取草稿图片文件
     */
    fun getDraftImageFile(draftId: String): File? {
        val file = File(context.filesDir, "$DRAFTS_DIR/${draftId}.jpg")
        return if (file.exists()) file else null
    }

    private fun loadDrafts() {
        _draftsFlow.value = getDraftsFromPrefs()
    }

    private fun getDraftsFromPrefs(): List<DraftModel> {
        val jsonString = prefs.getString(KEY_DRAFTS, "[]") ?: "[]"
        return parseDraftsJson(jsonString)
    }

    private fun saveDraftToPrefs(draft: DraftModel) {
        val drafts = getDraftsFromPrefs().toMutableList()
        drafts.add(0, draft) // Add to beginning
        saveDraftsToPrefs(drafts)
    }

    private fun updateDraftInPrefs(updatedDraft: DraftModel) {
        val drafts = getDraftsFromPrefs().toMutableList()
        // 移除旧的草稿
        drafts.removeAll { it.id == updatedDraft.id }
        // 将更新的草稿添加到最前面（最新的在前面）
        drafts.add(0, updatedDraft)
        saveDraftsToPrefs(drafts)
    }

    private fun saveDraftsToPrefs(drafts: List<DraftModel>) {
        val jsonArray = JSONArray()
        drafts.forEach { draft ->
            jsonArray.put(draftToJson(draft))
        }
        prefs.edit().putString(KEY_DRAFTS, jsonArray.toString()).apply()
    }

    private fun draftToJson(draft: DraftModel): JSONObject {
        return JSONObject().apply {
            put("id", draft.id)
            put("name", draft.name)
            put("originalUri", draft.originalImageUri.toString())
            put("lastEditTime", draft.lastEditTime)
            put("thumbnailUri", draft.thumbnailUri?.toString())
        }
    }

    private fun parseDraftsJson(jsonString: String): List<DraftModel> {
        val drafts = mutableListOf<DraftModel>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                drafts.add(
                    DraftModel(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        originalImageUri = Uri.parse(obj.getString("originalUri")),
                        lastEditTime = obj.getLong("lastEditTime"),
                        thumbnailUri = obj.optString("thumbnailUri")?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return drafts
    }
}
