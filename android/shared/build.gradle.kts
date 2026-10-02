import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

abstract class GenerateApiEnvironment : DefaultTask() {
    @get:Input abstract val environment: Property<String>
    @get:Input abstract val androidBaseUrl: Property<String>
    @get:Input abstract val iosBaseUrl: Property<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"").replace("$", "\\$") + "\""
        val output = outputDirectory.file("com/chaekchaek/app/data/remote/GeneratedApiEnvironment.kt").get().asFile
        output.parentFile.mkdirs()
        output.writeText("""
            package com.chaekchaek.app.data.remote

            internal object GeneratedApiEnvironment {
                const val name = ${literal(environment.get())}
                const val androidBaseUrl = ${literal(androidBaseUrl.get())}
                const val iosBaseUrl = ${literal(iosBaseUrl.get())}
            }
        """.trimIndent() + "\n")
    }
}

val generateApiEnvironment = tasks.register<GenerateApiEnvironment>("generateApiEnvironment") {
    environment.set(rootProject.extra["apiEnvironment"] as String)
    androidBaseUrl.set(rootProject.extra["apiAndroidBaseUrl"] as String)
    iosBaseUrl.set(rootProject.extra["apiIosBaseUrl"] as String)
    outputDirectory.set(layout.buildDirectory.dir("generated/apiEnvironment/kotlin"))
}

kotlin {
    jvmToolchain(21)

    // AGP 9 부터 KMP 라이브러리는 com.android.library 대신 이 DSL 을 쓴다.
    androidLibrary {
        namespace = "com.chaekchaek.app.shared"
        compileSdk = 36
        minSdk = 26
        androidResources.enable = true

        // 켜지 않으면 JVM 유닛 테스트 태스크가 생성되지 않아 iOS 시뮬레이터에서만 테스트가 돈다.
        withHostTestBuilder {}
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain { kotlin.srcDir(generateApiEnvironment.flatMap { it.outputDirectory }) }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.coil.compose)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            implementation(libs.coil.network.okhttp)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

// kotlin-inject 는 타겟마다 KSP 설정이 필요하다.
dependencies {
    add("kspAndroid", libs.kotlin.inject.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
}

tasks.configureEach {
    if (name == "generateAndroidHostTestLintModel" || name == "lintAnalyzeAndroidHostTest") {
        dependsOn("kspAndroidHostTest")
    }
}

tasks.configureEach {
    if (name.startsWith("linkRelease")) dependsOn(rootProject.tasks.named("verifyProductionApiEnvironment"))
}
