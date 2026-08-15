# RefGuard Release ProGuard Rules

# Preserve RefGuard API and Domain Models for serialization/deserialization
-keep class com.refguard.app.api.** { *; }
-keep class com.refguard.app.domain.** { *; }
-keep class com.refguard.platform.models.** { *; }

# Gson serialization rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ML Kit Barcode Scanning
-keep class com.google.mlkit.vision.barcode.** { *; }
