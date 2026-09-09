# ProGuard rules for a-Ha Launcher v0.2.0

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.coroutines.CoroutineContext { *; }
-dontwarn kotlin.reflect.**

# ─── Jetpack Compose ──────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { <fields>; }

# ─── AndroidX Security Crypto / Keystore ──────────────────────────────────────
-keep class androidx.security.crypto.** { *; }
-keep class android.security.keystore.** { *; }
-dontwarn androidx.security.crypto.**

# ─── Google Tink (pulled in transitively by androidx.security.crypto) ─────────
# Tink references errorprone and javax annotation stubs that are compile-only
# and are not present at runtime. Suppress all missing-class warnings for them.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn javax.annotation.**

# ─── Coroutines ───────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ─── Launcher-specific ────────────────────────────────────────────────────────
-keep class org.audhd.aha.presentation.MainActivity { *; }
-keep class org.audhd.aha.presentation.friction.MindfulDelayActivity { *; }
-keep class org.audhd.aha.presentation.desk.DeskModeDreamService { *; }

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.coroutines.CoroutineContext { *; }
-dontwarn kotlin.reflect.**

# ─── Jetpack Compose ──────────────────────────────────────────────────────────
# Compose uses reflection-free code generation; no keep rules required beyond Kotlin.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Room ─────────────────────────────────────────────────────────────────────
# Room DAO implementations are generated at compile time; entities must survive shrinking.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { <fields>; }

# ─── AndroidX Security Crypto / Keystore ──────────────────────────────────────
-keep class androidx.security.crypto.** { *; }
-keep class android.security.keystore.** { *; }
-dontwarn androidx.security.crypto.**

# ─── Coroutines ───────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ─── Serialization / Reflection ───────────────────────────────────────────────
# No Gson/Moshi — serialization is manual. Nothing extra needed.

# ─── Launcher-specific ────────────────────────────────────────────────────────
# Activities launched by explicit Intent must survive shrinking.
-keep class org.audhd.aha.presentation.MainActivity { *; }
-keep class org.audhd.aha.presentation.friction.MindfulDelayActivity { *; }
-keep class org.audhd.aha.presentation.desk.DeskModeDreamService { *; }
