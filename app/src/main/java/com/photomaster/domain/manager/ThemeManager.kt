package com.photomaster.domain.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主题模式
 */
enum class ThemeMode {
    LIGHT,      // 浅色模式
    DARK,       // 深色模式
    SYSTEM      // 跟随系统
}

/**
 * 应用主题
 */
enum class AppTheme {
    DEFAULT,    // 默认主题
    BLUE,       // 蓝色主题
    GREEN,      // 绿色主题
    PURPLE,     // 紫色主题
    ORANGE,     // 橙色主题
    PINK,       // 粉色主题
    RED,        // 红色主题
    TEAL        // 青色主题
}

/**
 * 主题管理器
 */
class ThemeManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _appTheme = MutableStateFlow(AppTheme.DEFAULT)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_THEME = "app_theme"
    }

    init {
        loadThemeSettings()
    }

    /**
     * 加载主题设置
     */
    private fun loadThemeSettings() {
        val themeModeOrdinal = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
        _themeMode.value = ThemeMode.values()[themeModeOrdinal]

        val appThemeOrdinal = prefs.getInt(KEY_APP_THEME, AppTheme.DEFAULT.ordinal)
        _appTheme.value = AppTheme.values()[appThemeOrdinal]
    }

    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
        _themeMode.value = mode
    }

    /**
     * 设置应用主题
     */
    fun setAppTheme(theme: AppTheme) {
        prefs.edit().putInt(KEY_APP_THEME, theme.ordinal).apply()
        _appTheme.value = theme
    }

    /**
     * 获取当前是否为深色模式
     */
    @Composable
    fun isDarkTheme(): Boolean {
        return when (_themeMode.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    }

    /**
     * 获取当前颜色方案
     */
    @Composable
    fun getColorScheme(): ColorScheme {
        val isDark = isDarkTheme()
        val theme = _appTheme.value

        return if (isDark) {
            getDarkColorScheme(theme)
        } else {
            getLightColorScheme(theme)
        }
    }

    /**
     * 获取浅色颜色方案
     */
    private fun getLightColorScheme(theme: AppTheme): ColorScheme {
        val baseScheme = lightColorScheme()

        return when (theme) {
            AppTheme.DEFAULT -> baseScheme
            AppTheme.BLUE -> baseScheme.copy(
                primary = Color(0xFF1976D2),
                primaryContainer = Color(0xFFBBDEFB),
                secondary = Color(0xFF1565C0)
            )
            AppTheme.GREEN -> baseScheme.copy(
                primary = Color(0xFF388E3C),
                primaryContainer = Color(0xFFC8E6C9),
                secondary = Color(0xFF2E7D32)
            )
            AppTheme.PURPLE -> baseScheme.copy(
                primary = Color(0xFF7B1FA2),
                primaryContainer = Color(0xFFE1BEE7),
                secondary = Color(0xFF6A1B9A)
            )
            AppTheme.ORANGE -> baseScheme.copy(
                primary = Color(0xFFF57C00),
                primaryContainer = Color(0xFFFFE0B2),
                secondary = Color(0xFFEF6C00)
            )
            AppTheme.PINK -> baseScheme.copy(
                primary = Color(0xFFC2185B),
                primaryContainer = Color(0xFFF8BBD9),
                secondary = Color(0xFFAD1457)
            )
            AppTheme.RED -> baseScheme.copy(
                primary = Color(0xFFD32F2F),
                primaryContainer = Color(0xFFFFCDD2),
                secondary = Color(0xFFC62828)
            )
            AppTheme.TEAL -> baseScheme.copy(
                primary = Color(0xFF00796B),
                primaryContainer = Color(0xFFB2DFDB),
                secondary = Color(0xFF00695C)
            )
        }
    }

    /**
     * 获取深色颜色方案
     */
    private fun getDarkColorScheme(theme: AppTheme): ColorScheme {
        val baseScheme = darkColorScheme()

        return when (theme) {
            AppTheme.DEFAULT -> baseScheme
            AppTheme.BLUE -> baseScheme.copy(
                primary = Color(0xFF90CAF9),
                primaryContainer = Color(0xFF1565C0),
                secondary = Color(0xFF64B5F6)
            )
            AppTheme.GREEN -> baseScheme.copy(
                primary = Color(0xFFA5D6A7),
                primaryContainer = Color(0xFF2E7D32),
                secondary = Color(0xFF81C784)
            )
            AppTheme.PURPLE -> baseScheme.copy(
                primary = Color(0xFFCE93D8),
                primaryContainer = Color(0xFF6A1B9A),
                secondary = Color(0xFFBA68C8)
            )
            AppTheme.ORANGE -> baseScheme.copy(
                primary = Color(0xFFFFB74D),
                primaryContainer = Color(0xFFEF6C00),
                secondary = Color(0xFFFFA726)
            )
            AppTheme.PINK -> baseScheme.copy(
                primary = Color(0xFFF48FB1),
                primaryContainer = Color(0xFFAD1457),
                secondary = Color(0xFFF06292)
            )
            AppTheme.RED -> baseScheme.copy(
                primary = Color(0xFFEF9A9A),
                primaryContainer = Color(0xFFC62828),
                secondary = Color(0xFFE57373)
            )
            AppTheme.TEAL -> baseScheme.copy(
                primary = Color(0xFF80CBC4),
                primaryContainer = Color(0xFF00695C),
                secondary = Color(0xFF4DB6AC)
            )
        }
    }

    /**
     * 获取主题显示名称
     */
    fun getThemeDisplayName(theme: AppTheme): String {
        return when (theme) {
            AppTheme.DEFAULT -> "默认"
            AppTheme.BLUE -> "蓝色"
            AppTheme.GREEN -> "绿色"
            AppTheme.PURPLE -> "紫色"
            AppTheme.ORANGE -> "橙色"
            AppTheme.PINK -> "粉色"
            AppTheme.RED -> "红色"
            AppTheme.TEAL -> "青色"
        }
    }

    /**
     * 获取主题模式显示名称
     */
    fun getThemeModeDisplayName(mode: ThemeMode): String {
        return when (mode) {
            ThemeMode.LIGHT -> "浅色"
            ThemeMode.DARK -> "深色"
            ThemeMode.SYSTEM -> "跟随系统"
        }
    }

    /**
     * 获取所有主题
     */
    fun getAllThemes(): List<AppTheme> = AppTheme.values().toList()

    /**
     * 获取所有主题模式
     */
    fun getAllThemeModes(): List<ThemeMode> = ThemeMode.values().toList()
}

/**
 * CompositionLocal 用于在 Compose 中提供主题管理器
 */
val LocalThemeManager = compositionLocalOf<ThemeManager> {
    error("ThemeManager not provided")
}

/**
 * 提供主题管理器的 Composable
 */
@Composable
fun ProvideThemeManager(
    themeManager: ThemeManager,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalThemeManager provides themeManager) {
        content()
    }
}

/**
 * 获取当前主题管理器
 */
@Composable
fun rememberThemeManager(): ThemeManager {
    return LocalThemeManager.current
}
