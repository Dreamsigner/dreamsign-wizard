import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    // Desktop target (Windows)
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    
    // iOS target (iPad & iPhone)
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidInstrumentedTest.dependencies {
            implementation("androidx.compose.ui:ui-test-junit4:1.9.4")
            implementation("androidx.test.ext:junit:1.2.1")
            implementation("androidx.test:runner:1.6.2")
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                // Native Compose semantics tests, independent of desktop UI automation.
                // https://kotlinlang.org/docs/multiplatform/compose-desktop-ui-testing.html
                implementation(compose.desktop.uiTestJUnit4)
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                // Material no longer supplies these icons transitively since Compose 1.8.
                // https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.8.0
                implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.duolingo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.duolingo.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Isolated test activity; never included in the release application.
    add("debugImplementation", "androidx.compose.ui:ui-test-manifest:1.9.4")
}

// Compose scene tests render offscreen; no desktop, display server or Computer Use required.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty("java.awt.headless", "true")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "DuolingoKmpApp"
            packageVersion = "1.0.0"
        }
    }
}
