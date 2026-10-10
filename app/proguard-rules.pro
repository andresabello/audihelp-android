# Moshi — keep generated adapters
-keep class com.audihelp.app.data.dto.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepnames @com.squareup.moshi.JsonClass class *

# Retrofit
-keepattributes Signature, Exceptions, *Annotation*
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
