rootProject.name = "collection"

pluginManagement {
    repositories {
        maven("https://nexus-proxy.wb.ru/repository/maven-proxy/")
        maven("https://nexus-proxy.wb.ru/repository/maven-proxy-gradle-plugins/")
        maven("https://nexus-proxy.wb.ru/repository/maven-proxy-google/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://nexus-proxy.wb.ru/repository/maven-proxy-google/")
        maven("https://nexus-proxy.wb.ru/repository/maven-proxy/")
        mavenCentral()
        google()
    }
}
