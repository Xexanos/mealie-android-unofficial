# Retrofit + OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class dev.xexanos.mealie.**$$serializer { *; }
-keepclassmembers class dev.xexanos.mealie.** {
    *** Companion;
}
-keepclasseswithmembers class dev.xexanos.mealie.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Koin
-keep class org.koin.** { *; }
-keepnames class * extends org.koin.core.module.Module

# DataStore + Tink
-keep class androidx.datastore.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
