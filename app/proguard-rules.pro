# ProGuard / R8 rules for Focentra

# Preserve Application & Core Components
-keep class com.niloy.focentra.FocentraApp { *; }
-keep class com.niloy.focentra.MainActivity { *; }

# Preserve Room entities, DAOs and Database
-keep class com.niloy.focentra.data.local.entity.** { *; }
-keep class com.niloy.focentra.data.local.dao.** { *; }
-keep class com.niloy.focentra.data.local.AppDatabase { *; }

# Preserve Content Provider Contract & Integration
-keep class com.niloy.focentra.data.provider.** { *; }

# Preserve ViewModels & Engines
-keep class com.niloy.focentra.viewmodel.** { *; }
-keep class com.niloy.focentra.engine.** { *; }

# Preserve Kotlin Coroutines and Reflection metadata
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}

# Keep attributes for debugging and stack trace safety
-keepattributes SourceFile,LineNumberTable,*Annotation*
