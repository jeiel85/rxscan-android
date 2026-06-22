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
        mavenCentral()
    }
}

rootProject.name = "RxScan Android"

fun includeAndroidModule(path: String, directory: String) {
    include(path)
    project(path).projectDir = file("apps/android/$directory")
}

includeAndroidModule(":app", "app")

includeAndroidModule(":core:model", "core/model")
includeAndroidModule(":core:ui", "core/ui")
includeAndroidModule(":core:logging", "core/logging")
includeAndroidModule(":core:security", "core/security")
includeAndroidModule(":core:testing", "core/testing")

includeAndroidModule(":engine:imagequality", "engine/imagequality")
includeAndroidModule(":engine:document", "engine/document")
includeAndroidModule(":engine:ocr", "engine/ocr")
includeAndroidModule(":engine:parser", "engine/parser")
includeAndroidModule(":engine:matcher", "engine/matcher")
includeAndroidModule(":engine:dur", "engine/dur")

includeAndroidModule(":data:publicdb", "data/publicdb")
includeAndroidModule(":data:privatedb", "data/privatedb")
includeAndroidModule(":data:updater", "data/updater")

includeAndroidModule(":feature:home", "feature/home")
includeAndroidModule(":feature:scan", "feature/scan")
includeAndroidModule(":feature:review", "feature/review")
includeAndroidModule(":feature:drugdetail", "feature/drugdetail")
includeAndroidModule(":feature:safety", "feature/safety")
includeAndroidModule(":feature:history", "feature/history")
includeAndroidModule(":feature:settings", "feature/settings")

project(":core").projectDir = file("apps/android/core")
project(":engine").projectDir = file("apps/android/engine")
project(":data").projectDir = file("apps/android/data")
project(":feature").projectDir = file("apps/android/feature")
