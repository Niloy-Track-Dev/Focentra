# ProGuard / R8 rules for Focentra

# Preserve Room entities and DAOs
-keep class com.niloy.focentra.data.local.entity.** { *; }
-keep class com.niloy.focentra.data.local.dao.** { *; }

# Preserve Content Provider Contract
-keep class com.niloy.focentra.data.provider.** { *; }

# Preserve ViewModel classes
-keep class com.niloy.focentra.viewmodel.** { *; }

# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
