package com.photomaster.domain.manager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.photomaster.data.model.BlendMode
import com.photomaster.data.model.Layer
import com.photomaster.data.model.LayerOperation
import com.photomaster.data.model.LayerPosition
import com.photomaster.data.model.LayerState
import com.photomaster.data.model.LayerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

/**
 * 图层管理器
 * 管理所有图层的增删改查和渲染
 */
class LayerManager {

    private val _layerState = MutableStateFlow(LayerState())
    val layerState: StateFlow<LayerState> = _layerState.asStateFlow()

    // 操作历史（用于撤销/重做）
    private val undoStack = Stack<LayerOperation>()
    private val redoStack = Stack<LayerOperation>()

    /**
     * 初始化图层（加载图片时调用）
     */
    fun initializeWithImage(bitmap: Bitmap) {
        val backgroundLayer = Layer(
            id = "background",
            name = "背景",
            type = LayerType.IMAGE,
            bitmap = bitmap,
            position = LayerPosition(
                width = bitmap.width.toFloat(),
                height = bitmap.height.toFloat()
            ),
            zIndex = 0,
            isLocked = true
        )

        _layerState.value = LayerState(
            layers = listOf(backgroundLayer),
            selectedLayerId = null,
            backgroundLayerId = backgroundLayer.id
        )
    }

    /**
     * 添加图层
     */
    fun addLayer(layer: Layer) {
        val currentState = _layerState.value
        val newLayer = layer.copy(zIndex = currentState.getNextZIndex())

        _layerState.value = currentState.copy(
            layers = currentState.layers + newLayer,
            selectedLayerId = newLayer.id
        )

        undoStack.push(LayerOperation.Add(newLayer))
        redoStack.clear()
    }

    /**
     * 添加文字图层
     */
    fun addTextLayer(name: String, bitmap: Bitmap) {
        val layer = Layer(
            name = name,
            type = LayerType.TEXT,
            bitmap = bitmap,
            position = LayerPosition(
                width = bitmap.width.toFloat(),
                height = bitmap.height.toFloat()
            )
        )
        addLayer(layer)
    }

    /**
     * 添加贴纸图层
     */
    fun addStickerLayer(name: String, bitmap: Bitmap) {
        val layer = Layer(
            name = name,
            type = LayerType.STICKER,
            bitmap = bitmap,
            position = LayerPosition(
                width = bitmap.width.toFloat(),
                height = bitmap.height.toFloat()
            )
        )
        addLayer(layer)
    }

    /**
     * 删除图层
     */
    fun removeLayer(layerId: String) {
        val currentState = _layerState.value
        val layer = currentState.layers.find { it.id == layerId } ?: return

        // 不能删除背景图层
        if (layerId == currentState.backgroundLayerId) return

        _layerState.value = currentState.copy(
            layers = currentState.layers.filter { it.id != layerId },
            selectedLayerId = if (currentState.selectedLayerId == layerId) null
            else currentState.selectedLayerId
        )

        undoStack.push(LayerOperation.Remove(layerId))
        redoStack.clear()
    }

    /**
     * 选择图层
     */
    fun selectLayer(layerId: String?) {
        val currentState = _layerState.value
        _layerState.value = currentState.copy(selectedLayerId = layerId)
    }

    /**
     * 移动图层（改变层级）
     */
    fun moveLayer(layerId: String, direction: Int) {
        val currentState = _layerState.value
        val layers = currentState.layers.toMutableList()
        val layerIndex = layers.indexOfFirst { it.id == layerId }

        if (layerIndex == -1) return

        val layer = layers[layerIndex]
        val newZIndex = (layer.zIndex + direction).coerceAtLeast(0)

        // 交换zIndex
        val targetLayer = layers.find { it.zIndex == newZIndex }
        if (targetLayer != null) {
            layers[layerIndex] = layer.copy(zIndex = newZIndex)
            val targetIndex = layers.indexOfFirst { it.id == targetLayer.id }
            layers[targetIndex] = targetLayer.copy(zIndex = layer.zIndex)
        }

        _layerState.value = currentState.copy(layers = layers.sortedBy { it.zIndex })

        undoStack.push(LayerOperation.Move(layerId, newZIndex))
        redoStack.clear()
    }

    /**
     * 将图层移到最上层
     */
    fun bringToFront(layerId: String) {
        val currentState = _layerState.value
        val maxZIndex = currentState.layers.maxOfOrNull { it.zIndex } ?: 0
        moveLayerToZIndex(layerId, maxZIndex + 1)
    }

    /**
     * 将图层移到最下层（背景图层之上）
     */
    fun sendToBack(layerId: String) {
        val currentState = _layerState.value
        val backgroundZIndex = currentState.layers
            .find { it.id == currentState.backgroundLayerId }?.zIndex ?: 0
        moveLayerToZIndex(layerId, backgroundZIndex + 1)
    }

    /**
     * 移动图层到指定zIndex
     */
    private fun moveLayerToZIndex(layerId: String, targetZIndex: Int) {
        val currentState = _layerState.value
        val layers = currentState.layers.map { layer ->
            if (layer.id == layerId) {
                layer.copy(zIndex = targetZIndex)
            } else if (layer.zIndex >= targetZIndex && layer.id != layerId) {
                layer.copy(zIndex = layer.zIndex + 1)
            } else {
                layer
            }
        }.sortedBy { it.zIndex }

        _layerState.value = currentState.copy(layers = layers)
    }

    /**
     * 更新图层不透明度
     */
    fun updateLayerOpacity(layerId: String, opacity: Float) {
        val currentState = _layerState.value
        val layers = currentState.layers.map { layer ->
            if (layer.id == layerId) layer.copy(opacity = opacity.coerceIn(0f, 1f))
            else layer
        }

        _layerState.value = currentState.copy(layers = layers)

        undoStack.push(LayerOperation.UpdateOpacity(layerId, opacity))
        redoStack.clear()
    }

    /**
     * 切换图层可见性
     */
    fun toggleLayerVisibility(layerId: String) {
        val currentState = _layerState.value
        val layer = currentState.layers.find { it.id == layerId } ?: return

        val newVisibility = !layer.visible
        val layers = currentState.layers.map { l ->
            if (l.id == layerId) l.copy(visible = newVisibility) else l
        }

        _layerState.value = currentState.copy(layers = layers)

        undoStack.push(LayerOperation.UpdateVisibility(layerId, newVisibility))
        redoStack.clear()
    }

    /**
     * 更新图层混合模式
     */
    fun updateLayerBlendMode(layerId: String, blendMode: BlendMode) {
        val currentState = _layerState.value
        val layers = currentState.layers.map { layer ->
            if (layer.id == layerId) layer.copy(blendMode = blendMode) else layer
        }

        _layerState.value = currentState.copy(layers = layers)

        undoStack.push(LayerOperation.UpdateBlendMode(layerId, blendMode))
        redoStack.clear()
    }

    /**
     * 更新图层位置
     */
    fun updateLayerPosition(layerId: String, position: LayerPosition) {
        val currentState = _layerState.value
        val layers = currentState.layers.map { layer ->
            if (layer.id == layerId) layer.copy(position = position) else layer
        }

        _layerState.value = currentState.copy(layers = layers)

        undoStack.push(LayerOperation.UpdatePosition(layerId, position))
        redoStack.clear()
    }

    /**
     * 合并图层
     */
    fun mergeLayers(layerIds: List<String>): Bitmap? {
        val currentState = _layerState.value
        val layersToMerge = currentState.layers
            .filter { it.id in layerIds && it.visible }
            .sortedBy { it.zIndex }

        if (layersToMerge.isEmpty()) return null

        // 获取画布尺寸（使用背景图层尺寸）
        val backgroundLayer = currentState.layers.find { it.id == currentState.backgroundLayerId }
        val width = backgroundLayer?.bitmap?.width ?: layersToMerge.first().bitmap?.width ?: 0
        val height = backgroundLayer?.bitmap?.height ?: layersToMerge.first().bitmap?.height ?: 0

        if (width == 0 || height == 0) return null

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 按顺序渲染图层
        layersToMerge.forEach { layer ->
            layer.bitmap?.let { bitmap ->
                renderLayer(canvas, layer, bitmap)
            }
        }

        // 移除已合并的图层（保留最底层的一个，替换为合并结果）
        val remainingLayers = currentState.layers.filter { it.id !in layerIds }
        val mergedLayer = Layer(
            name = "合并图层",
            type = LayerType.IMAGE,
            bitmap = result,
            position = LayerPosition(width = width.toFloat(), height = height.toFloat()),
            zIndex = layersToMerge.first().zIndex
        )

        _layerState.value = currentState.copy(
            layers = remainingLayers + mergedLayer,
            selectedLayerId = mergedLayer.id
        )

        undoStack.push(LayerOperation.Merge(layerIds))
        redoStack.clear()

        return result
    }

    /**
     * 复制图层
     */
    fun duplicateLayer(layerId: String) {
        val currentState = _layerState.value
        val layer = currentState.layers.find { it.id == layerId } ?: return

        val duplicatedLayer = layer.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${layer.name} 副本",
            zIndex = currentState.getNextZIndex()
        )

        _layerState.value = currentState.copy(
            layers = currentState.layers + duplicatedLayer,
            selectedLayerId = duplicatedLayer.id
        )

        undoStack.push(LayerOperation.Duplicate(layerId))
        redoStack.clear()
    }

    /**
     * 渲染图层到画布
     */
    private fun renderLayer(canvas: Canvas, layer: Layer, bitmap: Bitmap) {
        val paint = Paint().apply {
            alpha = (layer.opacity * 255).toInt()
            xfermode = getPorterDuffMode(layer.blendMode)
        }

        val matrix = android.graphics.Matrix().apply {
            // 应用位置变换
            val centerX = canvas.width / 2f
            val centerY = canvas.height / 2f
            postTranslate(centerX + layer.position.x - bitmap.width / 2f,
                centerY + layer.position.y - bitmap.height / 2f)
            postRotate(layer.position.rotation, centerX + layer.position.x,
                centerY + layer.position.y)
            postScale(layer.position.scaleX, layer.position.scaleY,
                centerX + layer.position.x, centerY + layer.position.y)
        }

        canvas.drawBitmap(bitmap, matrix, paint)
    }

    /**
     * 获取混合模式对应的PorterDuff模式
     */
    private fun getPorterDuffMode(blendMode: BlendMode): PorterDuffXfermode? {
        return when (blendMode) {
            BlendMode.NORMAL -> null
            BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            BlendMode.SOFT_LIGHT -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            BlendMode.HARD_LIGHT -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            BlendMode.COLOR_DODGE -> PorterDuffXfermode(PorterDuff.Mode.ADD)
            BlendMode.COLOR_BURN -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            BlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
            BlendMode.LIGHTEN -> PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
            BlendMode.DIFFERENCE -> PorterDuffXfermode(PorterDuff.Mode.XOR)
            BlendMode.EXCLUSION -> PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
    }

    /**
     * 渲染所有可见图层
     */
    fun renderAllLayers(): Bitmap? {
        val currentState = _layerState.value
        val visibleLayers = currentState.getVisibleLayers()

        if (visibleLayers.isEmpty()) return null

        // 获取画布尺寸
        val backgroundLayer = currentState.layers.find { it.id == currentState.backgroundLayerId }
        val width = backgroundLayer?.bitmap?.width ?: visibleLayers.first().bitmap?.width ?: 0
        val height = backgroundLayer?.bitmap?.height ?: visibleLayers.first().bitmap?.height ?: 0

        if (width == 0 || height == 0) return null

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 按zIndex顺序渲染
        visibleLayers.forEach { layer ->
            layer.bitmap?.let { bitmap ->
                renderLayer(canvas, layer, bitmap)
            }
        }

        return result
    }

    /**
     * 撤销
     */
    fun undo() {
        if (undoStack.isEmpty()) return

        val operation = undoStack.pop()
        redoStack.push(operation)

        // 执行反向操作
        when (operation) {
            is LayerOperation.Add -> {
                val currentState = _layerState.value
                _layerState.value = currentState.copy(
                    layers = currentState.layers.filter { it.id != operation.layer.id }
                )
            }
            is LayerOperation.Remove -> {
                // 需要保存完整图层信息才能恢复
            }
            else -> {
                // 其他操作的撤销逻辑
            }
        }
    }

    /**
     * 重做
     */
    fun redo() {
        if (redoStack.isEmpty()) return

        val operation = redoStack.pop()
        undoStack.push(operation)

        // 重新执行操作
        when (operation) {
            is LayerOperation.Add -> addLayer(operation.layer)
            is LayerOperation.Remove -> removeLayer(operation.layerId)
            is LayerOperation.Move -> moveLayerToZIndex(operation.layerId, operation.newZIndex)
            is LayerOperation.UpdateOpacity -> updateLayerOpacity(operation.layerId, operation.opacity)
            is LayerOperation.UpdateVisibility -> {
                val currentState = _layerState.value
                val layers = currentState.layers.map { layer ->
                    if (layer.id == operation.layerId) layer.copy(visible = operation.visible)
                    else layer
                }
                _layerState.value = currentState.copy(layers = layers)
            }
            is LayerOperation.UpdateBlendMode -> updateLayerBlendMode(operation.layerId, operation.blendMode)
            is LayerOperation.UpdatePosition -> updateLayerPosition(operation.layerId, operation.position)
            is LayerOperation.Merge -> {
                // 合并操作的撤销比较复杂，需要保存合并前的状态
            }
            is LayerOperation.Duplicate -> duplicateLayer(operation.layerId)
        }
    }

    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()

    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * 清空所有图层
     */
    fun clear() {
        _layerState.value = LayerState()
        undoStack.clear()
        redoStack.clear()
    }
}
