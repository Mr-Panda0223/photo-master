package com.photomaster.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.photomaster.security.LicenseStatus
import com.photomaster.ui.screens.settings.SettingsScreen
import com.photomaster.ui.theme.*

@Composable
fun ProfileContent(
    onNavigateToEditor: (String, String?) -> Unit,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val remainingDays by viewModel.remainingDays.collectAsState()
    val licenseStatus by viewModel.licenseStatus.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()

    if (showSettings) {
        SettingsScreen(
            onBackClick = { viewModel.hideSettings() }
        )
    } else {
        Scaffold(
            topBar = {
                ProfileTopBar()
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BackgroundGray)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 用户信息卡片
                    UserInfoCard(
                        isLoggedIn = isLoggedIn,
                        onLoginToggle = { viewModel.toggleLogin() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 剩余使用时间卡片
                    RemainingTimeCard(
                        remainingDays = remainingDays,
                        licenseStatus = licenseStatus
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 快捷入口
                    SectionTitle(title = "快捷入口")
                    QuickAccessCard(
                        onSettingsClick = { viewModel.showSettings() },
                        onBatchProcessClick = { /* TODO: 批量处理 */ },
                        onFavoritesClick = { /* TODO: 收藏 */ },
                        onStatisticsClick = { /* TODO: 统计 */ }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 编辑记录
                    SectionTitle(title = "编辑记录")
                    SettingsCard {
                        MenuItem(
                            icon = Icons.Default.History,
                            title = "编辑历史",
                            subtitle = "查看所有编辑记录",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )

                        Divider(color = BorderGray, thickness = 0.5.dp)

                        MenuItem(
                            icon = Icons.Default.Folder,
                            title = "草稿箱",
                            subtitle = "管理未完成的编辑",
                            showArrow = true,
                            onClick = { /* TODO: 跳转到首页草稿标签 */ }
                        )

                        Divider(color = BorderGray, thickness = 0.5.dp)

                        MenuItem(
                            icon = Icons.Default.CheckCircle,
                            title = "已完成作品",
                            subtitle = "查看已保存的作品",
                            showArrow = true,
                            onClick = { /* TODO: 跳转到首页已完成标签 */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 系统信息
                    SectionTitle(title = "系统信息")
                    SettingsCard {
                        MenuItem(
                            icon = Icons.Default.PhoneAndroid,
                            title = "设备信息",
                            subtitle = "Android 14 • v1.0.0",
                            showArrow = false
                        )

                        Divider(color = BorderGray, thickness = 0.5.dp)

                        MenuItem(
                            icon = Icons.Default.Info,
                            title = "关于应用",
                            subtitle = "PhotoMaster 专业版 v1.0.0",
                            showArrow = true,
                            onClick = { /* TODO: 关于页面 */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 版本信息
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PhotoMaster 专业版",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "版本 1.0.0 (Build 20240115)",
                                fontSize = 12.sp,
                                color = TextHint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 额外空间确保可以滚动
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBar() {
    Surface(
        color = Primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "我的",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UserInfoCard(
    isLoggedIn: Boolean,
    onLoginToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isLoggedIn) Primary else DisabledGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLoggedIn) Icons.Default.Person else Icons.Default.Login,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) "用户_9527" else "未登录",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = if (isLoggedIn) "已登录" else "登录后可同步编辑记录",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // 登录/退出按钮
            Button(
                onClick = onLoginToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoggedIn) ErrorRed else Primary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = if (isLoggedIn) "退出" else "登录",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RemainingTimeCard(
    remainingDays: Int,
    licenseStatus: LicenseStatus
) {
    val isWarning = licenseStatus == LicenseStatus.WARNING || licenseStatus == LicenseStatus.EXPIRED
    val backgroundColor = if (isWarning) ErrorRed.copy(alpha = 0.1f) else SuccessGreen.copy(alpha = 0.1f)
    val borderColor = if (isWarning) ErrorRed.copy(alpha = 0.3f) else SuccessGreen.copy(alpha = 0.3f)
    val textColor = if (isWarning) ErrorRed else SuccessGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isWarning) ErrorRed.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "剩余使用时间",
                    fontSize = 14.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$remainingDays 天",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            if (isWarning) {
                Surface(
                    color = ErrorRed,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "即将到期",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    onSettingsClick: () -> Unit,
    onBatchProcessClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onStatisticsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            MenuItem(
                icon = Icons.Default.Settings,
                title = "设置",
                subtitle = "语言、主题、存储管理",
                showArrow = true,
                onClick = onSettingsClick
            )

            Divider(color = BorderGray, thickness = 0.5.dp)

            MenuItem(
                icon = Icons.Default.BatchPrediction,
                title = "批量处理",
                subtitle = "批量编辑多张图片",
                showArrow = true,
                onClick = onBatchProcessClick
            )

            Divider(color = BorderGray, thickness = 0.5.dp)

            MenuItem(
                icon = Icons.Default.Favorite,
                title = "我的收藏",
                subtitle = "查看收藏的作品",
                showArrow = true,
                onClick = onFavoritesClick
            )

            Divider(color = BorderGray, thickness = 0.5.dp)

            MenuItem(
                icon = Icons.Default.BarChart,
                title = "使用统计",
                subtitle = "编辑次数、使用时长",
                showArrow = true,
                onClick = onStatisticsClick
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Primary,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(content = content)
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        trailing?.invoke() ?: run {
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = DisabledGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
