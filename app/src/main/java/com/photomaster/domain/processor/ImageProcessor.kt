package com.photomaster.domain.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import com.photomaster.data.model.EditOperation
import com.photomaster.data.model.OperationType

/**
 * 图片处理器
 * 处理所有编辑操作
 */
class ImageProcessor {

    /**
     * 处理图片
     */
    fun processImage(source: Bitmap, operations: List<EditOperation>): Bitmap {
        var currentBitmap = source

        operations.forEach { operation ->
            currentBitmap = when (operation.type) {
                OperationType.CROP -> applyCrop(currentBitmap, operation.params)
                OperationType.ROTATE -> applyRotation(currentBitmap, operation.params)
                OperationType.FLIP -> applyFlip(currentBitmap, operation.params)
                OperationType.FLIP_HORIZONTAL -> applyFlipHorizontal(currentBitmap)
                OperationType.FLIP_VERTICAL -> applyFlipVertical(currentBitmap)
                OperationType.FILTER -> applyFilter(currentBitmap, operation.params)
                OperationType.ADJUST -> applyAdjust(currentBitmap, operation.params)
                OperationType.LIGHT_SHADOW -> applyLightShadow(currentBitmap, operation.params)
                OperationType.BEAUTY -> applyBeauty(currentBitmap, operation.params)
                OperationType.TEXT -> applyText(currentBitmap, operation.params)
                OperationType.BRUSH -> applyBrush(currentBitmap, operation.params)
                OperationType.STICKER -> applySticker(currentBitmap, operation.params)
                OperationType.LOCAL_EDIT -> applyLocalEdit(currentBitmap, operation.params)
            }
        }

        return currentBitmap
    }

    /**
     * 应用裁剪 - 公开方法供ViewModel使用
     */
    fun applyCrop(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        val x = (params["x"] ?: 0f).toInt()
        val y = (params["y"] ?: 0f).toInt()
        val width = (params["width"] ?: bitmap.width.toFloat()).toInt()
        val height = (params["height"] ?: bitmap.height.toFloat()).toInt()

        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        val safeWidth = width.coerceAtMost(bitmap.width - safeX)
        val safeHeight = height.coerceAtMost(bitmap.height - safeY)

        return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth.coerceAtLeast(1), safeHeight.coerceAtLeast(1))
    }

    /**
     * 应用旋转 - 公开方法供ViewModel使用
     */
    fun applyRotation(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        val degrees = params["degrees"] ?: 0f
        if (degrees == 0f) return bitmap

        val matrix = Matrix()
        matrix.postRotate(degrees)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 应用水平翻转 - 公开方法供ViewModel使用
     */
    fun applyFlipHorizontal(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 应用垂直翻转 - 公开方法供ViewModel使用
     */
    fun applyFlipVertical(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 应用调整 - 公开方法供ViewModel使用
     */
    fun applyAdjustments(bitmap: Bitmap, params: com.photomaster.ui.editor.AdjustParams): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // 创建颜色矩阵
        val colorMatrix = ColorMatrix()

        // 亮度调整
        val brightness = params.brightness / 100f
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness * 255,
                0f, 1f, 0f, 0f, brightness * 255,
                0f, 0f, 1f, 0f, brightness * 255,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessMatrix)

        // 对比度调整
        val contrast = params.contrast
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix()
        contrastMatrix.set(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        // 饱和度调整
        val saturation = params.saturation
        val lumR = 0.3086f
        val lumG = 0.6094f
        val lumB = 0.0820f
        val sr = (1 - saturation) * lumR
        val sg = (1 - saturation) * lumG
        val sb = (1 - saturation) * lumB
        val saturationMatrix = ColorMatrix()
        saturationMatrix.set(
            floatArrayOf(
                sr + saturation, sg, sb, 0f, 0f,
                sr, sg + saturation, sb, 0f, 0f,
                sr, sg, sb + saturation, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(saturationMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 应用翻转（内部使用）
     */
    private fun applyFlip(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        val flipHorizontal = params["flipHorizontal"] == 1f
        val flipVertical = params["flipVertical"] == 1f

        if (!flipHorizontal && !flipVertical) return bitmap

        val matrix = Matrix()
        if (flipHorizontal) matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        if (flipVertical) matrix.postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 应用滤镜效果
     */
    fun applyFilter(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        val filterType = params["filterType"]?.toInt() ?: 0

        return when (filterType) {
            1 -> applyGrayscaleFilter(bitmap)
            2 -> applySepiaFilter(bitmap)
            3 -> applyVintageFilter(bitmap)
            4 -> applyCoolFilter(bitmap)
            5 -> applyWarmFilter(bitmap)
            6 -> applyFilmFilter(bitmap)
            7 -> applyFoodFilter(bitmap)
            else -> bitmap
        }
    }

    /**
     * 黑白滤镜
     */
    private fun applyGrayscaleFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f) // 完全去饱和

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 复古/怀旧滤镜
     */
    private fun applySepiaFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        // 复古色调矩阵
        colorMatrix.set(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 清新滤镜 - 提高亮度和饱和度
     */
    private fun applyVintageFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()

        // 提高亮度
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, 20f,
                0f, 1f, 0f, 0f, 20f,
                0f, 0f, 1f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessMatrix)

        // 提高对比度
        val contrastMatrix = ColorMatrix()
        contrastMatrix.set(
            floatArrayOf(
                1.1f, 0f, 0f, 0f, -12f,
                0f, 1.1f, 0f, 0f, -12f,
                0f, 0f, 1.1f, 0f, -12f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        // 轻微提高饱和度
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(1.2f)
        colorMatrix.postConcat(saturationMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 冷色滤镜
     */
    private fun applyCoolFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        // 增加蓝色，减少红色
        colorMatrix.set(
            floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1.2f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 暖色滤镜
     */
    private fun applyWarmFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        // 增加红色和黄色
        colorMatrix.set(
            floatArrayOf(
                1.2f, 0f, 0f, 0f, 20f,
                0f, 1.1f, 0f, 0f, 10f,
                0f, 0f, 0.9f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 胶片滤镜 - 增加对比度和颗粒感模拟
     */
    private fun applyFilmFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()

        // 提高对比度
        val contrastMatrix = ColorMatrix()
        contrastMatrix.set(
            floatArrayOf(
                1.3f, 0f, 0f, 0f, -30f,
                0f, 1.3f, 0f, 0f, -30f,
                0f, 0f, 1.3f, 0f, -30f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        // 轻微降低饱和度
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(0.85f)
        colorMatrix.postConcat(saturationMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 美食滤镜 - 提高饱和度和暖色调
     */
    private fun applyFoodFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()

        // 提高饱和度
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(1.3f)
        colorMatrix.postConcat(saturationMatrix)

        // 暖色调
        val warmMatrix = ColorMatrix()
        warmMatrix.set(
            floatArrayOf(
                1.15f, 0f, 0f, 0f, 15f,
                0f, 1.05f, 0f, 0f, 5f,
                0f, 0f, 0.95f, 0f, -5f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(warmMatrix)

        // 提高亮度
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, 10f,
                0f, 1f, 0f, 0f, 10f,
                0f, 0f, 1f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 应用基础调整（内部使用）
     */
    private fun applyAdjust(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        val brightness = params["brightness"] ?: 0f
        val contrast = params["contrast"] ?: 1f
        val saturation = params["saturation"] ?: 1f
        val temperature = params["temperature"] ?: 0f
        val tint = params["tint"] ?: 0f

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()

        // 亮度
        val brightnessDelta = brightness / 100f * 255
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightnessDelta,
                0f, 1f, 0f, 0f, brightnessDelta,
                0f, 0f, 1f, 0f, brightnessDelta,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessMatrix)

        // 对比度
        val scale = contrast.coerceIn(0f, 2f)
        val translate = (-0.5f * scale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix()
        contrastMatrix.set(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        // 饱和度
        val sat = saturation.coerceIn(0f, 2f)
        val lumR = 0.3086f
        val lumG = 0.6094f
        val lumB = 0.0820f
        val sr = (1 - sat) * lumR
        val sg = (1 - sat) * lumG
        val sb = (1 - sat) * lumB
        val saturationMatrix = ColorMatrix()
        saturationMatrix.set(
            floatArrayOf(
                sr + sat, sg, sb, 0f, 0f,
                sr, sg + sat, sb, 0f, 0f,
                sr, sg, sb + sat, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(saturationMatrix)

        // 色温调整 - 暖色/冷色
        if (temperature != 0f) {
            val tempFactor = temperature / 100f
            val tempMatrix = ColorMatrix()
            // 正数偏暖（增加红色和黄色），负数偏冷（增加蓝色）
            val redShift = tempFactor * 40f
            val blueShift = -tempFactor * 40f
            tempMatrix.set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, redShift,
                    0f, 1f, 0f, 0f, redShift * 0.5f, // 绿色变化较小
                    0f, 0f, 1f, 0f, blueShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(tempMatrix)
        }

        // 色调调整 - 洋红/绿色
        if (tint != 0f) {
            val tintFactor = tint / 100f
            val tintMatrix = ColorMatrix()
            // 正数偏洋红，负数偏绿
            val magentaShift = tintFactor * 30f
            val greenShift = -tintFactor * 30f
            tintMatrix.set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, magentaShift,
                    0f, 1f, 0f, 0f, greenShift,
                    1f, 0f, 1f, 0f, magentaShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(tintMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 应用光影效果 - 阴影和高光调整
     */
    fun applyLightShadow(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        val shadows = params["shadows"] ?: 0f  // -100 to 100
        val highlights = params["highlights"] ?: 0f  // -100 to 100
        val exposure = params["exposure"] ?: 0f  // -100 to 100

        if (shadows == 0f && highlights == 0f && exposure == 0f) return bitmap

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix()

        // 曝光调整 - 整体亮度
        if (exposure != 0f) {
            val exposureDelta = exposure / 100f * 255f
            val exposureMatrix = ColorMatrix()
            exposureMatrix.set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, exposureDelta,
                    0f, 1f, 0f, 0f, exposureDelta,
                    0f, 0f, 1f, 0f, exposureDelta,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(exposureMatrix)
        }

        // 阴影调整 - 使用曲线模拟，主要影响暗部
        if (shadows != 0f) {
            val shadowFactor = shadows / 100f
            // 阴影调整矩阵：提升或降低暗部
            val shadowMatrix = ColorMatrix()
            val shadowLift = shadowFactor * 80f  // 最大调整范围
            shadowMatrix.set(
                floatArrayOf(
                    1f + shadowFactor * 0.3f, 0f, 0f, 0f, shadowLift,
                    0f, 1f + shadowFactor * 0.3f, 0f, 0f, shadowLift,
                    0f, 0f, 1f + shadowFactor * 0.3f, 0f, shadowLift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(shadowMatrix)
        }

        // 高光调整 - 主要影响亮部
        if (highlights != 0f) {
            val highlightFactor = highlights / 100f
            // 高光压缩或扩展
            val highlightMatrix = ColorMatrix()
            val highlightCompress = -highlightFactor * 60f
            highlightMatrix.set(
                floatArrayOf(
                    1f - highlightFactor * 0.2f, 0f, 0f, 0f, highlightCompress,
                    0f, 1f - highlightFactor * 0.2f, 0f, 0f, highlightCompress,
                    0f, 0f, 1f - highlightFactor * 0.2f, 0f, highlightCompress,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(highlightMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 应用美颜效果
     */
    fun applyBeauty(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        val smooth = params["smooth"] ?: 0f  // 0 to 100
        val whiten = params["whiten"] ?: 0f  // 0 to 100
        val thinFace = params["thinFace"] ?: 0f  // 0 to 100
        val bigEyes = params["bigEyes"] ?: 0f  // 0 to 100
        val slimNose = params["slimNose"] ?: 0f  // 0 to 100

        // 如果所有参数都为0，直接返回原图
        if (smooth == 0f && whiten == 0f && thinFace == 0f && bigEyes == 0f && slimNose == 0f) {
            return bitmap
        }

        var result = bitmap

        // 应用磨皮效果（使用轻微模糊模拟）
        if (smooth > 0f) {
            result = applySkinSmooth(result, smooth)
        }

        // 应用美白效果
        if (whiten > 0f) {
            result = applySkinWhiten(result, whiten)
        }

        // 应用瘦脸效果（局部拉伸）
        if (thinFace > 0f) {
            result = applyThinFace(result, thinFace)
        }

        // 应用大眼效果（局部放大）
        if (bigEyes > 0f) {
            result = applyBigEyes(result, bigEyes)
        }

        // 应用瘦鼻效果（局部拉伸）
        if (slimNose > 0f) {
            result = applySlimNose(result, slimNose)
        }

        return result
    }

    /**
     * 磨皮效果 - 使用高斯模糊模拟
     */
    private fun applySkinSmooth(bitmap: Bitmap, intensity: Float): Bitmap {
        // 根据强度计算模糊半径
        val radius = (intensity / 100f * 5f).coerceIn(0.5f, 5f)

        // 创建模糊效果
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 使用 Paint 的模糊效果
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        // 绘制原图
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // 简单的模糊处理（使用缩放后放大模拟）
        val scale = 1f - (intensity / 100f * 0.1f)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()

        if (scaledWidth > 0 && scaledHeight > 0) {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            val blurredBitmap = Bitmap.createScaledBitmap(scaledBitmap, bitmap.width, bitmap.height, true)
            scaledBitmap.recycle()

            // 混合原图和模糊图
            val blendPaint = Paint().apply {
                alpha = (intensity / 100f * 128).toInt().coerceIn(0, 128)
                isAntiAlias = true
                isFilterBitmap = true
            }

            canvas.drawBitmap(blurredBitmap, 0f, 0f, blendPaint)
            blurredBitmap.recycle()
        }

        return result
    }

    /**
     * 美白效果
     */
    private fun applySkinWhiten(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix()

        // 美白：提升亮度，稍微降低饱和度
        val whitenFactor = intensity / 100f

        // 亮度提升
        val brightnessDelta = whitenFactor * 30f
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightnessDelta,
                0f, 1f, 0f, 0f, brightnessDelta,
                0f, 0f, 1f, 0f, brightnessDelta,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessMatrix)

        // 轻微降低饱和度（让皮肤更白皙）
        val saturation = 1f - whitenFactor * 0.2f
        val lumR = 0.3086f
        val lumG = 0.6094f
        val lumB = 0.0820f
        val sr = (1 - saturation) * lumR
        val sg = (1 - saturation) * lumG
        val sb = (1 - saturation) * lumB
        val saturationMatrix = ColorMatrix()
        saturationMatrix.set(
            floatArrayOf(
                sr + saturation, sg, sb, 0f, 0f,
                sr, sg + saturation, sb, 0f, 0f,
                sr, sg, sb + saturation, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(saturationMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * 瘦脸效果 - 使用局部拉伸
     */
    private fun applyThinFace(bitmap: Bitmap, intensity: Float): Bitmap {
        // 瘦脸：在脸部两侧向中心收缩
        // 中心点在图片中心，影响整个宽度
        val strength = -intensity / 100f * 30f  // 负值表示收缩

        return applyLocalStretch(
            bitmap,
            mapOf(
                "centerX" to bitmap.width * 0.5f,
                "centerY" to bitmap.height * 0.5f,
                "radius" to bitmap.width * 0.4f,
                "strength" to strength,
                "directionX" to 0f,
                "directionY" to 0f
            )
        )
    }

    /**
     * 大眼效果 - 使用局部放大
     */
    private fun applyBigEyes(bitmap: Bitmap, intensity: Float): Bitmap {
        // 大眼：在眼睛位置（假设在图片上半部分）进行局部放大
        val strength = intensity / 100f * 40f  // 正值表示膨胀

        // 左眼
        var result = applyLocalStretch(
            bitmap,
            mapOf(
                "centerX" to bitmap.width * 0.35f,
                "centerY" to bitmap.height * 0.35f,
                "radius" to bitmap.width * 0.12f,
                "strength" to strength,
                "directionX" to 0f,
                "directionY" to 0f
            )
        )

        // 右眼
        result = applyLocalStretch(
            result,
            mapOf(
                "centerX" to bitmap.width * 0.65f,
                "centerY" to bitmap.height * 0.35f,
                "radius" to bitmap.width * 0.12f,
                "strength" to strength,
                "directionX" to 0f,
                "directionY" to 0f
            )
        )

        return result
    }

    /**
     * 瘦鼻效果 - 使用局部收缩
     */
    private fun applySlimNose(bitmap: Bitmap, intensity: Float): Bitmap {
        // 瘦鼻：在鼻子位置（图片中心偏下）进行水平收缩
        val strength = -intensity / 100f * 25f  // 负值表示收缩

        return applyLocalStretch(
            bitmap,
            mapOf(
                "centerX" to bitmap.width * 0.5f,
                "centerY" to bitmap.height * 0.45f,
                "radius" to bitmap.width * 0.08f,
                "strength" to strength,
                "directionX" to 0f,  // 水平方向收缩
                "directionY" to 0f
            )
        )
    }

    /**
     * 应用文字到图片
     * 注意：文字内容通过外部传入，params中存储位置和样式参数
     */
    fun applyText(bitmap: Bitmap, text: String, params: Map<String, Float>): Bitmap {
        val x = (params["x"] ?: 0.5f) * bitmap.width
        val y = (params["y"] ?: 0.5f) * bitmap.height
        val textSize = params["textSize"] ?: 60f
        val textColor = (params["textColor"]?.toInt()) ?: android.graphics.Color.WHITE
        val rotation = params["rotation"] ?: 0f

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)

        // 绘制原图
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // 创建画笔
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = textColor
            this.textAlign = Paint.Align.CENTER
            // 添加阴影效果
            this.setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        }

        // 保存画布状态
        canvas.save()

        // 移动到文字位置并旋转
        canvas.translate(x, y)
        canvas.rotate(rotation)

        // 绘制文字
        canvas.drawText(text, 0f, 0f, paint)

        // 恢复画布状态
        canvas.restore()

        return result
    }

    /**
     * 应用文字（内部使用，用于processImage流程）
     * 注意：这个方法需要配合外部文字存储使用
     */
    private fun applyText(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        // 由于params只支持Float，文字内容需要在外部管理
        // 这里仅处理位置和样式
        return bitmap
    }

    /**
     * 应用画笔涂鸦
     * @param points 画笔路径点列表，每个点包含x, y坐标
     */
    fun applyBrush(bitmap: Bitmap, points: List<Pair<Float, Float>>, color: Int, strokeWidth: Float): Bitmap {
        if (points.isEmpty()) return bitmap

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)

        // 绘制原图
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // 创建画笔
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.strokeWidth = strokeWidth
            this.style = Paint.Style.STROKE
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
        }

        // 绘制路径
        if (points.size > 1) {
            val path = android.graphics.Path()
            path.moveTo(points[0].first, points[0].second)

            for (i in 1 until points.size) {
                path.lineTo(points[i].first, points[i].second)
            }

            canvas.drawPath(path, paint)
        } else if (points.size == 1) {
            // 单点绘制为圆点
            canvas.drawCircle(points[0].first, points[0].second, strokeWidth / 2, paint)
        }

        return result
    }

    /**
     * 应用画笔（内部使用）
     */
    private fun applyBrush(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        return bitmap
    }

    /**
     * 应用贴纸（Emoji）到图片
     */
    fun applySticker(bitmap: Bitmap, stickerCode: String, params: Map<String, Float>): Bitmap {
        val x = (params["x"] ?: 0.5f) * bitmap.width
        val y = (params["y"] ?: 0.5f) * bitmap.height
        val size = params["size"] ?: 100f
        val rotation = params["rotation"] ?: 0f
        val alpha = params["alpha"] ?: 1f

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)

        // 绘制原图
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // 创建画笔绘制贴纸
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = size
            this.textAlign = Paint.Align.CENTER
            this.alpha = (alpha * 255).toInt()
        }

        // 保存画布状态
        canvas.save()

        // 移动到贴纸位置并旋转
        canvas.translate(x, y)
        canvas.rotate(rotation)

        // 获取字体度量以垂直居中
        val metrics = paint.fontMetrics
        val textOffset = (metrics.descent - metrics.ascent) / 2 - metrics.descent

        // 绘制贴纸
        canvas.drawText(stickerCode, 0f, textOffset, paint)

        // 恢复画布状态
        canvas.restore()

        return result
    }

    /**
     * 应用贴纸（内部使用）
     */
    private fun applySticker(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        return bitmap
    }

    /**
     * 应用局部编辑 - 局部拉伸/液化效果
     * 使用网格变形算法实现局部拉伸
     */
    fun applyLocalStretch(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        val centerX = params["centerX"] ?: (bitmap.width / 2f)
        val centerY = params["centerY"] ?: (bitmap.height / 2f)
        val radius = params["radius"] ?: 100f
        val strength = params["strength"] ?: 0f  // -100 to 100, 负数收缩，正数膨胀
        val directionX = params["directionX"] ?: 0f  // -1 to 1, 水平方向
        val directionY = params["directionY"] ?: 0f  // -1 to 1, 垂直方向

        if (strength == 0f && directionX == 0f && directionY == 0f) return bitmap

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.isFilterBitmap = true

        // 创建变形矩阵
        val matrix = Matrix()

        // 使用液化算法
        val src = FloatArray(bitmap.width * bitmap.height * 2)
        val dst = FloatArray(bitmap.width * bitmap.height * 2)

        var index = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                src[index] = x.toFloat()
                src[index + 1] = y.toFloat()

                // 计算到中心的距离
                val dx = x - centerX
                val dy = y - centerY
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                if (distance < radius) {
                    // 在影响范围内，应用变形
                    val factor = 1 - (distance / radius)
                    val smoothFactor = factor * factor * (3 - 2 * factor) // 平滑插值

                    // 膨胀/收缩效果
                    val stretchStrength = strength / 100f * smoothFactor * 50f

                    // 方向拉伸
                    val dirX = directionX * smoothFactor * 30f
                    val dirY = directionY * smoothFactor * 30f

                    // 计算新位置
                    val newX = x + dx * stretchStrength / radius * 20f + dirX
                    val newY = y + dy * stretchStrength / radius * 20f + dirY

                    dst[index] = newX.coerceIn(0f, bitmap.width - 1f)
                    dst[index + 1] = newY.coerceIn(0f, bitmap.height - 1f)
                } else {
                    // 不在影响范围内，保持原位置
                    dst[index] = x.toFloat()
                    dst[index + 1] = y.toFloat()
                }
                index += 2
            }
        }

        // 使用双线性插值绘制变形后的图像
        val resultPixels = IntArray(bitmap.width * bitmap.height)
        val srcPixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(srcPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        index = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val srcX = dst[index]
                val srcY = dst[index + 1]

                // 双线性插值
                val x0 = srcX.toInt().coerceIn(0, bitmap.width - 2)
                val y0 = srcY.toInt().coerceIn(0, bitmap.height - 2)
                val x1 = (x0 + 1).coerceAtMost(bitmap.width - 1)
                val y1 = (y0 + 1).coerceAtMost(bitmap.height - 1)

                val fx = srcX - x0
                val fy = srcY - y0

                val idx00 = y0 * bitmap.width + x0
                val idx01 = y0 * bitmap.width + x1
                val idx10 = y1 * bitmap.width + x0
                val idx11 = y1 * bitmap.width + x1

                val c00 = srcPixels[idx00]
                val c01 = srcPixels[idx01]
                val c10 = srcPixels[idx10]
                val c11 = srcPixels[idx11]

                // 插值计算
                val r = ((1 - fx) * (1 - fy) * ((c00 shr 16) and 0xFF) +
                        fx * (1 - fy) * ((c01 shr 16) and 0xFF) +
                        (1 - fx) * fy * ((c10 shr 16) and 0xFF) +
                        fx * fy * ((c11 shr 16) and 0xFF)).toInt()
                val g = ((1 - fx) * (1 - fy) * ((c00 shr 8) and 0xFF) +
                        fx * (1 - fy) * ((c01 shr 8) and 0xFF) +
                        (1 - fx) * fy * ((c10 shr 8) and 0xFF) +
                        fx * fy * ((c11 shr 8) and 0xFF)).toInt()
                val b = ((1 - fx) * (1 - fy) * (c00 and 0xFF) +
                        fx * (1 - fy) * (c01 and 0xFF) +
                        (1 - fx) * fy * (c10 and 0xFF) +
                        fx * fy * (c11 and 0xFF)).toInt()
                val a = ((c00 shr 24) and 0xFF)

                resultPixels[y * bitmap.width + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
                index += 2
            }
        }

        result.setPixels(resultPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }

    /**
     * 应用局部编辑（内部使用）
     */
    private fun applyLocalEdit(bitmap: Bitmap, params: Map<String, Float>): Bitmap {
        return applyLocalStretch(bitmap, params)
    }

    /**
     * 应用自定义滤镜效果
     * 支持色相、饱和度、亮度、对比度、色温、褪色、颗粒、暗角等参数
     */
    fun applyCustomFilter(bitmap: Bitmap, params: com.photomaster.ui.editor.FilterParams): Bitmap {
        val hue = params.hue
        val saturation = params.saturation
        val brightness = params.brightness
        val contrast = params.contrast
        val warmth = params.warmth
        val fade = params.fade
        val grain = params.grain
        val vignette = params.vignette

        // 如果所有参数都是默认值，直接返回原图
        if (hue == 0f && saturation == 1f && brightness == 0f && contrast == 1f &&
            warmth == 0f && fade == 0f && grain == 0f && vignette == 0f) {
            return bitmap
        }

        var result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix()

        // 色相调整
        if (hue != 0f) {
            val hueMatrix = ColorMatrix()
            hueMatrix.setRotate(0, hue) // 红色通道
            hueMatrix.setRotate(1, hue) // 绿色通道
            hueMatrix.setRotate(2, hue) // 蓝色通道
            colorMatrix.postConcat(hueMatrix)
        }

        // 饱和度调整
        if (saturation != 1f) {
            val saturationMatrix = ColorMatrix()
            saturationMatrix.setSaturation(saturation.coerceIn(0f, 2f))
            colorMatrix.postConcat(saturationMatrix)
        }

        // 亮度调整
        if (brightness != 0f) {
            val brightnessDelta = brightness / 100f * 255f
            val brightnessMatrix = ColorMatrix()
            brightnessMatrix.set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, brightnessDelta,
                    0f, 1f, 0f, 0f, brightnessDelta,
                    0f, 0f, 1f, 0f, brightnessDelta,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(brightnessMatrix)
        }

        // 对比度调整
        if (contrast != 1f) {
            val scale = contrast.coerceIn(0f, 2f)
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMatrix = ColorMatrix()
            contrastMatrix.set(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(contrastMatrix)
        }

        // 色温调整（暖色/冷色）
        if (warmth != 0f) {
            val warmthFactor = warmth / 100f
            val warmthMatrix = ColorMatrix()
            // 正数偏暖（增加红色和黄色），负数偏冷（增加蓝色）
            val redShift = warmthFactor * 40f
            val blueShift = -warmthFactor * 40f
            warmthMatrix.set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, redShift,
                    0f, 1f, 0f, 0f, redShift * 0.5f,
                    0f, 0f, 1f, 0f, blueShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(warmthMatrix)
        }

        // 褪色效果（降低饱和度并提亮阴影）
        if (fade > 0f) {
            val fadeFactor = fade / 100f
            val fadeMatrix = ColorMatrix()
            // 降低对比度
            val fadeScale = 1f - fadeFactor * 0.3f
            val fadeTranslate = fadeFactor * 30f
            fadeMatrix.set(
                floatArrayOf(
                    fadeScale, 0f, 0f, 0f, fadeTranslate,
                    0f, fadeScale, 0f, 0f, fadeTranslate,
                    0f, 0f, fadeScale, 0f, fadeTranslate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(fadeMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // 颗粒效果
        if (grain > 0f) {
            result = applyGrainEffect(result, grain)
        }

        // 暗角效果
        if (vignette > 0f) {
            result = applyVignetteEffect(result, vignette)
        }

        return result
    }

    /**
     * 应用颗粒效果
     */
    private fun applyGrainEffect(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 绘制原图
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val grainIntensity = (intensity / 100f * 30f).toInt().coerceIn(0, 30)
        if (grainIntensity > 0) {
            val paint = Paint()
            val random = java.util.Random(12345) // 固定种子以获得可重复的结果

            // 创建颗粒层
            for (i in 0 until bitmap.width step 2) {
                for (j in 0 until bitmap.height step 2) {
                    if (random.nextInt(100) < grainIntensity) {
                        val noise = random.nextInt(256)
                        val color = android.graphics.Color.argb(
                            20, // 透明度
                            noise,
                            noise,
                            noise
                        )
                        paint.color = color
                        canvas.drawPoint(i.toFloat(), j.toFloat(), paint)
                    }
                }
            }
        }

        return result
    }

    /**
     * 应用暗角效果
     */
    private fun applyVignetteEffect(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 绘制原图
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val vignetteIntensity = intensity / 100f
        if (vignetteIntensity > 0f) {
            val centerX = bitmap.width / 2f
            val centerY = bitmap.height / 2f
            val maxDistance = kotlin.math.sqrt(centerX * centerX + centerY * centerY)

            // 创建暗角渐变
            val colors = intArrayOf(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb((vignetteIntensity * 180).toInt(), 0, 0, 0)
            )
            val positions = floatArrayOf(0.5f, 1f)

            val gradient = android.graphics.RadialGradient(
                centerX,
                centerY,
                maxDistance,
                colors,
                positions,
                android.graphics.Shader.TileMode.CLAMP
            )

            val paint = Paint().apply {
                isAntiAlias = true
                shader = gradient
            }

            canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        }

        return result
    }
}
