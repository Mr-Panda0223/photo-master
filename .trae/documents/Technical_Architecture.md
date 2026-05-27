# PhotoMaster - 技术架构文档

## 1. 技术选型

### 1.1 开发平台

| 项目 | 选择 | 说明 |
|------|------|------|
| 开发语言 | Kotlin | Android官方推荐，语法简洁，空安全 |
| 最低SDK | API 26 (Android 8.0) | 覆盖主流设备，支持Scoped Storage |
| 目标SDK | API 34 (Android 14) | 使用最新API特性 |
| 构建工具 | Gradle 8.x | 标准构建工具 |

### 1.2 核心库依赖

| 功能领域 | 库名称 | 版本 | 说明 |
|---------|--------|------|------|
| UI框架 | Jetpack Compose | 1.6.x | 现代声明式UI框架 |
| 图片加载 | Coil | 2.5.x | Kotlin友好，支持Compose |
| 图像处理 | GPUImage | 2.1.x | OpenGL滤镜处理 |
| 图像处理 | Android Graphics | 内置 | Canvas、Bitmap处理 |
| 协程 | Kotlin Coroutines | 1.7.x | 异步处理 |
| 生命周期 | Jetpack Lifecycle | 2.7.x | 生命周期管理 |
| 导航 | Jetpack Navigation | 2.7.x | 页面导航 |
| 存储 | Jetpack DataStore | 1.0.x | 偏好设置存储 |
| 权限 | Accompanist Permissions | 0.34.x | 权限处理 |

### 1.3 图像处理方案

#### 核心处理引擎
- **OpenGL ES 3.0**：GPU加速滤镜和效果处理
- **Android Canvas**：2D绘制、文字、涂鸦
- **RenderScript**（备选）：复杂图像算法加速

#### 处理流程
```
原始图片 → 解码为Bitmap → GPU处理滤镜 → CPU处理绘制 → 编码保存
```

---

## 2. 项目结构

```
app/
├── src/main/
│   ├── java/com/photomaster/
│   │   ├── PhotoMasterApp.kt          # Application类
│   │   ├── MainActivity.kt            # 主Activity
│   │   ├── navigation/
│   │   │   └── AppNavigation.kt       # 导航配置
│   │   ├── ui/
│   │   │   ├── theme/                 # 主题配置
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Theme.kt
│   │   │   │   └── Type.kt
│   │   │   ├── components/            # 通用组件
│   │   │   │   ├── CommonButton.kt
│   │   │   │   ├── SliderWithLabel.kt
│   │   │   │   ├── FilterThumbnail.kt
│   │   │   │   └── ToolButton.kt
│   │   │   ├── screens/               # 页面
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   └── components/
│   │   │   │   ├── editor/
│   │   │   │   │   ├── EditorScreen.kt
│   │   │   │   │   ├── EditorViewModel.kt
│   │   │   │   │   └── components/
│   │   │   │   ├── export/
│   │   │   │   │   ├── ExportScreen.kt
│   │   │   │   │   └── ExportViewModel.kt
│   │   │   │   └── settings/
│   │   │   │       ├── SettingsScreen.kt
│   │   │   │       └── SettingsViewModel.kt
│   │   ├── data/
│   │   │   ├── repository/
│   │   │   │   ├── ImageRepository.kt      # 图片数据仓库
│   │   │   │   └── SettingsRepository.kt   # 设置数据仓库
│   │   │   ├── local/
│   │   │   │   ├── ImageDataSource.kt      # 本地图片数据源
│   │   │   │   └── PreferencesDataSource.kt # 偏好设置数据源
│   │   │   └── model/
│   │   │       ├── ImageModel.kt
│   │   │       ├── FilterModel.kt
│   │   │       ├── EditOperation.kt
│   │   │       └── AppSettings.kt
│   │   ├── domain/
│   │   │   ├── processor/
│   │   │   │   ├── ImageProcessor.kt       # 图片处理核心
│   │   │   │   ├── FilterProcessor.kt      # 滤镜处理
│   │   │   │   ├── AdjustProcessor.kt      # 调整处理
│   │   │   │   ├── CropProcessor.kt        # 裁剪处理
│   │   │   │   ├── LightShadowProcessor.kt # 光影处理
│   │   │   │   ├── TextProcessor.kt        # 文字处理
│   │   │   │   ├── BrushProcessor.kt       # 画笔处理
│   │   │   │   └── LocalEditProcessor.kt   # 局部编辑处理
│   │   │   ├── engine/
│   │   │   │   ├── GPURenderEngine.kt      # GPU渲染引擎
│   │   │   │   └── CanvasRenderEngine.kt   # Canvas渲染引擎
│   │   │   └── manager/
│   │   │       ├── EditHistoryManager.kt   # 编辑历史管理
│   │   │       └── LayerManager.kt         # 图层管理（简化版）
│   │   ├── utils/
│   │   │   ├── ImageUtils.kt
│   │   │   ├── PermissionUtils.kt
│   │   │   ├── FileUtils.kt
│   │   │   └── DateUtils.kt
│   │   └── security/
│   │       └── LicenseValidator.kt         # 有效期验证
│   ├── res/
│   │   ├── drawable/                  # 图标资源
│   │   ├── mipmap/                    # 应用图标
│   │   └── values/
│   │       ├── strings.xml
│   │       └── colors.xml
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

---

## 3. 核心模块设计

### 3.1 图片处理引擎

#### 架构图
```
┌─────────────────────────────────────────────────────────┐
│                    ImageProcessor                        │
│                   (处理协调器)                            │
└─────────────┬─────────────────────────────┬─────────────┘
              │                             │
    ┌─────────▼─────────┐       ┌───────────▼──────────┐
    │  GPURenderEngine  │       │  CanvasRenderEngine  │
    │   (GPU渲染引擎)    │       │   (Canvas渲染引擎)    │
    └─────────┬─────────┘       └───────────┬──────────┘
              │                             │
    ┌─────────▼─────────┐       ┌───────────▼──────────┐
    │   OpenGL ES 3.0   │       │     Android Canvas   │
    │   滤镜、调整、光影  │       │     文字、涂鸦、裁剪   │
    └───────────────────┘       └──────────────────────┘
```

#### 处理流程
```kotlin
// 图片处理流程
class ImageProcessor {
    
    fun processImage(
        source: Bitmap,
        operations: List<EditOperation>
    ): Bitmap {
        var currentBitmap = source
        
        operations.forEach { operation ->
            currentBitmap = when (operation.type) {
                FILTER -> gpuEngine.applyFilter(currentBitmap, operation.params)
                ADJUST -> gpuEngine.applyAdjust(currentBitmap, operation.params)
                CROP -> canvasEngine.applyCrop(currentBitmap, operation.params)
                TEXT -> canvasEngine.addText(currentBitmap, operation.params)
                BRUSH -> canvasEngine.applyBrush(currentBitmap, operation.params)
                LIGHT_SHADOW -> gpuEngine.applyLightShadow(currentBitmap, operation.params)
                LOCAL_EDIT -> canvasEngine.applyLocalEdit(currentBitmap, operation.params)
            }
        }
        
        return currentBitmap
    }
}
```

### 3.2 编辑历史管理

```kotlin
// 编辑操作数据类
data class EditOperation(
    val id: String = UUID.randomUUID().toString(),
    val type: OperationType,
    val params: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OperationType {
    FILTER, ADJUST, CROP, TEXT, BRUSH, LIGHT_SHADOW, LOCAL_EDIT
}

// 历史管理器
class EditHistoryManager {
    private val history = mutableListOf<EditOperation>()
    private var currentIndex = -1
    private val maxHistorySize = 30
    
    fun addOperation(operation: EditOperation) {
        // 删除当前位置之后的历史
        if (currentIndex < history.size - 1) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        
        history.add(operation)
        currentIndex++
        
        // 限制历史记录大小
        if (history.size > maxHistorySize) {
            history.removeAt(0)
            currentIndex--
        }
    }
    
    fun undo(): EditOperation? {
        if (currentIndex >= 0) {
            currentIndex--
            return history.getOrNull(currentIndex)
        }
        return null
    }
    
    fun redo(): EditOperation? {
        if (currentIndex < history.size - 1) {
            currentIndex++
            return history[currentIndex]
        }
        return null
    }
    
    fun canUndo(): Boolean = currentIndex >= 0
    fun canRedo(): Boolean = currentIndex < history.size - 1
    fun getCurrentOperations(): List<EditOperation> = history.take(currentIndex + 1)
}
```

### 3.3 滤镜系统

```kotlin
// 滤镜数据类
data class Filter(
    val id: String,
    val name: String,
    val thumbnailRes: Int,
    val shaderCode: String,  // GLSL着色器代码
    val parameters: List<FilterParameter> = emptyList()
)

data class FilterParameter(
    val name: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float
)

// 预设滤镜库
object FilterLibrary {
    val filters = listOf(
        Filter(
            id = "original",
            name = "原图",
            thumbnailRes = R.drawable.filter_original,
            shaderCode = "..."
        ),
        Filter(
            id = "portrait",
            name = "人像",
            thumbnailRes = R.drawable.filter_portrait,
            shaderCode = "..."
        ),
        Filter(
            id = "landscape",
            name = "风景",
            thumbnailRes = R.drawable.filter_landscape,
            shaderCode = "..."
        ),
        // ... 更多滤镜
    )
}
```

### 3.4 有效期验证

```kotlin
class LicenseValidator(private val context: Context) {
    
    companion object {
        private const val INSTALL_DATE_KEY = "install_date"
        private const val VALIDITY_DAYS = 7L
    }
    
    private val dataStore: DataStore<Preferences> = context.dataStore
    
    // 获取安装日期（首次启动时记录）
    suspend fun getInstallDate(): Long {
        return dataStore.data.map { preferences ->
            preferences[longPreferencesKey(INSTALL_DATE_KEY)] ?: run {
                val currentTime = System.currentTimeMillis()
                dataStore.edit { it[longPreferencesKey(INSTALL_DATE_KEY)] = currentTime }
                currentTime
            }
        }.first()
    }
    
    // 获取剩余天数
    suspend fun getRemainingDays(): Int {
        val installDate = getInstallDate()
        val expiryDate = installDate + TimeUnit.DAYS.toMillis(VALIDITY_DAYS)
        val remainingMillis = expiryDate - System.currentTimeMillis()
        return (TimeUnit.MILLISECONDS.toDays(remainingMillis)).toInt().coerceAtLeast(0)
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
}

enum class LicenseStatus {
    VALID,      // 正常
    WARNING,    // 即将过期（剩余3天内）
    EXPIRED     // 已过期
}
```

---

## 4. 数据流设计

### 4.1 图片编辑数据流

```
┌──────────┐    select    ┌──────────┐    load     ┌──────────┐
│   首页    │ ───────────> │  编辑页   │ ──────────> │  加载图片  │
│  (相册)   │              │          │             │          │
└──────────┘              └────┬─────┘             └────┬─────┘
                               │                        │
                               │                        ▼
                               │                 ┌──────────┐
                               │                 │ 显示原图  │
                               │                 └────┬─────┘
                               │                      │
                               │ apply operation      ▼
                               │ <────────────────  ┌──────────┐
                               │                    │ 用户操作  │
                               │                    │(调节参数) │
                               │                    └────┬─────┘
                               │                         │
                               │                         ▼
                               │                    ┌──────────┐
                               │                    │ 实时预览  │
                               │                    │ (GPU渲染) │
                               │                    └────┬─────┘
                               │                         │
                               │ save to history         ▼
                               │ <────────────────  ┌──────────┐
                               │                    │ 确认操作  │
                               │                    │ (添加到  │
                               │                    │  历史记录)│
                               │                    └──────────┘
                               │
              export           │
┌──────────┐ <───────────────┘
│  导出页   │
│ (保存/分享)│
└──────────┘
```

### 4.2 状态管理

```kotlin
// 编辑页面状态
data class EditorState(
    val originalImage: Bitmap? = null,
    val currentImage: Bitmap? = null,
    val isLoading: Boolean = false,
    val currentTool: ToolType = ToolType.NONE,
    val operationParams: Map<String, Float> = emptyMap(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val error: String? = null
)

// ViewModel
class EditorViewModel(
    private val imageProcessor: ImageProcessor,
    private val historyManager: EditHistoryManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    
    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val bitmap = loadBitmapFromUri(uri)
                _state.update {
                    it.copy(
                        originalImage = bitmap,
                        currentImage = bitmap,
                        isLoading = false
                    )
                }
                historyManager.clear()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun applyAdjustment(params: AdjustmentParams) {
        viewModelScope.launch(Dispatchers.Default) {
            val original = _state.value.originalImage ?: return@launch
            
            val operation = EditOperation(
                type = OperationType.ADJUST,
                params = mapOf(
                    "brightness" to params.brightness,
                    "contrast" to params.contrast,
                    // ...
                )
            )
            
            val processed = imageProcessor.processImage(
                original,
                historyManager.getCurrentOperations() + operation
            )
            
            historyManager.addOperation(operation)
            
            _state.update {
                it.copy(
                    currentImage = processed,
                    canUndo = historyManager.canUndo(),
                    canRedo = historyManager.canRedo()
                )
            }
        }
    }
    
    fun undo() {
        viewModelScope.launch(Dispatchers.Default) {
            historyManager.undo()
            reprocessImage()
        }
    }
    
    fun redo() {
        viewModelScope.launch(Dispatchers.Default) {
            historyManager.redo()
            reprocessImage()
        }
    }
    
    private suspend fun reprocessImage() {
        val original = _state.value.originalImage ?: return
        val operations = historyManager.getCurrentOperations()
        val processed = imageProcessor.processImage(original, operations)
        
        _state.update {
            it.copy(
                currentImage = processed,
                canUndo = historyManager.canUndo(),
                canRedo = historyManager.canRedo()
            )
        }
    }
}
```

---

## 5. 性能优化策略

### 5.1 图片加载优化

```kotlin
object ImageLoadConfig {
    // 内存缓存大小
    const val MEMORY_CACHE_SIZE_MB = 100
    
    // 磁盘缓存大小
    const val DISK_CACHE_SIZE_MB = 250
    
    // 最大加载图片尺寸
    const val MAX_BITMAP_DIMENSION = 4096
    
    // 缩略图尺寸
    const val THUMBNAIL_SIZE = 300
}

// 图片加载配置
fun Context.createImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(ImageLoadConfig.DISK_CACHE_SIZE_MB * 1024 * 1024)
                .build()
        }
        .components {
            add(SvgDecoder.Factory())
        }
        .crossfade(true)
        .build()
}
```

### 5.2 大图处理优化

```kotlin
class LargeImageProcessor {
    
    // 分块处理大图
    fun processLargeImage(
        source: Bitmap,
        operation: (Bitmap) -> Bitmap
    ): Bitmap {
        // 如果图片尺寸在限制内，直接处理
        if (source.width <= MAX_SIZE && source.height <= MAX_SIZE) {
            return operation(source)
        }
        
        // 大图分块处理
        return processInTiles(source, operation)
    }
    
    private fun processInTiles(
        source: Bitmap,
        operation: (Bitmap) -> Bitmap
    ): Bitmap {
        val tileSize = 1024
        val result = Bitmap.createBitmap(source.width, source.height, source.config)
        
        for (y in 0 until source.height step tileSize) {
            for (x in 0 until source.width step tileSize) {
                val width = min(tileSize, source.width - x)
                val height = min(tileSize, source.height - y)
                
                // 提取瓦片
                val tile = Bitmap.createBitmap(source, x, y, width, height)
                
                // 处理瓦片
                val processedTile = operation(tile)
                
                // 合并到结果
                Canvas(result).drawBitmap(processedTile, x.toFloat(), y.toFloat(), null)
                
                // 及时回收临时Bitmap
                tile.recycle()
                if (processedTile != tile) processedTile.recycle()
            }
        }
        
        return result
    }
    
    companion object {
        const val MAX_SIZE = 4096
    }
}
```

### 5.3 内存管理

```kotlin
class MemoryManager(private val context: Context) {
    
    // 监听内存压力
    fun registerMemoryCallback() {
        context.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                when (level) {
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                    ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                        // 严重内存不足，清理所有缓存
                        clearAllCaches()
                    }
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
                    ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                        // 内存较低，清理部分缓存
                        clearNonEssentialCaches()
                    }
                }
            }
            
            override fun onConfigurationChanged(newConfig: Configuration) {}
            override fun onLowMemory() {
                clearAllCaches()
            }
        })
    }
    
    private fun clearAllCaches() {
        // 清理图片缓存
        Coil.imageLoader(context).memoryCache?.clear()
        // 清理处理中的Bitmap
        System.gc()
    }
    
    private fun clearNonEssentialCaches() {
        // 只清理非必要的缓存
        Coil.imageLoader(context).memoryCache?.let { cache ->
            // 清理最旧的50%缓存
            val keys = cache.keys
            keys.take(keys.size / 2).forEach { cache.remove(it) }
        }
    }
}
```

---

## 6. 存储方案

### 6.1 图片存储

```kotlin
class ImageStorageManager(private val context: Context) {
    
    // 保存编辑后的图片
    suspend fun saveImage(
        bitmap: Bitmap,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 90,
        customName: String? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val fileName = customName ?: "IMG_${System.currentTimeMillis()}.${format.extension}"
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用MediaStore
                saveImageUsingMediaStore(bitmap, fileName, format, quality)
            } else {
                // Android 9及以下使用传统方式
                saveImageLegacy(bitmap, fileName, format, quality)
            }
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageUsingMediaStore(
        bitmap: Bitmap,
        fileName: String,
        format: CompressFormat,
        quality: Int
    ): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoMaster")
        }
        
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IOException("Failed to create media store entry")
        
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(format, quality, outputStream)
        } ?: throw IOException("Failed to open output stream")
        
        return uri
    }
    
    private fun saveImageLegacy(
        bitmap: Bitmap,
        fileName: String,
        format: CompressFormat,
        quality: Int
    ): Uri {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "PhotoMaster").apply { mkdirs() }
        val file = File(appDir, fileName)
        
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(format, quality, outputStream)
        }
        
        // 通知系统扫描新文件
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
        
        return Uri.fromFile(file)
    }
}

// 扩展属性
val CompressFormat.extension: String
    get() = when (this) {
        CompressFormat.JPEG -> "jpg"
        CompressFormat.PNG -> "png"
        else -> "jpg"
    }

val CompressFormat.mimeType: String
    get() = when (this) {
        CompressFormat.JPEG -> "image/jpeg"
        CompressFormat.PNG -> "image/png"
        else -> "image/jpeg"
    }
```

### 6.2 草稿自动保存

```kotlin
class DraftManager(private val context: Context) {
    
    private val draftDir = File(context.cacheDir, "drafts")
    
    init {
        draftDir.mkdirs()
    }
    
    // 自动保存草稿
    suspend fun autoSaveDraft(
        originalUri: Uri,
        operations: List<EditOperation>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val draft = Draft(
                originalUri = originalUri.toString(),
                operations = operations,
                saveTime = System.currentTimeMillis()
            )
            
            val draftFile = File(draftDir, "draft_${System.currentTimeMillis()}.json")
            draftFile.writeText(Json.encodeToString(draft))
            
            // 清理旧草稿（只保留最近5个）
            cleanOldDrafts()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 加载最近的草稿
    suspend fun loadLatestDraft(): Draft? = withContext(Dispatchers.IO) {
        draftDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.maxByOrNull { it.lastModified() }
            ?.let { file ->
                try {
                    Json.decodeFromString<Draft>(file.readText())
                } catch (e: Exception) {
                    null
                }
            }
    }
    
    private fun cleanOldDrafts() {
        val drafts = draftDir.listFiles()?.filter { it.extension == "json" }
        if ((drafts?.size ?: 0) > 5) {
            drafts?.sortedBy { it.lastModified() }
                ?.dropLast(5)
                ?.forEach { it.delete() }
        }
    }
}

@Serializable
data class Draft(
    val originalUri: String,
    val operations: List<EditOperation>,
    val saveTime: Long
)
```

---

## 7. 权限管理

```kotlin
class PermissionManager {
    
    companion object {
        val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        } else {
            // Android 9及以下
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        }
    }
}

// Compose权限请求
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionManager.REQUIRED_PERMISSIONS
    )
    
    when {
        permissionsState.allPermissionsGranted -> {
            onPermissionsGranted()
        }
        permissionsState.shouldShowRationale -> {
            // 显示权限说明
            PermissionRationaleDialog(
                onConfirm = { permissionsState.launchMultiplePermissionRequest() },
                onDismiss = { /* 退出应用 */ }
            )
        }
        else -> {
            // 首次请求或永久拒绝
            LaunchedEffect(Unit) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}
```

---

## 8. 构建配置

### 8.1 build.gradle.kts (Module: app)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.photomaster"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.photomaster"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")
    
    // GPU Image Processing
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

### 8.2 proguard-rules.pro

```proguard
# 保持Compose相关
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }

# 保持Kotlin序列化
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }

# 保持GPUImage
-keep class jp.co.cyberagent.android.gpuimage.** { *; }

# 保持数据类
-keepclassmembers class com.photomaster.data.model.** { *; }

# 移除日志
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
```

---

## 9. 项目里程碑与开发计划

### Phase 1: MVP (3周)

**Week 1: 基础架构**
- [ ] 项目初始化与依赖配置
- [ ] 基础UI框架搭建（Compose + Navigation）
- [ ] 权限管理系统
- [ ] 相册读取功能
- [ ] 有效期验证机制

**Week 2: 核心编辑功能**
- [ ] 图片加载与显示
- [ ] 基础调整功能（亮度、对比度、饱和度）
- [ ] 裁剪与旋转功能
- [ ] 滤镜系统基础框架
- [ ] 撤销/重做功能

**Week 3: 导出与优化**
- [ ] 图片导出功能
- [ ] 导出设置（格式、质量、尺寸）
- [ ] 设置页面
- [ ] 草稿自动保存
- [ ] 性能优化与Bug修复

### Phase 2: V1.0 (2周)

**Week 4: 增强功能**
- [ ] 更多调整参数（色温、色调、曝光、高光、阴影）
- [ ] 光影效果（暗角、光晕）
- [ ] 文字添加功能
- [ ] 贴纸系统

**Week 5: 画笔与完善**
- [ ] 画笔涂鸦功能
- [ ] 马赛克功能
- [ ] UI polish
- [ ] 完整测试与优化

### Phase 3: V1.5 (2周)

**Week 6-7: 高级功能**
- [ ] 局部编辑（局部调亮/调暗）
- [ ] 简单液化功能
- [ ] 修复工具
- [ ] 性能深度优化
- [ ] 平板适配

---

**文档版本**：v1.0  
**创建日期**：2026-05-11  
**最后更新**：2026-05-11  
**文档状态**：待评审
