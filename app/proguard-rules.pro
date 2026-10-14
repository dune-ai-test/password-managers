# Aegis ProGuard / R8 rules.

# SQLCipher ships JNI entry points called reflectively across versions.
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.**
-dontwarn net.sqlcipher.**

# Keep Compose runtime annotations (only matters if minification is ever enabled).
-dontwarn androidx.compose.**
