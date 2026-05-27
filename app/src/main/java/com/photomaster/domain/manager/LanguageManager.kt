package com.photomaster.domain.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 支持的语言
 */
enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    CHINESE_SIMPLIFIED("zh-CN", "简体中文", "简体中文"),
    CHINESE_TRADITIONAL("zh-TW", "繁体中文", "繁體中文"),
    ENGLISH("en", "English", "English"),
    JAPANESE("ja", "日本語", "日本語"),
    KOREAN("ko", "한국어", "한국어"),
    FRENCH("fr", "Français", "Français"),
    GERMAN("de", "Deutsch", "Deutsch"),
    SPANISH("es", "Español", "Español"),
    RUSSIAN("ru", "Русский", "Русский");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().find { it.code == code } ?: CHINESE_SIMPLIFIED
        }

        fun getDefault(): AppLanguage {
            val systemLocale = Locale.getDefault()
            return when (systemLocale.language) {
                "zh" -> if (systemLocale.country == "TW" || systemLocale.country == "HK") {
                    CHINESE_TRADITIONAL
                } else {
                    CHINESE_SIMPLIFIED
                }
                "en" -> ENGLISH
                "ja" -> JAPANESE
                "ko" -> KOREAN
                "fr" -> FRENCH
                "de" -> GERMAN
                "es" -> SPANISH
                "ru" -> RUSSIAN
                else -> CHINESE_SIMPLIFIED
            }
        }
    }
}

/**
 * 语言管理器
 */
class LanguageManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(AppLanguage.CHINESE_SIMPLIFIED)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE_CODE = "language_code"
    }

    init {
        loadLanguage()
    }

    /**
     * 加载保存的语言设置
     */
    private fun loadLanguage() {
        val savedCode = prefs.getString(KEY_LANGUAGE_CODE, null)
        _currentLanguage.value = if (savedCode != null) {
            AppLanguage.fromCode(savedCode)
        } else {
            AppLanguage.getDefault()
        }
    }

    /**
     * 设置语言
     */
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE_CODE, language.code).apply()
        _currentLanguage.value = language
        updateLocale(language)
    }

    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(): AppLanguage = _currentLanguage.value

    /**
     * 更新应用语言环境
     */
    private fun updateLocale(language: AppLanguage) {
        val locale = Locale(language.code.split("-")[0])
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    /**
     * 获取字符串资源
     */
    fun getString(key: String): String {
        return translations[_currentLanguage.value.code]?.get(key)
            ?: translations[AppLanguage.CHINESE_SIMPLIFIED.code]?.get(key)
            ?: key
    }

    /**
     * 获取所有支持的语言
     */
    fun getSupportedLanguages(): List<AppLanguage> = AppLanguage.values().toList()
}

/**
 * CompositionLocal 用于在 Compose 中提供语言管理器
 */
val LocalLanguageManager = compositionLocalOf<LanguageManager> {
    error("LanguageManager not provided")
}

/**
 * 提供语言管理器的 Composable
 */
@Composable
fun ProvideLanguageManager(
    languageManager: LanguageManager,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLanguageManager provides languageManager) {
        content()
    }
}

/**
 * 获取当前语言管理器
 */
@Composable
fun rememberLanguageManager(): LanguageManager {
    return LocalLanguageManager.current
}

/**
 * 翻译字符串
 */
@Composable
fun stringResource(key: String): String {
    val languageManager = rememberLanguageManager()
    return remember(languageManager.currentLanguage.value) {
        languageManager.getString(key)
    }
}

/**
 * 翻译映射表
 */
private val translations = mapOf(
    "zh-CN" to mapOf(
        // 通用
        "app_name" to "PhotoMaster",
        "confirm" to "确定",
        "cancel" to "取消",
        "save" to "保存",
        "delete" to "删除",
        "edit" to "编辑",
        "share" to "分享",
        "export" to "导出",
        "back" to "返回",
        "next" to "下一步",
        "done" to "完成",
        "loading" to "加载中...",
        "error" to "错误",
        "success" to "成功",
        "warning" to "警告",
        "info" to "提示",

        // 首页
        "home_title" to "PhotoMaster",
        "create_new" to "新建编辑",
        "drafts" to "草稿箱",
        "completed" to "已完成",
        "deleted" to "回收站",
        "batch_process" to "批量处理",
        "settings" to "设置",
        "no_drafts" to "暂无草稿",
        "no_completed" to "暂无已完成作品",
        "no_deleted" to "回收站为空",

        // 编辑页面
        "editor_title" to "编辑",
        "crop" to "裁剪",
        "rotate" to "旋转",
        "flip" to "翻转",
        "adjust" to "调整",
        "filter" to "滤镜",
        "beauty" to "美颜",
        "text" to "文字",
        "sticker" to "贴纸",
        "brush" to "画笔",
        "light_shadow" to "光影",
        "local_stretch" to "局部",
        "undo" to "撤销",
        "redo" to "重做",
        "reset" to "重置",
        "apply" to "应用",

        // 调整选项
        "brightness" to "亮度",
        "contrast" to "对比度",
        "saturation" to "饱和度",
        "temperature" to "色温",
        "tint" to "色调",
        "exposure" to "曝光",
        "highlights" to "高光",
        "shadows" to "阴影",
        "sharpness" to "锐化",
        "vignette" to "暗角",

        // 美颜选项
        "smooth" to "磨皮",
        "whiten" to "美白",
        "thin_face" to "瘦脸",
        "big_eyes" to "大眼",
        "slim_nose" to "瘦鼻",

        // 滤镜
        "original" to "原图",
        "grayscale" to "黑白",
        "sepia" to "复古",
        "vintage" to "清新",
        "cool" to "冷色",
        "warm" to "暖色",
        "film" to "胶片",
        "food" to "美食",

        // 图层
        "layers" to "图层",
        "layer_background" to "背景",
        "layer_text" to "文字",
        "layer_sticker" to "贴纸",
        "layer_image" to "图片",
        "opacity" to "不透明度",
        "blend_mode" to "混合模式",
        "visible" to "可见",
        "hidden" to "隐藏",
        "lock" to "锁定",
        "unlock" to "解锁",
        "merge" to "合并",
        "duplicate" to "复制",

        // 批量处理
        "batch_title" to "批量处理",
        "select_images" to "选择图片",
        "output_settings" to "输出设置",
        "format" to "格式",
        "quality" to "质量",
        "resize" to "调整尺寸",
        "preset_social" to "社交媒体",
        "preset_web" to "网页优化",
        "preset_print" to "打印准备",
        "preset_thumbnail" to "缩略图",
        "start_batch" to "开始处理",
        "processing" to "处理中",
        "completed_count" to "已完成",

        // 设置
        "settings_title" to "设置",
        "language" to "语言",
        "theme" to "主题",
        "theme_light" to "浅色",
        "theme_dark" to "深色",
        "theme_system" to "跟随系统",
        "about" to "关于",
        "version" to "版本",
        "privacy_policy" to "隐私政策",
        "terms_of_service" to "服务条款",
        "feedback" to "反馈",
        "rate_app" to "评价应用",
        "share_app" to "分享应用",
        "preferences" to "偏好设置",
        "done" to "完成",

        // 许可证
        "license" to "许可证",
        "trial_status" to "试用状态",
        "activated" to "已激活",
        "not_activated" to "未激活",
        "remaining_days" to "剩余天数",
        "activate_now" to "立即激活",
        "enter_activation_code" to "输入激活码",
        "activation_success" to "激活成功",
        "activation_failed" to "激活失败",
        "trial_expired" to "试用期已结束",
        "trial_expiring_soon" to "试用期即将结束"
    ),

    "en" to mapOf(
        // Common
        "app_name" to "PhotoMaster",
        "confirm" to "Confirm",
        "cancel" to "Cancel",
        "save" to "Save",
        "delete" to "Delete",
        "edit" to "Edit",
        "share" to "Share",
        "export" to "Export",
        "back" to "Back",
        "next" to "Next",
        "done" to "Done",
        "loading" to "Loading...",
        "error" to "Error",
        "success" to "Success",
        "warning" to "Warning",
        "info" to "Info",

        // Home
        "home_title" to "PhotoMaster",
        "create_new" to "Create New",
        "drafts" to "Drafts",
        "completed" to "Completed",
        "deleted" to "Deleted",
        "batch_process" to "Batch Process",
        "settings" to "Settings",
        "no_drafts" to "No drafts",
        "no_completed" to "No completed works",
        "no_deleted" to "Trash is empty",

        // Editor
        "editor_title" to "Edit",
        "crop" to "Crop",
        "rotate" to "Rotate",
        "flip" to "Flip",
        "adjust" to "Adjust",
        "filter" to "Filter",
        "beauty" to "Beauty",
        "text" to "Text",
        "sticker" to "Sticker",
        "brush" to "Brush",
        "light_shadow" to "Light",
        "local_stretch" to "Local",
        "undo" to "Undo",
        "redo" to "Redo",
        "reset" to "Reset",
        "apply" to "Apply",

        // Adjustments
        "brightness" to "Brightness",
        "contrast" to "Contrast",
        "saturation" to "Saturation",
        "temperature" to "Temperature",
        "tint" to "Tint",
        "exposure" to "Exposure",
        "highlights" to "Highlights",
        "shadows" to "Shadows",
        "sharpness" to "Sharpness",
        "vignette" to "Vignette",

        // Beauty
        "smooth" to "Smooth",
        "whiten" to "Whiten",
        "thin_face" to "Thin Face",
        "big_eyes" to "Big Eyes",
        "slim_nose" to "Slim Nose",

        // Filters
        "original" to "Original",
        "grayscale" to "Grayscale",
        "sepia" to "Sepia",
        "vintage" to "Vintage",
        "cool" to "Cool",
        "warm" to "Warm",
        "film" to "Film",
        "food" to "Food",

        // Layers
        "layers" to "Layers",
        "layer_background" to "Background",
        "layer_text" to "Text",
        "layer_sticker" to "Sticker",
        "layer_image" to "Image",
        "opacity" to "Opacity",
        "blend_mode" to "Blend Mode",
        "visible" to "Visible",
        "hidden" to "Hidden",
        "lock" to "Lock",
        "unlock" to "Unlock",
        "merge" to "Merge",
        "duplicate" to "Duplicate",

        // Batch
        "batch_title" to "Batch Process",
        "select_images" to "Select Images",
        "output_settings" to "Output Settings",
        "format" to "Format",
        "quality" to "Quality",
        "resize" to "Resize",
        "preset_social" to "Social Media",
        "preset_web" to "Web Optimize",
        "preset_print" to "Print Ready",
        "preset_thumbnail" to "Thumbnail",
        "start_batch" to "Start Processing",
        "processing" to "Processing",
        "completed_count" to "Completed",

        // Settings
        "settings_title" to "Settings",
        "language" to "Language",
        "theme" to "Theme",
        "theme_light" to "Light",
        "theme_dark" to "Dark",
        "theme_system" to "System",
        "about" to "About",
        "version" to "Version",
        "privacy_policy" to "Privacy Policy",
        "terms_of_service" to "Terms of Service",
        "feedback" to "Feedback",
        "rate_app" to "Rate App",
        "share_app" to "Share App",
        "preferences" to "Preferences",
        "done" to "Done",

        // License
        "license" to "License",
        "trial_status" to "Trial Status",
        "activated" to "Activated",
        "not_activated" to "Not Activated",
        "remaining_days" to "Remaining Days",
        "activate_now" to "Activate Now",
        "enter_activation_code" to "Enter Activation Code",
        "activation_success" to "Activation Successful",
        "activation_failed" to "Activation Failed",
        "trial_expired" to "Trial Expired",
        "trial_expiring_soon" to "Trial Expiring Soon"
    )
)
