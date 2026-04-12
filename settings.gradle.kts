// D:/Ngoc/AndroidStudioProject/MyApplication/settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // Essential: Don't redirect this to JitPack
        maven { url = uri("https://jitpack.io") } // Correct way to add JitPack
    }
}

rootProject.name = "My Application"
include(":app")