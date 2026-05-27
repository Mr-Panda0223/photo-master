package com.photomaster.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.photomaster.domain.manager.*
import com.photomaster.security.SecureLicenseManager
import com.photomaster.ui.components.FlowRow
import com.photomaster.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val licenseManager = remember { SecureLicenseManager(context) }
    val themeManager = remember { ThemeManager(context) }
    val languageManager = remember { LanguageManager(context) }
    val storageManager = remember { StorageManager(context) }

    val licenseInfo by licenseManager.licenseInfo.collectAsState()
    val themeMode by themeManager.themeMode.collectAsState()
    val appTheme by themeManager.appTheme.collectAsState()
    val currentLanguage by languageManager.currentLanguage.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAutoSaveDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    var exportFormat by remember { mutableStateOf("JPEG") }
    var exportQuality by remember { mutableIntStateOf(90) }
    var autoSaveInterval by remember { mutableIntStateOf(30) }
    var storageInfo by remember { mutableStateOf<StorageInfo?>(null) }
    var isLoadingStorage by remember { mutableStateOf(false) }

    val remainingDays = licenseInfo.getRemainingDays()
    val isActivated = licenseInfo.isActivated

    // 加载存储信息
    LaunchedEffect(Unit) {
        isLoadingStorage = true
        storageInfo = storageManager.getStorageInfo()
        isLoadingStorage = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource("settings_title"),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource("back")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 许可证状态卡片
            LicenseStatusCard(
                remainingDays = remainingDays,
                isActivated = isActivated,
                expiryDate = licenseManager.getExpiryDateString(),
                onActivateClick = { showLicenseDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 外观设置
            SectionTitle(stringResource("preferences"))
            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource("language"),
                value = currentLanguage.displayName,
                onClick = { showLanguageDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource("theme"),
                value = themeManager.getThemeModeDisplayName(themeMode),
                onClick = { showThemeDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.ColorLens,
                title = "主题颜色",
                value = themeManager.getThemeDisplayName(appTheme),
                onClick = { showThemeDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 导出设置
            SectionTitle("导出设置")
            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.Image,
                title = "默认导出格式",
                value = exportFormat,
                onClick = { showExportFormatDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.HighQuality,
                title = "默认导出质量",
                value = "$exportQuality%",
                onClick = { showQualityDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Timer,
                title = "自动保存间隔",
                value = "$autoSaveInterval 秒",
                onClick = { showAutoSaveDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 存储管理
            SectionTitle("存储管理")
            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.CleaningServices,
                title = "清理缓存",
                value = if (isLoadingStorage) "计算中..." else storageInfo?.getAppCacheSizeMB() ?: "0 MB",
                onClick = { showClearCacheDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Folder,
                title = "草稿管理",
                value = if (isLoadingStorage) "计算中..." else storageInfo?.getDraftsSizeMB() ?: "0 MB",
                onClick = { /* 跳转到首页草稿标签 */ }
            )

            SettingsItem(
                icon = Icons.Default.Storage,
                title = "存储空间统计",
                value = if (isLoadingStorage) "计算中..." else storageInfo?.getAppTotalSizeMB() ?: "0 MB",
                onClick = { showStorageDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 关于
            SectionTitle(stringResource("about"))
            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource("version"),
                value = "v1.0.0",
                onClick = { },
                showArrow = false
            )

            SettingsItem(
                icon = Icons.Default.Help,
                title = "使用帮助",
                value = "",
                onClick = { showHelpDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Feedback,
                title = stringResource("feedback"),
                value = "",
                onClick = { showFeedbackDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Star,
                title = stringResource("rate_app"),
                value = "",
                onClick = {
                    // 打开应用商店
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                        context.startActivity(intent)
                    }
                }
            )

            SettingsItem(
                icon = Icons.Default.Share,
                title = stringResource("share_app"),
                value = "",
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "推荐你使用 PhotoMaster 专业图片编辑器！\nhttps://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "分享应用"))
                }
            )

            SettingsItem(
                icon = Icons.Default.Gavel,
                title = stringResource("privacy_policy"),
                value = "",
                onClick = { showPrivacyDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Description,
                title = stringResource("terms_of_service"),
                value = "",
                onClick = { showTermsDialog = true }
            )
        }
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            languages = languageManager.getSupportedLanguages(),
            onLanguageSelected = { language ->
                languageManager.setLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = themeMode,
            currentAppTheme = appTheme,
            themeModes = themeManager.getAllThemeModes(),
            appThemes = themeManager.getAllThemes(),
            onThemeModeSelected = { mode ->
                themeManager.setThemeMode(mode)
            },
            onAppThemeSelected = { theme ->
                themeManager.setAppTheme(theme)
            },
            onDismiss = { showThemeDialog = false },
            themeManager = themeManager
        )
    }

    // 许可证对话框
    if (showLicenseDialog) {
        LicenseDialog(
            licenseManager = licenseManager,
            onDismiss = { showLicenseDialog = false }
        )
    }

    // 导出格式对话框
    if (showExportFormatDialog) {
        ExportFormatDialog(
            currentFormat = exportFormat,
            onFormatSelected = { format ->
                exportFormat = format
                showExportFormatDialog = false
            },
            onDismiss = { showExportFormatDialog = false }
        )
    }

    // 质量选择对话框
    if (showQualityDialog) {
        QualitySelectionDialog(
            currentQuality = exportQuality,
            onQualitySelected = { quality ->
                exportQuality = quality
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    // 自动保存间隔对话框
    if (showAutoSaveDialog) {
        AutoSaveIntervalDialog(
            currentInterval = autoSaveInterval,
            onIntervalSelected = { interval ->
                autoSaveInterval = interval
                showAutoSaveDialog = false
            },
            onDismiss = { showAutoSaveDialog = false }
        )
    }

    // 存储空间统计对话框
    if (showStorageDialog && storageInfo != null) {
        StorageInfoDialog(
            storageInfo = storageInfo!!,
            onDismiss = { showStorageDialog = false }
        )
    }

    // 清理缓存确认对话框
    if (showClearCacheDialog) {
        ClearCacheDialog(
            cacheSize = storageInfo?.getAppCacheSizeMB() ?: "0 MB",
            onConfirm = {
                scope.launch {
                    storageManager.clearCache()
                    storageInfo = storageManager.getStorageInfo()
                    showClearCacheDialog = false
                }
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    // 使用帮助对话框
    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    // 反馈对话框
    if (showFeedbackDialog) {
        FeedbackDialog(
            onSubmit = { showFeedbackDialog = false },
            onDismiss = { showFeedbackDialog = false }
        )
    }

    // 隐私政策对话框
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    // 服务条款对话框
    if (showTermsDialog) {
        TermsOfServiceDialog(onDismiss = { showTermsDialog = false })
    }
}

@Composable
fun LicenseStatusCard(
    remainingDays: Int,
    isActivated: Boolean,
    expiryDate: String,
    onActivateClick: () -> Unit
) {
    val backgroundColor = when {
        isActivated -> Color(0xFFE8F5E9)
        remainingDays <= 0 -> ErrorRed.copy(alpha = 0.1f)
        remainingDays <= 3 -> WarningYellow.copy(alpha = 0.1f)
        else -> PrimaryLight
    }

    val textColor = when {
        isActivated -> Color(0xFF2E7D32)
        remainingDays <= 0 -> ErrorRed
        remainingDays <= 3 -> WarningYellow
        else -> Primary
    }

    val statusText = when {
        isActivated -> "已激活"
        remainingDays <= 0 -> "已过期"
        else -> "试用期"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isActivated) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource("license"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isActivated) {
                Text(
                    text = "永久有效",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = if (remainingDays > 0) "剩余 $remainingDays 天" else "试用期已结束",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "到期日期: $expiryDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.8f)
                )
            }

            if (!isActivated) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onActivateClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (remainingDays <= 0) ErrorRed else Primary
                    )
                ) {
                    Text(if (remainingDays <= 0) "立即激活" else "激活完整版")
                }
            }
        }
    }
}

@Composable
fun StorageInfoDialog(
    storageInfo: StorageInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("存储空间统计") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 设备存储
                Text(
                    text = "设备存储",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                StorageBar(
                    label = "总空间",
                    value = storageInfo.getTotalSpaceGB()
                )
                StorageBar(
                    label = "已用空间",
                    value = storageInfo.getUsedSpaceGB()
                )
                StorageBar(
                    label = "可用空间",
                    value = storageInfo.getAvailableSpaceGB()
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 应用存储
                Text(
                    text = "应用存储",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                StorageBar(
                    label = "缓存",
                    value = storageInfo.getAppCacheSizeMB()
                )
                StorageBar(
                    label = "草稿",
                    value = storageInfo.getDraftsSizeMB()
                )
                StorageBar(
                    label = "已完成作品",
                    value = storageInfo.getCompletedSizeMB()
                )
                StorageBar(
                    label = "回收站",
                    value = storageInfo.getDeletedSizeMB()
                )
                StorageBar(
                    label = "应用总计",
                    value = storageInfo.getAppTotalSizeMB(),
                    isHighlight = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("done"))
            }
        }
    )
}

@Composable
private fun StorageBar(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Medium else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) Primary else TextPrimary
        )
    }
}

@Composable
fun ClearCacheDialog(
    cacheSize: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("清理缓存") },
        text = {
            Text("确定要清理应用缓存吗？这将释放 $cacheSize 的存储空间。清理后不会影响您的草稿和已完成作品。")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("清理")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("使用帮助") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                HelpSection(
                    title = "基础编辑",
                    content = "选择照片后，您可以使用裁剪、旋转、翻转等基础功能调整图片构图。"
                )
                HelpSection(
                    title = "色彩调整",
                    content = "通过亮度、对比度、饱和度、色温等参数，精确控制图片色彩表现。"
                )
                HelpSection(
                    title = "美颜功能",
                    content = "提供磨皮、美白、瘦脸、大眼、瘦鼻等美颜效果，让人像更加出色。"
                )
                HelpSection(
                    title = "滤镜效果",
                    content = "内置多种风格滤镜，一键打造专业级视觉效果。"
                )
                HelpSection(
                    title = "图层管理",
                    content = "支持多图层编辑，可调整图层顺序、透明度和混合模式。"
                )
                HelpSection(
                    title = "导出分享",
                    content = "编辑完成后，可选择格式和质量导出，并直接分享到社交平台。"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("done"))
            }
        }
    )
}

@Composable
private fun HelpSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun FeedbackDialog(
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }
    var feedbackType by remember { mutableStateOf("建议") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("意见反馈") },
        text = {
            Column {
                Text(
                    text = "反馈类型",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("建议", "问题", "其他").forEach { type ->
                        FilterChip(
                            selected = feedbackType == type,
                            onClick = { feedbackType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    label = { Text("请输入您的反馈") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = feedbackText.isNotBlank()
            ) {
                Text("提交")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐私政策") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = """
                        隐私政策

                        1. 信息收集
                        我们仅收集应用正常运行所必需的信息，包括：
                        - 照片访问权限（用于编辑功能）
                        - 存储权限（用于保存编辑后的图片）
                        - 相机权限（用于拍照功能）

                        2. 信息使用
                        - 您的照片仅在本地处理，不会上传到服务器
                        - 我们不会收集或分享您的个人信息
                        - 编辑历史仅保存在您的设备上

                        3. 数据安全
                        - 所有数据处理都在本地完成
                        - 我们不会将您的数据分享给第三方

                        4. 权限说明
                        - 相册权限：用于选择要编辑的图片
                        - 相机权限：用于拍摄新照片
                        - 存储权限：用于保存编辑后的图片

                        5. 联系我们
                        如有任何隐私相关问题，请通过应用内的反馈功能联系我们。
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("done"))
            }
        }
    )
}

@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务条款") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = """
                        服务条款

                        1. 接受条款
                        使用本应用即表示您同意本服务条款的所有内容。

                        2. 使用许可
                        - 本应用提供7天免费试用期
                        - 试用期结束后需要激活才能继续使用
                        - 激活码仅限个人使用，不得转让或分享

                        3. 用户责任
                        - 您应对使用本应用产生的所有内容负责
                        - 不得使用本应用进行违法活动
                        - 不得尝试破解或逆向工程本应用

                        4. 免责声明
                        - 我们尽力确保应用的稳定性和安全性
                        - 但对于因使用本应用造成的任何损失不承担责任
                        - 建议定期备份重要数据

                        5. 条款修改
                        我们保留随时修改服务条款的权利，修改后的条款将在应用内公布。

                        6. 终止服务
                        如违反本条款，我们有权终止您的使用权限。
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("done"))
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    languages: List<AppLanguage>,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("language")) },
        text = {
            Column {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = language.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentThemeMode: ThemeMode,
    currentAppTheme: AppTheme,
    themeModes: List<ThemeMode>,
    appThemes: List<AppTheme>,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAppThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
    themeManager: ThemeManager
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("theme")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "主题模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                themeModes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeModeSelected(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentThemeMode,
                            onClick = { onThemeModeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = themeManager.getThemeModeDisplayName(mode),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "主题颜色",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    appThemes.forEach { theme ->
                        ThemeColorItem(
                            theme = theme,
                            isSelected = theme == currentAppTheme,
                            onClick = { onAppThemeSelected(theme) },
                            themeManager = themeManager
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("done"))
            }
        }
    )
}

@Composable
fun ThemeColorItem(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    themeManager: ThemeManager
) {
    val themeColor = when (theme) {
        AppTheme.DEFAULT -> Primary
        AppTheme.BLUE -> Color(0xFF1976D2)
        AppTheme.GREEN -> Color(0xFF388E3C)
        AppTheme.PURPLE -> Color(0xFF7B1FA2)
        AppTheme.ORANGE -> Color(0xFFF57C00)
        AppTheme.PINK -> Color(0xFFC2185B)
        AppTheme.RED -> Color(0xFFD32F2F)
        AppTheme.TEAL -> Color(0xFF00796B)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(themeColor)
                .then(
                    if (isSelected) {
                        Modifier.padding(2.dp)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = themeManager.getThemeDisplayName(theme),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun LicenseDialog(
    licenseManager: SecureLicenseManager,
    onDismiss: () -> Unit
) {
    var activationCode by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("activate_now")) },
        text = {
            Column {
                Text(
                    text = "请输入您的激活码以解锁完整功能",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = activationCode,
                    onValueChange = {
                        activationCode = it.uppercase()
                        showError = false
                    },
                    label = { Text(stringResource("enter_activation_code")) },
                    placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("激活码无效，请检查后重试") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "激活成功！感谢您的购买。",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (licenseManager.activate(activationCode)) {
                        showSuccess = true
                        showError = false
                    } else {
                        showError = true
                        showSuccess = false
                    }
                },
                enabled = activationCode.length >= 16
            ) {
                Text(stringResource("confirm"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Composable
fun ExportFormatDialog(
    currentFormat: String,
    onFormatSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val formats = listOf("JPEG", "PNG", "WebP")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导出格式") },
        text = {
            Column {
                formats.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFormatSelected(format) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = format == currentFormat,
                            onClick = { onFormatSelected(format) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = format,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Composable
fun QualitySelectionDialog(
    currentQuality: Int,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentQuality.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出质量") },
        text = {
            Column {
                Text(
                    text = "${sliderValue.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 50f..100f,
                    steps = 9
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("低质量", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("高质量", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onQualitySelected(sliderValue.toInt()) }) {
                Text(stringResource("confirm"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Composable
fun AutoSaveIntervalDialog(
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val intervals = listOf(0, 10, 30, 60, 120, 300)
    val intervalLabels = mapOf(
        0 to "关闭",
        10 to "10秒",
        30 to "30秒",
        60 to "1分钟",
        120 to "2分钟",
        300 to "5分钟"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动保存间隔") },
        text = {
            Column {
                intervals.forEach { interval ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntervalSelected(interval) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = interval == currentInterval,
                            onClick = { onIntervalSelected(interval) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = intervalLabels[interval] ?: "$interval 秒",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = DisabledGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
