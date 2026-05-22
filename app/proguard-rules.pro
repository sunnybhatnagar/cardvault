# Card Vault ProGuard Rules

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- SQLCipher ---
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.**

# --- ML Kit ---
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# --- Coil ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- OkHttp (transitive from Coil/ML Kit) ---
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Google Sign-In / Drive API ---
-keep class com.google.android.gms.** { *; }
-keep class com.google.api.** { *; }
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.android.gms.**

# --- AndroidX Security Crypto ---
-keep class androidx.security.crypto.** { *; }

# --- App model classes (Room entities, can't be obfuscated) ---
-keep class com.sunnyb.cardvault.data.db.entity.** { *; }
-keep class com.sunnyb.cardvault.data.db.** { *; }

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- Gson / Google HTTP ---
-dontwarn com.google.gson.**
-dontwarn com.google.api.client.extensions.**

# --- Apache HTTP (transitive from Google API client) ---
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**

# --- General ---
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-keep public class * extends android.app.Application
-keep class * extends androidx.fragment.app.FragmentActivity

# Keep ViewModels (used by reflection in Compose)
-keep class * extends androidx.lifecycle.ViewModel { *; }
