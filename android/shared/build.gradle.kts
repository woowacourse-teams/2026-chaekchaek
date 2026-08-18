plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)

    // AGP 9 부터 KMP 라이브러리는 com.android.library 대신 이 DSL 을 쓴다.
    androidLibrary {
        namespace = "com.chaekchaek.app.shared"
        compileSdk = 36
        minSdk = 26

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
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlin.inject.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
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
