// Aegis — root build file. Plugins are declared here (without applying them) so the
// module files can `alias()` them. Kotlin 2.x requires the Compose compiler to be a
// Gradle plugin — no composeOptions blocks anywhere.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
