package com.photomaster.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.photomaster.domain.manager.LicenseManager
import com.photomaster.domain.manager.LicenseStatus

/**
 * 首次启动对话框
 */
@Composable
fun FirstLaunchDialog(
    onDismiss: () -> Unit,
    onActivate: () -> Unit,
    onContinueTrial: () -> Unit
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
            Column {
                Text(
                    "感谢您选择 PhotoMaster！",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "您可以免费试用 7 天，体验所有高级功能。",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "如果您有激活码，可以立即激活获得永久使用权。",
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onActivate) {
                Text("立即激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onContinueTrial) {
                Text("开始试用")
            }
        }
    )
}

/**
 * 激活对话框
 */
@Composable
fun ActivationDialog(
    licenseManager: LicenseManager,
    onDismiss: () -> Unit,
    onActivated: () -> Unit
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
            Column {
                if (isSuccess) {
                    Text(
                        "激活成功！感谢您的购买。",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        "请输入您的激活码以解锁完整功能。",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = activationCode,
                        onValueChange = {
                            activationCode = it.uppercase()
                            errorMessage = null
                        },
                        label = { Text("激活码") },
                        placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "激活码格式：XXXX-XXXX-XXXX-XXXX",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(onClick = onActivated) {
                    Text("确定")
                }
            } else {
                Button(
                    onClick = {
                        if (licenseManager.activate(activationCode)) {
                            isSuccess = true
                            onActivated()
                        } else {
                            errorMessage = "激活码无效，请检查后重试"
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
 * 过期提示对话框
 */
@Composable
fun ExpiredDialog(
    onDismiss: () -> Unit,
    onActivate: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    "您的 7 天试用期已结束。",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "如需继续使用，请购买并激活完整版。",
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onActivate) {
                Text("立即激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("退出应用")
            }
        }
    )
}

/**
 * 试用状态提示条
 */
@Composable
fun TrialStatusBar(
    licenseManager: LicenseManager,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val licenseInfo by licenseManager.licenseInfo.collectAsState()
    val remainingDays = licenseInfo.getRemainingDays()
    val isActivated = licenseInfo.isActivated

    if (isActivated) return

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
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (remainingDays <= 1)
                        Icons.Default.Warning
                    else
                        Icons.Default.Info,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        remainingDays <= 0 -> "试用期已结束"
                        remainingDays == 1 -> "试用期还剩 1 天"
                        else -> "试用期还剩 $remainingDays 天"
                    },
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                onClick = onActivate,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = contentColor
                )
            ) {
                Text("激活", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 许可证信息卡片
 */
@Composable
fun LicenseInfoCard(
    licenseManager: LicenseManager,
    modifier: Modifier = Modifier
) {
    val licenseInfo by licenseManager.licenseInfo.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "许可证信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LicenseInfoRow(
                label = "状态",
                value = if (licenseInfo.isActivated) "已激活" else "试用中",
                isHighlighted = licenseInfo.isActivated
            )

            LicenseInfoRow(
                label = "首次使用",
                value = licenseManager.getFirstLaunchDateString()
            )

            if (!licenseInfo.isActivated) {
                LicenseInfoRow(
                    label = "到期日期",
                    value = licenseManager.getExpiryDateString()
                )

                LicenseInfoRow(
                    label = "剩余天数",
                    value = "${licenseManager.getRemainingDays()} 天"
                )

                LicenseInfoRow(
                    label = "已使用",
                    value = "${licenseManager.getUsedDays()} 天"
                )
            }

            if (licenseInfo.isActivated && licenseInfo.activationCode != null) {
                LicenseInfoRow(
                    label = "激活码",
                    value = licenseInfo.activationCode!!
                )
            }
        }
    }
}

/**
 * 许可证信息行
 */
@Composable
private fun LicenseInfoRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 检查并显示许可证对话框
 */
@Composable
fun LicenseCheck(
    licenseManager: LicenseManager,
    onLicenseValid: () -> Unit
) {
    val licenseStatus by licenseManager.licenseStatus.collectAsState()

    when (licenseStatus) {
        LicenseStatus.FIRST_LAUNCH -> {
            FirstLaunchDialog(
                onDismiss = { onLicenseValid() },
                onActivate = { /* 显示激活对话框 */ },
                onContinueTrial = { onLicenseValid() }
            )
        }
        LicenseStatus.EXPIRED -> {
            ExpiredDialog(
                onDismiss = { },
                onActivate = { /* 显示激活对话框 */ },
                onExit = { /* 退出应用 */ }
            )
        }
        else -> {
            onLicenseValid()
        }
    }
}
