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
                implementation("com.nimbusds:nimbus-jose-jwt:10.10")
                implementation("org.apache.pdfbox:pdfbox:3.0.8")
                implementation("ca.weblite:webview:1.0.10")
                implementation("org.scilab.forge:jlatexmath:1.0.7") {
                    // The core math glyphs (including \alpha and \sum) work without these
                    // optional language-font modules; do not bundle their extra font sets.
                    exclude(group = "org.scilab.forge", module = "jlatexmath-font-greek")
                    exclude(group = "org.scilab.forge", module = "jlatexmath-font-cyrillic")
                }
            }
        }
    }
}

android {
    namespace = "com.dreamsigner.wizard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dreamsigner.wizard"
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

val desktopTestTask = tasks.named<org.gradle.api.tasks.testing.Test>("desktopTest") {
    exclude("**/SourcePdfSmokeTest.class")
    exclude("**/NativeVideoSmokeTest.class")
    jvmArgs("--add-modules=jdk.httpserver")
}
tasks.register<org.gradle.api.tasks.testing.Test>("sourcePdfSmokeTest") {
    description = "Opt-in network check of an official MIT PDF with the production desktop reader."
    group = "verification"
    dependsOn("desktopTestClasses")
    testClassesDirs = desktopTestTask.get().testClassesDirs
    classpath = desktopTestTask.get().classpath
    filter.includeTestsMatching("com.dreamsigner.wizard.ui.pdf.SourcePdfSmokeTest")
}

val desktopReleaseVersion = "2.6.0"

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs("-Dwizard.version=${providers.gradleProperty("desktopPackageVersion").orElse(desktopReleaseVersion).get()}")
        nativeDistributions {
            // PDFBox 3 uses sun.misc.Unsafe.invokeCleaner via this module. The reduced
            // jlink runtime otherwise falls back to inaccessible Java 8 internals.
            // Studio IA uses Java's HTTP client; keep it in the packaged runtime too.
            modules("jdk.unsupported", "jdk.httpserver", "java.net.http")
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "DreamsignWizard"
            packageVersion = providers.gradleProperty("desktopPackageVersion").orElse(desktopReleaseVersion).get()
            appResourcesRootDir.set(layout.buildDirectory.dir("installer-resources"))
            vendor = "Dreamsigner"
            description = "Dreamsign Wizard — Mathématiques, informatique et TCF"
            windows {
                // Stable identity for subsequent installer upgrades; never regenerate this UUID.
                upgradeUuid = "a7739778-106b-4e27-8c53-857132e4d69f"
                perUserInstall = true
                menu = true
                menuGroup = "Dreamsign Wizard"
                shortcut = true
                dirChooser = true
                installationPath = "DreamsignWizardApp"
            }
        }
    }
}


val prepareInstallerResources by tasks.registering(Sync::class) {
    from(rootProject.file("docs/licences")) { include("*.txt") }
    from(rootProject.file("docs/windows-testing.md")) { rename { "LISEZ-MOI.md" } }
    into(layout.buildDirectory.dir("installer-resources/common"))
}
tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    dependsOn(prepareInstallerResources)
    inputs.files(prepareInstallerResources)
}

tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(prepareInstallerResources) }


val desktopVideoNativeTest by tasks.registering(Test::class) {
    description = "Opt-in real WebView2 video fixture playback on disposable Windows CI, without Computer Use."
    group = "verification"
    dependsOn("desktopTestClasses")
    testClassesDirs = desktopTestTask.get().testClassesDirs
    classpath = desktopTestTask.get().classpath
    filter.includeTestsMatching("com.dreamsigner.wizard.ui.video.NativeVideoSmokeTest")
    systemProperty("wizard.native.video.test", "true")
    systemProperty("wizard.native.video.local.optin", providers.gradleProperty("nativeVideoLocalOptIn").orElse("false").get())
    systemProperty("java.awt.headless", "false")
    jvmArgs("--add-modules=jdk.httpserver")
}
