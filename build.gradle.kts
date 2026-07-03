import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform") version "2.3.21"
    id("com.vanniktech.maven.publish") version "0.37.0"
}

group = "io.github.tret9"
version = "1.6.0-alpha01-js-0.1"

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
    val bundleOutputDir = file("${mainDir.path}/dist")
    val qjsBinary = file("${quickjsSourceDir}/qjs")

    workingDir = mainDir
    commandLine(
        "sh", "-c", """
        cp ${file("native-test/webpack-test.config.js").absolutePath} ${mainDir.absolutePath}/webpack.config.js
        mkdir -p dist
        node ${file("node_modules/webpack/bin/webpack.js").absolutePath} --config webpack.config.js
        ${qjsBinary.absolutePath} ${bundleOutputDir.absolutePath}/collection-bundle.js
    """
    )
}



kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("nonJs") {
                withJvm()
                withNative()
            }
        }
    }

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
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
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
        val nativeMain by getting
        val linuxMain by getting
        val mingwMain by getting
        val appleMain by getting
        val iosMain by getting
        val macosMain by getting
        val tvosMain by getting
        val watchosMain by getting
        val nonJsMain by getting

        commonMain.dependencies {
            api(kotlin("stdlib"))
            api("androidx.annotation:annotation:1.9.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("com.google.truth:truth:1.4.4")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
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

    }
}

mavenPublishing {
    // Publishes to the Central Portal (central.sonatype.com), the current Maven
    // Central publishing system. Credentials are read from Gradle properties
    // mavenCentralUsername / mavenCentralPassword, which are supplied via the
    // ORG_GRADLE_PROJECT_mavenCentralUsername / ORG_GRADLE_PROJECT_mavenCentralPassword
    // environment variables. The deployment is left manual so the first releases
    // can be reviewed and released from the Central Portal UI; pass
    // publishToMavenCentral(automaticRelease = true) to release automatically
    // once validation passes.
    publishToMavenCentral()

    // GPG-signs every artifact using the in-memory key supplied via the
    // ORG_GRADLE_PROJECT_signingInMemoryKey environment variable (plus
    // ...signingInMemoryKeyId / ...signingInMemoryKeyPassword when needed).
    //
    // Only enabled when a signing key is actually configured. This keeps
    // `publishToMavenLocal` (and any build without release secrets) working
    // without a GPG signatory, while the Maven Central release build — which
    // supplies ORG_GRADLE_PROJECT_signingInMemoryKey — is still fully signed as
    // Central requires.
    if (project.hasProperty("signingInMemoryKey") || project.hasProperty("signing.keyId")) {
        signAllPublications()
    }

    // groupId:artifactId:version. The per-target publications are derived
    // automatically: collection, collection-jvm, collection-js, collection-iosarm64, ...
    coordinates(group.toString(), "collection", version.toString())

    pom {
        name.set("collection")
        description.set(
            "Kotlin Multiplatform port of AndroidX Collection: memory-efficient " +
                    "collections including primitive-keyed maps and sets.",
        )
        url.set("https://github.com/tret9/androidx-collection-js")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("tret9")
                name.set("tret9")
                url.set("https://github.com/tret9")
            }
        }

        scm {
            url.set("https://github.com/tret9/androidx-collection-js")
            connection.set("scm:git:git://github.com/tret9/androidx-collection-js.git")
            developerConnection.set("scm:git:ssh://git@github.com/tret9/androidx-collection-js.git")
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
