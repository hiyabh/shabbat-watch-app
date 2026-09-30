plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// The project lives under a Hebrew path. Gradle test workers on Windows cannot load classes
// from a non-ASCII classpath, so all build outputs go to an ASCII directory instead.
val asciiBuildRoot: File? = System.getenv("LOCALAPPDATA")?.let { File(it, "shabbat-watch-build") }
allprojects {
    if (asciiBuildRoot != null) {
        layout.buildDirectory.set(File(asciiBuildRoot, project.name))
    }
}
