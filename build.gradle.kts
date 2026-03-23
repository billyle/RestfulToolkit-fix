buildscript {
    repositories {
        mavenLocal()
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
        maven { url = uri("https://dl.bintray.com/jetbrains/intellij-plugin-service") }
        maven { url = uri("https://dl.bintray.com/jetbrains/intellij-third-party-dependencies/") }
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:gradle-intellij-plugin:1.7.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.0")
    }
}

plugins {
    java
    kotlin("jvm") version "1.7.0"
    id("org.jetbrains.intellij") version "1.7.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

intellij {
    type.set("IU")
    plugins.set(
        listOf(
            "java",
            "com.intellij.java",
            "org.jetbrains.kotlin",
            "com.intellij.spring",
            "com.intellij.spring.boot",
            "properties",
            "yaml"
        )
    )
//     version = "2021.3"
//     version .set( "2021.1.3")
//     version.set("2023.3.7")  // 注释掉，使用 localPath 代替
//    version.set("2022.1.4")
    // 使用本地 IDEA 安装路径，会自动使用其 JBR
    // localPath.set("C:/Program Files/JetBrains/IntelliJ IDEA 2025.3.2")
    version.set("2023.3.7")
    pluginName.set("RestfulToolkit-fix")
    updateSinceUntilBuild.set(false)
    downloadSources.set(true)
}

// 禁用代码检测任务，避免JDK路径问题
tasks.named("instrumentCode") {
    enabled = false
}

// 禁用 searchable options 构建，避免 JVM 初始化问题
tasks.named("buildSearchableOptions") {
    enabled = false
}

group = "me.jinghong.restful.toolkit"
version = "2.1.22"

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("com.fifesoft:rsyntaxtextarea:3.1.6")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
