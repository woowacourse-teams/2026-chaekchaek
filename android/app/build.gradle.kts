import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
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
        versionCode = 2
        versionName = "1.0"

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
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
  implementation(libs.compose.runtime)
  implementation(libs.compose.foundation)
  implementation(libs.compose.ui)
}
