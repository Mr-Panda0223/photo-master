package com.photomaster.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photomaster.data.model.ImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageDataSource(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * 获取相册中的所有图片
     */
    suspend fun getAllImages(): List<ImageModel> = withContext(Dispatchers.IO) {
        val images = mutableListOf<ImageModel>()

        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE
            )
        } else {
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE
            )
        }

        val selection = "${MediaStore.Images.Media.MIME_TYPE} IN (?, ?)"
        val selectionArgs = arrayOf("image/jpeg", "image/png")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                images.add(
                    ImageModel(
                        id = id,
                        uri = uri,
                        name = cursor.getString(nameColumn),
                        dateAdded = cursor.getLong(dateColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        size = cursor.getLong(sizeColumn),
                        mimeType = cursor.getString(mimeTypeColumn)
                    )
                )
            }
        }

        images
    }

    /**
     * 获取最近添加的图片（限制数量）
     */
    suspend fun getRecentImages(limit: Int = 20): List<ImageModel> = withContext(Dispatchers.IO) {
        getAllImages().take(limit)
    }

    /**
     * 根据ID获取单张图片
     */
    fun getImageById(id: Long): ImageModel? {
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE
        )

        contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                return ImageModel(
                    id = cursor.getLong(idColumn),
                    uri = uri,
                    name = cursor.getString(nameColumn),
                    dateAdded = cursor.getLong(dateColumn),
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    size = cursor.getLong(sizeColumn),
                    mimeType = cursor.getString(mimeTypeColumn)
                )
            }
        }

        return null
    }
}
