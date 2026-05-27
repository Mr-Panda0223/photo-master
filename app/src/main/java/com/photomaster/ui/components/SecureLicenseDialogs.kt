package com.photomaster.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.photomaster.security.SecureLicenseManager
import com.photomaster.security.SecureLicenseStatus

/**
 * 首次启动欢迎对话框
 */
@Composable
fun WelcomeDialog(
    licenseManager: SecureLicenseManager,
    onStartTrial: () -> Unit,
    onActivate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.EmojiEmotions,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("欢迎使用 PhotoMaster") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "PhotoMaster 是一款专业的照片编辑工具，为您提供：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeatureItem("✓ 强大的编辑功能")
                FeatureItem("✓ 丰富的滤镜效果")
                FeatureItem("✓ 智能美颜工具")
                FeatureItem("✓ 批量处理能力")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "您可以免费试用 ${licenseManager.getRemainingDays()} 天，",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "或立即激活获得永久使用权。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onActivate) {
                Text("立即激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onStartTrial) {
                Text("开始试用")
            }
        }
    )
}

/**
 * 许可证过期对话框
 */
@Composable
fun LicenseExpiredDialog(
    onActivate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = Icons.Default.TimerOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("试用期已结束") },
        text = {
            Column {
                Text(
                    "您的试用期已经结束，请激活应用以继续使用所有功能。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "激活后可获得：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FeatureItem("✓ 永久免费更新")
                FeatureItem("✓ 无限制使用所有功能")
                FeatureItem("✓ 专业技术支持")
            }
        },
        confirmButton = {
            Button(
                onClick = onActivate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("立即激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("退出应用")
            }
        }
    )
}

/**
 * 检测到篡改对话框
 */
@Composable
fun TamperDetectedDialog(
    onActivate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("安全警告") },
        text = {
            Column {
                Text(
                    "检测到应用数据可能被篡改或异常修改。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "为保护您的使用权益，试用期已被重置。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "请激活应用以继续使用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onActivate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("立即激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("退出")
            }
        }
    )
}

/**
 * 激活对话框
 */
@Composable
fun ActivationDialog(
    licenseManager: SecureLicenseManager,
    onActivate: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var activationCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("激活 PhotoMaster") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSuccess) {
                    SuccessContent()
                } else {
                    Text(
                        "请输入您的激活码以解锁完整功能",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = activationCode,
                        onValueChange = { 
                            activationCode = it.uppercase().take(19)
                            errorMessage = null
                        },
                        label = { Text("激活码") },
                        placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "格式: XXXX-XXXX-XXXX-XXXX",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(onClick = onDismiss) {
                    Text("开始使用")
                }
            } else {
                Button(
                    onClick = {
                        if (activationCode.length >= 16) {
                            val success = onActivate(activationCode)
                            if (success) {
                                isSuccess = true
                            } else {
                                errorMessage = "激活码无效，请检查后重试"
                            }
                        } else {
                            errorMessage = "请输入完整的激活码"
                        }
                    },
                    enabled = activationCode.isNotBlank()
                ) {
                    Text("激活")
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

/**
 * 试用状态提示条
 */
@Composable
fun TrialStatusBar(
    licenseManager: SecureLicenseManager,
    onActivate: () -> Unit
) {
    val remainingDays = licenseManager.getRemainingDays()
    val isExpired = licenseManager.isExpired()

    if (isExpired) return

    val backgroundColor = when {
        remainingDays <= 1 -> MaterialTheme.colorScheme.errorContainer
        remainingDays <= 3 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = when {
        remainingDays <= 1 -> MaterialTheme.colorScheme.onErrorContainer
        remainingDays <= 3 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (remainingDays <= 1) Icons.Default.Warning else Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        remainingDays == 0 -> "试用期今天结束"
                        remainingDays == 1 -> "试用期还剩 1 天"
                        else -> "试用期还剩 $remainingDays 天"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                onClick = onActivate,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = contentColor
                )
            ) {
                Text("立即激活")
            }
        }
    }
}

/**
 * 许可证信息卡片
 */
@Composable
fun LicenseInfoCard(
    licenseManager: SecureLicenseManager
) {
    val isActivated = licenseManager.isActivated()
    val remainingDays = licenseManager.getRemainingDays()
    val usedDays = licenseManager.getUsedDays()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActivated) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isActivated) Icons.Default.Verified else Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = if (isActivated) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isActivated) "已激活" else "试用中",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActivated) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isActivated) {
                Text(
                    "永久有效",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    "已使用 $usedDays 天，还剩 $remainingDays 天",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { usedDays.toFloat() / (usedDays + remainingDays).coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        remainingDays <= 1 -> MaterialTheme.colorScheme.error
                        remainingDays <= 3 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

/**
 * 功能项
 */
@Composable
private fun FeatureItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
    )
}

/**
 * 激活成功内容
 */
@Composable
private fun SuccessContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "激活成功！",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "感谢您激活 PhotoMaster，现在您可以无限制地使用所有功能。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 根据许可证状态显示对应对话框
 */
@Composable
fun SecureLicenseDialogHandler(
    licenseManager: SecureLicenseManager,
    onDismiss: () -> Unit = {}
) {
    val licenseStatus by licenseManager.licenseStatus.collectAsState()

    when (licenseStatus) {
        SecureLicenseStatus.FIRST_LAUNCH -> {
            WelcomeDialog(
                licenseManager = licenseManager,
                onStartTrial = onDismiss,
                onActivate = { /* 显示激活对话框 */ },
                onDismiss = onDismiss
            )
        }
        SecureLicenseStatus.EXPIRED -> {
            LicenseExpiredDialog(
                onActivate = { /* 显示激活对话框 */ },
                onDismiss = onDismiss
            )
        }
        SecureLicenseStatus.TAMPERED -> {
            TamperDetectedDialog(
                onActivate = { /* 显示激活对话框 */ },
                onDismiss = onDismiss
            )
        }
        else -> {
            // 其他状态不显示对话框
        }
    }
}
