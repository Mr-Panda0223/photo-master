package com.photomaster.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 安全许可证状态
 */
enum class SecureLicenseStatus {
    VALID,          // 有效
    EXPIRED,        // 已过期
    FIRST_LAUNCH,   // 首次启动
    TAMPERED,       // 检测到篡改
    SUSPICIOUS,     // 可疑行为
    UNKNOWN         // 未知
}

/**
 * 安全许可证信息
 */
data class SecureLicenseInfo(
    val firstLaunchDate: Long = 0L,
    val lastCheckDate: Long = 0L,
    val trialDays: Int = 7,
    val isActivated: Boolean = false,
    val activationCode: String? = null,
    val deviceFingerprint: String = "",
    val checksum: String = ""
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

    /**
     * 生成校验和
     */
    fun generateChecksum(): String {
        val data = "$firstLaunchDate|$lastCheckDate|$trialDays|$isActivated|$deviceFingerprint"
        val bytes = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
        return bytes.take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * 验证校验和
     */
    fun verifyChecksum(): Boolean {
        return checksum == generateChecksum()
    }
}

/**
 * 安全许可证管理器
 * 综合加密方案：多位置存储 + 设备指纹 + 时间防篡改 + 数据加密
 */
class SecureLicenseManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val encryption = LicenseEncryption(context)
    private val deviceFingerprint = DeviceFingerprintManager(context)

    private val _licenseStatus = MutableStateFlow(SecureLicenseStatus.UNKNOWN)
    val licenseStatus: StateFlow<SecureLicenseStatus> = _licenseStatus.asStateFlow()

    private val _licenseInfo = MutableStateFlow(SecureLicenseInfo())
    val licenseInfo: StateFlow<SecureLicenseInfo> = _licenseInfo.asStateFlow()

    companion object {
        private const val PREFS_NAME = "secure_license_prefs"
        private const val KEY_LICENSE_DATA = "license_data"
        private const val KEY_BACKUP_DATA = "backup_data"
        private const val DEFAULT_TRIAL_DAYS = 7
        private const val TAMPER_THRESHOLD_HOURS = 24

        // 外部存储文件名（混淆）
        private const val EXTERNAL_FILE_NAME = ".sys_config"
        private const val INTERNAL_FILE_NAME = ".app_data"
    }

    init {
        initialize()
    }

    /**
     * 初始化许可证
     */
    private fun initialize() {
        val info = loadSecureLicenseInfo()
        _licenseInfo.value = info
        _licenseStatus.value = checkLicenseStatusInternal(info)
    }

    /**
     * 安全加载许可证信息（多位置存储 + 验证）
     */
    private fun loadSecureLicenseInfo(): SecureLicenseInfo {
        // 从多个位置读取数据
        val fromPrefs = loadFromPrefs()
        val fromExternal = loadFromExternalFile()
        val fromInternal = loadFromInternalFile()

        // 分析数据一致性
        val validSources = listOfNotNull(
            fromPrefs?.takeIf { it.verifyChecksum() },
            fromExternal?.takeIf { it.verifyChecksum() },
            fromInternal?.takeIf { it.verifyChecksum() }
        )

        return when {
            // 所有来源一致，返回任意一个
            validSources.size >= 2 && validSources.all { it.firstLaunchDate == validSources[0].firstLaunchDate } -> {
                validSources[0]
            }
            // 只有一个有效来源，恢复其他位置
            validSources.size == 1 -> {
                restoreToAllLocations(validSources[0])
                validSources[0]
            }
            // 有数据但校验失败，可能篡改
            fromPrefs != null || fromExternal != null || fromInternal != null -> {
                createTamperedLicenseInfo()
            }
            // 真正首次启动
            else -> {
                createNewLicenseInfo()
            }
        }
    }

    /**
     * 从 SharedPreferences 加载
     */
    private fun loadFromPrefs(): SecureLicenseInfo? {
        return try {
            val encryptedData = prefs.getString(KEY_LICENSE_DATA, null)
            val json = encryptedData?.let { encryption.decrypt(it) }
            json?.let { parseLicenseInfo(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从外部存储加载
     */
    private fun loadFromExternalFile(): SecureLicenseInfo? {
        return try {
            val file = File(context.getExternalFilesDir(null), EXTERNAL_FILE_NAME)
            if (!file.exists()) return null

            val encryptedData = file.readText()
            val json = encryption.decrypt(encryptedData)
            json?.let { parseLicenseInfo(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从内部存储加载
     */
    private fun loadFromInternalFile(): SecureLicenseInfo? {
        return try {
            val file = File(context.filesDir, INTERNAL_FILE_NAME)
            if (!file.exists()) return null

            val encryptedData = file.readText()
            val json = encryption.decrypt(encryptedData)
            json?.let { parseLicenseInfo(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建新的许可证信息（首次启动）
     */
    private fun createNewLicenseInfo(): SecureLicenseInfo {
        val fingerprint = deviceFingerprint.generateFingerprint()
        val info = SecureLicenseInfo(
            firstLaunchDate = System.currentTimeMillis(),
            lastCheckDate = System.currentTimeMillis(),
            trialDays = DEFAULT_TRIAL_DAYS,
            deviceFingerprint = fingerprint,
            checksum = "" // 会在save时生成
        )
        val infoWithChecksum = info.copy(checksum = info.generateChecksum())
        saveToAllLocations(infoWithChecksum)
        return infoWithChecksum
    }

    /**
     * 创建篡改标记的许可证信息
     */
    private fun createTamperedLicenseInfo(): SecureLicenseInfo {
        val info = SecureLicenseInfo(
            firstLaunchDate = System.currentTimeMillis(),
            lastCheckDate = System.currentTimeMillis(),
            trialDays = 0, // 标记为已过期
            deviceFingerprint = deviceFingerprint.generateFingerprint()
        ).copy(checksum = "")
        val infoWithChecksum = info.copy(checksum = info.generateChecksum())
        saveToAllLocations(infoWithChecksum)
        return infoWithChecksum
    }

    /**
     * 保存到所有存储位置
     */
    private fun saveToAllLocations(info: SecureLicenseInfo) {
        val json = serializeLicenseInfo(info)
        val encrypted = encryption.encrypt(json)

        // 保存到 SharedPreferences
        prefs.edit { putString(KEY_LICENSE_DATA, encrypted) }

        // 保存到外部存储
        try {
            val externalFile = File(context.getExternalFilesDir(null), EXTERNAL_FILE_NAME)
            externalFile.writeText(encrypted)
        } catch (e: Exception) {
            // 外部存储可能不可用
        }

        // 保存到内部存储
        try {
            val internalFile = File(context.filesDir, INTERNAL_FILE_NAME)
            internalFile.writeText(encrypted)
        } catch (e: Exception) {
            // 内部存储应该总是可用
        }

        // 保存备份（用于恢复）
        prefs.edit { putString(KEY_BACKUP_DATA, encrypted) }
    }

    /**
     * 恢复到所有位置
     */
    private fun restoreToAllLocations(info: SecureLicenseInfo) {
        saveToAllLocations(info)
    }

    /**
     * 检查许可证状态
     */
    private fun checkLicenseStatusInternal(info: SecureLicenseInfo): SecureLicenseStatus {
        // 1. 检查设备指纹
        if (info.deviceFingerprint.isNotEmpty()) {
            val currentFingerprint = deviceFingerprint.generateFingerprint()
            if (info.deviceFingerprint != currentFingerprint) {
                return SecureLicenseStatus.SUSPICIOUS
            }
        }

        // 2. 检查时间篡改
        if (detectTimeTampering(info)) {
            return SecureLicenseStatus.TAMPERED
        }

        // 3. 检查校验和
        if (!info.verifyChecksum()) {
            return SecureLicenseStatus.TAMPERED
        }

        // 4. 确定状态
        return when {
            info.isActivated -> SecureLicenseStatus.VALID
            info.isExpired() -> SecureLicenseStatus.EXPIRED
            info.firstLaunchDate == 0L -> SecureLicenseStatus.FIRST_LAUNCH
            else -> SecureLicenseStatus.VALID
        }
    }

    /**
     * 检测时间篡改
     */
    private fun detectTimeTampering(info: SecureLicenseInfo): Boolean {
        val currentTime = System.currentTimeMillis()

        // 检查1: 当前时间是否比上次检查时间还早（用户修改了系统时间）
        if (info.lastCheckDate > currentTime) {
            return true
        }

        // 检查2: 检查网络时间（如果有网络）
        val networkTime = getNetworkTime()
        if (networkTime != null) {
            val timeDiff = kotlin.math.abs(currentTime - networkTime)
            if (timeDiff > TimeUnit.HOURS.toMillis(TAMPER_THRESHOLD_HOURS.toLong())) {
                return true
            }
        }

        // 更新最后检查时间
        if (info.lastCheckDate != currentTime) {
            val updatedInfo = info.copy(lastCheckDate = currentTime, checksum = "")
                .copy(checksum = info.copy(lastCheckDate = currentTime).generateChecksum())
            saveToAllLocations(updatedInfo)
            _licenseInfo.value = updatedInfo
        }

        return false
    }

    /**
     * 获取网络时间
     */
    private fun getNetworkTime(): Long? {
        return try {
            val urls = listOf(
                "https://www.google.com",
                "https://www.baidu.com",
                "https://www.microsoft.com"
            )

            for (urlString in urls) {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.instanceFollowRedirects = false
                    connection.connect()

                    val dateHeader = connection.getHeaderField("Date")
                    connection.disconnect()

                    if (dateHeader != null) {
                        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                        return sdf.parse(dateHeader)?.time
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 激活应用
     */
    fun activate(activationCode: String): Boolean {
        if (isValidActivationCode(activationCode)) {
            val currentInfo = _licenseInfo.value
            val updatedInfo = currentInfo.copy(
                isActivated = true,
                activationCode = activationCode,
                checksum = ""
            ).copy(checksum = currentInfo.copy(isActivated = true, activationCode = activationCode).generateChecksum())

            saveToAllLocations(updatedInfo)
            _licenseInfo.value = updatedInfo
            _licenseStatus.value = SecureLicenseStatus.VALID
            return true
        }
        return false
    }

    /**
     * 验证激活码
     */
    private fun isValidActivationCode(code: String): Boolean {
        val cleaned = code.replace("-", "").uppercase()

        if (cleaned.length != 16) return false
        if (!cleaned.all { it.isLetterOrDigit() }) return false

        val checksum = cleaned.take(12).sumOf { it.code }
        val expectedChecksum = cleaned.takeLast(4).toIntOrNull(16) ?: 0

        return checksum % 65536 == expectedChecksum
    }

    /**
     * 生成示例激活码
     */
    fun generateSampleActivationCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val sb = StringBuilder()
        repeat(12) {
            sb.append(chars.random())
        }
        val checksum = sb.toString().sumOf { it.code } % 65536
        sb.append(String.format("%04X", checksum))
        return sb.toString().chunked(4).joinToString("-")
    }

    /**
     * 序列化许可证信息
     */
    private fun serializeLicenseInfo(info: SecureLicenseInfo): String {
        return buildString {
            append("${info.firstLaunchDate},")
            append("${info.lastCheckDate},")
            append("${info.trialDays},")
            append("${info.isActivated},")
            append("${info.activationCode ?: ""},")
            append("${info.deviceFingerprint},")
            append(info.checksum)
        }
    }

    /**
     * 解析许可证信息
     */
    private fun parseLicenseInfo(json: String): SecureLicenseInfo? {
        return try {
            val parts = json.split(",")
            if (parts.size >= 7) {
                SecureLicenseInfo(
                    firstLaunchDate = parts[0].toLongOrNull() ?: 0L,
                    lastCheckDate = parts[1].toLongOrNull() ?: 0L,
                    trialDays = parts[2].toIntOrNull() ?: 7,
                    isActivated = parts[3].toBoolean(),
                    activationCode = parts[4].takeIf { it.isNotEmpty() },
                    deviceFingerprint = parts[5],
                    checksum = parts[6]
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 字符串哈希
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // 公共API
    fun getRemainingDays(): Int = _licenseInfo.value.getRemainingDays()
    fun getUsedDays(): Int = _licenseInfo.value.getUsedDays()
    fun isActivated(): Boolean = _licenseInfo.value.isActivated
    fun isExpired(): Boolean = _licenseInfo.value.isExpired()
    fun getExpiryDateString(): String {
        val info = _licenseInfo.value
        return if (info.isActivated) {
            "永久有效"
        } else {
            val expiryDate = info.firstLaunchDate + TimeUnit.DAYS.toMillis(info.trialDays.toLong())
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(expiryDate))
        }
    }
}

/**
 * 设备指纹管理器
 */
class DeviceFingerprintManager(private val context: Context) {

    /**
     * 生成设备指纹
     */
    fun generateFingerprint(): String {
        val components = listOf(
            Build.BOARD,
            Build.BRAND,
            Build.DEVICE,
            Build.HARDWARE,
            Build.ID,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.PRODUCT,
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        )

        val combined = components.filterNot { it.isNullOrBlank() }.joinToString("_")
        return hashString(combined)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(16).joinToString("") { "%02x".format(it) }
    }
}

/**
 * 许可证加密器
 */
class LicenseEncryption(private val context: Context) {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
    private val masterKeyAlias = "license_master_key"

    init {
        keyStore.load(null)
        if (!keyStore.containsAlias(masterKeyAlias)) {
            generateMasterKey()
        }
    }

    /**
     * 生成主密钥
     */
    private fun generateMasterKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            masterKeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    /**
     * 获取主密钥
     */
    private fun getMasterKey(): SecretKey {
        return (keyStore.getEntry(masterKeyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * 加密数据
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())

        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 组合 IV + 加密数据 + 额外混淆
        val combined = iv + encrypted
        return obfuscate(combined)
    }

    /**
     * 解密数据
     */
    fun decrypt(ciphertext: String): String? {
        return try {
            val combined = deobfuscate(ciphertext)

            // 提取 IV (12 bytes for GCM)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), GCMParameterSpec(128, iv))

            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 混淆数据（简单的字节重排）
     */
    private fun obfuscate(data: ByteArray): String {
        // 添加随机前缀和后缀
        val prefix = ByteArray(8) { (it * 7 % 256).toByte() }
        val suffix = ByteArray(8) { (it * 13 % 256).toByte() }

        val obfuscated = prefix + data + suffix

        // 简单的字节替换
        val substituted = obfuscated.mapIndexed { index, byte ->
            (byte.toInt() xor (index % 256) xor 0x5A).toByte()
        }.toByteArray()

        return android.util.Base64.encodeToString(substituted, android.util.Base64.NO_WRAP)
    }

    /**
     * 反混淆数据
     */
    private fun deobfuscate(data: String): ByteArray {
        val substituted = android.util.Base64.decode(data, android.util.Base64.NO_WRAP)

        // 反向字节替换
        val deobfuscated = substituted.mapIndexed { index, byte ->
            (byte.toInt() xor (index % 256) xor 0x5A).toByte()
        }.toByteArray()

        // 去除前缀和后缀
        return deobfuscated.copyOfRange(8, deobfuscated.size - 8)
    }
}
