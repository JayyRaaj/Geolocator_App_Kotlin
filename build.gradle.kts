// Root build file — configuration here applies to all subprojects/modules.
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    // Compose compiler is now a standalone Kotlin plugin in Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}
