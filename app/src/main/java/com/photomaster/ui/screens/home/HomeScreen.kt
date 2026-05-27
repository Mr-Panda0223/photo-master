package com.photomaster.ui.screens.home

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.photomaster.data.model.DeletedItemModel
import com.photomaster.data.model.DeletedItemType
import com.photomaster.data.model.DraftModel
import com.photomaster.data.model.EditedImageModel
import com.photomaster.data.model.ImageModel
import com.photomaster.ui.theme.Primary
import com.photomaster.ui.theme.BackgroundGray
import com.photomaster.ui.theme.TextPrimary
import com.photomaster.ui.theme.TextSecondary
import com.photomaster.utils.PermissionUtils

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (String, String?) -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val completedImages by viewModel.completedImages.collectAsState()
    val deletedItems by viewModel.deletedItems.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 权限请求
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionUtils.getRequiredPermissions()
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // 当权限被授予后加载数据
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.loadData()
        }
    }

    // 控制底部弹窗显示
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // 图片选择器回调（相册）
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onNavigateToEditor(it.toString(), null) // 新建编辑，不传 draftId
        }
    }

    // 相机拍照回调
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempPhotoUri?.let { uri ->
                onNavigateToEditor(uri.toString(), null) // 新建编辑，不传 draftId
            }
        }
        tempPhotoUri = null
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                onNewEdit = {
                    // 显示底部弹窗选择拍照或相册
                    showImageSourceDialog = true
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundGray)
        ) {
            // Tab 切换器
            TabSelector(
                selectedTab = selectedTab,
                draftsCount = drafts.size,
                completedCount = completedImages.size,
                deletedCount = deletedItems.size,
                onTabSelected = viewModel::selectTab
            )

            // 内容区域
            when (selectedTab) {
                HomeTab.DRAFTS -> {
                    DraftsContent(
                        drafts = drafts,
                        isLoading = isLoading,
                        onDraftClick = { draft ->
                            // 打开草稿图片进行继续编辑，传递 draftId
                            val draftImageUri = viewModel.getDraftImageUri(draft.id)
                            onNavigateToEditor(
                                draftImageUri?.toString() ?: draft.originalImageUri.toString(),
                                draft.id
                            )
                        },
                        onDraftDelete = { draft ->
                            viewModel.deleteDraft(draft)
                        }
                    )
                }
                HomeTab.COMPLETED -> {
                    CompletedContent(
                        images = completedImages,
                        isLoading = isLoading,
                        onImageClick = { image ->
                            onNavigateToEditor(image.uri.toString(), null)
                        },
                        onImageDelete = { image ->
                            viewModel.deleteCompletedImage(image)
                        }
                    )
                }
                HomeTab.DELETED -> {
                    DeletedContent(
                        items = deletedItems,
                        isLoading = isLoading,
                        onItemDelete = { item ->
                            viewModel.permanentlyDeleteItem(item.id)
                        },
                        onClearAll = {
                            viewModel.clearAllDeletedItems()
                        }
                    )
                }
            }
        }
    }

    // 底部弹窗：选择图片来源
    if (showImageSourceDialog) {
        ImageSourceBottomSheet(
            onDismiss = { showImageSourceDialog = false },
            onCameraClick = {
                showImageSourceDialog = false
                // 创建临时文件URI并打开相机
                val photoUri = viewModel.createTempImageUri(context)
                photoUri?.let {
                    tempPhotoUri = it
                    takePictureLauncher.launch(it)
                }
            },
            onGalleryClick = {
                showImageSourceDialog = false
                // 打开相册选择器
                pickImageLauncher.launch("image/*")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSourceBottomSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "选择图片来源",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 拍照选项
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCameraClick() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "拍照",
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "拍照",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Divider()

            // 相册选项
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGalleryClick() }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "从相册选择",
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "从相册选择",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeTopBar(
    onNewEdit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
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
                text = "PhotoPS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )

            // 新建按钮
            Button(
                onClick = onNewEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "新建",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TabSelector(
    selectedTab: HomeTab,
    draftsCount: Int,
    completedCount: Int,
    deletedCount: Int,
    onTabSelected: (HomeTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {
        TabButton(
            text = "正在编辑",
            count = draftsCount,
            isSelected = selectedTab == HomeTab.DRAFTS,
            onClick = { onTabSelected(HomeTab.DRAFTS) },
            modifier = Modifier.weight(1f)
        )
        TabButton(
            text = "已完成",
            count = completedCount,
            isSelected = selectedTab == HomeTab.COMPLETED,
            onClick = { onTabSelected(HomeTab.COMPLETED) },
            modifier = Modifier.weight(1f)
        )
        TabButton(
            text = "已删除",
            count = deletedCount,
            isSelected = selectedTab == HomeTab.DELETED,
            onClick = { onTabSelected(HomeTab.DELETED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isSelected) Primary else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextPrimary
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            else Primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftsContent(
    drafts: List<DraftModel>,
    isLoading: Boolean,
    onDraftClick: (DraftModel) -> Unit,
    onDraftDelete: (DraftModel) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    if (drafts.isEmpty()) {
        EmptyState(
            title = "暂无编辑中的照片",
            subtitle = "点击新建开始编辑照片"
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 只显示草稿
        items(drafts) { draft ->
            DraftGridItem(
                draft = draft,
                onClick = { onDraftClick(draft) },
                onDelete = { onDraftDelete(draft) }
            )
        }
    }
}

@Composable
private fun DraftGridItem(
    draft: DraftModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, Primary, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // 显示草稿缩略图
        AsyncImage(
            model = draft.thumbnailUri ?: draft.originalImageUri,
            contentDescription = draft.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 草稿标签
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .background(Primary.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "编辑中",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // 删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                .size(24.dp)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // 时间标签
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = formatTime(draft.lastEditTime),
                fontSize = 10.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        else -> {
            val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

@Composable
private fun CompletedContent(
    images: List<EditedImageModel>,
    isLoading: Boolean,
    onImageClick: (EditedImageModel) -> Unit,
    onImageDelete: (EditedImageModel) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    if (images.isEmpty()) {
        EmptyState(
            title = "暂无已完成照片",
            subtitle = "编辑并保存的照片会显示在这里"
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(images) { image ->
            CompletedImageGridItem(
                image = image,
                onClick = { onImageClick(image) },
                onDelete = { onImageDelete(image) }
            )
        }
    }
}

@Composable
private fun CompletedImageGridItem(
    image: EditedImageModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                .size(24.dp)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeletedContent(
    items: List<DeletedItemModel>,
    isLoading: Boolean,
    onItemDelete: (DeletedItemModel) -> Unit,
    onClearAll: () -> Unit
) {
    // 单个项目删除确认对话框
    var showDeleteConfirmDialog by remember { mutableStateOf<DeletedItemModel?>(null) }
    // 一键清空确认对话框
    var showClearAllDialog by remember { mutableStateOf(false) }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    if (items.isEmpty()) {
        EmptyState(
            title = "暂无已删除项目",
            subtitle = "删除的编辑中或已完成项目会显示在这里"
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 一键清空按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = { showClearAllDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("一键清空 (${items.size}个项目)")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            items(items) { item ->
                DeletedItemGridItem(
                    item = item,
                    onDelete = { showDeleteConfirmDialog = item }
                )
            }
        }
    }

    // 单个项目彻底删除确认对话框
    showDeleteConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("彻底删除") },
            text = { 
                Text("确定要彻底删除这个项目吗？此操作不可恢复，删除后将无法找回。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = null
                        onItemDelete(item)
                    }
                ) {
                    Text("彻底删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 一键清空确认对话框
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("一键清空") },
            text = { 
                Text("确定要清空所有已删除项目吗？共 ${items.size} 个项目将被永久删除，此操作不可恢复，删除后将无法找回。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllDialog = false
                        onClearAll()
                    }
                ) {
                    Text("确认清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DeletedItemGridItem(
    item: DeletedItemModel,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .alpha(0.7f)
    ) {
        // 显示缩略图
        AsyncImage(
            model = item.thumbnailUri ?: item.imageUri,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 类型标签（显示原本是编辑中还是已完成）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .background(
                    if (item.originalType == DeletedItemType.DRAFT) 
                        Color.Gray.copy(alpha = 0.8f) 
                    else 
                        Color(0xFF4CAF50).copy(alpha = 0.8f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (item.originalType == DeletedItemType.DRAFT) "编辑中" else "已完成",
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // 永久删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Color.Red.copy(alpha = 0.9f), CircleShape)
                .size(24.dp)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // 删除时间标签
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "删除于 ${formatTime(item.deletedTime)}",
                fontSize = 9.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
