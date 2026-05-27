package com.photomaster.utils

import android.Manifest
import android.os.Build

object PermissionUtils {
    
    // 获取所需的权限列表（根据Android版本）
    fun getRequiredPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.CAMERA
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-12
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
            }
            else -> {
                // Android 9及以下
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
            }
        }
    }
    
    // 权限说明文本
    fun getPermissionRationale(permission: String): String {
        return when {
            permission.contains("CAMERA") -> "需要相机权限来拍摄照片"
            permission.contains("STORAGE") || permission.contains("MEDIA") -> "需要存储权限来访问和保存图片"
            else -> "需要此权限以继续使用应用功能"
        }
    }
}
