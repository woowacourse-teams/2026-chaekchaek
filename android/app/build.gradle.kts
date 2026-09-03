import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
}

val releaseSigningPropertiesFile = rootProject.file("keystore.properties")
val releaseSigningProperties =
  Properties().apply {
    if (releaseSigningPropertiesFile.exists()) {
      releaseSigningPropertiesFile.reader(Charsets.UTF_8).use { load(it) }
    }
  }
val releaseSigningKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val hasReleaseSigning = releaseSigningKeys.all { !releaseSigningProperties.getProperty(it).isNullOrBlank() }
val releaseStoreFile = releaseSigningProperties.getProperty("storeFile")?.let(::file)
val releaseSigningError =
  when {
    !hasReleaseSigning ->
      "Release signing is not configured. Copy keystore.properties.example to keystore.properties " +
        "and obtain the values from the team Google Drive and private Slack channel."
    releaseStoreFile?.isFile != true -> "Release keystore does not exist: $releaseStoreFile"
    else -> null
  }

android {
    namespace = "com.chamsae.chaekchaek"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.chamsae.chaekchaek"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags.addAll(
                    listOf(
                        "-std=c++17",
                        "-Wno-unused-command-line-argument",
                        "-Wl,--hash-style=both",
                        "-fno-exceptions",
                        "-fno-unwind-tables",
                        "-fno-asynchronous-unwind-tables",
                        "-fno-rtti",
                        "-ffast-math",
                        "-ffp-contract=fast",
                        "-fvisibility-inlines-hidden",
                        "-fvisibility=hidden",
                        "-fomit-frame-pointer",
                        "-ffunction-sections",
                        "-fdata-sections",
                        "-Wl,--gc-sections",
                        "-Wl,-Bsymbolic-functions",
                        "-nostdlib++",
                    ),
                )
            }
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        create("integration") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".integration"
            versionNameSuffix = "-integration"
            matchingFallbacks += listOf("debug")
        }
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            ndk.debugSymbolLevel = "SYMBOL_TABLE"
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/androidx-graphics-path/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      jniLibs {
        pickFirsts += "**/libandroidx.graphics.path.so"
      }
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Verifies that release signing credentials are present before a release build."
    val error = releaseSigningError
    doLast {
        check(error == null) { error ?: "Release signing verification failed." }
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn("verifyReleaseSigning")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
  implementation(project(":shared"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  releaseImplementation(platform(libs.firebase.bom))
  releaseImplementation(libs.firebase.crashlytics)
  implementation(libs.compose.runtime)
  implementation(libs.compose.foundation)
  implementation(libs.compose.ui)

  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
