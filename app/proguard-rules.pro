# Myco ProGuard rules

# Keep application class
-keep class com.batallagroup.myco.MycoApplication

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Lazysodium / JNA
-keep class com.sun.jna.** { *; }
-keep class com.goterl.lazysodium.** { *; }
-dontwarn com.sun.jna.**

# ZXing
-keep class com.google.zxing.** { *; }

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class com.batallagroup.myco.domain.model.** { *; }

# Kotlin
-keep class kotlin.Metadata
-dontwarn kotlin.**

# Compose
-keep class androidx.compose.** { *; }
