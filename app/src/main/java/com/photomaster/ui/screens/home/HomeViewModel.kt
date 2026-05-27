package com.photomaster.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.photomaster.data.model.DeletedItemModel
import com.photomaster.data.model.DraftModel
import com.photomaster.data.model.EditedImageModel
import com.photomaster.data.model.ImageModel
import com.photomaster.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {

    // 当前选中的Tab
    private val _selectedTab = MutableStateFlow(HomeTab.DRAFTS)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    // 草稿列表
    private val _drafts = MutableStateFlow<List<DraftModel>>(emptyList())
    val drafts: StateFlow<List<DraftModel>> = _drafts.asStateFlow()

    // 已完成图片列表
    private val _completedImages = MutableStateFlow<List<EditedImageModel>>(emptyList())
    val completedImages: StateFlow<List<EditedImageModel>> = _completedImages.asStateFlow()

    // 已删除项目列表
    private val _deletedItems = MutableStateFlow<List<DeletedItemModel>>(emptyList())
    val deletedItems: StateFlow<List<DeletedItemModel>> = _deletedItems.asStateFlow()

    // 相册图片列表
    private val _galleryImages = MutableStateFlow<List<ImageModel>>(emptyList())
    val galleryImages: StateFlow<List<ImageModel>> = _galleryImages.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        // 设置加载状态
        _isLoading.value = true

        // 启动多个协程并行加载数据
        viewModelScope.launch {
            try {
                // 只取第一个值来结束加载状态
                imageRepository.getDrafts().collect { drafts ->
                    _drafts.value = drafts
                    if (_isLoading.value) {
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            try {
                imageRepository.getEditedImages().collect {
                    _completedImages.value = it
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }

        viewModelScope.launch {
            try {
                imageRepository.getAllImages().collect {
                    _galleryImages.value = it
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }

        viewModelScope.launch {
            try {
                imageRepository.getDeletedItems().collect {
                    _deletedItems.value = it
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }



    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    /**
     * 获取草稿图片的URI
     */
    fun getDraftImageUri(draftId: String): Uri? {
        val file = imageRepository.getDraftImageFile(draftId)
        return file?.let { Uri.fromFile(it) }
    }

    /**
     * 删除草稿（移动到已删除）
     */
    fun deleteDraft(draft: DraftModel) {
        viewModelScope.launch {
            try {
                imageRepository.deleteDraft(draft)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * 删除已完成图片（移动到已删除）
     */
    fun deleteCompletedImage(image: EditedImageModel) {
        viewModelScope.launch {
            try {
                imageRepository.deleteCompletedImage(image)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * 永久删除已删除项目
     */
    fun permanentlyDeleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                imageRepository.permanentlyDeleteItem(itemId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * 清空所有已删除项目
     */
    fun clearAllDeletedItems() {
        viewModelScope.launch {
            try {
                imageRepository.clearAllDeletedItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * 创建临时图片文件URI，用于相机拍照
     */
    fun createTempImageUri(context: Context): Uri? {
        return try {
            val tempFile = File.createTempFile(
                "IMG_${System.currentTimeMillis()}_",
                ".jpg",
                context.cacheDir
            )
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(ImageRepository(context)) as T
                }
            }
        }
    }
}

enum class HomeTab {
    DRAFTS,      // 正在编辑
    COMPLETED,   // 已完成
    DELETED      // 已删除
}
