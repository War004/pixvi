# Preserve generic type signatures and annotations for reflection (CRITICAL for Retrofit & Coroutines)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod
-keepattributes Exceptions

# Retain Retrofit interface methods, HTTP annotations, and response classes
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep custom CallAdapter and Converter Factories
-keep class * extends retrofit2.CallAdapter$Factory
-keep class * extends retrofit2.Converter$Factory

# Keep NetworkResult sealed hierarchy
-keep class com.cryptic.piyek.core.network.NetworkResult* { *; }

# Kotlinx Serialization rules
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}