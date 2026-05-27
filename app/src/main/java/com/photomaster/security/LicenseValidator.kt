package com.photomaster.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "license_prefs")

class LicenseValidator(private val context: Context) {
    
    companion object {
        private const val INSTALL_DATE_KEY = "install_date"
        private const val VALIDITY_DAYS = 7L
    }
    
    // 获取安装日期（首次启动时记录）
    suspend fun getInstallDate(): Long {
        return context.dataStore.data.map { preferences ->
            preferences[longPreferencesKey(INSTALL_DATE_KEY)] ?: run {
                val currentTime = System.currentTimeMillis()
                context.dataStore.edit { it[longPreferencesKey(INSTALL_DATE_KEY)] = currentTime }
                currentTime
            }
        }.first()
    }
    
    // 获取剩余天数
    suspend fun getRemainingDays(): Int {
        val installDate = getInstallDate()
        val expiryDate = installDate + TimeUnit.DAYS.toMillis(VALIDITY_DAYS)
        val remainingMillis = expiryDate - System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toDays(remainingMillis).toInt().coerceAtLeast(0)
    }
    
    // 检查是否过期
    suspend fun isExpired(): Boolean {
        return getRemainingDays() <= 0
    }
    
    // 获取有效期状态
    suspend fun getLicenseStatus(): LicenseStatus {
        val remainingDays = getRemainingDays()
        return when {
            remainingDays <= 0 -> LicenseStatus.EXPIRED
            remainingDays <= 3 -> LicenseStatus.WARNING
            else -> LicenseStatus.VALID
        }
    }
    
    // 重置安装日期（用于测试）
    suspend fun resetInstallDate() {
        context.dataStore.edit { 
            it[longPreferencesKey(INSTALL_DATE_KEY)] = System.currentTimeMillis() 
        }
    }
}

enum class LicenseStatus {
    VALID,      // 正常
    WARNING,    // 即将过期（剩余3天内）
    EXPIRED     // 已过期
}
