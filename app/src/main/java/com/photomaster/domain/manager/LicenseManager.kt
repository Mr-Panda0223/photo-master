package com.photomaster.domain.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * 许可证状态
 */
enum class LicenseStatus {
    VALID,          // 有效
    EXPIRED,        // 已过期
    FIRST_LAUNCH,   // 首次启动
    UNKNOWN         // 未知
}

/**
 * 许可证信息
 */
data class LicenseInfo(
    val firstLaunchDate: Long = 0L,
    val lastCheckDate: Long = 0L,
    val trialDays: Int = 7,
    val isActivated: Boolean = false,
    val activationCode: String? = null
) {
    /**
     * 获取剩余天数
     */
    fun getRemainingDays(): Int {
        if (isActivated) return Int.MAX_VALUE
        if (firstLaunchDate == 0L) return trialDays

        val elapsedTime = System.currentTimeMillis() - firstLaunchDate
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedTime).toInt()
        return (trialDays - elapsedDays).coerceAtLeast(0)
    }

    /**
     * 是否已过期
     */
    fun isExpired(): Boolean {
        if (isActivated) return false
        return getRemainingDays() <= 0
    }

    /**
     * 获取已使用天数
     */
    fun getUsedDays(): Int {
        if (firstLaunchDate == 0L) return 0
        val elapsedTime = System.currentTimeMillis() - firstLaunchDate
        return TimeUnit.MILLISECONDS.toDays(elapsedTime).toInt()
    }
}

/**
 * 许可证管理器
 * 管理应用的试用期和激活状态
 */
class LicenseManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _licenseStatus = MutableStateFlow(LicenseStatus.UNKNOWN)
    val licenseStatus: StateFlow<LicenseStatus> = _licenseStatus.asStateFlow()

    private val _licenseInfo = MutableStateFlow(LicenseInfo())
    val licenseInfo: StateFlow<LicenseInfo> = _licenseInfo.asStateFlow()

    companion object {
        private const val PREFS_NAME = "license_prefs"
        private const val KEY_FIRST_LAUNCH_DATE = "first_launch_date"
        private const val KEY_LAST_CHECK_DATE = "last_check_date"
        private const val KEY_TRIAL_DAYS = "trial_days"
        private const val KEY_IS_ACTIVATED = "is_activated"
        private const val KEY_ACTIVATION_CODE = "activation_code"
        private const val DEFAULT_TRIAL_DAYS = 7
    }

    init {
        loadLicenseInfo()
    }

    /**
     * 初始化许可证
     * 在应用启动时调用
     */
    fun initialize() {
        val currentInfo = _licenseInfo.value

        // 首次启动
        if (currentInfo.firstLaunchDate == 0L) {
            val firstLaunchDate = System.currentTimeMillis()
            saveLicenseInfo(currentInfo.copy(
                firstLaunchDate = firstLaunchDate,
                lastCheckDate = firstLaunchDate,
                trialDays = DEFAULT_TRIAL_DAYS
            ))
            _licenseStatus.value = LicenseStatus.FIRST_LAUNCH
            return
        }

        // 更新最后检查时间
        saveLicenseInfo(currentInfo.copy(lastCheckDate = System.currentTimeMillis()))

        // 检查许可证状态
        checkLicenseStatus()
    }

    /**
     * 检查许可证状态
     */
    fun checkLicenseStatus(): LicenseStatus {
        val info = _licenseInfo.value

        val status = when {
            info.isActivated -> LicenseStatus.VALID
            info.isExpired() -> LicenseStatus.EXPIRED
            info.firstLaunchDate == 0L -> LicenseStatus.FIRST_LAUNCH
            else -> LicenseStatus.VALID
        }

        _licenseStatus.value = status
        return status
    }

    /**
     * 激活应用
     */
    fun activate(activationCode: String): Boolean {
        // 简单的激活码验证（实际应用中应该使用服务器验证）
        if (isValidActivationCode(activationCode)) {
            val currentInfo = _licenseInfo.value
            saveLicenseInfo(currentInfo.copy(
                isActivated = true,
                activationCode = activationCode
            ))
            _licenseStatus.value = LicenseStatus.VALID
            return true
        }
        return false
    }

    /**
     * 验证激活码是否有效
     */
    private fun isValidActivationCode(code: String): Boolean {
        // 简单的激活码格式验证
        // 格式: XXXX-XXXX-XXXX-XXXX (16位，每4位一组)
        val cleaned = code.replace("-", "").uppercase()

        // 基本格式检查
        if (cleaned.length != 16) return false
        if (!cleaned.all { it.isLetterOrDigit() }) return false

        // 简单的校验和验证（示例）
        // 实际应用中应该使用服务器验证或更复杂的算法
        val checksum = cleaned.take(12).sumOf { it.code }
        val expectedChecksum = cleaned.takeLast(4).toIntOrNull(16) ?: 0

        return checksum % 65536 == expectedChecksum
    }

    /**
     * 延长试用期（用于促销或特殊情况）
     */
    fun extendTrial(additionalDays: Int) {
        val currentInfo = _licenseInfo.value
        saveLicenseInfo(currentInfo.copy(
            trialDays = currentInfo.trialDays + additionalDays
        ))
        checkLicenseStatus()
    }

    /**
     * 重置试用期（仅用于测试）
     */
    fun resetTrial() {
        saveLicenseInfo(LicenseInfo(
            firstLaunchDate = System.currentTimeMillis(),
            lastCheckDate = System.currentTimeMillis(),
            trialDays = DEFAULT_TRIAL_DAYS,
            isActivated = false,
            activationCode = null
        ))
        _licenseStatus.value = LicenseStatus.FIRST_LAUNCH
    }

    /**
     * 获取剩余试用天数
     */
    fun getRemainingDays(): Int {
        return _licenseInfo.value.getRemainingDays()
    }

    /**
     * 获取已使用天数
     */
    fun getUsedDays(): Int {
        return _licenseInfo.value.getUsedDays()
    }

    /**
     * 是否已激活
     */
    fun isActivated(): Boolean {
        return _licenseInfo.value.isActivated
    }

    /**
     * 是否已过期
     */
    fun isExpired(): Boolean {
        return _licenseInfo.value.isExpired()
    }

    /**
     * 获取到期日期字符串
     */
    fun getExpiryDateString(): String {
        val info = _licenseInfo.value
        return if (info.isActivated) {
            "永久有效"
        } else {
            val expiryDate = info.firstLaunchDate + TimeUnit.DAYS.toMillis(info.trialDays.toLong())
            val date = Date(expiryDate)
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
        }
    }

    /**
     * 获取首次启动日期字符串
     */
    fun getFirstLaunchDateString(): String {
        val info = _licenseInfo.value
        return if (info.firstLaunchDate == 0L) {
            "未记录"
        } else {
            val date = Date(info.firstLaunchDate)
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
        }
    }

    /**
     * 加载许可证信息
     */
    private fun loadLicenseInfo() {
        val info = LicenseInfo(
            firstLaunchDate = prefs.getLong(KEY_FIRST_LAUNCH_DATE, 0L),
            lastCheckDate = prefs.getLong(KEY_LAST_CHECK_DATE, 0L),
            trialDays = prefs.getInt(KEY_TRIAL_DAYS, DEFAULT_TRIAL_DAYS),
            isActivated = prefs.getBoolean(KEY_IS_ACTIVATED, false),
            activationCode = prefs.getString(KEY_ACTIVATION_CODE, null)
        )
        _licenseInfo.value = info
    }

    /**
     * 保存许可证信息
     */
    private fun saveLicenseInfo(info: LicenseInfo) {
        prefs.edit().apply {
            putLong(KEY_FIRST_LAUNCH_DATE, info.firstLaunchDate)
            putLong(KEY_LAST_CHECK_DATE, info.lastCheckDate)
            putInt(KEY_TRIAL_DAYS, info.trialDays)
            putBoolean(KEY_IS_ACTIVATED, info.isActivated)
            putString(KEY_ACTIVATION_CODE, info.activationCode)
            apply()
        }
        _licenseInfo.value = info
    }

    /**
     * 生成示例激活码（用于测试）
     */
    fun generateSampleActivationCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val sb = StringBuilder()
        repeat(12) {
            sb.append(chars.random())
        }
        val checksum = sb.toString().sumOf { it.code } % 65536
        sb.append(String.format("%04X", checksum))

        // 格式化为 XXXX-XXXX-XXXX-XXXX
        return sb.toString().chunked(4).joinToString("-")
    }
}

/**
 * 许可证验证回调
 */
interface LicenseCallback {
    fun onLicenseValid()
    fun onLicenseExpired()
    fun onFirstLaunch()
}
