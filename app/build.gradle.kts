plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.downloadanything"
    // Compose 1.12(BOM 2026.09)가 compileSdk 37 이상을 요구한다. targetSdk는 36 유지.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.downloadanything"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // youtubedl-android는 ABI마다 Python·ffmpeg를 넣는다. 개인용이라 arm64만 빌드한다.
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // youtubedl-android가 실행 파일을 lib 폴더에서 꺼내 쓰므로 압축 해제 상태로 설치해야 한다.
    packaging {
        jniLibs { useLegacyPackaging = true }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
