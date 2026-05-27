package com.photomaster.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.photomaster.security.LicenseStatus
import com.photomaster.security.LicenseValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val context: Context,
    private val licenseValidator: LicenseValidator
) : ViewModel() {

    // 登录状态
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // 剩余天数
    private val _remainingDays = MutableStateFlow(7)
    val remainingDays: StateFlow<Int> = _remainingDays.asStateFlow()

    // 许可证状态
    private val _licenseStatus = MutableStateFlow(LicenseStatus.VALID)
    val licenseStatus: StateFlow<LicenseStatus> = _licenseStatus.asStateFlow()

    // 深色模式
    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    // 语言选择
    private val _selectedLanguage = MutableStateFlow("zh-CN")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // 显示设置页面
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    // 语言列表
    val languages = listOf(
        "zh-CN" to "简体中文",
        "zh-TW" to "繁體中文",
        "en" to "English",
        "ja" to "日本語"
    )

    init {
        loadLicenseInfo()
    }

    private fun loadLicenseInfo() {
        viewModelScope.launch {
            _remainingDays.value = licenseValidator.getRemainingDays()
            _licenseStatus.value = licenseValidator.getLicenseStatus()
        }
    }

    fun toggleLogin() {
        _isLoggedIn.value = !_isLoggedIn.value
    }

    fun toggleDarkMode() {
        _darkMode.value = !_darkMode.value
    }

    fun showSettings() {
        _showSettings.value = true
    }

    fun hideSettings() {
        _showSettings.value = false
    }

    fun getLanguageName(code: String): String {
        return languages.find { it.first == code }?.second ?: "简体中文"
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(
                        context,
                        LicenseValidator(context)
                    ) as T
                }
            }
        }
    }
}
