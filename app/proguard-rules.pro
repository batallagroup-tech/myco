# Myco ProGuard / R8 Rules

# Preserve stack traces and line numbers for Play Console crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Android Components & Services
-keep class com.batallagroup.myco.MycoApplication
-keep class com.batallagroup.myco.service.MycoBleService
-keep class com.batallagroup.myco.service.BootReceiver
-keep class com.batallagroup.myco.service.SmsReceiver

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# Gson & Domain Models (Preserve serialized fields, obfuscate class & methods)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.batallagroup.myco.domain.model.** { <fields>; }
-keepclassmembers enum * { *; }

# Google Tink & Security Crypto
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**

# OkHttp, Okio & ZXing Barcode Scanner
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.journeyapps.barcodescanner.**
-dontwarn kotlin.**
