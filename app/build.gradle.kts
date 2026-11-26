import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(property: String, env: String): String? =
    System.getenv(env) ?: signingProps.getProperty(property)

android {
    namespace = "org.ivanivorontsov.englishworld"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.ivanivorontsov.englishworld"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        resourceConfigurations += listOf("ru", "en")
    }
    signingConfigs {
        create("release") {
            val store = signingValue("storeFile", "ENGLISHWORLD_KEYSTORE_FILE")
            if (store != null) {
                storeFile = rootProject.file(store)
                storePassword = signingValue("storePassword", "ENGLISHWORLD_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "ENGLISHWORLD_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "ENGLISHWORLD_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.version", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
        }
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

gradle.taskGraph.whenReady {
    val needsReleaseSignature = allTasks.any {
        it.name == "assembleRelease" || it.name.startsWith("packageRelease")
    }
    if (needsReleaseSignature) {
        require(signingValue("storeFile", "ENGLISHWORLD_KEYSTORE_FILE") != null) {
            "Production signing key missing. Add keystore.properties locally or ENGLISHWORLD_* signing secrets in GitHub Actions."
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
}
