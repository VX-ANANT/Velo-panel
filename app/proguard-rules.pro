# ============================================================================
# Velorix Admin Panel — R8 Full-Mode Shrinking & DEX Optimization Rules
# ============================================================================

# Preserve generic signatures, annotations, and Crashlytics line numbers
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Keep all Velorix domain models for Firebase RTDB, Firestore, and Serialization
-keep class com.example.domain.model.** { *; }
-keepclassmembers class com.example.domain.model.** { *; }

# Keep Firebase PropertyName / IgnoreExtraProperties / Exclude annotated members
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.database.Exclude <fields>;
    @com.google.firebase.database.Exclude <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}
-keep @com.google.firebase.database.IgnoreExtraProperties class * { *; }
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }

# Keep Kotlinx Serialization classes, companions, and generated serializers
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }

# Keep Credential Manager & Google Identity Services
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Retrofit & OkHttp & Moshi reflection entry points
-keepattributes Exceptions
-keepclasseswithmembers,allowshrinking interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.squareup.moshi.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn java.lang.invoke.StringConcatFactory

# Keep Liquid Glass Backdrop SDK shaders/effects if invoked reflectively
-keep class com.kashif_e.backdrop.** { *; }
-dontwarn com.kashif_e.backdrop.**

# Suppress harmless R8 warnings from Firebase / GMS / Coroutines / Kotlin reflection
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.reflect.**
-dontwarn sun.misc.Unsafe
