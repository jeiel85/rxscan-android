import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val compileApi = 36
val minimumApi = 26
val targetApi = 36
val appNamespace = "io.github.jeiel85.rxscan"
val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

val composeProjects = setOf(
    ":app",
    ":core:ui",
    ":feature:home",
    ":feature:scan",
    ":feature:review",
    ":feature:drugdetail",
    ":feature:safety",
)

val androidLibraryProjects = subprojects
    .map { it.path }
    .filterNot { it == ":app" }
    .toSet()

subprojects {
    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
        }
    }
}

project(":app") {
    apply(plugin = "com.android.application")
    apply(plugin = "org.jetbrains.kotlin.plugin.compose")

    // Release signing is read from keystore.properties (gitignored). When absent
    // (e.g. CI, which only assembles debug), the release build is left unsigned.
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = java.util.Properties().apply {
        if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
    }

    extensions.configure<ApplicationExtension>("android") {
        namespace = appNamespace
        compileSdk = compileApi

        defaultConfig {
            applicationId = appNamespace
            minSdk = minimumApi
            targetSdk = targetApi
            versionCode = 2
            versionName = "0.1.0"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        if (keystorePropertiesFile.exists()) {
            signingConfigs {
                create("release") {
                    storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                }
            }
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
                if (keystorePropertiesFile.exists()) {
                    signingConfig = signingConfigs.getByName("release")
                }
            }
        }

        buildFeatures {
            compose = true
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    dependencies {
        add("implementation", project(":feature:home"))
        add("implementation", project(":feature:scan"))
        add("implementation", project(":feature:review"))
        add("implementation", project(":feature:safety"))
        add("implementation", project(":core:model"))
        add("implementation", project(":core:ui"))
        add("implementation", libsCatalog.findLibrary("androidx-core-ktx").get())
        add("implementation", libsCatalog.findLibrary("androidx-activity-compose").get())
        add("implementation", platform(libsCatalog.findLibrary("androidx-compose-bom").get()))
        add("implementation", libsCatalog.findLibrary("androidx-compose-ui").get())
        add("implementation", libsCatalog.findLibrary("androidx-compose-material3").get())
        add("debugImplementation", libsCatalog.findLibrary("androidx-compose-ui-tooling").get())
        add("testImplementation", libsCatalog.findLibrary("junit").get())
        add("androidTestImplementation", platform(libsCatalog.findLibrary("androidx-compose-bom").get()))
        add("androidTestImplementation", libsCatalog.findLibrary("androidx-test-ext-junit").get())
        add("androidTestImplementation", libsCatalog.findLibrary("androidx-test-rules").get())
        add("androidTestImplementation", libsCatalog.findLibrary("androidx-compose-ui-test-junit4").get())
        add("debugImplementation", libsCatalog.findLibrary("androidx-compose-ui-test-manifest").get())
    }
}

configure(subprojects.filter { it.path in androidLibraryProjects }) {
    apply(plugin = "com.android.library")
    if (path in composeProjects) {
        apply(plugin = "org.jetbrains.kotlin.plugin.compose")
    }

    extensions.configure<LibraryExtension>("android") {
        namespace = "$appNamespace.${path.removePrefix(":").replace(':', '.')}"
        compileSdk = compileApi

        defaultConfig {
            minSdk = minimumApi
        }

        buildFeatures {
            compose = path in composeProjects
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    dependencies {
        add("testImplementation", libsCatalog.findLibrary("junit").get())

        when (path) {
            ":engine:imagequality", ":engine:document" -> {
                add("implementation", project(":core:model"))
            }
            ":engine:ocr" -> {
                add("implementation", project(":core:model"))
                add("implementation", libsCatalog.findLibrary("kotlinx-coroutines-core").get())
                add("implementation", libsCatalog.findLibrary("kotlinx-coroutines-android").get())
                add("implementation", libsCatalog.findLibrary("mlkit-text-korean").get())
                add("testImplementation", libsCatalog.findLibrary("kotlinx-coroutines-test").get())
            }
            ":engine:parser" -> {
                add("implementation", project(":core:model"))
                add("implementation", project(":engine:ocr"))
            }
            ":engine:matcher" -> {
                add("implementation", project(":core:model"))
            }
            ":data:publicdb" -> {
                add("implementation", project(":core:model"))
            }
            ":data:privatedb" -> {
                add("implementation", project(":core:security"))
            }
            ":engine:dur" -> {
                add("implementation", project(":core:model"))
            }
            ":feature:drugdetail" -> {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:ui"))
            }
            ":feature:safety" -> {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:ui"))
            }
            ":feature:review" -> {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":feature:drugdetail"))
                add("implementation", libsCatalog.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            }
            ":feature:scan" -> {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":engine:imagequality"))
                add("implementation", project(":engine:document"))
                add("implementation", project(":engine:ocr"))
                add("implementation", libsCatalog.findLibrary("androidx-core-ktx").get())
                add("implementation", libsCatalog.findLibrary("androidx-activity-compose").get())
                add("implementation", libsCatalog.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libsCatalog.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libsCatalog.findLibrary("kotlinx-coroutines-android").get())
                add("implementation", libsCatalog.findLibrary("androidx-camera-core").get())
                add("implementation", libsCatalog.findLibrary("androidx-camera-camera2").get())
                add("implementation", libsCatalog.findLibrary("androidx-camera-lifecycle").get())
                add("implementation", libsCatalog.findLibrary("androidx-camera-view").get())
                add("implementation", libsCatalog.findLibrary("mlkit-text-korean").get())
            }
        }

        if (path in composeProjects) {
            add("implementation", platform(libsCatalog.findLibrary("androidx-compose-bom").get()))
            add("implementation", libsCatalog.findLibrary("androidx-compose-ui").get())
            add("implementation", libsCatalog.findLibrary("androidx-compose-material3").get())
            add("implementation", libsCatalog.findLibrary("androidx-compose-ui-tooling-preview").get())
        }
    }
}
