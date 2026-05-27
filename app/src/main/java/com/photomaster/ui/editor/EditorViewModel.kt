package com.photomaster.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photomaster.data.model.EditOperation
import com.photomaster.data.model.OperationType
import com.photomaster.domain.manager.EditHistoryManager
import com.photomaster.domain.processor.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

sealed class EditorState {
    object Loading : EditorState()
    data class Ready(
        val originalBitmap: Bitmap,
        val currentBitmap: Bitmap,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
        val currentTool: ToolType = ToolType.NONE,
        val isProcessing: Boolean = false
    ) : EditorState()
    data class Error(val message: String) : EditorState()
}

enum class ToolType {
    NONE,
    CROP,
    ROTATE,
    FLIP,
    FILTER,
    ADJUST,
    LIGHT_SHADOW,  // 光影调整
    LOCAL_STRETCH, // 局部拉伸
    BEAUTY,        // 美颜
    TEXT,
    STICKER,
    BRUSH
}

data class AdjustParams(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val sharpness: Float = 0f,
    val vignette: Float = 0f
)

// 光影调整参数
data class LightShadowParams(
    val shadows: Float = 0f,      // -100 to 100
    val highlights: Float = 0f,   // -100 to 100
    val exposure: Float = 0f      // -100 to 100
)

// 局部拉伸参数
data class LocalStretchParams(
    val centerX: Float = 0.5f,    // 0 to 1 (相对于图片宽度的比例)
    val centerY: Float = 0.5f,    // 0 to 1 (相对于图片高度的比例)
    val radius: Float = 100f,     // 影响半径
    val strength: Float = 0f,     // -100 to 100 (负数收缩，正数膨胀)
    val directionX: Float = 0f,   // -1 to 1 (水平方向)
    val directionY: Float = 0f    // -1 to 1 (垂直方向)
)

// 美颜参数
data class BeautyParams(
    val smooth: Float = 0f,       // 0 to 100 (磨皮程度)
    val whiten: Float = 0f,       // 0 to 100 (美白程度)
    val thinFace: Float = 0f,     // 0 to 100 (瘦脸程度)
    val bigEyes: Float = 0f,      // 0 to 100 (大眼程度)
    val slimNose: Float = 0f      // 0 to 100 (瘦鼻程度)
)

// 自定义滤镜参数
data class FilterParams(
    val hue: Float = 0f,          // -180 to 180 (色相)
    val saturation: Float = 1f,   // 0 to 2 (饱和度)
    val brightness: Float = 0f,   // -100 to 100 (亮度)
    val contrast: Float = 1f,     // 0 to 2 (对比度)
    val warmth: Float = 0f,       // -100 to 100 (色温)
    val fade: Float = 0f,         // 0 to 100 (褪色)
    val grain: Float = 0f,        // 0 to 100 (颗粒)
    val vignette: Float = 0f      // 0 to 100 (暗角)
)

// Crop state for interactive cropping
data class CropState(
    val cropRect: Rect = Rect.Zero,
    val aspectRatio: Float? = null,  // null means free form
    val imageWidth: Float = 0f,
    val imageHeight: Float = 0f
)

// 文本框状态
data class TextBoxState(
    val text: String = "",
    val x: Float = 0.5f,  // 相对于图片宽度的比例 (0-1)
    val y: Float = 0.5f,  // 相对于图片高度的比例 (0-1)
    val textSize: Float = 60f,
    val textColor: Int = android.graphics.Color.WHITE,
    val rotation: Float = 0f,
    val isEditing: Boolean = false,  // 是否正在编辑（显示输入法）
    val isVisible: Boolean = false   // 是否显示文本框
)

// 贴纸框状态
data class StickerBoxState(
    val stickerCode: String = "",
    val x: Float = 0.5f,  // 相对于图片宽度的比例 (0-1)
    val y: Float = 0.5f,  // 相对于图片高度的比例 (0-1)
    val size: Float = 100f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val isVisible: Boolean = false   // 是否显示贴纸框
)

class EditorViewModel : ViewModel() {
    private val historyManager = EditHistoryManager()
    private val imageProcessor = ImageProcessor()

    private val _editorState = MutableStateFlow<EditorState>(EditorState.Loading)
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    // Crop state
    private val _cropState = MutableStateFlow(CropState())
    val cropState: StateFlow<CropState> = _cropState.asStateFlow()

    // Text box state
    private val _textBoxState = MutableStateFlow(TextBoxState())
    val textBoxState: StateFlow<TextBoxState> = _textBoxState.asStateFlow()

    // Sticker box state
    private val _stickerBoxState = MutableStateFlow(StickerBoxState())
    val stickerBoxState: StateFlow<StickerBoxState> = _stickerBoxState.asStateFlow()

    private var originalBitmap: Bitmap? = null
    // 用于实时预览的基准位图（应用了历史操作但未应用当前调整的位图）
    private var previewBaseBitmap: Bitmap? = null
    private var currentAdjustParams = AdjustParams()
    private var currentLightShadowParams = LightShadowParams()
    private var currentLocalStretchParams = LocalStretchParams()
    private var currentBeautyParams = BeautyParams()
    private var currentCropRatio: Float? = null

    // 当前编辑的草稿ID（如果是从草稿继续编辑）
    private var currentDraftId: String? = null

    // 用于取消之前的协程，避免闪烁
    private var adjustJob: kotlinx.coroutines.Job? = null
    private var lightShadowJob: kotlinx.coroutines.Job? = null
    private var localStretchJob: kotlinx.coroutines.Job? = null
    private var beautyJob: kotlinx.coroutines.Job? = null

    // 防抖延迟时间（毫秒）
    private val DEBOUNCE_DELAY = 50L

    // 缓存的预览位图，避免频繁创建
    private var cachedPreviewBitmap: Bitmap? = null

    /**
     * 设置当前草稿ID（从草稿继续编辑时调用）
     */
    fun setDraftId(draftId: String?) {
        currentDraftId = draftId
    }

    fun loadImage(imageUri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            _editorState.value = EditorState.Loading
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    // 尝试从 URI 加载图片，支持 file:// 和 content:// 协议
                    context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    } ?: throw IllegalArgumentException("Cannot open image from URI: $imageUri")
                }
                originalBitmap = bitmap
                previewBaseBitmap = bitmap
                _editorState.value = EditorState.Ready(
                    originalBitmap = bitmap,
                    currentBitmap = bitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo()
                )
            } catch (e: Exception) {
                _editorState.value = EditorState.Error("Failed to load image: ${e.message}")
            }
        }
    }

    fun selectTool(tool: ToolType) {
        val currentState = _editorState.value
        if (currentState is EditorState.Ready) {
            _editorState.value = currentState.copy(currentTool = tool)
        }
    }

    // ==================== Crop Functions ====================

    fun initCropRect(imageWidth: Float, imageHeight: Float) {
        val padding = 50f
        val cropWidth = imageWidth - 2 * padding
        val cropHeight = imageHeight - 2 * padding
        _cropState.value = CropState(
            cropRect = Rect(
                left = padding,
                top = padding,
                right = padding + cropWidth,
                bottom = padding + cropHeight
            ),
            aspectRatio = null,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    }

    fun moveCropRect(dx: Float, dy: Float, maxWidth: Float, maxHeight: Float) {
        val currentCrop = _cropState.value
        val rect = currentCrop.cropRect

        var newLeft = rect.left + dx
        var newTop = rect.top + dy
        var newRight = rect.right + dx
        var newBottom = rect.bottom + dy

        // Constrain to bounds
        if (newLeft < 0) {
            newLeft = 0f
            newRight = rect.width
        }
        if (newTop < 0) {
            newTop = 0f
            newBottom = rect.height
        }
        if (newRight > maxWidth) {
            newRight = maxWidth
            newLeft = maxWidth - rect.width
        }
        if (newBottom > maxHeight) {
            newBottom = maxHeight
            newTop = maxHeight - rect.height
        }

        _cropState.value = currentCrop.copy(
            cropRect = Rect(newLeft, newTop, newRight, newBottom)
        )
    }

    fun resizeCropRect(dx: Float, dy: Float, edge: CropEdge, maxWidth: Float, maxHeight: Float) {
        val currentCrop = _cropState.value
        val rect = currentCrop.cropRect
        val aspectRatio = currentCrop.aspectRatio

        var newLeft = rect.left
        var newTop = rect.top
        var newRight = rect.right
        var newBottom = rect.bottom

        when (edge) {
            CropEdge.TOP_LEFT -> {
                newLeft = (rect.left + dx).coerceIn(0f, rect.right - 50f)
                newTop = (rect.top + dy).coerceIn(0f, rect.bottom - 50f)
                // Only apply aspect ratio constraint if explicitly set
                if (aspectRatio != null && aspectRatio > 0) {
                    val newWidth = rect.right - newLeft
                    newTop = rect.bottom - newWidth / aspectRatio
                }
            }
            CropEdge.TOP_RIGHT -> {
                newRight = (rect.right + dx).coerceIn(rect.left + 50f, maxWidth)
                newTop = (rect.top + dy).coerceIn(0f, rect.bottom - 50f)
                // Only apply aspect ratio constraint if explicitly set
                if (aspectRatio != null && aspectRatio > 0) {
                    val newWidth = newRight - rect.left
                    newTop = rect.bottom - newWidth / aspectRatio
                }
            }
            CropEdge.BOTTOM_LEFT -> {
                newLeft = (rect.left + dx).coerceIn(0f, rect.right - 50f)
                newBottom = (rect.bottom + dy).coerceIn(rect.top + 50f, maxHeight)
                // Only apply aspect ratio constraint if explicitly set
                if (aspectRatio != null && aspectRatio > 0) {
                    val newWidth = rect.right - newLeft
                    newBottom = rect.top + newWidth / aspectRatio
                }
            }
            CropEdge.BOTTOM_RIGHT -> {
                newRight = (rect.right + dx).coerceIn(rect.left + 50f, maxWidth)
                newBottom = (rect.bottom + dy).coerceIn(rect.top + 50f, maxHeight)
                // Only apply aspect ratio constraint if explicitly set
                if (aspectRatio != null && aspectRatio > 0) {
                    val newWidth = newRight - rect.left
                    newBottom = rect.top + newWidth / aspectRatio
                }
            }
        }

        _cropState.value = currentCrop.copy(
            cropRect = Rect(newLeft, newTop, newRight, newBottom)
        )
    }

    fun setCropAspectRatio(ratio: Float?) {
        val currentCrop = _cropState.value
        _cropState.value = currentCrop.copy(aspectRatio = ratio)

        // Adjust current crop rect to match new aspect ratio
        if (ratio != null) {
            val rect = currentCrop.cropRect
            val currentWidth = rect.width
            val newHeight = currentWidth / ratio

            val newTop = rect.top + (rect.height - newHeight) / 2
            val newBottom = newTop + newHeight

            _cropState.value = currentCrop.copy(
                aspectRatio = ratio,
                cropRect = Rect(rect.left, newTop, rect.right, newBottom)
            )
        }
    }

    fun resetCrop() {
        val currentCrop = _cropState.value
        initCropRect(currentCrop.imageWidth, currentCrop.imageHeight)
    }

    fun applyCropFromState() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        val cropRect = _cropState.value.cropRect
        if (cropRect.width <= 0 || cropRect.height <= 0) return

        // Convert screen coordinates to bitmap coordinates
        val bitmap = currentState.currentBitmap
        val imageWidth = _cropState.value.imageWidth
        val imageHeight = _cropState.value.imageHeight

        val scaleX = bitmap.width.toFloat() / imageWidth
        val scaleY = bitmap.height.toFloat() / imageHeight

        val x = (cropRect.left * scaleX).toInt()
        val y = (cropRect.top * scaleY).toInt()
        val width = (cropRect.width * scaleX).toInt()
        val height = (cropRect.height * scaleY).toInt()

        applyCrop(x, y, width, height)
    }

    fun applyCrop(x: Int, y: Int, width: Int, height: Int) {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val operation = EditOperation(
                    type = OperationType.CROP,
                    params = mapOf(
                        "x" to x.toFloat(),
                        "y" to y.toFloat(),
                        "width" to width.toFloat(),
                        "height" to height.toFloat()
                    )
                )
                historyManager.addOperation(operation)
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyCrop(currentState.currentBitmap, operation.params)
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    // ==================== Other Edit Functions ====================

    fun setCropRatio(ratio: Float?) {
        currentCropRatio = ratio
    }

    fun applyCurrentCrop() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        val ratio = currentCropRatio
        if (ratio == null) {
            // 原始比例，不裁剪
            _editorState.value = currentState.copy(currentTool = ToolType.NONE)
            return
        }

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val bitmap = currentState.currentBitmap
                val width = bitmap.width
                val height = bitmap.height
                val currentRatio = width.toFloat() / height.toFloat()

                val (cropWidth, cropHeight) = when {
                    currentRatio > ratio -> {
                        // 图片更宽，按高度计算
                        val newWidth = (height * ratio).toInt()
                        newWidth to height
                    }
                    else -> {
                        // 图片更高或相等，按宽度计算
                        val newHeight = (width / ratio).toInt()
                        width to newHeight
                    }
                }

                val x = (width - cropWidth) / 2
                val y = (height - cropHeight) / 2

                applyCrop(x, y, cropWidth, cropHeight)
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun applyRotation(degrees: Float) {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val operation = EditOperation(
                    type = OperationType.ROTATE,
                    params = mapOf("degrees" to degrees)
                )
                historyManager.addOperation(operation)
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyRotation(currentState.currentBitmap, operation.params)
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun applyFlip(horizontal: Boolean) {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val operation = EditOperation(
                    type = if (horizontal) OperationType.FLIP_HORIZONTAL else OperationType.FLIP_VERTICAL,
                    params = emptyMap()
                )
                historyManager.addOperation(operation)
                val newBitmap = withContext(Dispatchers.Default) {
                    if (horizontal) {
                        imageProcessor.applyFlipHorizontal(currentState.currentBitmap)
                    } else {
                        imageProcessor.applyFlipVertical(currentState.currentBitmap)
                    }
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun updateAdjustParams(params: AdjustParams) {
        currentAdjustParams = params
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        // 取消之前的调整任务，避免闪烁
        adjustJob?.cancel()
        
        adjustJob = viewModelScope.launch {
            try {
                // 添加防抖延迟，减少频繁处理
                delay(DEBOUNCE_DELAY)
                
                // 使用预览基准位图而不是原始位图，实现平滑过渡
                val baseBitmap = previewBaseBitmap ?: currentState.originalBitmap
                
                // 复用缓存的位图或创建新的
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyAdjustments(baseBitmap, params)
                }
                
                // 确保只有在没有被取消的情况下才更新状态
                if (isActive) {
                    _editorState.value = currentState.copy(currentBitmap = newBitmap)
                }
            } catch (e: Exception) {
                // Ignore adjustment errors
            }
        }
    }

    fun applyAdjustments() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val operation = EditOperation(
                    type = OperationType.ADJUST,
                    params = mapOf(
                        "brightness" to currentAdjustParams.brightness,
                        "contrast" to currentAdjustParams.contrast,
                        "saturation" to currentAdjustParams.saturation,
                        "temperature" to currentAdjustParams.temperature,
                        "tint" to currentAdjustParams.tint
                    )
                )
                historyManager.addOperation(operation)
                currentAdjustParams = AdjustParams()
                // 更新预览基准位图为当前效果
                previewBaseBitmap = currentState.currentBitmap
                _editorState.value = currentState.copy(
                    originalBitmap = currentState.currentBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    // ==================== Light & Shadow Functions ====================

    fun updateLightShadowParams(params: LightShadowParams) {
        currentLightShadowParams = params
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        // 取消之前的光影调整任务，避免闪烁
        lightShadowJob?.cancel()

        lightShadowJob = viewModelScope.launch {
            try {
                // 添加防抖延迟，减少频繁处理
                delay(DEBOUNCE_DELAY)
                
                // 使用预览基准位图而不是原始位图，实现平滑过渡
                val baseBitmap = previewBaseBitmap ?: currentState.originalBitmap
                
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyLightShadow(
                        baseBitmap,
                        mapOf(
                            "shadows" to params.shadows,
                            "highlights" to params.highlights,
                            "exposure" to params.exposure
                        )
                    )
                }
                
                // 确保只有在没有被取消的情况下才更新状态
                if (isActive) {
                    _editorState.value = currentState.copy(currentBitmap = newBitmap)
                }
            } catch (e: Exception) {
                // Ignore adjustment errors
            }
        }
    }

    fun applyLightShadow() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val operation = EditOperation(
                    type = OperationType.LIGHT_SHADOW,
                    params = mapOf(
                        "shadows" to currentLightShadowParams.shadows,
                        "highlights" to currentLightShadowParams.highlights,
                        "exposure" to currentLightShadowParams.exposure
                    )
                )
                historyManager.addOperation(operation)
                currentLightShadowParams = LightShadowParams()
                // 更新预览基准位图为当前效果
                previewBaseBitmap = currentState.currentBitmap
                _editorState.value = currentState.copy(
                    originalBitmap = currentState.currentBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    // ==================== Local Stretch Functions ====================

    fun updateLocalStretchParams(params: LocalStretchParams) {
        currentLocalStretchParams = params
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        // 取消之前的局部拉伸任务，避免闪烁
        localStretchJob?.cancel()

        localStretchJob = viewModelScope.launch {
            try {
                // 添加防抖延迟，减少频繁处理
                delay(DEBOUNCE_DELAY)
                
                // 使用预览基准位图而不是原始位图，实现平滑过渡
                val baseBitmap = previewBaseBitmap ?: currentState.originalBitmap
                
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyLocalStretch(
                        baseBitmap,
                        mapOf(
                            "centerX" to params.centerX * baseBitmap.width,
                            "centerY" to params.centerY * baseBitmap.height,
                            "radius" to params.radius,
                            "strength" to params.strength,
                            "directionX" to params.directionX,
                            "directionY" to params.directionY
                        )
                    )
                }
                
                // 确保只有在没有被取消的情况下才更新状态
                if (isActive) {
                    _editorState.value = currentState.copy(currentBitmap = newBitmap)
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    fun applyLocalStretch() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val bitmap = currentState.currentBitmap
                val operation = EditOperation(
                    type = OperationType.LOCAL_EDIT,
                    params = mapOf(
                        "centerX" to currentLocalStretchParams.centerX * bitmap.width,
                        "centerY" to currentLocalStretchParams.centerY * bitmap.height,
                        "radius" to currentLocalStretchParams.radius,
                        "strength" to currentLocalStretchParams.strength,
                        "directionX" to currentLocalStretchParams.directionX,
                        "directionY" to currentLocalStretchParams.directionY
                    )
                )
                historyManager.addOperation(operation)
                currentLocalStretchParams = LocalStretchParams()
                // 更新预览基准位图为当前效果
                previewBaseBitmap = currentState.currentBitmap
                _editorState.value = currentState.copy(
                    originalBitmap = currentState.currentBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    // ==================== Beauty Functions ====================

    fun updateBeautyParams(params: BeautyParams) {
        currentBeautyParams = params
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        // 取消之前的美颜任务，避免闪烁
        beautyJob?.cancel()

        beautyJob = viewModelScope.launch {
            try {
                // 添加防抖延迟，减少频繁处理
                delay(DEBOUNCE_DELAY)

                // 使用预览基准位图而不是原始位图，实现平滑过渡
                val baseBitmap = previewBaseBitmap ?: currentState.originalBitmap

                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyBeauty(
                        baseBitmap,
                        mapOf(
                            "smooth" to params.smooth,
                            "whiten" to params.whiten,
                            "thinFace" to params.thinFace,
                            "bigEyes" to params.bigEyes,
                            "slimNose" to params.slimNose
                        )
                    )
                }

                // 确保只有在没有被取消的情况下才更新状态
                if (isActive) {
                    _editorState.value = currentState.copy(currentBitmap = newBitmap)
                }
            } catch (e: Exception) {
                // Ignore adjustment errors
            }
        }
    }

    fun applyBeauty() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val operation = EditOperation(
                    type = OperationType.BEAUTY,
                    params = mapOf(
                        "smooth" to currentBeautyParams.smooth,
                        "whiten" to currentBeautyParams.whiten,
                        "thinFace" to currentBeautyParams.thinFace,
                        "bigEyes" to currentBeautyParams.bigEyes,
                        "slimNose" to currentBeautyParams.slimNose
                    )
                )
                historyManager.addOperation(operation)
                currentBeautyParams = BeautyParams()
                // 更新预览基准位图为当前效果
                previewBaseBitmap = currentState.currentBitmap
                _editorState.value = currentState.copy(
                    originalBitmap = currentState.currentBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    // ==================== Filter Functions ====================

    private var currentFilterParams = FilterParams()
    private var filterJob: kotlinx.coroutines.Job? = null

    fun updateFilterParams(params: FilterParams) {
        currentFilterParams = params
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        // 取消之前的滤镜任务，避免闪烁
        filterJob?.cancel()

        filterJob = viewModelScope.launch {
            try {
                // 添加防抖延迟，减少频繁处理
                delay(DEBOUNCE_DELAY)

                // 使用预览基准位图而不是原始位图，实现平滑过渡
                val baseBitmap = previewBaseBitmap ?: currentState.originalBitmap

                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyCustomFilter(baseBitmap, params)
                }

                // 确保只有在没有被取消的情况下才更新状态
                if (isActive) {
                    _editorState.value = currentState.copy(currentBitmap = newBitmap)
                }
            } catch (e: Exception) {
                // Ignore adjustment errors
            }
        }
    }

    fun applyCustomFilter() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val operation = EditOperation(
                    type = OperationType.FILTER,
                    params = mapOf(
                        "hue" to currentFilterParams.hue,
                        "saturation" to currentFilterParams.saturation,
                        "brightness" to currentFilterParams.brightness,
                        "contrast" to currentFilterParams.contrast,
                        "warmth" to currentFilterParams.warmth,
                        "fade" to currentFilterParams.fade,
                        "grain" to currentFilterParams.grain,
                        "vignette" to currentFilterParams.vignette
                    )
                )
                historyManager.addOperation(operation)
                currentFilterParams = FilterParams()
                // 更新预览基准位图为当前效果
                previewBaseBitmap = currentState.currentBitmap
                _editorState.value = currentState.copy(
                    originalBitmap = currentState.currentBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun applyFilter(filterType: Int) {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val operation = EditOperation(
                    type = OperationType.FILTER,
                    params = mapOf("filterType" to filterType.toFloat())
                )
                historyManager.addOperation(operation)
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyFilter(currentState.currentBitmap, operation.params)
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    // ==================== Text Functions ====================

    fun createTextBox() {
        _textBoxState.value = TextBoxState(
            text = "",
            x = 0.5f,
            y = 0.5f,
            textSize = 60f,
            textColor = android.graphics.Color.WHITE,
            rotation = 0f,
            isEditing = true,
            isVisible = true
        )
    }

    fun updateTextBoxText(text: String) {
        _textBoxState.value = _textBoxState.value.copy(text = text)
    }

    fun updateTextBoxPosition(x: Float, y: Float) {
        _textBoxState.value = _textBoxState.value.copy(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f)
        )
    }

    fun updateTextBoxStyle(textSize: Float? = null, textColor: Int? = null, rotation: Float? = null) {
        val current = _textBoxState.value
        _textBoxState.value = current.copy(
            textSize = textSize ?: current.textSize,
            textColor = textColor ?: current.textColor,
            rotation = rotation ?: current.rotation
        )
    }

    fun setTextBoxEditing(isEditing: Boolean) {
        _textBoxState.value = _textBoxState.value.copy(isEditing = isEditing)
    }

    fun hideTextBox() {
        _textBoxState.value = _textBoxState.value.copy(isVisible = false, isEditing = false)
    }

    fun applyTextFromBox() {
        val boxState = _textBoxState.value
        if (boxState.text.isBlank()) {
            hideTextBox()
            return
        }

        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val params = mapOf(
                    "x" to boxState.x,
                    "y" to boxState.y,
                    "textSize" to boxState.textSize,
                    "textColor" to boxState.textColor.toFloat(),
                    "rotation" to boxState.rotation
                )
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyText(currentState.currentBitmap, boxState.text, params)
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                hideTextBox()
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun cancelTextBox() {
        hideTextBox()
        val currentState = _editorState.value
        if (currentState is EditorState.Ready) {
            _editorState.value = currentState.copy(currentTool = ToolType.NONE)
        }
    }

    // ==================== Sticker Functions ====================

    fun createStickerBox(stickerCode: String) {
        _stickerBoxState.value = StickerBoxState(
            stickerCode = stickerCode,
            x = 0.5f,
            y = 0.5f,
            size = 100f,
            rotation = 0f,
            alpha = 1f,
            isVisible = true
        )
    }

    fun updateStickerBoxPosition(x: Float, y: Float) {
        _stickerBoxState.value = _stickerBoxState.value.copy(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f)
        )
    }

    fun updateStickerBoxStyle(size: Float? = null, rotation: Float? = null, alpha: Float? = null) {
        val current = _stickerBoxState.value
        _stickerBoxState.value = current.copy(
            size = size ?: current.size,
            rotation = rotation ?: current.rotation,
            alpha = alpha ?: current.alpha
        )
    }

    fun hideStickerBox() {
        _stickerBoxState.value = _stickerBoxState.value.copy(isVisible = false)
    }

    fun applyStickerFromBox() {
        val boxState = _stickerBoxState.value
        if (boxState.stickerCode.isBlank() || !boxState.isVisible) {
            hideStickerBox()
            return
        }

        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val params = mapOf(
                    "x" to boxState.x,
                    "y" to boxState.y,
                    "size" to boxState.size,
                    "rotation" to boxState.rotation,
                    "alpha" to boxState.alpha
                )
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applySticker(currentState.currentBitmap, boxState.stickerCode, params)
                }
                // 保存到历史记录
                val operation = EditOperation(
                    type = OperationType.STICKER,
                    params = params
                )
                historyManager.addOperation(operation)
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                hideStickerBox()
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun cancelStickerBox() {
        hideStickerBox()
        val currentState = _editorState.value
        if (currentState is EditorState.Ready) {
            _editorState.value = currentState.copy(currentTool = ToolType.NONE)
        }
    }

    // 旧版贴纸方法（保留兼容性）
    fun applySticker(stickerCode: String, x: Float = 0.5f, y: Float = 0.5f, size: Float = 100f, rotation: Float = 0f, alpha: Float = 1f) {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val params = mapOf(
                    "x" to x,
                    "y" to y,
                    "size" to size,
                    "rotation" to rotation,
                    "alpha" to alpha
                )
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applySticker(currentState.currentBitmap, stickerCode, params)
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    currentTool = ToolType.NONE,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    // ==================== Brush Functions ====================

    private var currentBrushPoints = mutableListOf<Pair<Float, Float>>()
    private var currentBrushColor = android.graphics.Color.BLACK
    private var currentBrushSize = 10f

    fun setBrushColor(color: Int) {
        currentBrushColor = color
    }

    fun setBrushSize(size: Float) {
        currentBrushSize = size
    }

    fun addBrushPoint(x: Float, y: Float) {
        currentBrushPoints.add(Pair(x, y))
    }

    fun clearBrushPoints() {
        currentBrushPoints.clear()
    }

    fun applyBrush() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready || currentBrushPoints.isEmpty()) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                val newBitmap = withContext(Dispatchers.Default) {
                    imageProcessor.applyBrush(
                        currentState.currentBitmap,
                        currentBrushPoints.toList(),
                        currentBrushColor,
                        currentBrushSize
                    )
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    isProcessing = false
                )
                currentBrushPoints.clear()
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun undo() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready || !historyManager.canUndo()) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                historyManager.undo()
                val operations = historyManager.getCurrentHistory()
                val newBitmap = withContext(Dispatchers.Default) {
                    originalBitmap?.let { source ->
                        imageProcessor.processImage(source, operations)
                    } ?: currentState.currentBitmap
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun redo() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready || !historyManager.canRedo()) return

        viewModelScope.launch {
            _editorState.value = currentState.copy(isProcessing = true)
            try {
                historyManager.redo()
                val operations = historyManager.getCurrentHistory()
                val newBitmap = withContext(Dispatchers.Default) {
                    originalBitmap?.let { source ->
                        imageProcessor.processImage(source, operations)
                    } ?: currentState.currentBitmap
                }
                // 更新预览基准位图
                previewBaseBitmap = newBitmap
                _editorState.value = currentState.copy(
                    currentBitmap = newBitmap,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo(),
                    isProcessing = false
                )
            } catch (e: Exception) {
                _editorState.value = currentState.copy(isProcessing = false)
            }
        }
    }

    fun reset() {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) return

        viewModelScope.launch {
            originalBitmap?.let { original ->
                historyManager.clear()
                currentAdjustParams = AdjustParams()
                // 重置预览基准位图为原始图片
                previewBaseBitmap = original
                _editorState.value = currentState.copy(
                    currentBitmap = original,
                    canUndo = false,
                    canRedo = false,
                    currentTool = ToolType.NONE
                )
            }
        }
    }

    // ==================== Save and Export ====================

    /**
     * Save the edited image as draft (not to gallery)
     * @param existingDraftId 如果提供，则更新已有草稿；否则创建新草稿
     */
    fun saveImage(context: Context, originalUri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit, existingDraftId: String? = null) {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) {
            onError("No image to save")
            return
        }

        viewModelScope.launch {
            try {
                val bitmap = currentState.currentBitmap
                val repository = com.photomaster.data.repository.ImageRepository(context)
                // 优先使用传入的existingDraftId，否则使用currentDraftId
                val draftIdToUpdate = existingDraftId ?: currentDraftId
                val draft = repository.saveDraft(originalUri, bitmap, existingDraftId = draftIdToUpdate)

                onSuccess(draft.id)
            } catch (e: Exception) {
                onError("Failed to save image: ${e.message}")
            }
        }
    }

    /**
     * Export the edited image to gallery and add to completed list
     */
    fun exportImage(
        context: Context,
        originalUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentState = _editorState.value
        if (currentState !is EditorState.Ready) {
            onError("No image to export")
            return
        }

        viewModelScope.launch {
            try {
                val bitmap = currentState.currentBitmap
                val filename = "PhotoPS_${System.currentTimeMillis()}.jpg"

                val uri = withContext(Dispatchers.IO) {
                    saveBitmapToGallery(context, bitmap, filename)
                }

                if (uri != null) {
                    // 创建已完成图片记录
                    val repository = com.photomaster.data.repository.ImageRepository(context)
                    val completedImage = com.photomaster.data.model.EditedImageModel(
                        id = System.currentTimeMillis(),
                        name = filename,
                        uri = uri,
                        originalImageUri = originalUri,
                        dateEdited = System.currentTimeMillis(),
                        width = bitmap.width,
                        height = bitmap.height
                    )
                    
                    // 保存到已完成列表
                    repository.saveCompletedImage(completedImage)
                    
                    // 如果是编辑草稿，删除该草稿
                    currentDraftId?.let { draftId ->
                        val drafts = repository.getDrafts().first()
                        val draft = drafts.find { it.id == draftId }
                        draft?.let {
                            repository.deleteDraft(it)
                        }
                    }
                    
                    onSuccess(uri.toString())
                } else {
                    onError("Failed to export image")
                }
            } catch (e: Exception) {
                onError("Failed to export image: ${e.message}")
            }
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
        }

        return uri
    }

    fun getCurrentBitmap(): Bitmap? {
        return when (val state = _editorState.value) {
            is EditorState.Ready -> state.currentBitmap
            else -> null
        }
    }

    /**
     * 删除当前草稿（移动到已删除）
     */
    fun deleteDraft(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val draftId = currentDraftId
        if (draftId == null) {
            onError("没有可删除的草稿")
            return
        }

        viewModelScope.launch {
            try {
                val repository = com.photomaster.data.repository.ImageRepository(context)
                // 从 Flow 中获取草稿列表
                val drafts = repository.getDrafts().first()
                val draft = drafts.find { it.id == draftId }
                if (draft != null) {
                    repository.deleteDraft(draft)
                    onSuccess()
                } else {
                    onError("找不到草稿")
                }
            } catch (e: Exception) {
                onError("删除失败: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        originalBitmap?.recycle()
        originalBitmap = null
    }
}

enum class CropEdge {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}
