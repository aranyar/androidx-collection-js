import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform") version "2.3.21"
    `maven-publish`
}

group = "androidx.collection"
version = "1.5.0-js-0.1"

val quickjsSourceDir = file("native-test/quickjs")

tasks.register<Exec>("buildQjs") {
    group = "build"
    description = "Build qjs executable from source"

    workingDir = quickjsSourceDir
    commandLine("sh", "-c", "cmake -S . -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build")
}

tasks.register<Exec>("jsQjsTest") {
    group = "verification"
    description = "Run JS tests with qjs"

    dependsOn("jsBrowserDevelopmentWebpack")

    val project = project
    val mainDir = file("${project.layout.buildDirectory.get().asFile.path}/compileSync/js/main/developmentExecutable")
    val webpackConfig = file("native-test/webpack-test.config.js")
    val bundleOutputDir = file("${mainDir.path}/dist")
    val qjsBinary = file("${quickjsSourceDir}/qjs")

    workingDir = mainDir
    commandLine("sh", "-c", """
        cp '${webpackConfig.path}' webpack.config.js
        mkdir -p dist
        node ${project.layout.buildDirectory.get().asFile.path}/js/node_modules/webpack/bin/webpack.js --config webpack.config.js
        ${qjsBinary.absolutePath} ${bundleOutputDir.absolutePath}/collection-bundle.js
    """)
}



kotlin {
    applyDefaultHierarchyTemplate()

    jvm()
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
        binaries.executable()
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosArm64()
    macosX64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosX64()
    linuxArm64()
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val appleMain by getting
        val nativeMain by getting
        val linuxMain by getting
        val mingwMain by getting
        val iosMain by getting
        val macosMain by getting
        val tvosMain by getting
        val watchosMain by getting

        commonMain.dependencies {
            api(kotlin("stdlib"))
            api("androidx.annotation:annotation:1.9.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("com.google.truth:truth:1.4.4")
        }

        jvmMain.dependencies {
            api("org.jspecify:jspecify:1.0.0")
        }

        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.contracts.ExperimentalContracts")
            }
        }

        targets.configureEach {
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.add("-Xexpect-actual-classes")
                    }
                }
            }
        }

        // Per 1.5.0 build: explicit native target → source set wiring
        targets.withType<KotlinNativeTarget>().configureEach {
            compilations.getByName("main").defaultSourceSet {
                val konanTargetFamily = konanTarget.family
                when (konanTargetFamily) {
                    Family.OSX, Family.IOS, Family.WATCHOS, Family.TVOS -> dependsOn(appleMain)
                    Family.LINUX -> dependsOn(linuxMain)
                    Family.MINGW -> dependsOn(mingwMain)
                    else -> {}
                }
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication>().configureEach {
            artifactId = when (name) {
                "jvm" -> "collection-jvm"
                "js" -> "collection-js"
                else -> artifactId
            }
        }
    }
}
