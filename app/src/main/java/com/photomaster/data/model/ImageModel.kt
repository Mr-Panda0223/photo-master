package com.photomaster.data.model

import android.net.Uri

/**
 * 图片数据模型
 */
data class ImageModel(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val size: Long,
    val mimeType: String
)

/**
 * 草稿数据模型
 */
data class DraftModel(
    val id: String,
    val name: String,
    val originalImageUri: Uri,
    val lastEditTime: Long,
    val thumbnailUri: Uri? = null
)

/**
 * 已编辑完成的图片
 */
data class EditedImageModel(
    val id: Long,
    val uri: Uri,
    val name: String,
    val originalImageUri: Uri,
    val dateEdited: Long,
    val width: Int,
    val height: Int
)

/**
 * 删除项目的类型
 */
enum class DeletedItemType {
    DRAFT,      // 原本是编辑中的草稿
    COMPLETED   // 原本是已完成的编辑
}

/**
 * 已删除的项目
 */
data class DeletedItemModel(
    val id: String,
    val name: String,
    val imageUri: Uri,
    val thumbnailUri: Uri? = null,
    val deletedTime: Long,
    val originalType: DeletedItemType,  // 原本是什么类型
    val originalEditTime: Long          // 原本的编辑/完成时间
)
