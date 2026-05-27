package com.photomaster.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.photomaster.data.local.CompletedImagesManager
import com.photomaster.data.local.DeletedItemsManager
import com.photomaster.data.local.DraftManager
import com.photomaster.data.local.ImageDataSource
import com.photomaster.data.model.DeletedItemModel
import com.photomaster.data.model.DraftModel
import com.photomaster.data.model.EditedImageModel
import com.photomaster.data.model.ImageModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ImageRepository(context: Context) {

    private val imageDataSource = ImageDataSource(context)
    private val draftManager = DraftManager.getInstance(context)
    private val deletedItemsManager = DeletedItemsManager.getInstance(context)
    private val completedImagesManager = CompletedImagesManager.getInstance(context)

    /**
     * 获取所有相册图片
     */
    fun getAllImages(): Flow<List<ImageModel>> = flow {
        emit(imageDataSource.getAllImages())
    }

    /**
     * 获取最近图片
     */
    fun getRecentImages(limit: Int = 20): Flow<List<ImageModel>> = flow {
        emit(imageDataSource.getRecentImages(limit))
    }

    /**
     * 获取草稿列表
     */
    fun getDrafts(): Flow<List<DraftModel>> = draftManager.getDrafts()

    /**
     * 保存草稿
     * @param existingDraftId 如果提供，则更新已有草稿；否则创建新草稿
     */
    suspend fun saveDraft(
        originalUri: Uri,
        editedBitmap: Bitmap,
        name: String = "",
        existingDraftId: String? = null
    ): DraftModel {
        val draftName = name.ifEmpty { "编辑_${System.currentTimeMillis()}" }
        return draftManager.saveDraft(originalUri, editedBitmap, draftName, existingDraftId)
    }

    /**
     * 删除草稿（移动到已删除）
     */
    suspend fun deleteDraft(draft: DraftModel) {
        deletedItemsManager.deleteDraft(draft)
    }

    /**
     * 直接删除草稿（不移到已删除，用于更新草稿时）
     */
    suspend fun permanentlyDeleteDraft(draftId: String) {
        draftManager.deleteDraft(draftId)
    }

    /**
     * 删除已完成图片（移动到已删除）
     */
    suspend fun deleteCompletedImage(image: EditedImageModel) {
        deletedItemsManager.deleteCompletedImage(image)
    }

    /**
     * 获取已删除项目列表
     */
    fun getDeletedItems(): Flow<List<DeletedItemModel>> = deletedItemsManager.getDeletedItems()

    /**
     * 永久删除已删除项目
     */
    suspend fun permanentlyDeleteItem(itemId: String) {
        deletedItemsManager.permanentlyDelete(itemId)
    }

    /**
     * 清空所有已删除项目
     */
    suspend fun clearAllDeletedItems() {
        deletedItemsManager.clearAllDeleted()
    }

    /**
     * 获取草稿图片文件
     */
    fun getDraftImageFile(draftId: String): java.io.File? {
        return draftManager.getDraftImageFile(draftId)
    }

    /**
     * 获取已编辑完成的图片
     */
    fun getEditedImages(): Flow<List<EditedImageModel>> = completedImagesManager.getCompletedImages()

    /**
     * 保存已完成图片
     */
    suspend fun saveCompletedImage(image: EditedImageModel) {
        completedImagesManager.saveCompletedImage(image)
    }

    /**
     * 根据ID获取图片
     */
    fun getImageById(id: Long): ImageModel? {
        return imageDataSource.getImageById(id)
    }
}
