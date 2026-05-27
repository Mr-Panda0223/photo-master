# 保持Compose相关
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }

# 保持Kotlin序列化
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }

# 保持数据类
-keepclassmembers class com.photomaster.data.model.** { *; }
-keepclassmembers class com.photomaster.security.** { *; }

# 移除日志
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
