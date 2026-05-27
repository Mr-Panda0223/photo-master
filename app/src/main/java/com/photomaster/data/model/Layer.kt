package com.photomaster.data.model

import android.graphics.Bitmap
import java.util.UUID

/**
 * 图层类型
 */
enum class LayerType {
    IMAGE,      // 图片图层（基础图层）
    TEXT,       // 文字图层
    STICKER,    // 贴纸图层
    SHAPE,      // 形状图层
    ADJUSTMENT  // 调整图层
}

/**
 * 混合模式
 */
enum class BlendMode {
    NORMAL,     // 正常
    MULTIPLY,   // 正片叠底
    SCREEN,     // 滤色
    OVERLAY,    // 叠加
    SOFT_LIGHT, // 柔光
    HARD_LIGHT, // 强光
    COLOR_DODGE,// 颜色减淡
    COLOR_BURN, // 颜色加深
    DARKEN,     // 变暗
    LIGHTEN,    // 变亮
    DIFFERENCE, // 差值
    EXCLUSION   // 排除
}

/**
 * 图层数据类
 */
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "图层",
    val type: LayerType = LayerType.IMAGE,
    var bitmap: Bitmap? = null,           // 图层内容（图片、文字渲染结果等）
    var visible: Boolean = true,          // 是否可见
    var opacity: Float = 1f,              // 不透明度 0-1
    var blendMode: BlendMode = BlendMode.NORMAL,  // 混合模式
    var position: LayerPosition = LayerPosition(), // 位置和变换
    var zIndex: Int = 0,                  // 层级顺序
    val isLocked: Boolean = false,        // 是否锁定
    val createTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取图层缩略图名称
     */
    fun getDisplayName(): String {
        return when (type) {
            LayerType.IMAGE -> name
            LayerType.TEXT -> "文字: $name"
            LayerType.STICKER -> "贴纸: $name"
            LayerType.SHAPE -> "形状: $name"
            LayerType.ADJUSTMENT -> "调整: $name"
        }
    }
}

/**
 * 图层位置和变换信息
 */
data class LayerPosition(
    var x: Float = 0f,                    // X坐标（相对于画布中心）
    var y: Float = 0f,                    // Y坐标（相对于画布中心）
    var scaleX: Float = 1f,               // X轴缩放
    var scaleY: Float = 1f,               // Y轴缩放
    var rotation: Float = 0f,             // 旋转角度（度）
    var width: Float = 0f,                // 宽度
    var height: Float = 0f                // 高度
)

/**
 * 图层操作类型
 */
sealed class LayerOperation {
    data class Add(val layer: Layer) : LayerOperation()
    data class Remove(val layerId: String) : LayerOperation()
    data class Move(val layerId: String, val newZIndex: Int) : LayerOperation()
    data class UpdateOpacity(val layerId: String, val opacity: Float) : LayerOperation()
    data class UpdateVisibility(val layerId: String, val visible: Boolean) : LayerOperation()
    data class UpdateBlendMode(val layerId: String, val blendMode: BlendMode) : LayerOperation()
    data class UpdatePosition(val layerId: String, val position: LayerPosition) : LayerOperation()
    data class Merge(val layerIds: List<String>) : LayerOperation()
    data class Duplicate(val layerId: String) : LayerOperation()
}

/**
 * 图层状态
 */
data class LayerState(
    val layers: List<Layer> = emptyList(),
    val selectedLayerId: String? = null,
    val backgroundLayerId: String? = null
) {
    /**
     * 获取选中图层
     */
    fun getSelectedLayer(): Layer? {
        return layers.find { it.id == selectedLayerId }
    }

    /**
     * 获取可见图层（按zIndex排序）
     */
    fun getVisibleLayers(): List<Layer> {
        return layers.filter { it.visible }.sortedBy { it.zIndex }
    }

    /**
     * 获取下一个zIndex
     */
    fun getNextZIndex(): Int {
        return (layers.maxOfOrNull { it.zIndex } ?: 0) + 1
    }
}
