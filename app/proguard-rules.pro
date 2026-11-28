# Add project specific ProGuard rules here.
-keep class com.material3tim.data.model.** { *; }

# Keep Compose runtime metadata for reflection-free runtime (safety).
-keepclassmembers class androidx.compose.runtime.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep navigation route classes (miuix nav saves them).
-keep class com.material3tim.ui.navigation3.** { *; }

# LSPosed 通过 META-INF/xposed/java_init.list 按名字加载入口类，必须保留
-keep class com.material3tim.xposed.** { *; }
-keep class io.github.libxposed.** { *; }
-dontwarn io.github.libxposed.**

# Lottie 依赖的 okio 引用了 javax.annotation.Nullable（仅编译期注解）
-dontwarn javax.annotation.**
