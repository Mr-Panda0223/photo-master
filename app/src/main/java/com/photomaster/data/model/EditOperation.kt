package com.photomaster.data.model

import java.util.UUID

/**
 * 编辑操作类型
 */
enum class OperationType {
    CROP,           // 裁剪
    ROTATE,         // 旋转
    FLIP,           // 翻转
    FLIP_HORIZONTAL, // 水平翻转
    FLIP_VERTICAL,   // 垂直翻转
    FILTER,         // 滤镜
    ADJUST,         // 基础调整
    LIGHT_SHADOW,   // 光影效果
    BEAUTY,         // 美颜
    TEXT,           // 文字
    BRUSH,          // 画笔
    STICKER,        // 贴纸
    LOCAL_EDIT      // 局部编辑
}

/**
 * 编辑操作数据类
 */
data class EditOperation(
    val id: String = UUID.randomUUID().toString(),
    val type: OperationType,
    val params: Map<String, Float> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 裁剪参数
 */
data class CropParams(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
    val rotation: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

/**
 * 调整参数
 */
data class AdjustParams(
    val brightness: Float = 0f,      // -100 ~ +100
    val contrast: Float = 0f,        // -100 ~ +100
    val saturation: Float = 0f,      // -100 ~ +100
    val temperature: Float = 0f,     // -100 ~ +100
    val tint: Float = 0f,            // -100 ~ +100
    val exposure: Float = 0f,        // -100 ~ +100
    val highlights: Float = 0f,      // -100 ~ +100
    val shadows: Float = 0f,         // -100 ~ +100
    val sharpness: Float = 0f        // 0 ~ 100
)

/**
 * 滤镜参数
 */
data class FilterParams(
    val filterId: String = "original",
    val intensity: Float = 1f        // 0 ~ 1
)

/**
 * 工具类型
 */
enum class ToolType {
    NONE,
    CROP,
    ADJUST,
    FILTER,
    LIGHT_SHADOW,
    TEXT,
    BRUSH
}

/**
 * 工具信息
 */
data class ToolInfo(
    val type: ToolType,
    val name: String,
    val iconRes: Int
)
